# CODE-RED Master Plan (Backend Only)

For the full detailed plan (PM + senior-dev "do the work" plan), see:
- `docs/CODE-RED/plan-v2.md`

This is the team-facing plan for CODE-RED. It is grounded in:
- the repo state machine docs in `erp-domain/docs/*STATE_MACHINES.md` (current behavior only),
- the audit findings in `docs/CODE-RED/CODE RED.txt`,
- the executable gates in `scripts/verify_local.sh` (CI must run the same steps; see `.github/workflows/ci.yml`).

Rule: preserve existing flows and external behavior wherever possible. Changes are "betterment":
idempotency, determinism, invariants, data integrity, and deploy safety.

## 0) Deploy Gates (Non-Negotiable)

Release commit must pass:
- Local: `bash scripts/verify_local.sh`
- CI: schema drift scan + time API scan + `mvn verify` (mirror `scripts/verify_local.sh`)
- Staging/prod-like DB: `scripts/db_predeploy_scans.sql` returns zero rows (NO-SHIP if any rows)

## 1) Module-by-Module Flow (Current Behavior)

Authoritative flow references (these describe *current behavior only*):
- O2C: `erp-domain/docs/ORDER_TO_CASH_STATE_MACHINES.md`
- P2P: `erp-domain/docs/PROCURE_TO_PAY_STATE_MACHINES.md`
- Payroll: `erp-domain/docs/HIRE_TO_PAY_STATE_MACHINES.md`
- Flow overview: `erp-domain/docs/MODULE_FLOW_MAP.md`

This plan does not invent new flows; it hardens the existing ones.

Notes (to match the real “sales → dealer → order” workflow)
- Dealer onboarding is an explicit step: sales can search/create dealers via `/api/v1/dealers/*` (SalesController also exposes read/search aliases under `/api/v1/sales/dealers/*`).
- Creating a dealer automatically creates and links the dealer’s AR account (`AR-<dealerCode>`) and (if needed) a portal user.

## 2) EPIC Structure (What We Fix, In What Order)

Ordering principle: remove "ledger/inventory corruption risk" first, then usability/duplication, then reporting.

### EPIC A - O2C: Dispatch Safety, Single Truth, No Side-Effects

Source findings:
- CODE RED.txt: O2C findings + duplicate dispatch entry points + mutating GET slip lookup.

Problems (current):
- `/api/v1/dispatch/order/{orderId}` is a mutating GET and selects "most recent slip" when multiple exist.
- Orchestrator can mark SHIPPED/DISPATCHED without canonical dispatch invariants (status lies).
- Dispatch confirmation has multiple entrypoints (sales + inventory alias + orchestrator alias).

Deliverables (betterment, no behavior loss):
1) Make slip lookup deterministic + non-mutating
- Change `/api/v1/dispatch/order/{orderId}` to:
  - return existing slip only (read-only)
  - if multiple slips exist -> fail closed (require slipId)
  - if no slips exist -> return 404/validation error (no lazy reserve on GET)

2) Remove "most recent slip" selection from externally reachable paths
- Keep it only for internal/admin repair tooling if absolutely needed, otherwise delete.

3) Orchestrator cannot set SHIPPED/DISPATCHED directly
- Only canonical dispatch (`SalesService.confirmDispatch`) can produce SHIPPED/DISPATCHED.
- Orchestrator may set PROCESSING/READY_TO_SHIP (as today), but shipping requires dispatch-truth invariants.

Tests to add
- CR_O2C_GetSlipByOrder_NoSideEffectsIT
- CR_O2C_GetSlipByOrder_MultiSlip_FailsClosedIT
- CR_O2C_Orchestrator_ShippedRejectedWithoutDispatchIT

Acceptance criteria
- No API can create a slip/reservation on GET.
- No code path can produce SHIPPED/DISPATCHED status without slip DISPATCHED + invoice + journals.

---

### EPIC B - Dealer AR: Settlement/Receipts/Returns Idempotency + Consistency

Source findings:
- CODE RED.txt: Dealer/AR section (idempotency key not bound to journal reference, allocation uniqueness).

Problems (current):
- Some AR settlement/receipt operations can create duplicate journals on retries.
- Allocation idempotency must be reservation-safe and payload-safe.
- Sales return behavior can drift invoice outstanding vs ledger vs portal.

Deliverables
1) Reserve-first idempotency for AR settlement/receipt
- Require/accept an explicit idempotency key at the API boundary for dealer receipts/settlements.
- Bind idempotency key -> canonical journal reference using the same pattern as manual journals:
  - reserve key in a mapping table first (atomic)
  - then post journal with deterministic reference
  - then write allocations with uniqueness enforced

2) Allocation uniqueness
- Ensure DB uniqueness exists for:
  - (company_id, idempotency_key, invoice_id) for dealer allocations
  - (company_id, idempotency_key, purchase_id) for supplier allocations
  (Already partially present; verify against actual repository usage and fix any missing uniqueness.)

