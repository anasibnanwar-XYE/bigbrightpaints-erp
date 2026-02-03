# Duplicates Report

## Endpoints
- Dealers directory
  - Canonical: `GET /api/v1/dealers`, `GET /api/v1/dealers/search`
  - Alias (deprecated): `GET /api/v1/sales/dealers`, `GET /api/v1/sales/dealers/search`
  - Status: alias endpoints marked deprecated for OpenAPI visibility.
- Dealer onboarding (implementation)
  - Canonical: `POST /api/v1/dealers` -> `DealerService.createDealer(...)` (creates AR account + portal user)
  - Legacy/duplicate implementation: `SalesService.createDealer(...)` (does not match canonical onboarding behavior)
  - Status: do not call the legacy implementation from controllers/orchestrator; route to `DealerService` only.
- Invoices (views)
  - Staff/canonical: `/api/v1/invoices/*` (Invoice module)
  - Staff/dealer list alias: `/api/v1/dealers/{dealerId}/invoices` (DealerController)
  - Dealer portal: `/api/v1/dealer-portal/invoices` (DealerPortalController)
  - Status: keep all functional, but ensure company scoping + RBAC + consistent filtering.
- Payroll runs
  - Canonical: `GET/POST /api/v1/payroll/runs` + extended payroll workflow endpoints under `/api/v1/payroll/*`
  - Legacy (deprecated): `GET/POST /api/v1/hr/payroll-runs`
  - Status: legacy endpoints marked deprecated; keep for backward compatibility.
- Sales dispatch
  - Canonical: `POST /api/v1/sales/dispatch/confirm` (factory alias: `POST /api/v1/dispatch/confirm`)
  - Legacy (deprecated/internal): `POST /api/v1/orchestrator/dispatch`, `POST /api/v1/orchestrator/dispatch/{orderId}`
  - Status: orchestrator dispatch is hard deprecated (always 410 + canonicalPath); use sales dispatch for AR/COGS + inventory.

## DTOs
- Payroll run DTOs exist in two shapes:
  - `com.bigbrightpaints.erp.modules.hr.dto.PayrollRunDto`
  - `com.bigbrightpaints.erp.modules.hr.service.PayrollService.PayrollRunDto`
- Status: documented for future consolidation; no runtime changes yet to avoid breaking clients.

## Flyway Migrations
- Duplicate table definitions (guarded by `IF NOT EXISTS` / DO blocks):
  - `payroll_runs` in `V7__hr_tables.sql` and `V78__payroll_enhancement.sql`
  - `payroll_run_lines` in `V57__payroll_run_lines.sql` and `V78__payroll_enhancement.sql`
- Duplicate uniqueness enforcement:
  - `journal_entries` reference uniqueness appears in table DDL and again in `V66__accounting_journal_unique_constraint.sql`
  - `accounting_events` sequence uniqueness appears in table DDL and again in `V115__accounting_event_sequence_unique.sql`
- Duplicate index creation (guarded by `IF NOT EXISTS`):
  - `idx_invoices_dealer` in `V12__invoices.sql` and `V62__performance_indexes.sql`
  - `idx_journal_lines_account_id` in `V62__performance_indexes.sql` and `V65__performance_accounting_inventory_indexes.sql`
  - `idx_partner_settlement_idempotency` in `V48__settlement_idempotency_keys.sql` and `V102__partner_settlement_idempotency_scope.sql`
  - `uk_payroll_runs_company_idempotency` in `V68__payroll_idempotency_key.sql` and `V100__payroll_idempotency_scope.sql`
- Status: do not rewrite applied migrations; forward-fix only (see Flyway strategy doc).
- CODE-RED convergence plan (proposed, forward-only):
  - `V120__payroll_convergence.sql`
  - `V121__journal_uniqueness_convergence.sql`
  - `V122__accounting_events_uniqueness_convergence.sql`
  - `V123__index_consolidation.sql`
  - Optional: `V124__auth_token_mfa_convergence.sql`

## Notes
- `docs/endpoint_inventory.tsv` contains the full endpoint list for audit traceability.
- Orchestrator health endpoints (`/api/v1/orchestrator/health/*`) are not duplicates, but are high-risk if unauthenticated in non-dev envs.
