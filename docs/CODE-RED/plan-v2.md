# CODE-RED Program Plan (V2, Backend Only)

Last updated: 2026-02-02

This is the "team handoff" plan: what to fix, in what order, how to test it, and how we ship without breaking
accounting/inventory.

Grounded in:
- Audit: `docs/CODE-RED/CODE RED.txt`
- Current behavior docs (do not invent new flows): `erp-domain/docs/*STATE_MACHINES.md`, `erp-domain/docs/MODULE_FLOW_MAP.md`
- Release gates: `scripts/verify_local.sh`, `scripts/db_predeploy_scans.sql` (CI must mirror `scripts/verify_local.sh`; see `.github/workflows/ci.yml`)

Repo/worktree (important):
- `/home/realnigga/Desktop/CLI_BACKEND_epic04`

---

## 0) CODE-RED Rules (What We Will / Will Not Do)

Non-negotiable constraints
- Preserve external behavior/flows wherever possible. Changes must be "betterment": idempotency, determinism, locking,
  invariants, data integrity, period locks, and deploy safety.
- If a behavior is ambiguous and can corrupt stock/ledger, we prefer **fail-closed** over "best effort".
- Never edit historical Flyway migrations. Fix forward with new convergence migrations.

What counts as "no functionality loss" in CODE-RED
- Endpoints may be gated/blocked only when they are proven to corrupt data or bypass canonical invariants; we keep an
  explicit "repair/admin-only" path if users need recovery.
- We do not rename existing documents/SKUs in-place during stabilization.

How we reduce "this might break prod"
- Every PR must include: (1) tests, (2) a DB scan update (if relevant), (3) an explicit rollback note.
- Every release must pass: `bash scripts/verify_local.sh` and zero-row predeploy scans on a prod-like dataset.

---

## 0.1) CODE SMELL (Duplicate / Scattered Logic) — The Root Cause We Must Fix

Observed smell
- Cross-cutting logic is duplicated and scattered (often with inconsistent domain vocabulary), which creates:
  - double-posting risks (same business event posted via different reference namespaces)
  - inconsistent idempotency behavior across endpoints
  - uneven tenant isolation (some paths are company-scoped, others “findById then check company”)
  - brittle auditability (some writes have a trace/audit envelope, others don’t)
  - identifier confusion (e.g., `companyId` meaning `companyCode`; `traceId` vs `correlationId` vs `requestId`)

CODE-RED stance
- We do NOT “clean up for beauty”. We centralize only when it reduces corruption/duplication risk and makes deploy safer.
- The goal is a single source of truth per concern:
  - posting boundaries (AccountingFacade)
  - idempotency contract (normalize + hash + conflict semantics)
  - reference resolution (canonical vs legacy mapping)
  - tenant-scoped lookups (CompanyEntityLookup + scoped repositories)
  - audit envelope (who/when/company/ids/outcome) for every privileged write

Working docs (keep these in sync)
- Centralization plan: `docs/CODE-RED/CROSS_CUTTING_CENTRALIZATION.md`
- De-dup backlog: `docs/CODE-RED/DEDUPLICATION_BACKLOG.md`
- P0 blockers: `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`
 - Identity contract: `docs/CODE-RED/IDENTITY_AND_NAMING.md`
 - Observability identifiers contract: `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md`

---

## 0.2) What Is Already Implemented (So We Don’t Redo It)

These are CODE-RED “betterment” changes that already exist in this workspace; treat them as baseline, then build forward.

- Orchestrator strong-arm hardening (no parallel truth):
  - `/api/v1/orchestrator/dispatch*` is hard deprecated (410 + canonicalPath).
  - Orchestrator fulfillment rejects `SHIPPED/DISPATCHED` with a canonicalPath hint.
  - Orchestrator command idempotency ledger exists (`orchestrator_commands`) and write endpoints require `Idempotency-Key`.
  - Orchestrator outbox is company-scoped (`orchestrator_outbox.company_id`).
  - See: `docs/CODE-RED/ORCHESTRATOR_STRONG_ARM_SPEC.md`
- Payroll payment correctness (no double-expense):
  - Payment journal is separate from posting journal (`payroll_runs.payment_journal_entry_id`).
  - Accounting payroll payment clears `SALARY-PAYABLE` (liability clearing) and HR mark-as-paid fails closed without a payment journal.
  - See: `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md` (Accounting Correctness section)
- Performance/query hotspots (safe fixes only):
  - Inventory adjustment locks finished goods in one query; payroll auto-calc prefetches attendance + bulk inserts run lines.
  - See: `docs/CODE-RED/PERFORMANCE_QUERY_HOTSPOTS.md`

---

## 1) Definition of Deploy-Ready (What "Done" Means)

We are deploy-ready when:
- Deploy is repeatable: no ad-hoc DB edits, no hotfix SQL, no "rerun and pray".
- Retrying any write action (double-click, network retry, orchestrator retry) does **not** create duplicate:
  - invoices / slips / packing records
  - inventory movements / reservations
  - journal entries / allocations / settlements
- Accounting is centralized and enterprise-grade:
  - `AccountingFacade` is treated as the canonical posting boundary (“posting firewall”) for module-level journals.
  - Dedupe/idempotency is mismatch-safe across reference namespaces (canonical vs custom references).
- For every business event, "operational truth" and "financial truth" agree:
  - dispatched slip -> invoice exists -> sales journal exists -> COGS journal exists
  - GRN/purchase -> inventory moves -> AP postings reconcile -> period close gates are correct
  - payroll run -> posting -> payment -> HR advances/lines updated consistently
- Flyway is converged in CODE-RED areas: "fresh DB" and "upgraded DB" result in the same schema for converged tables.

### 1.1) Golden Path (Layman Flow, Current System)

This is the end-to-end sequence you described, mapped to current modules and APIs (not a new flow).

1) Dealer lookup → create if missing
   - Search: `GET /api/v1/dealers/search` (alias: `GET /api/v1/sales/dealers/search`)
   - Create (sales can do this): `POST /api/v1/dealers`
   - On create, the system auto-creates the dealer’s AR account (`AR-<dealerCode>`) and (if needed) a portal user.

2) Sales order = “proforma stage”
   - Create order: `POST /api/v1/sales/orders` (uses `dealerId`)
   - System reserves finished goods and creates packaging slip(s).
   - If shortages exist, it creates factory tasks and the order moves to `PENDING_PRODUCTION` (otherwise `RESERVED`).

3) Factory produces / packs to satisfy shortages
   - Production + packing creates finished-good batches; raw materials are consumed using FIFO where configured.

4) Dispatch confirmation finalizes financial truth
   - Dispatch confirm: `POST /api/v1/sales/dispatch/confirm`
   - Inventory is issued and linked; invoice + journals are posted and linked (traceable by order/slip/invoice references).

5) Dealer ledger / settlements
   - Dealer can see invoices/aging/ledger; accounting can record receipts/settlements idempotently.

For the code/table chain and reference scheme, see `docs/cross-module-trace-map.md` and `erp-domain/docs/ORDER_TO_CASH_STATE_MACHINES.md`.

---

## 2) Release Strategy (Sequence That Minimizes Risk)

CODE-RED ships by *reducing blast radius first*, then hardening.

### Phase RC0 (Stop The Bleeding)
Goal: block known data corruption pathways before any deeper refactors.

Deliverables (must ship first)
- Orchestrator cannot cause "status says shipped" without canonical dispatch truth.
- Slip lookup by orderId cannot be nondeterministic and cannot create hidden side-effects.
- Inventory->GL auto-posting cannot silently fail and drift (must be enterprise-grade: durable + observable + idempotent).
- Invoice creation cannot happen without dispatch confirmation (dispatch-truth invariant).

Gates
- `scripts/db_predeploy_scans.sql` is clean on staging snapshot.
- All CODE-RED tests pass (via `scripts/verify_local.sh`).

### Phase RC1 (Hardening Idempotency + Dates + Period Locks)
Goal: make retries safe and dates/periods correct across modules.

Deliverables (must ship before we claim "enterprise deploy-ready")
- Dealer AR settlements/receipts are exactly-once (idempotency + allocation uniqueness).
- Inventory adjustments + opening stock + GRN are idempotent and post with the correct business date.
- Manual journal idempotency is required and concurrency-safe (second caller gets same journal id).
- Period close is concurrency-safe and uses period-end snapshots for CLOSED periods (no "current balance" drift).

