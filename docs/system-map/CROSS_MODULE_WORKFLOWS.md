# Cross-Module Workflows

Terminology note:
- Use `partner` as the canonical cross-module entity term.
- Use `dealer` and `supplier` only where the role-specific workflow matters.

## M18-S3A Canonical Write-Path Registry (Decision Guard)
Decision semantics:
- `keep`: compatibility path stays available but must preserve canonical invariants.
- `merge`: duplicate path remains only as a thin route into canonical service ownership.
- `deprecate`: duplicate path is retired or hard-blocked with canonical redirect guidance.

Source alignment:
- `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md` (`M18-S3` objective and decision classes)
- `docs/CODE-RED/DEDUPLICATION_BACKLOG.md` (canonical path and duplicate-path evidence)

| Workflow | Business event | Canonical write path (owner) | Duplicate or alternate path | Decision | Guard requirement |
|---|---|---|---|---|---|
| O2C | Dispatch confirmation | `POST /api/v1/sales/dispatch/confirm` (`sales`, `SalesService.confirmDispatch`) | `POST /api/v1/dispatch/confirm` | keep | Alias must call canonical dispatch path and keep slip -> invoice -> AR/COGS linkage idempotent. |
| O2C | Dispatch command surface | `POST /api/v1/sales/dispatch/confirm` (`sales`) | `POST /api/v1/orchestrator/dispatch`, `POST /api/v1/orchestrator/dispatch/{orderId}` | deprecate | Orchestrator dispatch commands stay hard-deprecated (410 + canonical path hint), with no direct dispatch truth writes. |
| O2C | Fulfillment status write | `POST /api/v1/sales/dispatch/confirm` (`sales`) | `POST /api/v1/orchestrator/orders/{orderId}/fulfillment` when used for `SHIPPED`/`DISPATCHED` mutation | merge | Status updates must route through canonical dispatch truth checks or fail closed/prod-gated. |
| P2P | Invoice + supplier settlement chain | `POST /api/v1/purchasing/raw-material-purchases`, `POST /api/v1/accounting/suppliers/payments`, `POST /api/v1/accounting/settlements/suppliers` (`purchasing` -> `accounting`) | `POST /api/v1/raw-materials/intake`, `POST /api/v1/inventory/opening-stock` | keep | Manual intake/opening-stock writes remain migration or emergency tools only and stay prod-gated with idempotency + audit linkage. |
| P2P | Settlement idempotency normalization | Canonical supplier settlement key semantics at `accounting` boundary | Duplicated normalization helpers across `purchasing`/`inventory`/`production` write helpers | merge | Converge key normalization to single semantics so retries cannot diverge by module. |
| Production-to-Pack | Production logging | `POST /api/v1/factory/production/logs` (`factory`, `ProductionLogController`) | `POST /api/v1/factory/production-batches` | deprecate | Legacy production-batch path remains admin-only/prod-gated until fully retired; orchestrator must not rely on it. |
| Production-to-Pack | Packing write surface | `POST /api/v1/factory/packing-records` (`factory`, `PackingService`/`BulkPackingService`) | `POST /api/v1/factory/pack` | merge | Both endpoints must share one service-layer idempotency + accounting boundary (`AccountingFacade`). |
| Payroll | Run lifecycle | `POST /api/v1/payroll/runs` + `/payroll/runs/{id}/calculate|approve|post|mark-paid` (`hr`, `PayrollService`) | `POST /api/v1/hr/payroll-runs` | merge | HR legacy endpoint remains compatibility-only and must resolve through canonical payroll-run state machine checks. |
| Payroll | Orchestrator payroll trigger | `POST /api/v1/payroll/runs*` (`hr`) | `POST /api/v1/orchestrator/payroll/run` | deprecate | Orchestrator payroll trigger stays disabled/prod-gated until idempotent canonical routing parity is proven. |

