# CODE-RED Module Audit Summary (Current System)

Last updated: 2026-02-02

Purpose: a module-by-module snapshot of **current behavior** (not new features) so new engineers/agents can quickly see:
canonical entrypoints, cross-module linkage, known duplicates, deploy blockers, and the CODE-RED stabilization worklist.

Start here:
- Entry doc: `docs/CODE-RED/START_HERE.md`
- P0 deploy blockers: `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`
- Cross-module trace evidence: `docs/cross-module-trace-map.md`
- State machines: `erp-domain/docs/*STATE_MACHINES.md`

## Global “Enterprise” Invariants (Must Hold)
- **Single write truth per business event**: aliases are allowed, but must call canonical services and must not bypass invariants.
- **Dispatch truth is financial truth**: invoice + AR/COGS journals are created from shipped quantities during dispatch confirmation.
- **Idempotency at the boundary**: retries (double-click/network/orchestrator replays) must not duplicate slips, invoices, movements, journals, or allocations.
- **Company isolation**: no endpoint may trust `X-Company-Id` in a way that can override authenticated company context.

## ADMIN / AUTH / RBAC (Company Context + Permissions)
Canonical flow:
- Auth issues JWT; company context is derived from authenticated context; RBAC is enforced at controller/service boundaries.

Where it lives today:
- Auth: `/api/v1/auth/*`
- Admin/RBAC: `/api/v1/admin/*`
- Company switching: `/api/v1/multi-company/companies/switch` (sets company context used by services)

Deploy blockers (if not fixed/gated):
- Any endpoint that can override authenticated company context via header spoofing.
- Missing `@PreAuthorize` (or equivalent) on sensitive endpoints (especially orchestrator health/commands).
- Swagger/OpenAPI and actuator exposure must be intentional in prod (public attack surface):
  - Prefer disabling `/swagger-ui/**` and `/v3/**` in prod (or securing them on a management port).
  - Limit actuator exposure to the minimal safe set (prefer `health` + `info`) and avoid leaking details.
- CORS must be safe-by-default:
  - Reject wildcard origins (`*`) when credentials are enabled; prefer explicit HTTPS origins.
  - Validate admin-updated origin settings; block invalid schemes/hosts.
- Company membership enforcement must be defense-in-depth:
  - Company update/delete and multi-company switch paths must enforce membership in the service layer too (not only controllers).

## SALES (Order-to-Cash)
Canonical flow (what you described):
- Dealer lookup → if missing, salesperson creates dealer → order created → inventory reserved / factory shortage task → pack → dispatch confirm → invoice + journals + dealer ledger updated.

Where it lives today:
- Dealer create: `POST /api/v1/dealers` → `DealerService.createDealer` (auto-creates AR account + portal user).
- Sales order create/reserve: `POST /api/v1/sales/orders` → `SalesService.createOrder` → `FinishedGoodsService.reserveForOrder` (creates packaging slips/reservations).
- Dispatch confirm (canonical): `POST /api/v1/sales/dispatch/confirm` (alias: `POST /api/v1/dispatch/confirm`) → `SalesService.confirmDispatch` (issues stock + creates invoice + posts AR/COGS via `AccountingFacade`).
- Dealer receipts/settlement: `POST /api/v1/accounting/receipts/dealer*` + `POST /api/v1/accounting/settlements/dealers` → `AccountingService` → `InvoiceSettlementPolicy`.

Known duplicates / risks:
- **Dealer onboarding logic duplication**: `SalesService.createDealer` exists but does not match `DealerService` behavior (portal user + AR account creation).
- **Invoice view overlap**: staff invoice APIs vs dealer portal invoice APIs overlap in surface area (must keep consistent and scoped).
- **Dispatch duplication**: orchestrator dispatch endpoints can set statuses without enforcing slip/invoice/journal linkage (must remain prod-gated).