Gates
- All RC0 gates remain green.
- New "exactly-once" tests cover both retry and concurrency for AR/AP/Inventory/Payroll.
- Predeploy scans include: duplicate allocations, unlinked slips/invoices/journals, and any FAILED/PENDING outbox rows.

### Phase RC2 (Flyway Convergence + "Make Drift Impossible")
Goal: eliminate "works on my DB" by converging schema and enabling stricter drift gates.

Deliverables
- Flyway convergence migrations applied for drift-heavy tables (payroll, accounting uniqueness, events, auth decisions).
- New CODE-RED tables/constraints are present and consistent on both:
  - a clean DB migration run
  - an upgraded prod-like DB snapshot
- Schema drift scan is strict for converged areas (eventually FAIL_ON_FINDINGS=true once convergence is complete).

Gates
- Clean DB apply + `bash scripts/verify_local.sh` is green.
- Upgrade DB apply + `scripts/db_predeploy_scans.sql` is clean.

---

## 3) Epic Tracker (What We Fix, In What Order)

Priority order: corruption risk -> idempotency/concurrency -> period close correctness -> migration convergence -> reporting.

Epics (V2 naming)
- EPIC 00: Duplicate surface consolidation (route aliases to canonical services; block unsafe bypasses)
- EPIC 01: O2C dispatch-truth safety (Sales/Inventory/Invoice)
- EPIC 01B: Dealer AR safety (receipts/settlements/returns consistency)
- EPIC 02: Inventory safety (reservations, adjustments, opening stock, manual intake)
- EPIC 03: Purchasing + Supplier/AP safety (GRN, purchase invoice, settlement/payment, debit notes)
- EPIC 04: Payroll safety (single run/post/pay truth)
- EPIC 05: Accounting period close + audit/temporal truth
- EPIC 06: Manufacturing/packing safety (idempotency + timezone correctness + posting boundary)
- EPIC 07: Catalog/SKU policy + import concurrency safety
- EPIC 08: Flyway convergence migrations (forward-only)
- EPIC 09: Release runbook + monitoring + rollback (operational)
- EPIC 10: Reports/Analytics correctness (post-stabilization; time-windowed/as-of truth)

---

## 4) Module-by-Module Plan (Detailed)

This section is written so a senior dev can pick any epic and implement with confidence.

### EPIC 00 - Duplicate Surface Consolidation (Unified System, No Function Loss)

Source: "Duplicate / Confusing Endpoints" sections in `docs/CODE-RED/CODE RED.txt` + repo controller scan.

Goal
- Reduce production risk by ensuring there is ONE canonical write path per business event.
- Keep functionality: duplicate endpoints remain available short-term, but they must route to the canonical service and must
  not bypass invariants.

Rules
- Do not invent new flows. Canonical paths must match existing state machine docs.
- For each duplicated endpoint:
  - either it becomes an alias (calls canonical service only), OR
  - it becomes admin-only repair tooling, OR
  - it is gated off in prod (returns 410/403 with a canonicalPath hint).

High-risk duplicates to address first (write paths)
1) Dispatch confirmation (O2C)
   - Sales: `SalesController`
   - Inventory alias: `DispatchController`
   - Orchestrator alias: `OrchestratorController` -> `CommandDispatcher`
   Action: keep SalesService confirmDispatch as canonical; others become aliases or are prod-gated.

2) Packaging/packing (manufacturing)
   - `/api/v1/factory/packing-records` vs `/api/v1/factory/pack`
   Action: keep both endpoints but force both through ONE canonical service layer that enforces idempotency + posting
   boundary (AccountingFacade) and prevents double-pack.

3) Production logging
   - legacy `FactoryController` "production-batches"
   - canonical `ProductionLogController`
   Action: production logs are canonical; legacy endpoint becomes admin-only or is prod-gated; orchestrator must not call
   legacy batch logging.

4) Payroll endpoints
   - `HrController` vs `HrPayrollController` vs `PayrollController` (accounting-side)
   Action: one canonical payroll run/post/pay workflow; legacy endpoints become aliases or are prod-gated.

5) Manual stock/procurement bypasses
   - raw-material intake endpoints (`RawMaterialController`) vs procurement chain (PO->GRN->Invoice)
   - opening stock import endpoints
   Action: keep for migration/emergency only; prod gated; add explicit idempotency + audit if enabled.

6) Invoice issuance / fulfillment duplicates (dispatch side-effects hidden behind "invoice")
   - `InvoiceService.issueInvoiceForOrder(...)` (used by fulfillment flows)
   - `SalesFulfillmentService` options that can trigger invoice issuance
   Action: these must never create an invoice unless dispatch confirmation is executed (inventory + journals + slip links).
   They may remain as aliases, but must route to the canonical dispatch confirm path and must require slipId when ambiguous.

Secondary duplicates (read paths; lower risk but still unify over time)
- Dealer list/portal endpoints across `SalesController`, `DealerController`, `DealerPortalController`
- Invoice access endpoints across `InvoiceController` and dealer portal controllers
- Catalog import vs catalog list across `AccountingCatalogController` and `ProductionCatalogController`
- Dealer onboarding implementation is duplicated in code (`DealerService.createDealer(...)` vs legacy `SalesService.createDealer(...)`):
  - onboarding must route to `DealerService` only (portal user + AR account + dedupe rules).

Implementation tasks (repeatable template)
1) Declare canonical per business event (doc + code comment + deprecation message)
2) Route alias endpoints to canonical service methods
3) Add deprecation logging:
   - log a WARN with endpoint name + canonicalPath
4) Add prod gates:
   - feature flag OR role-based restriction for endpoints that can corrupt state
5) Add "alias parity" tests:
   - calling alias endpoint produces the same result as canonical endpoint and does not bypass invariants
6) Update evidence + tracking docs (keep the system centralized, not scattered)
   - Add/adjust entries in:
     - `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md` (if the change closes a blocker)
     - `docs/CODE-RED/DEDUPLICATION_BACKLOG.md` (if this is an alias/canonical change)
     - `docs/CODE-RED/CROSS_CUTTING_CENTRALIZATION.md` (if it removes a code smell by centralizing)
     - `docs/cross-module-trace-map.md` and `docs/idempotency-inventory.md` (if references/idempotency semantics changed)
     - `docs/CODE-RED/decision-log.md` (if behavior is clarified/locked for enterprise safety)
7) Update deploy gates when invariants change
   - If you introduced a new “must not exist” condition, add a read-only query to `scripts/db_predeploy_scans.sql` so staging/prod
     deploys fail fast instead of discovering drift later.

Orchestrator/outbox safety note (because this repo includes orchestrator code)
- In production, orchestrator command endpoints must remain feature-flagged OFF until:
  - they only call canonical services (no direct status updates)
  - they are idempotent under at-least-once outbox publishing
  - they do not lose shortage/reservation truth on retries
  - feature flags are enforced in the service layer too (defense-in-depth; controller gating is not sufficient)
  - denied attempts still create an auditable trace record (who/when/company/command) without side effects

Orchestrator security hardening (deploy blocker)
- Orchestrator must not trust raw `X-Company-Id` to set company context; derive company from authenticated context and fail closed on mismatch.
- Orchestrator health endpoints must require auth in non-dev environments.

Cross-cutting tenant isolation + attack-surface hardening (deploy blockers)
- Swagger/OpenAPI exposure must be intentional:
  - In prod, `/swagger-ui/**` and `/v3/**` must be disabled or secured on a management port with auth.
- Actuator exposure must be intentional:
  - In prod, expose only the minimal safe set (prefer `health` + `info`) and avoid leaking details.
- CORS must be safe-by-default:
  - Reject wildcard origins (`*`) when credentials are enabled; prefer explicit HTTPS origins only.
  - Validate admin-updated origins and block invalid schemes/hosts.
- Company membership enforcement must be defense-in-depth:
  - Any company update/delete and multi-company “switch” paths must enforce membership in the service layer too
    (not only in controllers).

