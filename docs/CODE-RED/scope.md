# CODE-RED Scope (Backend)

This is the scope contract for CODE-RED so we stop scope-creep and ship safely.

Primary references:
- `docs/CODE-RED/plan-v2.md` (full execution plan)
- `docs/CODE-RED/release-plan.md` (release gates + rollout/rollback)
- `docs/CODE-RED/CODE RED.txt` (audit findings)

## Scope A - Stabilize Current Behavior (CODE-RED V2)

Rule: "betterment only" (no new business flows; no functionality loss). We improve safety:
idempotency, determinism, locking, invariants, period locks, and migrations.

In scope
1) Sales -> Inventory -> Dispatch -> Invoice -> Accounting
   - Dispatch-truth only (shipped quantities drive invoice + sales journal + COGS).
   - Partial dispatch/backorder remains supported.
   - No nondeterministic slip selection; fail closed when ambiguous.

2) Inventory mutation paths (stability hardening)
   - Reservation concurrency safety (no double-slip / double-reserve).
   - Inventory adjustments:
     - journal date must use adjustmentDate (period-correct)
     - idempotency keys for retries
   - Opening stock import:
     - idempotent per file/company
     - AccountingFacade-only posting
     - admin-only or migration-mode gated
   - Manual raw-material intake:
     - keep only as a gated path (disabled in prod by default)

3) Purchasing / Supplier/AP
   - GRN receiptDate respects period locks (fail closed in closed/locked periods).
   - GRN + purchase invoice APIs become idempotent (retry-safe).
   - Supplier payment/settlement/debit-note flows become exactly-once (idempotency + allocation uniqueness).
   - Period close purchase checks must not false-fail due to PARTIAL/PAID statuses.

4) HR / Payroll
   - One run per scope (company + runType + period).
   - Post + pay workflows are consistent: cash journal and HR side-effects agree.
   - Legacy rows are backfilled/converged so period close gates are accurate.

5) Accounting period close + audit/temporal truth
   - Close/reopen uses canonical posting path and is concurrency-safe.
   - As-of strategy is period-end snapshots and is implemented for close (closed means closed).
   - AccountingEventStore is explicitly not relied upon for temporal truth (journals + snapshots are truth).

6) Manufacturing/packing
   - Keep existing endpoints working; make them idempotent and deterministic.
   - Fix company-timezone producedAt parsing bugs.
   - Route packing journals via AccountingFacade (posting boundary only; do not change account selection rules).
   - Gate legacy bypass endpoints (factory legacy batch logging, manual FG batch injection) in prod.

7) SKU policy (Safari requirement)
   - Enforce canonical SKU segment order for NEW SKUs:
     BRAND -> PRODUCT_TYPE -> COLOR -> SIZE -> sequence
   - Add a deterministic size ranking for NEW products (for sorting variants), without changing existing SKU codes.
   - Do not rename existing SKUs during stabilization.

8) Flyway migration convergence (forward-only)
   - Convergence migrations for drift-heavy tables (payroll, accounting uniqueness, auth tokens as needed).
   - No edits to existing migrations.

Out of scope (for stabilization)
- New UI features / portal work
- New accounting features beyond correctness (e.g., GRNI accrual), unless required to prevent ledger corruption
- Renaming/rewriting existing SKUs or historical document numbers
- Packaging hard cutover to a new schema (variants/BOM) unless explicitly approved as Plan B

## Scope B - Full Canonicalization (Plan B, After Stabilization)

This is intentionally separated because it is a higher-risk behavior change.

Included (only if explicitly approved)
- New packaging schema (per-product variants + BOM) and hard cutover of packing logic.
- Deprecation/removal of legacy packaging mapping endpoints and fallback behaviors.

Reference plan:
- `docs/CODE-RED/full-v1-cutover-plan.md`