3) Sales returns credit policy (DECIDED: separate dealer credit balance)
- Decision is documented in `docs/CODE-RED/decision-log.md` (2026-01-30).
- Returns create a dealer credit balance (credit note) and do **not** reduce the original invoice outstanding at return time.
- Dealer portal aging/ledger must reflect:
  - invoice outstanding (unchanged by return)
  - available credit balance (from returns)
  - deterministic credit redemption on future dispatch (FIFO recommended)

Tests to add
- CR_AR_DealerSettlement_Idempotent_ConcurrencyIT
- CR_AR_DealerReceipt_Idempotent_ConcurrencyIT
- CR_AR_SalesReturn_CreatesDealerCreditNoteIT
- CR_AR_DealerPortal_ShowsCreditBalanceIT

Acceptance criteria
- Retried settlement/receipt cannot create duplicate journals or duplicate allocations.
- Portal aging and AR control balance agree under the chosen return semantics.

---

### EPIC C - Supplier/AP: Payment + Settlement + Debit Note Safety

Source findings:
- CODE RED.txt: supplier/AP section (idempotency not bound to journal reference; debit note replay bug).

Problems (current):
- Supplier payment/settlement idempotency inconsistencies (two APIs with different safety guarantees).
- Debit note replay can reduce outstanding multiple times.
- Manual intake/GRN backdating risk vs period lock.

Deliverables
1) Unify idempotency contract across supplier payment/settlement APIs
- All supplier payment/settlement endpoints accept a required idempotency key.
- Reserve-first mapping binds idempotency key to a canonical journal reference.

2) Debit note idempotency
- Ensure postDebitNote is safe under retries:
  - same idempotency key -> same journal + same outstanding changes exactly once

3) Period lock enforcement for stock-mutating intake paths
- Ensure GRN/intake respects period locks (no backdated inventory into CLOSED periods without admin override).

Tests to add
- CR_AP_SupplierSettlement_Idempotent_ConcurrencyIT
- CR_AP_SupplierPayment_Idempotent_ConcurrencyIT
- CR_AP_DebitNote_ReplaySafeIT
- CR_P2P_PeriodLock_BlocksBackdatedIntakeIT

Acceptance criteria
- Retried supplier settlement/payment cannot create duplicates.
- Outstanding reductions are exactly-once for the same idempotency key.

---

### EPIC D - Factory/Manufacturing: Packing Safety Without Changing Flows

Source findings:
- CODE RED.txt: manufacturing audit (retry unsafe bulk pack; dual packing entrypoints; cost allocation concurrency risk).
- Current behavior reference: `erp-domain/docs/MODULE_FLOW_MAP.md` + existing production/packing endpoints.

Goal: keep BOTH endpoints (`/packing-records` and `/pack`) functional, but make them safe and non-duplicative.

Deliverables
1) Bulk-to-size packing idempotency
- Add request-level idempotency key to `BulkPackRequest`.
- Replace timestamp-based packReference with deterministic reference derived from the idempotency key.
- Ensure:
  - retry does not double-consume bulk
  - retry does not double-consume packaging RM
  - retry does not create duplicate child batches
  - retry does not post duplicate journals

2) Packaging consumption safety (no flow change)
- Keep `PackagingMaterialService` as the existing BOM/mapping mechanism, but:
  - make consumption reserve-first at the packReference boundary
  - ensure raw_material_movements dedupe on referenceId + material batch
  - ensure accounting posting uses a deterministic reference, then link movements

3) Posting boundary alignment
- Route packing-related journals through `AccountingFacade` (policy, reserved references, period locks).
- Do not change what accounts are hit; only change the posting entrypoint + idempotency enforcement.

4) Dual entrypoint safety (`skipPackagingConsumption`)
- Preserve the flag, but make it safe:
  - if skip=true, require a reference to the prior packaging consumption event (or recorded packing event)
  - disallow "skip=true" without proof of earlier consumption (fail closed)

5) Cost allocation concurrency
- Add locking/idempotency guard so two cost-allocation runs cannot partially update costs while only one variance journal posts.

Tests to add
- CR_MFG_BulkPack_Idempotent_RetryIT
- CR_MFG_BulkPack_Idempotent_ConcurrencyIT
- CR_MFG_BulkPack_SkipPackagingRequiresEvidenceIT
- CR_MFG_CostAllocation_ConcurrencySafeIT

Acceptance criteria
- Both endpoints still exist and work, but retries are safe and results are deterministic.

---

### EPIC E - Importers: Opening Stock + Catalog Import Safety

Source findings:
- CODE RED.txt: opening stock import non-idempotent; wrong permissions; raw-material batch code uniqueness risk.

Deliverables
1) Opening stock import idempotency
- Require an idempotency key for opening stock import (file hash + company + "mode" is acceptable).
- Reserve-first mapping binds the import key to a single OPEN-STOCK journal reference.