Orchestrator hardening tasks (to enable orchestrator safely; still "betterment")
1) Fix auto-approval state locking semantics
   - Replace any REQUIRES_NEW lock "helper" that returns detached state with an in-transaction row lock that actually
     protects the rest of the workflow.
   - Ensure status updates persist and the lock scope covers reserve/approve side effects.

2) Preserve shortage/reservation truth across retries
   - Do not allow "inventoryReserved=true" to erase shortages or force awaitingProduction=false.
   - Retry must recompute (or reload) shortages and keep the order from incorrectly transitioning to READY_TO_SHIP.

3) Outbox idempotency contract
   - Producers must attach an idempotency key to outbox events (or derive one deterministically).
   - Consumers must be idempotent by event id/key (at-least-once safe), especially for any call that touches dispatch,
     packing, or posting.

4) Scheduler multi-instance safety
   - Scheduler must honor DB "active" flags and avoid system-time-only logic.
   - ShedLock must be applied consistently for any scheduled job that can trigger side effects.

5) Service-level prod gating (feature flags) + denied-attempt audit
   - Enforce `orchestrator.factory-dispatch.enabled` and `orchestrator.payroll.enabled` in the service layer
     (`CommandDispatcher` / `IntegrationCoordinator`) so internal calls cannot bypass prod gating.
   - Ensure denied attempts are still trace/audit-visible (but produce zero business side effects).

6) Fulfillment updates must respect a real sales state machine
   - `updateFulfillment` must not be a free-form status setter.
   - Route status transitions through canonical sales workflow/state-machine guards (or explicitly constrain transitions
     and enforce them centrally).

7) Mismatch-safe idempotency for orchestrator-triggered posting
   - If a replay hits an existing reference (e.g., `DISPATCH-<batchId>` journal) but payload differs materially
     (amount/accounts), fail closed with a conflict (409). Never silently “return existing” when it does not match.

8) Remove “false security” policy hooks
   - `PolicyEnforcer` must enforce real RBAC/permissions (not just null checks), OR be removed/renamed so engineers do not
     assume a security boundary exists where it doesn’t.

Tests to add/extend
- CR_Dupes_DispatchAliases_CallCanonicalPathIT
- CR_Dupes_FactoryLegacyEndpoint_ProdGatedIT
- CR_Dupes_PayrollLegacyEndpoints_ProdGatedOrAliasedIT
- CR_Dupes_ManualIntake_ProdGatedIT
- CR_Dupes_IssueInvoice_AliasToDispatchConfirmIT
- CR_ORCH_CommandEndpoints_ProdGatedIT
- CR_ORCH_AutoApproval_LockPersistsIT
- CR_ORCH_AutoApproval_RetryDoesNotLoseShortagesIT
- CR_ORCH_Outbox_IdempotentConsumersIT
- CR_ORCH_FlagOff_FailsClosed_EvenInternalInvocationIT
- CR_ORCH_DispatchJournal_ReplayMismatch_FailsClosedIT
- CR_ORCH_Fulfillment_UsesStateMachineGuardsIT
- CR_SEC_Orchestrator_CompanyHeaderMismatch_ForbiddenIT
- CR_SEC_Orchestrator_HealthEndpoints_RequireAuthIT
- CR_SEC_ActuatorAndDocs_ProdHardenedIT
- CR_SEC_CorsRejectsWildcardWithCredentialsIT
- CR_SEC_CompanyMembership_DefenseInDepthIT

Acceptance criteria
- There is exactly one write path per business event that can mutate stock and/or journals without a "repair/admin" flag.
- Duplicate endpoints are either safe aliases or gated off in production.

#### EPIC 00A - Identity + Observability Vocabulary Centralization (Enterprise Grade)

Source: CODE-RED audit findings + identifier/trace smell review.

Goal
- Eliminate vocabulary ambiguity that causes tenant isolation and audit bugs:
  - `companyId` vs `companyCode`
  - `traceId` vs `correlationId` vs `requestId`
  - `idempotencyKey` vs `referenceNumber`

Contracts (docs-first)
- Identity + naming: `docs/CODE-RED/IDENTITY_AND_NAMING.md`
- Observability identifiers: `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md`

Tasks (implementation sequence)
1) Identity: standardize company context naming
   - `companyCode` is the tenant context string; reserve `companyId` for numeric DB ids.
   - Deprecate `X-Company-Id` header name in favor of `X-Company-Code` (backward compatible parsing).
   - Deprecate JWT claim `cid` in favor of `companyCode` (backward compatible parsing).
   - Align DTOs/OpenAPI: `/auth/me` returns `companyCode` (not `companyId` for code strings).
2) Observability: standardize request/trace identifiers across modules
   - Add ingress context initializer to normalize/attach `requestId` and `traceId` (MDC + request attrs + response echo).
   - Ensure exceptions reuse existing identifiers; never generate a new traceId if one exists.
   - Add outbox identifier columns (traceId/correlationId/requestId/idempotencyKey/referenceNumber when applicable) so operators can query without parsing payload.
   - Align accounting event store correlationId with the request/trace context (no detached UUIDs).
3) Idempotency vocabulary: stop mixing `referenceNumber` and `idempotencyKey`
   - For every write endpoint, declare which one is required and how mismatches are handled (409 on conflict).
   - No “fallback between referenceNumber and idempotencyKey” unless explicitly designed and mismatch-safe.
4) Guardrails
   - Add a CI grep/lint rule preventing new usages of ambiguous identifiers in `core.security`, `auth`, `orchestrator`.

Tests (P0)
- `CR_SEC_CompanyContext_UsesCompanyCode_NotCompanyIdIT`
- `CR_SEC_CompanyHeader_Deprecation_AcceptsOldAndNewIT`
- `CR_OBS_RequestAndTraceId_EchoAndReuseIT`
- `CR_OBS_OutboxRows_AreQueryableByTraceIdIT`

Acceptance criteria
- A new engineer cannot accidentally treat companyCode as companyId by reading code or OpenAPI.
- Given a journal entry reference, operators can trace it to requestId/traceId/user without parsing JSON payloads.

---

### EPIC 01 - Sales / O2C Dispatch-Truth Safety

Source: O2C audit in `docs/CODE-RED/CODE RED.txt`, plus existing flow docs.

Non-breaking invariants (must remain true)
- Dispatch truth is canonical: shipped quantities drive invoice lines + sales journals + COGS.
- Invoices are only created as part of dispatch confirmation (no "invoice without dispatch").
- One packaging slip -> at most one invoice, one sales journal, one COGS journal (idempotent on slip boundary).
- Multiple slips per order are allowed for backorder/partial dispatch, but *must not be selected nondeterministically*.

Known issues to fix (repo-verified)
- Mutating GET + "most recent slip" selection:
  - `DispatchController.getPackagingSlipByOrder` and `FinishedGoodsService.getPackagingSlipByOrder`
- Orchestrator bypass:
  - `IntegrationCoordinator` can call `SalesService.updateStatusInternal` and mark shipped/dispatched without invariants.
- AR/Revenue double-post risk from reference namespace mismatch:
  - Dispatch confirm can create an AR/Revenue journal using a custom invoice-number reference even when a canonical
    `INV-<orderNumber>` journal already exists (but isn’t linked on the order). This must dedupe.
- COGS double-post risk from slip-vs-order reference mismatch:
  - Dispatch confirm posts COGS as `COGS-<slipNumber>`; other helper flows can post as `COGS-<orderNumber>`. Both can exist
    for the same economic event unless explicitly guarded.
- Multiple entry points into dispatch confirmation:
  - `confirmDispatch` is reachable via multiple controllers and internal helpers; partial linkage state can still trigger
    reposting unless idempotency is mismatch-safe and slip-scoped.

Tasks
1) Make slip lookup deterministic and side-effect free
   - Change `/api/v1/dispatch/order/{orderId}` to:
     - if exactly 1 slip exists -> return it
     - if >1 slips -> fail closed ("provide slipId")
     - if 0 slips -> 404 (no lazy reserve on GET)
   - Add an explicit admin-only repair endpoint if recovery is needed (optional; only if users depend on this behavior).

2) Remove "select most recent slip" from any externally reachable flow
   - If a selection is needed, require explicit `packingSlipId` and lock that row.

3) Orchestrator status gating
   - In orchestrator fulfillment update:
     - reject transitions to SHIPPED/DISPATCHED unless canonical dispatch has happened
     - keep lower statuses (PROCESSING/READY_TO_SHIP) if needed

