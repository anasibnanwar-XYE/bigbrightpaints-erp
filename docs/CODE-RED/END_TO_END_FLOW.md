# End-to-End Flow (Current Reality, CODE-RED)

This document is the **layman + engineering** view of how the backend works today across modules, end-to-end.
It is **not** a proposal for new behavior; it documents current behavior and CODE-RED invariants.

Primary references:
- State machines (current behavior): `erp-domain/docs/*STATE_MACHINES.md`
- Canonical module map: `erp-domain/docs/MODULE_FLOW_MAP.md`
- Cross-module linkage checks: `erp-domain/docs/CROSS_MODULE_LINKAGE_MATRIX.md`
- Duplicates/aliases: `erp-domain/docs/DUPLICATES_REPORT.md`
- CODE-RED decisions: `docs/CODE-RED/decision-log.md`

---

## Company / Tenant Context (Applies Everywhere)

- The active `companyId` is derived from the authenticated context (JWT claim `cid` → `CompanyContextHolder`).
- Clients may still send `X-Company-Id` (for older “switch company” UX), but if present it **must match** the JWT `cid`
  or the request is rejected (fail-closed, 403).

---

## Order-to-Cash (Sales → Inventory/Factory → Dispatch → Invoice/Journals)

### 0) Dealer onboarding (sales can do this)
1) Sales searches dealer directory: `GET /api/v1/dealers/search`.
2) If dealer is not found, sales creates a dealer: `POST /api/v1/dealers`.
3) On dealer creation the system auto-creates and links:
   - dealer AR account (pattern `AR-<dealerCode>`)
   - dealer portal user (if email not registered) + `ROLE_DEALER`

Canonical behavior: `DealerService.createDealer(...)` (do not use legacy `SalesService.createDealer(...)`).

### 1) Sales order creation (“proforma stage”)
1) Sales creates order: `POST /api/v1/sales/orders` with `dealerId` and line items.
2) System reserves stock and creates a packaging slip (reservation/fulfillment document).
   - If stock is available → order becomes `RESERVED`, slip status becomes `RESERVED`.
   - If stock is not available → order becomes `PENDING_PRODUCTION`, slip status becomes `PENDING_PRODUCTION` and
     factory tasks may be created (production planning).

Note: there is **no separate proforma invoice object** today; the order + reservation/slip is the operational “proforma”.

### 2) Factory/production (when shortages exist)
1) Factory produces bulk/semi-finished via production logs.
2) Packing converts semi-finished/bulk into finished goods (creates batches + movements; posts manufacturing journals).
3) Bulk packing converts bulk batches into size SKU child batches (creates movements; posts conversion journals).

These flows live in `factory` + `inventory` modules; see `erp-domain/docs/PRODUCTION_TO_PACK_STATE_MACHINES.md`.

### 3) Dispatch confirmation (financial truth)
1) Dispatch is confirmed via the **canonical** endpoint: `POST /api/v1/sales/dispatch/confirm`
   (alias: `POST /api/v1/dispatch/confirm`).
2) Canonical service path: `SalesService.confirmDispatch(...)`.
3) On success the system updates and links (same business transaction):
   - packaging slip status → `DISPATCHED` (inventory issued)
   - invoice created/linked (invoice numbering uses Indian FY derived from issueDate/dispatch date)
   - AR journal posted (AR/Revenue/Tax)
   - COGS journal posted (COGS/Inventory)
   - dealer ledger updated (aging/outstanding reflects invoice)

Invariant: **No other API may mark an order `SHIPPED/DISPATCHED` without the slip → invoice → journals → ledger chain.**

---

## Procure-to-Pay (Purchasing → Inventory → AP/Journals)

1) Purchase order + goods receipt (GRN) record operational receiving and inventory movements.
2) GL posting happens at supplier invoice creation (raw material purchase), not at GRN.
3) Supplier payments/settlements post journals and update supplier ledger.

See `erp-domain/docs/PROCURE_TO_PAY_STATE_MACHINES.md`.

---

## Hire-to-Pay (Payroll → Posting → Payment)

Canonical payroll run workflow (HR module):
1) Create payroll run: `POST /api/v1/payroll/runs` (or weekly/monthly shortcuts)
2) Calculate: `POST /api/v1/payroll/runs/{id}/calculate`
3) Approve: `POST /api/v1/payroll/runs/{id}/approve`
4) Post to accounting: `POST /api/v1/payroll/runs/{id}/post`
   - Posts a balanced journal via `AccountingFacade.postPayrollRun(...)` (reference `PAYROLL-<runNumber>`).
5) Mark-as-paid: `POST /api/v1/payroll/runs/{id}/mark-paid`
   - Current behavior: updates HR run/line statuses and advance balances (does not post cash/bank journal).

Accounting-side payroll payment (separate, current reality):
- Batch payroll payment journal (not linked to a PayrollRun): `POST /api/v1/accounting/payroll/payments/batch`
- PayrollRun-linked payment journal (legacy/safety-hardening in progress): `POST /api/v1/accounting/payroll/payments`

Orchestrator payroll:
- Exists but is **disabled by default** in CODE-RED; when enabled it must route to canonical payroll flows and remain
  idempotent under retries.

See `erp-domain/docs/HIRE_TO_PAY_STATE_MACHINES.md`.

---

## Period Close / Reporting (Accounting)

Truth sources (CODE-RED):
- Journals + locks are accounting truth for postings.
- Closed-period reporting must read **period-end snapshots** (“closed means closed”).
- `accounting_events` is an audit/diagnostic log only (not temporal truth).

See `docs/CODE-RED/decision-log.md` (2026-02-01) and `erp-domain/docs/ACCOUNTING_MODEL_AND_POSTING_CONTRACT.md`.