2) Permission hardening (no feature loss)
- Opening stock import must be admin-only (or gated by an explicit "migration mode" flag).

3) DB uniqueness for batch codes on imports
- Enforce uniqueness at DB level (or deterministic code generation + unique constraint) so retries don't create duplicates.

Tests to add
- CR_Import_OpeningStock_IdempotentIT
- CR_Import_OpeningStock_PermissionGuardIT
- CR_Import_RawMaterialBatchCode_UniqueIT

Acceptance criteria
- Re-running the same import does not double inventory/equity.

---

### EPIC F - Accounting Period Close + As-Of Correctness (No Silent Drift)

Source findings:
- CODE RED.txt: period close uses current balances; closing journals bypass posting path; event store unused.

Deliverables
1) Close is atomic (no race window)
- Block postings while computing close (introduce locking/closing status).

2) Close/reopen uses canonical posting path
- Closing/reopening entries must go through AccountingService/Facade so balance updates, audit, and idempotency rules are consistent.

3) As-of reconciliation strategy (choose one)
- DECIDED: snapshots at period end (inventory + GL + subledgers). Closed means closed.

4) AccountingEventStore decision
- DECIDED: do not rely on it for temporal truth; remove/demote its "audit/temporal truth" claims.

Tests to add
- CR_PeriodClose_NoRaceWindowIT
- CR_PeriodClose_AsOfReconciliationIT (depends on chosen strategy)

Acceptance criteria
- Closing a past period cannot be affected by "today's inventory".

---

### EPIC G - Flyway Convergence / Migration Cleanup (Forward-Only)

Source findings:
- CODE RED.txt: Flyway duplicates (payroll tables defined twice, redundant uniques, event store uniqueness, etc).
- Repo doc to update: `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md` (currently outdated).

Important rule: do not edit applied migrations; use forward-only convergence migrations.

Deliverables (migrations to add)
1) Payroll convergence migration (example file name)
- `V117__converge_payroll_schema.sql`
  - Ensure final column set matches JPA entities.
  - Ensure unique constraints match the intended idempotency scope.
  - Drop duplicate indexes/constraints that were introduced redundantly (only if safe).

2) Journal + accounting_events uniqueness convergence
- `V118__converge_accounting_uniques.sql`
  - Ensure only one uniqueness mechanism exists per intent (avoid duplicate unique indexes).

3) MFA/token source-of-truth convergence (decision required)
- `V119__converge_auth_tokens.sql`
  - Decide canonical storage for MFA recovery codes and token lifecycle, then migrate and constrain.

4) Sequences rationalization (non-breaking)
- Do not delete existing sequences; add a policy doc + optional forward migration that standardizes where new code should use `number_sequences`.

Flyway verification
- Update `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md` with the current migration count and the new convergence approach.
- Add explicit "convergence checks" to `scripts/schema_drift_scan.sh` output review (do not fail CI until convergence is complete).

Acceptance criteria
- Fresh DB schema == prod schema for converged areas.
- No more "duplicate definition depending on bootstrap path" risk.

## 3) SKU Policy (For Your SAFARI/WHITE Requirement)

Goal: SKUs sort and group cleanly: BRAND -> PRODUCT_TYPE -> COLOR -> SIZE.

Canonical SKU segments (existing generator already matches this):
- BRAND: `production_brands.code` (example: `SAFARI`)
- PRODUCT_TYPE: `production_products.category` (example: `EMULSION`)
- COLOR: `production_products.default_colour` (example: `WHITE`)
- SIZE: `production_products.size_label` (example: `1L`, `5L`)
- Optional uniqueness suffix: `-001`, `-002`, ... (existing behavior for auto-generated SKUs)

Example (with sequence):
- `SAFARI-EMULSION-WHITE-1L-001`
- `SAFARI-EMULSION-WHITE-5L-001`

How to create it (current catalog behavior)
- Brand:
  - create/set brand code = `SAFARI` (`production_brands.code`)
- Product type:
  - use `category` as the product type segment (examples: `EMULSION`, `PRIMER`, `PUTTY`)
- Color:
  - set `defaultColour` = `WHITE`
- Size:
  - set `sizeLabel` = `1L` / `5L` / `10L` etc
- SKU code:
  - let the system generate it (adds `-001/-002/...` for uniqueness), OR provide `customSkuCode` if you need an exact code.

Notes
- Do not rename existing SKUs in CODE-RED unless a full alias/migration strategy is implemented (too risky).
- Enforce this format for all NEW products created via catalog tooling; keep legacy SKUs as-is.

## 4) Tracking / Reporting

Single source of truth for status:
- `docs/CODE-RED/stabilization-plan.md` (milestones, done vs pending)
- `docs/CODE-RED/hydration.md` (what was actually verified and how)

Suggested working rhythm:
- 1 PR per milestone, with:
  - tests + invariant scans,
  - migration(s) if schema changes,
  - a hydration entry with command output link (or summary).