4) Remove note-based invoice linkage from normal idempotency (no fragile "Dispatch <slipNumber>" matching)
   - After dispatch confirm, slip.invoiceId must always be set (and persisted) in the same transaction.
   - For already-dispatched slips with missing links:
     - allow an explicit admin-only repair action to reconcile and link slip -> invoice -> journals (fail closed if ambiguous).
   - Notes can remain informational, but must not be the primary linkage mechanism.

5) Make AR/Revenue posting dedupe canonical across reference namespaces (single-truth)
   - `AccountingFacade.postSalesJournal(...)` must dedupe across BOTH:
     - requested reference (e.g., invoiceNumber-based reference) AND
     - canonical reference (`INV-<orderNumber>`)
   - Enforce mismatch-safe idempotency:
     - if a reference exists but payload differs materially, fail closed with a conflict (409), not “return existing”.
   - Ensure `journal_reference_mappings` links canonical `INV-<orderNumber>` to the stored reference for postmortems.

6) Standardize COGS idempotency to the slip boundary (single-truth)
   - The canonical COGS reference is `COGS-<slipNumber>` (dispatch-truth is slip-scoped).
   - Any “order-level” helper that can create `COGS-<orderNumber>` must be removed/prod-gated or forced to reuse the
     slip-based COGS journal id.
   - Dedupe must consider both slip and order references for safety during convergence (legacy/partial-state protection).

7) Make COGS/movement linkage slip-scoped (not order-scoped)
   - Today `linkDispatchMovementsToJournal(orderId, cogsJournalId)` risks linking the wrong movements when an order has
     multiple slips or legacy movements.
   - Update dispatch movement creation to carry the slip identifier (slipId and/or slipNumber) and link COGS journals
     to movements by slip boundary only.

8) Backorder slip provenance (deterministic partial dispatch chain)
   - When creating a backorder slip, store its parent/origin slip id (or a deterministic chain key).
   - Any "find existing backorder slip" logic must be deterministic and must not silently collapse multiple chains.
   - If ambiguity is detected (multiple backorder slips and no explicit id), fail closed and require slipId.

9) Fiscal-year numbering policy (proforma + invoice)
   Goal: numbering is consistent and auditable in India.

   - India FY boundary is 1 April -> 31 March (see decision log).
   - Implement a single "FY resolver" utility/service:
     - input: company + business date
     - output: fiscal year bucket used for sequences
   - Update BOTH sequence generators to use it:
     - order/proforma number (OrderNumberService): bucket by order business date
     - invoice number (InvoiceNumberService): bucket by invoice issueDate (dispatch date)
   - Add tests around the FY boundary:
     - 2026-03-31 vs 2026-04-01 must land in different FY buckets
   - Keep this as "betterment": no change to posting logic, only numbering policy + determinism.

Tests to add/extend
- CR_O2C_GetSlipByOrder_NoSideEffectsIT
- CR_O2C_GetSlipByOrder_MultiSlip_FailsClosedIT
- CR_O2C_Orchestrator_ShippedRejectedWithoutDispatchIT
- CR_O2C_ArJournal_DedupesAcrossCanonicalAndInvoiceReferencesIT
- CR_O2C_CogsJournal_DedupesAcrossSlipAndOrderReferencesIT
- CR_O2C_AlreadyDispatched_DoesNotUseInvoiceNotesForLinkingIT
- CR_O2C_CogsMovementsLinkedToSlipOnlyIT
- CR_O2C_BackorderSlipChain_DeterministicIT
- CR_Numbering_FiscalYearBoundary_OrderAndInvoiceIT

Predeploy scans to add (if missing)
- Orders with >1 slips where slipId is missing from dispatch requests (surfaced by logs/tests rather than SQL).
- Dispatched slips where invoice/journal links are missing and notes-based reconciliation would be ambiguous.

Acceptance criteria
- There is no reachable code path that can set "SHIPPED/DISPATCHED" without:
  - slip.status=DISPATCHED
  - slip.invoice_id non-null
  - slip.journal_entry_id non-null
  - slip.cogs_journal_entry_id non-null
- There is at most one AR/Revenue journal per order-truth (`INV-<orderNumber>` canonical reference), even if dispatch uses
  an invoice-number reference internally.
- COGS journals only link to the slip's movements, never "any movement on the order".

---

### EPIC 01B - Dealer AR Safety (Receipts, Settlements, Returns)

Source: Dealer/AR audit in `docs/CODE-RED/CODE RED.txt` (AccountingService settlement/receipt flows).

Non-breaking invariants
- Retrying a dealer cash receipt or settlement cannot double-post journals or double-apply invoice settlements.
- Allocations are unique and exactly-once under concurrency (no duplicate rows for the same invoice + key).
- Dealer portal aging and AR control balance are consistent under the chosen return semantics.

Known issues to fix (repo-verified)
- Dealer settlements:
  - idempotency key is not bound to the journal reference in settleDealerInvoices (concurrent retries can create multiple
    journals even if invoice settlement is idempotent).
- Dealer receipts:
  - recordDealerReceipt/recordDealerReceiptSplit have no required idempotency key (auto-generated reference per call),
    and allocation dedupe is non-atomic with no unique constraint (concurrent calls can insert duplicates).
- Sales returns:
  - returns post AR credits and restock inventory, but do not consistently reduce invoice outstanding / payment refs,
    so invoice aging and dealer portal can drift from AR ledger.

Tasks
1) Dealer settlement idempotency becomes reserve-first
   - Bind request idempotency key -> canonical journal reference using the same reserve-first pattern as manual journals:
     - reserve key (company + key) first
     - then create or load journal entry by reserved reference
     - then write allocations with uniqueness
   - Validate key reuse:
     - same key + different dealer/payload -> fail closed

2) Dealer receipt idempotency becomes explicit (no more "auto reference = idempotency")
   - Require a stable client reference for receipts:
     - either `referenceNumber` is mandatory, OR add a dedicated `idempotencyKey` field/header
   - Reserve-first mapping binds the idempotency key to the journal reference.
   - Allocation writes must be protected by DB uniqueness (see task 3).

3) Allocation uniqueness (DB + backfill)
   - Add a forward-only migration to enforce uniqueness on allocations so concurrency cannot create duplicates:
     - dealer allocations: unique per (company_id, idempotency_key, invoice_id)
     - supplier allocations: unique per (company_id, idempotency_key, purchase_id)
     (Validate against the actual PartnerSettlementAllocation schema; this is the intended invariant.)
   - Add a deterministic dedupe/backfill step before adding the constraint (keep the "first" row, merge amounts only if
     the journal reference matches; otherwise fail and require manual repair).