Deploy blockers (if not fixed/gated):
- Any path that can mark `SHIPPED/DISPATCHED` without slip→invoice→journals→ledger linkage.
- Any nondeterministic slip selection (“most recent slip”) when multiple slips exist.
- Dealer receipts/settlements must be idempotent (no duplicate journals/allocations under retries).

## INVENTORY + CATALOG (Products/SKUs + FIFO)
Current model:
- Catalog SKU entity: `ProductionProduct` (brand/product/color/size fields + `skuCode` generation in `ProductionCatalogService.determineSku`).
- Finished goods inventory entity: `FinishedGood`/`FinishedGoodBatch` (issue/reserve/dispatch uses FIFO batches).
- Raw materials inventory entity: `RawMaterial`/`RawMaterialBatch` (FIFO batch deduction for production/packing).

Cross-module links:
- Sales reserve/dispatch allocates finished-good batches and writes inventory movements.
- Production consumes raw-material batches FIFO and creates finished-good batches for packing/dispatch.
- Purchasing GRN creates raw-material batches/movements; GL posts at supplier invoice step (not at GRN).

Known duplicates / risks:
- Catalog↔inventory mapping is mostly string-based (SKU drift risk if `ProductionProduct.skuCode` and `FinishedGood.productCode` diverge).
- Raw material/catalog sync exists in both directions (risk: “last write wins” divergence).
- Inventory adjustments previously took a pessimistic lock per line; now they lock all referenced finished goods up front (deterministic order) to reduce query count and lock overhead.
- Production catalog import now resolves brands via company-scoped lookups (no cross-tenant `findById` reads before rejection).

Deploy blockers (if not fixed/gated):
- Missing deterministic mapping/health checks leading to “SKU exists but no FG/RM exists” at posting/dispatch time.
- Opening stock import must be idempotent (retry-safe) or kept admin-only/prod-gated.
- Packaging slip status updates must enforce a real state machine (no free-form statuses that skip inventory/reservation rules).

## PURCHASING (Procure-to-Pay / AP)
Current flow:
- Purchase order → goods receipt (GRN) → supplier invoice (raw material purchase) → supplier settlement/payment.

Canonical entrypoints:
- PO/GRN: `/api/v1/purchasing/purchase-orders/*`, `/api/v1/purchasing/goods-receipts/*`
- Supplier invoice: `/api/v1/purchasing/raw-material-purchases/*`
- Settlement/payment: `/api/v1/accounting/settlements/suppliers`, `/api/v1/accounting/suppliers/payments`

Accounting linkage (important):
- GRN records inventory (raw material batches + movements) but does **not** post GL journals.
- GL posts at supplier invoice (raw material purchase) step and is linked to movements + supplier ledger.

Deploy blockers (if not fixed/gated):
- Retry/concurrency creating duplicate supplier invoices, settlements, or allocations (must be exactly-once).
- Period close must block when un-invoiced GRNs exist (no silent drift).

## HR / PAYROLL
Current flow:
- Payroll run creation → post to accounting → payment (mark paid) with traceable journals.

Canonical entrypoints:
- Payroll runs/workflow: `/api/v1/payroll/*` (legacy: `/api/v1/hr/payroll-runs`)
- Payroll payment journals (HR runs): `/api/v1/accounting/payroll/payments`
- Payroll batch payments (statutory/withholdings tooling): `/api/v1/accounting/payroll/payments/batch`

Known duplicates / risks:
- Orchestrator payroll exists but must remain prod-gated until it routes to canonical HR + Accounting flows and is idempotent.
- Payroll auto-calculation previously queried attendance per employee and saved run lines one-by-one; now it prefetches attendance in one query and bulk inserts run lines to reduce query count.

Deploy blockers (if not fixed/gated):
- Payroll payment correctness (cannot double-expense; must clear Salary Payable).
  - `recordPayrollPayment` now requires a POSTED payroll run and posts **Dr SALARY-PAYABLE / Cr CASH**, storing `payment_journal_entry_id`.
  - HR `markAsPaid` is blocked unless a payment journal exists (line statuses/advances updated only after accounting payment is recorded).

