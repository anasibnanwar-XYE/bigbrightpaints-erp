# Task 01 — Architecture + Module Map (Modules, Entities, Tables, Financial Touchpoints)

## Purpose
**Accountant-level:** define the system’s “books of record” and where financially significant truth lives (documents, journals, ledgers, reconciliation), so audit trails are reviewable and repeatable.

**System-level:** produce a **verified module map** (controllers/services/entities/tables) and identify **financial touchpoints** (where inventory/accounting state changes), to prevent blind spots during deep debugging.

## Scope guard (explicitly NOT allowed)
- No new endpoints, workflows, or UI.
- No refactors for “cleanliness”; only mapping and documentation (and later: tests/invariants that enforce intended behavior).
- Do not change posting semantics while building the map.

## Milestones

### M1 — Create a verified module inventory (code + DB)
Deliverables:
- Update the “Module map (snapshot)” below so it is **source‑anchored**:
  - controllers (REST entry points)
  - primary services (business orchestration)
  - key entities/tables (persistent truth)
  - financial touchpoints (where money/stock state changes)
- Record any uncertain areas as explicit “UNKNOWN → verify” items (do not guess silently).

Verification gates (run after M1):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`
- Focused (inventory of API surface): `mvn -f erp-domain/pom.xml -Dtest=OpenApiSnapshotIT test`

Evidence to capture:
- A short “module inventory delta” note: what was added/confirmed/removed and why (include file anchors).
- Any mismatches between `erp-domain/docs/endpoint_inventory.tsv` and `openapi.json` (if found).

Stop conditions + smallest decision needed:
- If the module map cannot be verified due to missing sources (e.g., generated code not present): choose the fail‑closed option of marking the section as UNKNOWN and proceed; do not invent lists.

### M2 — Identify financial touchpoints and their expected “evidence chain”
Deliverables:
- For each module, list its financially significant actions and the minimum required chain of evidence:
  - `source document` (invoice/purchase/payroll run/production log/etc.)
  - `journal entry id` (and lines)
  - `ledger/subledger references`
  - `reconciliation endpoint/report` that should tie out
- Explicitly mark touchpoints that **must be idempotent** under retries.

Verification gates (run after M2):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`
- Focused: `mvn -f erp-domain/pom.xml -Dtest=ErpInvariantsSuiteIT test`

Evidence to capture:
- A one-page “financial touchpoints list” (can live in this task file) referencing endpoints + services.

Stop conditions + smallest decision needed:
- If a flow appears to post without a journal link: treat as an integrity gap and record it; smallest decision is whether to (A) add invariant/test only, or (B) also add a minimal fail‑closed guard in code (later task).

### M3 — Lock the map to tests (what evidence is already enforced vs missing)
Deliverables:
- For each touchpoint, list the **exact tests** that currently enforce linkage/auditability (or mark as missing).
- Create a short “gaps to close” checklist that feeds Task 03 and Task 04.

Verification gates (run after M3):
- `mvn -f erp-domain/pom.xml -DskipTests compile`
- `mvn -f erp-domain/pom.xml -Dcheckstyle.failOnViolation=false checkstyle:check`
- `mvn -f erp-domain/pom.xml test`
- Focused: `mvn -f erp-domain/pom.xml -Dtest=ErpInvariantsSuiteIT,ReconciliationControlsIT,PeriodCloseLockIT test`

Evidence to capture:
- The gap checklist (tests missing / invariants missing / endpoint auth gaps).

Stop conditions + smallest decision needed:
- If the only way to enforce a gap is a new endpoint: stop and re‑check for an equivalent existing endpoint; smallest decision is whether the gap can be closed by test + server‑side guard on an existing endpoint.

---

## Module map (snapshot; verify and update during M1)

Note: “Key tables” are taken from Flyway migrations in `erp-domain/src/main/resources/db/migration/` and may be extended by later migrations. Always verify against code + DB schema.

