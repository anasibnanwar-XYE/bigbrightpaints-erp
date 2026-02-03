# Performance / Query Hotspots (CODE-RED)

Last updated: 2026-02-02

Purpose: track known query-count / N+1 patterns that can amplify DB load or lock contention. In CODE-RED, we only take
**low-risk** improvements that do not change accounting/inventory semantics.

## TL;DR (Current Status)

Implemented (safe, semantics-preserving)
- Inventory adjustments: lock finished goods in one deterministic query; remove redundant per-line `save(...)` calls.
- Payroll auto-calculation: prefetch attendance in one query; bulk insert payroll run lines.

Not applicable in this repo
- Tally ingestion stock import bulk update: `tally-ingestion-backend/...StockImportService` is not present in this workspace.

Deferred (bigger changes; do after deploy safety work)
- Catalog CSV import batching (brands/products/raw materials).
- Production material consumption batching (FIFO + batch costing makes this non-trivial).
- Security notification integrations (email/SMS/Slack).
- Audit log batching/queueing (currently bounded by async executor; still worth revisiting under sustained load).

## Implemented

### Inventory Adjustment (Finished Goods) — reduce N+1 lock queries

What was happening
- `createAdjustment(...)` called `lockByCompanyAndId(...)` once per adjustment line.
- `applyMovements(...)` called `finishedGoodRepository.save(...)` once per line even though entities are already managed.
- `adjustBatchQuantities(...)` called `finishedGoodBatchRepository.save(...)` per batch even though batches are managed.

What we changed
- Lock all referenced finished goods up front, deterministically, with one pessimistic-lock query:
  - `FinishedGoodRepository.lockByCompanyAndIdInOrderById(...)`
- Build lines from the already-locked `FinishedGood` instances.
- Remove redundant `save(...)` calls for managed entities (flush still happens at transaction commit).

Files
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodRepository.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/InventoryAdjustmentService.java`

Notes
- This does **not** eliminate row updates (stock/batches still change), but it reduces DB round-trips and avoids per-line lock queries.

### Payroll Auto-Calculation — reduce attendance N+1 + bulk insert lines

What was happening
- Per employee attendance query: `findByEmployeeAndAttendanceDateBetween(...)`.
- Per payroll line employee lookup: `employeeRepository.findByCompanyAndId(...)`.
- Per payroll line insert: `payrollRunLineRepository.save(...)`.

What we changed
- Prefetch attendance for all employees in the run in one query and group in-memory:
  - `AttendanceRepository.findByCompanyAndEmployeeIdsAndDateRange(...)`
- Reuse `Employee` instances already loaded from the employee list; no per-line employee lookup.
- Build payroll run lines in memory and persist via `payrollRunLineRepository.saveAll(...)`.

Files
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/AttendanceRepository.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollCalculationService.java`

Notes
- `saveAll(...)` still emits one INSERT per row; the big win is eliminating extra SELECTs and reducing per-entity repository calls.

## Backlog (Defer / More Risk)

### Catalog Import (CSV)

Risk
- Requires multi-entity batching (brands/products/raw materials) with careful uniqueness rules and error reporting.

Candidate approach
- Parse CSV → collect unique keys → batch fetch existing entities → upsert in memory → `saveAll(...)`.

File
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogService.java`

### Production Material Consumption (FIFO)

Risk
- FIFO batch selection + costing + pessimistic locks makes “fetch everything in one query” tricky; correctness > speed here.

File
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogService.java`

## Verification

- `bash scripts/verify_local.sh`
