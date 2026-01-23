# Duplicates Report

## Endpoints
- Dealers directory
  - Canonical: `GET /api/v1/dealers`, `GET /api/v1/dealers/search`
  - Alias (deprecated): `GET /api/v1/sales/dealers`, `GET /api/v1/sales/dealers/search`
  - Status: alias endpoints marked deprecated for OpenAPI visibility.
- Payroll runs
  - Canonical: `GET/POST /api/v1/payroll/runs` + extended payroll workflow endpoints under `/api/v1/payroll/*`
  - Legacy (deprecated): `GET/POST /api/v1/hr/payroll-runs`
  - Status: legacy endpoints marked deprecated; keep for backward compatibility.
- Sales dispatch
  - Canonical: `POST /api/v1/sales/dispatch/confirm` (factory alias: `POST /api/v1/dispatch/confirm`)
  - Legacy (deprecated/internal): `POST /api/v1/orchestrator/dispatch`, `POST /api/v1/orchestrator/dispatch/{orderId}`
  - Status: orchestrator dispatch disabled by default (feature flag `orchestrator.order-dispatch.enabled`); use sales dispatch for AR/COGS + inventory.

## DTOs
- Payroll run DTOs exist in two shapes:
  - `com.bigbrightpaints.erp.modules.hr.dto.PayrollRunDto`
  - `com.bigbrightpaints.erp.modules.hr.service.PayrollService.PayrollRunDto`
- Status: documented for future consolidation; no runtime changes yet to avoid breaking clients.

## Flyway Migrations
- Duplicate table definitions (guarded by `IF NOT EXISTS` / DO blocks):
  - `payroll_runs` in `V7__hr_tables.sql` and `V78__payroll_enhancement.sql`
  - `payroll_run_lines` in `V57__payroll_run_lines.sql` and `V78__payroll_enhancement.sql`
- Duplicate index creation (guarded by `IF NOT EXISTS`):
  - `idx_invoices_dealer` in `V12__invoices.sql` and `V62__performance_indexes.sql`
  - `idx_journal_lines_account_id` in `V62__performance_indexes.sql` and `V65__performance_accounting_inventory_indexes.sql`
- Status: do not rewrite applied migrations; forward-fix only (see Flyway strategy doc).

## Notes
- `docs/endpoint_inventory.tsv` contains the full endpoint list for audit traceability.