4) Sales returns + credit balance (DECIDED: separate credit balance)
   Goal: sales returns create a dealer credit balance (credit note) that can be redeemed on future dispatches.

   Rules (matches your requirement)
   - Sales return does NOT reduce the original invoice outstanding at return time.
   - Sales return creates a dealer credit (credit note) that is linked to dealerId and visible in the dealer portal.
   - On new orders (proforma stage): show "available credit" and allow the user to choose to redeem it later.
   - On dispatch confirmation: apply the chosen credit amount to the newly created invoice, reducing that invoice's
     outstanding (no cash journal).

   Implementation tasks
   4.1) Persist credit notes as first-class "open items" (subledger)
   - Introduce a `dealer_credit_notes` table (or equivalent) that tracks:
     - company_id, dealer_id
     - source_journal_entry_id (the sales return journal / credit note journal)
     - source_invoice_id / source_invoice_number (for audit)
     - credit_total_amount, credit_remaining_amount
     - status (OPEN/APPLIED/VOID)
     - created_at/updated_at
   - When SalesReturnService posts the sales return journal, create (or idempotently upsert) the credit note record.

   4.2) Expose credit balance in dealer views (proforma visibility)
   - Dealer portal endpoints must return:
     - totalOutstanding (from invoices OR ledger; but must be consistent)
     - availableCreditFromReturns (sum of OPEN credit_remaining_amount)
     - netReceivable = max(0, outstanding - availableCredit)
     - remainingHeadroom = creditLimit - netReceivable (if creditLimit is configured)
   - This is how proforma can "tell" the dealer they can redeem credit.
   - Also expose the same fields in the sales order/proforma view (so the order screen can show the credit before dispatch).

   4.3) Apply credit on dispatch (finalization)
   - Extend dispatch confirm request with an OPTIONAL `applyCreditAmount` (or `useAvailableCredit` boolean).
   - During dispatch confirmation (after invoice is created):
     - allocate credit notes to the new invoice (recommended: FIFO by oldest OPEN credit notes, deterministic)
     - reduce invoice.outstanding_amount by the applied credit
     - write a durable allocation record linking credit_note -> invoice for audit/idempotency
   - IMPORTANT: applying credit must NOT create a new journal entry (AR is already reduced by the credit note).
   - Manual contract:
     - user chooses `applyCreditAmount` (must be <= available credit and <= invoice outstanding)
     - system chooses which credit notes to consume (FIFO), unless later extended to allow explicit selection

   4.3.1) Credit limit enforcement with credit applied (dispatch-truth)
   - At dispatch time, credit limit checks must consider the credit the user is applying:
     - newExposure = invoiceTotal - applyCreditAmount
     - enforce (currentNetReceivable + newExposure) <= creditLimit unless adminOverrideCreditLimit=true
   - After dispatch is posted, invoice view shows:
     - invoice outstanding
     - credit applied
     not "remaining headroom" (credit limit visibility is a pre-dispatch sales control).

   4.4) Idempotency + safety for credit application
   - Ensure credit application is exactly-once per (invoice_id, credit_note_id) with a DB unique constraint.
   - If dispatch confirmation is retried, the credit application step must detect existing allocations and no-op.
   - Disallow applying more credit than invoice outstanding (after tax/discount) and more than credit_remaining_amount.

Tests to add/extend
- CR_AR_SettleDealerInvoices_Idempotent_ConcurrentIT
- CR_AR_DealerReceipt_Idempotent_RetryIT
- CR_AR_DealerReceiptSplit_Idempotent_RetryIT
- CR_AR_Allocation_UniqueConstraint_ConcurrentIT
- CR_AR_SalesReturn_CreatesDealerCreditNoteIT
- CR_AR_DealerPortal_ShowsCreditBalanceIT
- CR_AR_DispatchApplyCredit_IdempotentIT

Predeploy scans to add/extend
- Dealer allocations duplicated by (company_id, idempotency_key, invoice_id).
- Dealer credit notes with negative remaining or remaining > total.
- Credit allocations duplicated by (company_id, invoice_id, credit_note_id).

Acceptance criteria
- Same idempotency key cannot produce more than one AR/cash journal.
- Same receipt/settlement cannot apply to the same invoice more than once.
- Returns create a credit balance that is visible to the dealer and redeemable on future dispatch.
- Portal displays both invoice outstanding and available credit (and optionally a net receivable).

---

### EPIC 02 - Inventory Safety (Reservations, Adjustments, Opening Stock, Manual Intake)

Source: Inventory + adjustment + import audits in `docs/CODE-RED/CODE RED.txt`.

Non-breaking invariants
- Inventory cannot go negative (fail before posting journals).
- Reservation is safe under retry/concurrency: does not double-reserve or create multiple slips silently.
- Backdated adjustments must hit the correct period (journal date must match adjustment date).

Known issues to fix (repo-verified)
- Reservations have no DB uniqueness guard, and slip creation selection is nondeterministic under concurrency.
- Inventory adjustments:
  - adjustmentDate stored, but journals may post on companyClock.today (wrong period)
  - no idempotency key (retries create multiple adjustments/movements/journals)
- Opening stock import:
  - non-idempotent, posts a fresh OPEN-STOCK reference each run
  - bypasses AccountingFacade and permissions may be too broad
- Manual raw-material intake paths bypass procurement and can post AP without PO/GRN (must be gated in prod).
- Inventory->GL auto-posting is not enterprise-grade today:
  - enabled-by-default and swallows failures (inventory/GL drift risk)
  - posts via AccountingService directly (bypasses AccountingFacade policy/idempotency)
  - inventory movement events can carry the wrong business date if callers default to "today"
  - event defaults can be UTC when company context is missing (midnight drift risk)

Tasks
1) Reservations: make them concurrency-safe
   - Add DB uniqueness + dedupe migration for reservations (forward-only):
     - propose unique intent: `(company_id, reference_type, reference_id, finished_good_batch_id)` (validate against usage)
   - Ensure slip creation is locked per order (row-level lock) to prevent parallel "two slips" creation.

2) Inventory adjustments: date correctness + idempotency
   - Ensure AccountingFacade inventory-adjustment posting uses `adjustmentDate` (not "today").
   - Add request idempotency key:
     - store on `inventory_adjustments` with unique constraint `(company_id, idempotency_key)`
     - reserve-first mapping -> deterministic journal reference -> movements dedupe

3) Opening stock import: make it idempotent and permission-safe
   - Add `opening_stock_imports` table (or reuse `journal_reference_mappings`) with unique import key
     - import key = file hash + company + mode
   - Opening stock posting must go through `AccountingFacade`
   - Restrict endpoint to admin-only (or require an explicit "migration mode" flag)
   - Include the created journal reference/id in API response (auditability)

4) Manual raw material intake: gate it
   - Keep endpoints for emergency/migration, but:
     - prod must run with the feature flag OFF
     - if ON, require explicit idempotency key + distinct reference type (never reuse purchase invoice reference logic)

5) Inventory->GL auto-posting (DECIDED: enterprise-grade rework)
   Goal: inventory->GL posting is durable, exactly-once, observable, and uses the same posting policy as everything else.

   5.1) Durability (outbox pattern)
   - Add a new table `inventory_gl_outbox` (forward migration) with:
     - company_id
     - event_type (MOVEMENT / REVALUATION)
     - idempotency_key (unique per company)
     - business_date (the intended journal entry date)
     - payload (minimal JSON or explicit columns for amounts/accounts)
     - status (PENDING/POSTED/FAILED), attempts, last_error, created_at, updated_at
     - posted_journal_entry_id (nullable)
   - IMPORTANT: write the outbox row in the SAME transaction as the inventory movement/valuation change that triggered it.

   5.2) Posting worker (retries + observability)
   - Add a single-instance worker (ShedLock) that:
     - polls PENDING rows
     - posts via AccountingFacade using the outbox's business_date and reserved reference
     - marks row POSTED with journal id
     - on failure: increments attempts, stores last_error, marks FAILED after threshold
   - Expose admin-only endpoints (or logs) to:
     - list FAILED/PENDING inventory->GL postings
     - retry a FAILED posting

   5.3) Idempotency + no silent drift
   - Unique constraint: (company_id, idempotency_key) on outbox table.
   - Accounting reference for auto-posting must be deterministic and derived from the same idempotency_key.
   - Remove log-and-continue behavior: failures must be visible and must block period close (via scans/gates).

   5.4) Business date correctness
   - Remove any fallback to CompanyTime.today() without company context for GL posting.
   - Publishers must pass the true business date:
     - dispatch date for sales issues
     - receiptDate for GRN receipts
     - adjustmentDate for adjustments

   5.5) Transition plan (no behavior break)
   - Keep the current listener temporarily, but change it to ONLY enqueue into `inventory_gl_outbox` (no direct posting).
   - Once worker is verified, disable direct posting paths entirely.

Tests to add/extend
- CR_INV_ReserveForOrder_ConcurrencySafeIT
- CR_INV_Adjustment_UsesAdjustmentDate_ForJournalIT
- CR_INV_Adjustment_Idempotent_ConcurrentIT
- CR_INV_OpeningStockImport_Idempotent_FileHashIT
- CR_INV_OpeningStockImport_AdminOnlyIT
- CR_INV_RawMaterialIntake_BypassBlockedInProdIT
- CR_INV_InventoryOutbox_ExactlyOnceIT
- CR_INV_InventoryOutbox_RetryOnFailureIT
- CR_INV_InventoryOutbox_UsesBusinessDateIT

Predeploy scans to add/extend
- Adjustments whose journal entry date != adjustmentDate.
- Opening stock import duplicates (multiple OPEN-STOCK journals per company) if the system currently allows it.
- Inventory->GL outbox rows in FAILED/PENDING state (must be zero for deploy/period close).

