# Module Flow Map (Portals vs Domains)

This document intentionally separates:
- **Portals (UI surface / responsibility slices)**: what users see and operate
- **Domains (code modules / business responsibilities)**: where logic lives in the backend

For deep linkage keys and traceability expectations, see `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md`.

---

## Global invariants (ERP-grade “must be true”)

These are the non-negotiables that prevent silent drift:

1) **Document → Journal → Ledger invariant (most important)**
   - If a document is **posted/issued/confirmed** (examples: invoice issued, dispatch confirmed, purchase posted, payroll posted, inventory adjustment posted), then:
     - it must link to a `JournalEntry` (directly or via a stable reference mapping)
     - the journal must be **balanced** (debits == credits within tolerance)
     - it must reconcile with the relevant subledger where applicable (dealer/supplier) within tolerance

2) **Company scoping**
   - Every record is company-scoped; requests are scoped by `X-Company-Id` (company code) and must not cross boundaries.

3) **Inventory integrity**
   - Inventory movements are idempotent under retries and remain traceable to their source document.
   - No negative stock where enforcement applies.

4) **Period controls**
   - Posting into locked/closed periods is rejected; reversals follow the same control rules.

---

## Portals (UI surface)

### Admin portal
- Auth + session lifecycle: `/api/v1/auth/*` (JWT issuance/refresh/logout), MFA flows.
- Users/roles/settings: `/api/v1/admin/*` and role endpoints.
- Company context switching: `/api/v1/multi-company/companies/switch` sets `X-Company-Id` for downstream calls.

### Accounting portal
- Journals, periods, settlements, month-end checklist, statements/aging, reconciliation dashboards.
- Important: this portal **uses** Purchasing/Inventory/Payroll data, but those remain separate domains in code ownership.

### Sales portal
- Dealers, orders, dispatch, invoicing/returns, credit limit overrides.

### Manufacturing portal
- Production logs, packing records, packaging size mappings, stock/cost visibility.

### Dealer portal (read-only, self-scoped)
- Dealer can only see **their own** records (no cross-dealer access).
- Current backend exposure is read-only GET endpoints:
  - `/api/v1/dealer-portal/orders`
  - `/api/v1/dealer-portal/invoices` (+ PDF)
  - `/api/v1/dealer-portal/ledger`
  - `/api/v1/dealer-portal/aging`
  - `/api/v1/dealer-portal/dashboard`

---

## Domains (code modules)

These are backend responsibilities (code ownership boundaries), independent of portals.

### Auth/Admin/Security
- Packages: `modules/auth/**`, `modules/rbac/**`, `modules/admin/**`, `modules/company/**`
- Responsibilities: JWT, MFA, RBAC, multi-company scoping, admin operations, audit metadata.

### Accounting core + Reports
- Packages: `modules/accounting/**`, `modules/reports/**`
- Responsibilities: chart of accounts, journal engine, periods close/lock/reopen, settlement posting, statements/aging, reconciliation controls.
- Posting contract reference: `erp-domain/docs/ACCOUNTING_MODEL_AND_POSTING_CONTRACT.md`

### Sales (Order-to-Cash)
- Packages: `modules/sales/**`, plus integration to `modules/invoice/**` and accounting posting services.
- Typical lifecycle (simplified; see state machine doc for full set):
  - Draft/Booked order → Confirmed/Reserved → Dispatch (stock issue) → Invoice (AR created) → Settlement (cash receipt) → Return/Credit note (reversal)
- Reference: `erp-domain/docs/ORDER_TO_CASH_STATE_MACHINES.md`

### Purchasing (Procure-to-Pay)
- Packages: `modules/purchasing/**`
- Responsibilities: supplier purchases, intake/receipts, returns, supplier settlement allocation rules.
- Reference: `erp-domain/docs/PROCURE_TO_PAY_STATE_MACHINES.md`

### Inventory
- Packages: `modules/inventory/**`
- Responsibilities: raw materials + finished goods masters, batches, movements, reservations, dispatch slips, adjustments, valuation signals.

### Factory/Production
- Packages: `modules/factory/**`, `modules/production/**`
- Responsibilities: production logs (material issue), packing records (finished goods receipt), packaging mappings, costing signals.

**Costing source of truth (production)**
- Authoritative “production cost record”: `ProductionLog` fields stored in `production_logs`:
  - `material_cost_total`, `labor_cost_total`, `overhead_cost_total`, `unit_cost`
- How costing is computed today (actual-cost basis, not standard/BOM):
  - Material cost comes from **FIFO raw material batch consumption** (`RawMaterialBatch`), recorded as `RawMaterialMovement` with `unit_cost` snapshots.
  - Optional labor/overhead are provided on the production log request and included in `unit_cost`.
  - Packing produces `FinishedGoodBatch.unit_cost` and `InventoryMovement.unit_cost`, including per-unit packaging cost where consumed.
- Implementation anchors: `modules/factory/service/ProductionLogService`, `modules/factory/service/PackingService`

### HR/Payroll
- Packages: `modules/hr/**`
- Responsibilities: employees, attendance, payroll runs, payroll posting, mark-paid tracking.
- Reference: `erp-domain/docs/HIRE_TO_PAY_STATE_MACHINES.md`

### Orchestrator/Outbox (coordination, not ownership of domain rules)
- Packages: `orchestrator/**`
- Responsibilities: outbox/event publishing, workflow/command dispatch endpoints, integration coordination where used.
- Note: many flows may still be synchronous within domain services; orchestrator is used for long-running/workflow-style coordination and operational visibility.

---

## Cross-module chains (audit traceability)

These chains define how an auditor/operator traces an outcome to its cause:

- **O2C**: Order → Dispatch/Slip → Inventory Movements → Invoice → Journal → Dealer Ledger → Settlement/Return/Reversal
- **P2P**: Purchase → Receipt (RM movements) → Journal → Supplier Ledger → Settlement → Return/Reversal
- **Production**: Production Log (RM issue) → Packing (FG receipt) → Inventory Movements → Journals (WIP/semi-finished/FG) → Downstream COGS on dispatch
- **Payroll**: Payroll Run → Journal → Mark-paid tracking → (optional external cash/bank handling)