### ADMIN (Users/Roles/Settings) + Company/Tenancy
- Code:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/**`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/**`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**`
- Primary controllers:
  - `AdminSettingsController` (`/api/v1/admin/**`)
  - `AdminUserController` (`/api/v1/admin/users/**`)
  - `RoleController` (`/api/v1/admin/roles/**`)
  - `CompanyController` (`/api/v1/companies`)
  - `MultiCompanyController` (`/api/v1/multi-company/**`)
- Key tables (representative):
  - `companies`, `app_users`, `roles`, `permissions`, `user_roles`, `user_companies`
  - security tables: `mfa_recovery_codes`, `password_reset_tokens`, `user_password_history`
  - audit tables: `audit_log_entries` (name may vary; verify in migrations)
- Financial touchpoints:
  - Company defaults for posting accounts (inventory/COGS/revenue/tax/payroll) and enforcement of “cannot post without defaults”.
  - Company boundary enforcement via `X-Company-Id` and membership checks (high risk for cross‑company leakage).

### AUTH (JWT/MFA/Profile)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**` and `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/**`
- Primary controllers:
  - `AuthController` (`/api/v1/auth/**`)
  - `MfaController` (`/api/v1/auth/mfa/**`)
  - `UserProfileController` (`/api/v1/auth/profile`)
- Key tables (representative):
  - `refresh_tokens` / token blacklist (verify actual table names)
  - `mfa_recovery_codes`, `user_profile` fields
- Financial touchpoints:
  - None directly, but auth controls access to all financially significant endpoints.

### ACCOUNTING (GL/Journals/Periods/Settlements/Reconciliation)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/**` + related shared services
- Primary controllers:
  - `AccountingController` (`/api/v1/accounting/**`)
  - `AccountingCatalogController` (`/api/v1/accounting/catalog/**`)
  - `AccountingConfigurationController` (`/api/v1/accounting/config/**` or similar; verify)
  - `OnboardingController` (`/api/v1/onboarding/**` or `/api/v1/accounting/onboarding/**`; verify)
  - `PayrollController` (batch payroll payment under accounting: `/api/v1/accounting/payroll/**`)
- Key tables (representative):
  - `accounts`
  - `journal_entries`, `journal_lines`
  - `ledger_entries`
  - `accounting_periods` / `periods`, month-end checklist tables (verify)
  - subledgers: `dealer_ledger_entries`, `supplier_ledger_entries`, `partner_settlement_allocations`
- Financial touchpoints:
  - Creating/posting journal entries (must be balanced, linked, same-company).
  - Posting settlements/receipts/payments and producing subledger allocations.
  - Period lock/close/reopen (must prevent posting into locked/closed periods).
  - Reconciliation controls (inventory↔GL, AR/AP subledger↔control).

### SALES (Dealers/Orders/Dispatch Confirm Trigger/Invoice issuance)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/**` + `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/**`
- Primary controllers:
  - `DealerController` (`/api/v1/dealers/**`)
  - `SalesController` (`/api/v1/sales/**`)
  - `CreditLimitOverrideController` (`/api/v1/sales/credit-limit-overrides/**`)
  - `InvoiceController` (`/api/v1/invoices/**`)
  - `DealerPortalController` (`/api/v1/dealer-portal/**`)
- Key tables (representative):
  - `dealers`
  - `sales_orders`, `sales_order_items`
  - `packaging_slips` (dispatch artifact; may live under inventory migrations)
  - `invoices` (+ journal linkage via `invoice_journal_link` migration)
  - `dealer_ledger_entries`
- Financial touchpoints:
  - Dispatch confirmation posts inventory movements and (typically) invoices + journals (AR/COGS/inventory).
  - Dealer settlements update dealer ledger + allocations and post journals.
  - Rounding/tax computations must be stable and auditable (GST inclusive rules).

### INVENTORY (Finished goods, raw materials, movements, opening stock, adjustments)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/**`
- Primary controllers:
  - `DispatchController` (dispatch workflows; verify mapping to `/api/v1/dispatch/**`)
  - `FinishedGoodController`
  - `RawMaterialController`
  - `InventoryAdjustmentController`
  - `OpeningStockImportController`
- Key tables (representative):
  - `finished_goods`, `finished_good_batches`
  - `inventory_movements` (+ journal linkage via `inventory_movement_journal_link` migration)
  - `raw_materials`, `raw_material_batches`, `raw_material_movements`
  - `packaging_slips`
- Financial touchpoints:
  - Inventory movements (receipt/issue/adjustment) must be ordered, idempotent, and linked when posting is expected.
  - Opening stock import must be idempotent and tie to opening balance journals when configured.

### PURCHASING/AP (Suppliers, purchases/receipts, supplier settlements/payments)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/**`
- Primary controllers:
  - `SupplierController` (`/api/v1/suppliers/**`)
  - `RawMaterialPurchaseController` (`/api/v1/purchasing/raw-material-purchases/**`)
- Key tables (representative):
  - `suppliers`
  - `raw_material_purchases` (+ items/lines; verify)
  - `raw_material_movements` (receipts/returns)
  - `supplier_ledger_entries`, `partner_settlement_allocations`
- Financial touchpoints:
  - Recording purchases/receipts must post AP and inventory effects with linkage.
  - Supplier settlements/payments must be idempotent and reconcile to AP control.

### FACTORY/PRODUCTION (Factory ops + production catalog/logs/packing)
- Code:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/**`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/**`
- Primary controllers:
  - `FactoryController`
  - `ProductionLogController`
  - `PackingController`
  - `PackagingMappingController`
  - `ProductionCatalogController`
- Key tables (representative):
  - factory ops: `production_plans`, `production_batches`, `factory_tasks`
  - production logs: `production_logs`, `production_log_materials`
  - packing/catalog: `packing_records`, product/brand/catalog tables (`products`, `brands`, `product_variants`, pricing tables; verify)
  - inventory movements and batches (RM + FG)
- Financial touchpoints:
  - Production logs consume RM and create WIP/FG movements; costing journals (WIP→FG, COGS) must be traceable where enabled.
  - Packing must not create orphan finished goods batches/movements.

### HR/PAYROLL (Employees, leave, payroll runs, posting + payment marking)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/**` + accounting payroll endpoints
- Primary controllers:
  - `HrController`
  - `HrPayrollController` (HR payroll runs alias endpoints; verify)
- Key tables (representative):
  - `employees`, `leave_requests`
  - `payroll_runs` (+ totals migration)
  - payroll line/adjustment tables (verify)
- Financial touchpoints:
  - Payroll run calculate→approve→post must produce a linked journal.
  - Payroll payments/mark-paid must be auditable and reversible where intended.

### DEALER PORTAL (Read‑only dealer self‑service)
- Code: dealer-facing endpoints primarily in `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerPortalController.java`
- Intended surface:
  - Ledger viewer, invoices/orders/outstanding only (no posting).
- Financial touchpoints:
  - None directly (read-only), but must never expose cross‑dealer or cross‑company data.

### ORCHESTRATOR/OUTBOX (Workflows, auto-approval, background processing)
- Code: `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/**`
- Primary controllers:
  - `DashboardController` (admin dashboards)
  - `OrchestratorController` (workflow actions, traces, health endpoints)
  - `IntegrationHealthController` (`/api/integration/health`, permitAll)
- Key tables (representative):
  - `orchestrator_outbox`, `orchestrator_audit`, `scheduled_jobs`
- Financial touchpoints:
  - At-least-once dispatching must be idempotent; retries must not duplicate postings/movements.
  - Trace/audit endpoints must not leak sensitive data; health endpoints must be safe for public exposure.