Acceptance criteria
- Retrying the same adjustment/import cannot double-change stock or ledger.
- Backdated inventory adjustments cannot sneak into an open period when the target period is locked.

---

### EPIC 03 - Purchasing + Supplier/AP Safety

Source: P2P + supplier settlement audits in `docs/CODE-RED/CODE RED.txt`.

Non-breaking invariants
- PO -> GRN -> Purchase Invoice remains the canonical procurement chain.
- Period close must not be blocked forever by "status drift" (POSTED vs PARTIAL/PAID).
- Supplier payment/settlement is exactly-once under retries and concurrency.

Known issues to fix (repo-verified)
- Period close "unposted purchase" logic treats PARTIAL/PAID as unposted.
- GRN creation:
  - accepts arbitrary receiptDate without period lock validation
  - not idempotent (no idempotency key)
- GRN movements are tagged with an ambiguous reference type (RAW_MATERIAL_PURCHASE) instead of a dedicated GOODS_RECEIPT
  reference, conflating procurement receipts with manual intake and invoice fallback references.
- Purchase invoice API not idempotent.
- Supplier settlement/payment:
  - missing idempotency key (payment)
  - idempotency short-circuit does not validate payload/partner
  - allocation table has no uniqueness (race -> duplicates)
- Debit note replay can reduce outstanding multiple times (missing reference guard).

Tasks
1) Period close purchase-status fix (no flow change)
   - Treat POSTED|PARTIAL|PAID as "posted for period close".
   - Still require journal linkage (status alone is not proof).

2) GRN period lock enforcement
   - Reject GRNs whose receiptDate falls in CLOSED/LOCKED periods (unless explicit admin override).

3) GRN + purchase invoice idempotency
   - Add idempotency keys to:
     - `GoodsReceiptRequest`
     - `RawMaterialPurchaseRequest`
   - Reserve-first: same key + same payload -> return existing record.

4) GRN reference-type correctness (non-breaking)
   - For NEW movements created from GRN:
     - use a dedicated reference type (GOODS_RECEIPT) instead of RAW_MATERIAL_PURCHASE.
   - Do not rewrite historical rows during stabilization unless a safe, deterministic migration is approved.
   - Update any downstream logic that assumes RAW_MATERIAL_PURCHASE == GRN.

5) Supplier settlement/payment idempotency + allocation uniqueness
   - Require idempotency key on payment endpoint.
   - Bind idempotency key -> canonical journal reference (reserve-first).
   - Add uniqueness on allocation tables:
     - `(company_id, idempotency_key, purchase_id)` (verify exact model)
   - Validate idempotency key reuse:
     - same key + different supplier/payload -> fail closed (prevents cross-document corruption).

6) Debit note replay guard
   - Ensure applyDebitNoteToPurchase is exactly-once for the same reference/idempotency key.

Tests to add/extend
- CR_P2P_GrnInClosedPeriodRejectedIT
- CR_P2P_GrnIdempotency_ConcurrentIT
- CR_P2P_PurchaseInvoice_Idempotency_ConcurrentIT
- CR_AP_SupplierPayment_Idempotent_ConcurrentIT
- CR_AP_SupplierSettlement_Idempotent_ConcurrentIT
- CR_AP_DebitNote_ReplaySafeIT

Predeploy scans to add/extend
- Purchases with status PARTIAL/PAID but missing journal linkage.
- Duplicate supplier allocations by (company, purchase, reference).

Acceptance criteria
- Supplier settlement/payment retries cannot double-post or double-reduce outstanding.
- Period close logic no longer false-fails due to status transitions.

---

### EPIC 04 - Payroll Safety (Run -> Post -> Pay Truth)

Source: Payroll audit in `docs/CODE-RED/CODE RED.txt`.

Non-breaking invariants
- One payroll run per (company, runType, period) unless explicitly allowed.
- Payment is not "marked paid" unless the cash/bank journal exists and HR side-effects (advances/lines) are consistent.

Known issues to fix (repo-verified)
- Multiple payroll creation paths and missing required fields for legacy runs (periodStart/periodEnd/runType/runNumber).
- Payment flow split between HR and accounting can drift.
- `recordPayrollPayment` idempotency guard was broken (string vs enum comparison); fixed via enum checks + payroll run row lock.
  Remaining: canonicalize “post vs pay” semantics so we don’t double-expense payroll under mixed flows.

Tasks
1) Canonicalize payment workflow
   - Single method that:
     - posts the payment journal
     - updates payroll lines/payment refs
     - updates advances
     in a single transaction (or with a strictly ordered, idempotent two-step with reserved references).

2) Fix idempotency guards
   - Ensure "already paid" detection works reliably and is concurrency-safe.

3) Legacy run hardening (schema + data)
   - Add a convergence/backfill migration:
     - backfill runType/runNumber/periodStart/periodEnd for legacy rows
     - enforce non-null for new rows (after backfill)

4) Period close payroll gate
   - Ensure periods cannot close while payroll runs in that period are unpaid/unposted (including legacy runs).

Tests to add/extend
- CR_Payroll_RunCreation_Idempotent_ConcurrentIT
- CR_Payroll_PostOnce_ConcurrentIT
- CR_Payroll_Payment_ConsistencyIT
- CR_Payroll_PeriodClose_Blocks_LegacyRunIT

Acceptance criteria
- Payment retries cannot double-post cash/bank journals and cannot drift HR vs accounting.

---

### EPIC 05 - Accounting Period Close + Audit/Temporal Truth

Source: Accounting audit in `docs/CODE-RED/CODE RED.txt`.

Non-breaking invariants
- Closing a past period must not depend on today's balances/stock.
- Close/reopen must be atomic and auditable (no silent balance edits).

Known issues to fix (repo-verified)
- Period close checklist uses "current stock/current GL" instead of as-of.
- Close/reopen bypasses normal posting path and mutates balances directly (race + stale cache risk).
- AccountingEventStore exists but has no call sites (temporal claims are unreliable).

Tasks
1) Close/reopen must use canonical posting mechanics
   - Route closing/reversal journal creation through AccountingService/AccountingFacade so:
     - validations apply (period lock, date validation)
     - balances update atomically and caches invalidate
     - audit trail is consistent

2) Eliminate race window
   - Introduce a "CLOSING" state or DB lock so no journals can be posted while close is computed.

3) As-of strategy (DECIDED: snapshots)
   - Persist **period-end snapshots** at close (company + period):
     - trial balance (GL balances)
     - inventory valuation
     - subledger totals (AR/AP)
   - Reports/reconciliation must read snapshots for CLOSED periods.
   - Closed periods must remain immutable by construction (late postings cannot rewrite the past).

4) AccountingEventStore is explicitly not relied upon for temporal truth
   - Do not build critical reports on `accounting_events`.
   - Remove “temporal truth / replay” claims from docs and any reporting code paths.
   - If `accounting_events` remains, treat it as an internal audit/diagnostic log only (no correctness guarantees).

5) Manual journal idempotency and safety (CODE-RED accounting hardening)
   - Require a client idempotency key for manual journals (no "optional idempotency").
   - Concurrency behavior must be stable:
     - same idempotency key called twice -> both callers get the SAME journal entry id (no INTERNAL_CONCURRENCY_FAILURE).
   - Enforce key validation in the service layer (not only controller):
     - reserved namespace check
     - max length check (matches DB limit)
   - This is "betterment only": it does not change journal math, only idempotency and safety.

Tests to add/extend
- CR_PeriodClose_NoRaceWindowIT
- CR_PeriodClose_AsOfInventoryAndGlIT
- CR_PeriodClose_CloseReopen_UsesPostingPathIT
- CR_ManualJournal_Idempotent_ReturnsSameIdUnderConcurrencyIT
- CR_ManualJournal_IdempotencyKeyRequiredIT

Acceptance criteria
- Close/reopen is idempotent, auditable, and consistent under concurrency.
- Manual journals cannot be double-posted by retries, and internal calls cannot bypass reserved-namespace safety.

---

### EPIC 06 - Manufacturing/Packing Safety (No Flow Change, Just Safety)

Source: Manufacturing audit in `docs/CODE-RED/CODE RED.txt` + additional repo scan.

Non-breaking invariants
- Existing endpoints remain functional:
  - `/api/v1/factory/packing-records`
  - `/api/v1/factory/pack`