## ACCOUNTING (GL + AR/AP)
Canonical posting boundary:
- `AccountingFacade` owns module-level posting wrappers and reserved reference namespaces.
- `AccountingService.createJournalEntry` is the core posting engine (period/date validation, balance checks, ledger updates).

Current posting timing (high-level):
- Sales: AR/Revenue/Tax + COGS are posted at dispatch confirmation (dispatch truth).
- Purchasing: GRN records inventory movements; GL posts at supplier invoice step.
- Payroll: payroll run posts to accounting; payment marks run as paid and posts cash/bank journals (must be idempotent).

Known duplicates / risks:
- As-of truth for CLOSED periods is **snapshots** (period-end snapshots are the source-of-truth).
- `AccountingEventStore` is explicitly not relied upon for temporal truth (journals + snapshots are truth).
- Inventory→GL auto-posting must be enterprise-grade (durable/observable/idempotent) or be disabled.

Deploy blockers (if not fixed/gated):
- Period close/as-of drift (closed periods must not change when “today” changes).
- Period-end snapshot persistence is not optional (closed-period reports must read snapshots, not live balances/movements).
- FIFO valuation must use remaining/available quantities (depleted batches must not inflate valuation).
- Period close postings must not bypass accounting posting boundaries (no direct balance mutation outside canonical posting).
- Payroll payment idempotency/concurrency correctness (exactly-once).

## FACTORY (Production + Packing)
Current endpoints:
- Production logs: `/api/v1/factory/production/logs/*` → `ProductionLogService.createLog`
- Packing records: `/api/v1/factory/packing-records/*` → `PackingService.recordPacking` / `completePacking`
- Bulk pack: `/api/v1/factory/pack/*` → `BulkPackingService.pack`

Known duplicates / risks:
- Production log creation is not idempotent (retry can issue materials + post again).
- Packing and bulk pack are not idempotent at the API boundary (retry can double-consume + double-post).
- Bulk pack reference is deterministic (no `System.currentTimeMillis()`), but idempotency must cover partial-failure
  scenarios (e.g., packaging consumed but FG movements not written).
- Bulk pack parent-batch reads are company-scoped (no cross-tenant `findById` reads before rejection).

Deploy blockers (if not fixed/gated):
- Any manufacturing endpoint that can be retried and double-consume/double-post without a deterministic idempotency key.
- Bulk packing reference generation must remain deterministic (no `System.currentTimeMillis()`-based references).

## ORCHESTRATOR (Cross-Module Automation)
Current reality:
- Orchestrator exists to automate workflows, but it must not be a parallel “truth” path.

Deploy blockers (must be fixed or kept prod-gated):
- Any orchestrator endpoint that can mutate core business state (shipping/dispatch/payroll posting) without routing to canonical module services.
- Any orchestrator path that can override authenticated company context (tenant isolation).
- Any public orchestrator health endpoint without auth in non-dev environments.
- Orchestrator must not accept a caller-controlled `X-Company-Id` that can diverge from authenticated context (fail-closed on mismatch).
- Orchestrator must fail-closed on any attempt to set `SHIPPED/DISPATCHED` via fulfillment/status endpoints (dispatch truth must come from `POST /api/v1/sales/dispatch/confirm`).
- Orchestrator write commands must be idempotent at the boundary (`Idempotency-Key` + `orchestrator_commands` scope reservation).
- Orchestrator outbox/trace must be company-scoped (`orchestrator_outbox.company_id`; trace writes require valid company).

## FLYWAY / DB CONVERGENCE
Current reality:
- Total migrations: **119** (as of 2026-02-02).
- “Cleanup” is **forward-only** (no edits to applied migrations).

Deploy blockers:
- Schema drift between “fresh DB” and “upgraded DB” in CODE-RED critical tables (payroll, journals, event store, indexes).

Convergence plan:
- See `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md` and `erp-domain/docs/DUPLICATES_REPORT.md`.