## Order-to-Cash (O2C)
- Owner module/service: `sales` (`SalesController`, `SalesService`, `SalesFulfillmentService`)
- Handoff module/service: `inventory` (`FinishedGoodsService`) -> `accounting` (`SalesJournalService`, `AccountingFacade`, `DealerLedgerService`)
- Key invariants:
  - Dispatch confirmation drives one invoice (`invoices`) and one AR journal + one COGS journal reference.
  - Sales order, packaging slip, invoice, and dealer ledger remain link-consistent by id.
  - Idempotency keys and references must prevent duplicate postings.
- Duplicate/overlap risks:
  - Decision guard: keep `POST /api/v1/dispatch/confirm` as compatibility alias into canonical sales dispatch.
  - Decision guard: deprecate `/api/v1/orchestrator/dispatch*` and merge fulfillment status writes into canonical dispatch truth checks.

## Procure-to-Pay (P2P)
- Owner module/service: `purchasing` (`PurchasingService`, `SupplierService`)
- Handoff module/service: `inventory` (`RawMaterialService`) -> `accounting` (`AccountingService`)
- Key invariants:
  - GRN intake can’t exceed PO requirement.
  - Raw material movements link to purchase records.
  - Purchase invoice, partner settlement (supplier role), and journal lines are balanced and idempotent.
- Duplicate/overlap risks:
  - Decision guard: keep manual intake/opening-stock writes prod-gated for migration/emergency only.
  - Decision guard: merge duplicated settlement/idempotency helper behavior to single accounting-boundary semantics.
  - Migration overlap between purchasing/inventory schemas (`V27`, `V120` family and related v2 migrations) requires periodic overlap scan.

## Production-to-Pack
- Owner module/service: `production` (`ProductionCatalogService`) + `factory` (`PackingService`, `BulkPackingService`)
- Handoff module/service: `inventory` (`FinishedGoodsService`, `RawMaterialService`) -> `accounting` (`AccountingFacade`)
- Key invariants:
  - Production/log/bulk pack operations generate deterministic movement/reference linkage.
  - Packaging operations cannot silently duplicate movements/journals on retries.
  - Inventory quantities remain non-negative and FIFO/non-FIFO paths remain configured.
- Duplicate/overlap risks:
  - Decision guard: deprecate legacy `POST /api/v1/factory/production-batches` and treat production logs as the canonical write entrypoint.
  - Decision guard: merge `POST /api/v1/factory/pack` into the same canonical packing service boundary as `POST /api/v1/factory/packing-records`.
  - Shared naming/behavior for `PackingService` and `InventoryMovement` updates can diverge.

## Payroll
- Owner module/service: `hr` (`PayrollService`, `PayrollCalculationService`)
- Handoff module/service: `accounting` (`AccountingService`, `AccountingFacade`)
- Key invariants:
  - Payroll run state is linear (`DRAFT -> CALCULATED -> APPROVED -> POSTED -> PAID`).
  - `AccountingPeriodService`/period lock constraints apply.
  - Posted journal remains linked to run and attendance lines.
- Duplicate/overlap risks:
  - Decision guard: merge `POST /api/v1/hr/payroll-runs` into canonical payroll run endpoints (`/api/v1/payroll/runs*`) for write semantics.
  - Decision guard: deprecate/prod-gate `POST /api/v1/orchestrator/payroll/run` until canonical routing and idempotency parity is proven.
  - Existing duplicate DTO/helper methods around payroll references increase risk.

## Period Close
- Owner module/service: `accounting` (`AccountingPeriodService`, `AccountingPeriodSnapshotService`)
- Handoff module/service: `reports` (`ReportService`) and `reconciliation` flows via `accounting` service callers
- Key invariants:
  - Ledger entries posted under closed periods are blocked unless override policy is explicitly used.
  - Close and reopen are explicit and should be reflected in snapshots.
  - Subledger totals (`AR/AP` partner subledgers: dealer/supplier, and payroll) remain reconciled at close boundaries.
- Duplicate/overlap risks:
  - Period hooks in multiple modules (accounting + reporting) can apply inconsistent assumptions on close status.
  - Legacy period-close workarounds in scripts/handlers must stay aligned with canonical service checks.