- Retrying packing/bulk-pack cannot double-consume or double-post.
- Business dates use company timezone consistently (no UTC midnight drift).

Known issues to fix (repo-verified)
- Bulk packing reference must be deterministic and retry-safe (no `System.currentTimeMillis()`).
- Packing and bulk packing bypass AccountingFacade (direct AccountingService posting).
- Production producedAt parsing uses UTC for local strings (timezone bug):
  - `ProductionLogService.resolveProducedAt(...)` converts local date/time to UTC without company zone.
- recordPacking may mark output batches bulk=true (double-packing risk).
- Cost allocation uses mixedQuantity instead of totalPackedQuantity (variance skew under wastage).
- Legacy factory batch logging and manual FG batch registration bypass WIP controls (must be gated).
- Orchestrator can call legacy factory batch logging, creating phantom production records outside the production-log/WIP
  flow (must be blocked or routed).

Tasks
1) Fix producedAt parsing to use company timezone (bug fix, not flow change)
   - For local date/time strings (dd-MM-yyyy HH:mm[:ss], yyyy-MM-dd), interpret in company timezone.
   - Add tests around midnight for non-UTC companies.

2) Bulk pack idempotency
   - BulkPackRequest supports an optional `idempotencyKey` (recommended); derive deterministic `packReference` from input.
   - Idempotent replays must return the same result (no double-consume / no double-post).
   - Fail-closed or self-heal on partial-failure scenarios (e.g., packaging consumed but FG movements not written).

3) Posting boundary alignment (packing)
   - Route packing/bulk-pack journals via AccountingFacade:
     - do not change account selection logic in stabilization
     - only change *how* we post (idempotency + date validation + period lock).

4) Double-packing protection
   - Ensure already-packed size batches cannot be treated as bulk again.
   - If changing flags is risky, add a "cannot pack if output batches already exist for this reference" idempotency guard.

5) Cost allocation correctness guard
   - Use totalPackedQuantity for variance allocation when wastage exists.
   - Add lock so concurrent cost allocations cannot race.

6) Gate legacy bypass endpoints in prod
   - `FactoryController` legacy "production-batches" and manual FG batch injection must be admin-only or disabled in prod.
   - Ensure orchestrator does not call legacy batch logging/service methods in production paths (block or route to
     ProductionLogService).

Tests to add/extend
- CR_MFG_ProducedAt_UsesCompanyTimezoneIT
- CR_MFG_BulkPack_Idempotent_RetryIT
- CR_MFG_BulkPack_Idempotent_ConcurrentIT
- CR_MFG_Packing_PostsViaAccountingFacadeIT
- CR_MFG_CostAllocation_UsesPackedQtyIT

Acceptance criteria
- Retrying a bulk-pack request cannot change stock/journals after the first success.
- ProducedAt journals land in the correct company business date/period.

---

### EPIC 07 - Catalog/SKU Policy + Import Concurrency Safety

User requirement
- "SAFARI" is BRAND, "WHITE" is COLOR under that brand, and SKU ordering must be:
  - BRAND -> PRODUCT_TYPE -> COLOR -> SIZE -> sequence
  - Example: `SAFARI-EMULSION-WHITE-1L-001`

Current reality
- `ProductionCatalogService.determineSku(...)` already builds SKU segments in the desired order.
- Bulk variant creation (`createVariants`) uses a different SKU scheme (prefix + baseName + color + size) and may not match.

Tasks
1) Enforce canonical SKU format for NEW products
   - Keep existing SKUs unchanged.
   - For `createProduct` flow: already correct; add tests to lock it in.

2) Bulk variant SKU behavior (safe transition)
   - Add an opt-in switch (request flag) to generate SKUs using canonical `determineSku(...)`.
   - Default to current behavior to avoid breaking existing users if they rely on the legacy format.
   - Document "use canonical SKU flag" for all new Safari/White SKUs.

3) Size ranking (so variants sort correctly for Safari and other brands)
   - Define a deterministic size ordering rule for NEW products:
     - parse `sizeLabel` into a numeric base unit and store a sortable integer rank:
       - volume: ML/L (examples: 200ML, 500ML, 1L, 2.5L, 4L, 10L) -> store `size_rank_ml` in milliliters
       - weight: G/KG (examples: 1KG, 5KG, 25KG, 500G) -> store `size_rank_g` in grams
       - pick one column name in implementation (e.g., `size_rank_base`) but define the unit in docs.
     - fallback behavior: if sizeLabel cannot be parsed, set rank to NULL and sort these last (do not fail imports).
   - Update any "list variants" API ordering (where applicable) to sort by:
     - brand, category, colour, size_rank_ml, sku_code
     (This is about sorting/presentation; it does not change SKU codes.)

   Notes (raw materials)
   - Raw materials already have a type split in the model (`RawMaterial.materialType`):
     - PRODUCTION (normal RMs consumed in production/WIP)
     - PACKAGING (packaging RMs consumed during packing/conversion)
   - Catalog/import tooling must set `materialType` correctly so packing vs production consumption stays clean and auditable.

4) Import concurrency hardening
   - Brand code and SKU code generation must be reserve-first under parallel imports:
     - add DB unique constraints (already exist for many) and handle collisions deterministically.
   - Ensure auto-created RawMaterial records have required defaults so later postings don't fail.

Tests to add/extend
- CR_SKU_CreateProduct_UsesBrandCategoryColorSizeOrderIT
- CR_SKU_BulkVariants_CanonicalSkuFlagIT
- CR_Import_Catalog_ConcurrentSkuCreationSafeIT
- CR_SKU_SizeRank_ParsesAndSortsVariantsIT

Acceptance criteria
- New SKUs created for Safari/White follow the correct segment ordering and sort correctly.

---

### EPIC 08 - Flyway Convergence / Migration Cleanup (Forward-Only)

Goal: eliminate schema drift so fresh installs match upgraded environments in CODE-RED areas.

Hard rules
- Do not edit applied migrations.
- Add new migrations to converge schema + constraints to the current entity intent.
- Prefer deterministic data fixes (dedupe/backfill) before adding constraints.

Convergence targets (from audit)
1) Payroll schema drift
   - `V7__hr_tables.sql` creates payroll_runs minimal columns; `V78__payroll_enhancement.sql` conditionally recreates/extends.
   - Add: `V128__converge_payroll_schema.sql`
     - Ensure column set, types, nullability match JPA entities.
     - Backfill required fields for legacy rows (runType/runNumber/periodStart/periodEnd).
     - Ensure idempotency constraints match canonical scope.

2) Accounting uniqueness drift
   - Journal reference uniqueness defined multiple times (`V5`, `V66`) and packaging slip journal uniqueness in `V116`.
   - Add: `V129__converge_journal_uniqueness.sql`
     - Keep exactly one unique mechanism per intent; drop redundant constraints/indexes if safe.

3) Accounting event store uniqueness drift
   - `V70` vs `V115` uniqueness overlap.
   - Add: `V130__converge_accounting_events.sql`

3b) Index consolidation (performance drift)
   - Duplicate indexes created with different names (finished goods + batches).
   - Add: `V131__index_consolidation.sql`

4) MFA recovery codes drift (decision required)
   - Migrations currently leave two sources of truth (column + table) due to a commented-out drop.
   - Add: `V132__converge_mfa_recovery_codes.sql`
     - Decide canonical storage (recommended: table).
     - Backfill table from column if needed.
     - Keep backward compatibility for one release; only then drop/ignore the old column.

5) Token lifecycle drift (decision required)
   - Token lifecycle is split across parallel tables (refresh_tokens vs blacklist/revocations).
   - Add: `V133__converge_auth_tokens.sql`
     - Decide canonical tables and enforce constraints so there is one source of truth.

6) Sequence mechanisms drift (non-breaking)
   - order_sequences, invoice_sequences, and number_sequences exist in parallel.
   - Do not drop sequences in stabilization.
   - Add a policy doc + optional convergence migration to standardize new code on `number_sequences`.

7) New CODE-RED tables/constraints (supporting stabilization epics)
   - Add forward-only migrations for new "safety tables" introduced by this plan:
     - `inventory_gl_outbox` (EPIC 02)
     - `dealer_credit_notes` + `dealer_credit_allocations` (EPIC 01B)
   - Add forward-only uniqueness constraints needed for exactly-once behavior:
     - dealer allocations uniqueness
     - supplier allocations uniqueness
     - (invoice_id, credit_note_id) uniqueness for credit allocations
   - Add any needed backfills/dedupes before constraints so production upgrades do not fail.

Flyway validation plan (must be executed for each convergence PR)
1) Clean DB apply
   - start with an empty Postgres DB
   - boot app / run migrations
   - run `bash scripts/verify_local.sh`
2) Upgrade DB apply
   - take a snapshot from staging/prod-like
   - apply migrations
   - run `scripts/db_predeploy_scans.sql` (must be clean)

Acceptance criteria
- Converged tables have identical schema on clean vs upgraded DB.

---

### EPIC 09 - Release Runbook / Monitoring / Rollback

Deliverables
1) Go/No-Go checklist (already partially in `docs/CODE-RED/release-plan.md`)
   - Add explicit "feature flags to check" list for prod.
2) Monitoring
   - Alert on:
     - duplicate journal reference attempts
     - slip dispatched missing links (scan query)
     - failed inventory->GL postings (if listener remains enabled anywhere)
3) Rollback
   - "Rollback app first" rule
   - Post-rollback scans to assess any drift

Acceptance criteria
- Every deployment has a rehearsed rollback path and post-deploy verification.

---

### EPIC 10 - Reports/Analytics Correctness (Post-Stabilization)

Source: Reports/Analytics audit in `docs/CODE-RED/CODE RED.txt`.

Scope note
- This epic is intentionally after stabilization because it can change reported numbers and requires stakeholder sign-off.
- Do not "fix reports" by changing posting logic; reports must follow operational + accounting truth.

Known issues to fix (repo-verified)
- Cash-flow report is not actually cash-flow (it sums all journal line deltas, which nets ~0 on balanced books).
- Balance sheet / P&L / trial balance use current account balances with no as-of controls, so historic reporting drifts.
- Enterprise/portal dashboards use current cash/inventory valuation even when a historical window is requested.

Tasks (high level)
1) Define report truth model (decision)
   - Cash-flow: direct method (cash/bank movements) vs indirect (P&L adjustments).
   - As-of reporting: use period snapshots (recommended) vs date-driven recompute.

2) Implement as-of reporting consistent with EPIC 05
   - If EPIC 05 chooses snapshots: reports read snapshot tables for closed periods.
   - If EPIC 05 chooses date-driven: reports query journal lines by entryDate and inventory movements by movementDate.

3) Fix mislabeled portal analytics
   - Ensure date windows are respected and metric labels match the computed window.

Tests to add
- CR_Reports_AsOfBalancesStable_AfterLatePostingIT
- CR_Reports_CashFlow_NotZeroByConstructionIT

Acceptance criteria
- For a closed period, re-running reports after later postings does not change the closed-period output.

---

## 5) Testing Plan (Team Checklist)

Automated gates (required)
- `bash scripts/verify_local.sh`
- CI green on same commit
- Predeploy scans: `scripts/db_predeploy_scans.sql` on staging/prod snapshot

What to add in CODE-RED tests
- Idempotency tests must include both:
  - retry (same request twice)
  - concurrency (N threads calling same action)
- Every "exactly-once" business event must have a unique reference boundary:
  - slip number / pack reference / payroll run idempotency key / supplier settlement key

Manual QA (minimal but required for release candidates)
- Create order -> partial dispatch -> retry dispatch confirm -> verify one invoice per slip and no double journals.
- Create GRN -> create invoice -> settle supplier -> retry settlement -> verify outstanding reduced once.
- Run payroll -> post -> pay -> retry pay -> verify cash journal once + advances consistent.
- Bulk pack -> retry -> verify no double-consumption (if manufacturing enabled).

---

## 6) Notes / Open Decisions (Must Be Made Explicit)

1) InventoryAccountingEventListener
- Current behavior is enabled-by-default and swallows failures (drift risk).
- DECIDED: rework to enterprise-grade.
- "Enterprise-grade" requirements (non-negotiable):
  - durable processing: inventory->GL events must be persisted and retried (no fire-and-forget)
  - no silent swallow: failures must surface via status + scans + alerts
  - idempotency: one event -> at most one journal (reference reserved-first)
  - posting boundary: use AccountingFacade (period locks, date validation, cache invalidation)
  - correct business date: event must carry the operation's business date (not "today" defaults)

2) Dealer AR semantics (returns)
- DECIDED: separate credit balance semantics.
- Dealer credit must be:
  - linked to dealerId
  - visible in portal (available credit)
  - redeemable on future dispatch (reduces the new invoice outstanding, no cash journal)

3) Dealer receipts idempotency contract
- DECIDED: add a dedicated idempotency key for receipts/settlements and keep referenceNumber as display/audit only.
- Backward compatibility: if older clients send only referenceNumber, treat it as idempotencyKey but validate payload matches.

4) As-of strategy for period close + reports
- DECIDED: **snapshots** at period close.
  - Closing persists immutable period-end snapshots; CLOSED period reports read snapshots.

5) Invoice numbering "year"
- DECIDED: India financial year runs 1 April -> 31 March (FY boundary), so invoice numbering must be fiscal-year based.
- DECIDED: fiscal-year bucket is derived from invoice issueDate (dispatch date), never server "today".
- DECIDED: print FY label as a two-year label `YYYY-YY` (example: `2025-26`).

6) Dealer credit redemption strategy (open details)
- Allocation strategy:
  - recommended default: FIFO by oldest OPEN credit notes (deterministic)
  - alternative: user selects specific credit notes to apply (more UI + more edge cases)
- Credit limit checks:
  - DECIDED: show "available credit" + "remaining headroom" to sales at proforma stage (credit limit visibility).
  - DECIDED: dispatch credit-limit enforcement considers applied credit:
    - newExposure = invoiceTotal - applyCreditAmount
    - enforce (currentNetReceivable + newExposure) <= creditLimit unless adminOverrideCreditLimit=true
  - After dispatch is posted, we show invoice outstanding + credit usage (not "credit limit headroom") on that invoice.

---

## 7) CODE RED.txt Coverage Check (Quick Audit)

This is the "did we miss anything obvious?" checklist against the critical/high findings in `docs/CODE-RED/CODE RED.txt`.

Covered in this plan (mapped)
- Orchestrator can set SHIPPED/DISPATCHED without canonical dispatch -> EPIC 01 (task 3) + RC0.
- Mutating/nondeterministic slip lookup by orderId -> EPIC 01 (task 1/2) + RC0.
- reserveForOrder concurrency + duplicate reservations -> EPIC 02 (task 1).
- Inventory adjustments wrong journal date + no idempotency -> EPIC 02 (task 2).
- Opening stock import non-idempotent + permission too broad -> EPIC 02 (task 3).
- Manual raw-material intake bypassing procurement/period gates -> EPIC 02 (task 4).
- GRN receiptDate period lock missing -> EPIC 03 (task 2).
- GRN + purchase invoice not idempotent -> EPIC 03 (task 3).
- Supplier settlement/payment idempotency + allocation uniqueness -> EPIC 03 (task 5).
- Debit note replay reduces outstanding multiple times -> EPIC 03 (task 6).
- Dealer settlement/receipt idempotency + allocation uniqueness -> EPIC 01B (tasks 1-3).
- Sales returns cause AR/portal drift -> EPIC 01B (task 4) (credit balance + redemption).
- Payroll payment/concurrency drift across HR vs accounting -> EPIC 04.
- Period close race window + bypass posting path + as-of drift -> EPIC 05.
- producedAt timezone parsing bug -> EPIC 06 (task 1).
- Bulk pack non-idempotent + packing bypass posting boundary -> EPIC 06 (tasks 2-3).
- Flyway drift (payroll/journal/events/MFA/tokens/sequences) -> EPIC 08.
- Reports/analytics time-window/as-of drift -> EPIC 10.

Explicitly flagged as decision (cannot safely "just change it" without product sign-off)
- Reports: cash-flow model (direct vs indirect) (EPIC 10).

Not CODE-RED blocking (can be deferred, but should be tracked)
- RBAC role visibility/tenant scoping quirks; MFA lockout rules; portal metric labeling beyond correctness.
