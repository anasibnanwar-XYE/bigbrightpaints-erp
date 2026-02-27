# Sales Portal Module Contract

Last updated: 2026-02-27  
Module: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales`

## Purpose

Sales portal is the tenant-side order intake and dealer relationship module.
It manages dealer onboarding, proforma-style order entry, promotion publishing, credit-limit request lifecycle, and sales progress visibility.

Final dispatch execution is factory-owned, and final financial posting is accounting-owned.

## Scope

In scope for sales users:
- Dealer create/update/search/list and dealer financial visibility (ledger, invoices, aging).
- Sales order create/update/confirm/cancel/status updates.
- Sales dashboard metrics.
- Promotion create/update/delete/list (including optional image URL).
- Credit request create/update/list.

Not in scope for sales users:
- Final dispatch confirmation endpoint.
- Accounting month-end/ledger posting operations.
- Raw inventory internals outside sales-facing product/order flows.

## Role Contract

1. `ROLE_SALES`
- Can manage dealers, sales orders, promotions, targets (read), and credit requests (create/update/list).
- Cannot call final dispatch confirm.

2. `ROLE_FACTORY`
- Can call final dispatch confirm only with `dispatch.confirm` authority.

3. `ROLE_ADMIN`
- Full tenant oversight across sales module, including dispatch confirm (with `dispatch.confirm`).

4. `ROLE_ACCOUNTING`
- Can approve/reject credit requests and run dispatch marker reconciliation.

## Core API Surface

## Dashboard

- `GET /api/v1/sales/dashboard`
- auth: `ROLE_ADMIN|ROLE_SALES|ROLE_FACTORY|ROLE_ACCOUNTING`
- response (`SalesDashboardDto`):
  - `activeDealers`
  - `totalOrders`
  - `orderStatusBuckets`:
    - `open`
    - `in_progress`
    - `dispatched`
    - `completed`
    - `cancelled`
    - `other`
  - `pendingCreditRequests`

## Dealer APIs

- `POST /api/v1/dealers`
- `GET /api/v1/dealers`
- `GET /api/v1/dealers/search`
- `PUT /api/v1/dealers/{dealerId}`
- `GET /api/v1/dealers/{dealerId}/ledger`
- `GET /api/v1/dealers/{dealerId}/invoices`
- `GET /api/v1/dealers/{dealerId}/aging`
- `POST /api/v1/dealers/{dealerId}/dunning/hold`

## Sales Order APIs

- `GET /api/v1/sales/orders`
- `POST /api/v1/sales/orders`
- `PUT /api/v1/sales/orders/{id}`
- `DELETE /api/v1/sales/orders/{id}`
- `POST /api/v1/sales/orders/{id}/confirm`
- `POST /api/v1/sales/orders/{id}/cancel`
- `PATCH /api/v1/sales/orders/{id}/status`

Order request contract (`SalesOrderRequest`):
- `dealerId`, `totalAmount`, `currency`, `notes`, `items[]`, `gstTreatment`, `gstRate`, `gstInclusive`, `idempotencyKey`, `paymentMode`.

## Credit Request APIs

- `GET /api/v1/sales/credit-requests`
- `POST /api/v1/sales/credit-requests`
- `PUT /api/v1/sales/credit-requests/{id}`
- `POST /api/v1/sales/credit-requests/{id}/approve` (admin/accounting)
- `POST /api/v1/sales/credit-requests/{id}/reject` (admin/accounting)

Approval contract:
- Requires pending request.
- Requires non-empty decision reason.
- Requires linked dealer.
- Locks dealer row and increments `dealer.creditLimit` by `amountRequested`.
- Writes audit metadata (`oldLimit`, `newLimit`, `increment`).

## Promotion APIs

- `GET /api/v1/sales/promotions`
- `POST /api/v1/sales/promotions`
- `PUT /api/v1/sales/promotions/{id}`
- `DELETE /api/v1/sales/promotions/{id}`

Promotion request contract (`PromotionRequest`):
- `name`, `description`, `imageUrl` (optional, max 1024), `discountType`, `discountValue`, `startDate`, `endDate`, `status`.

DB contract:
- `promotions.image_url` added by `db/migration_v2/V22__promotions_image_url.sql`.

## Dispatch/Finance Boundary APIs

- `POST /api/v1/sales/dispatch/confirm`
  - auth: `ROLE_FACTORY|ROLE_ADMIN` + `dispatch.confirm`
  - purpose: final dispatch confirmation and downstream invoice/accounting linkage.

- `POST /api/v1/sales/dispatch/reconcile-order-markers`
  - auth: `ROLE_ACCOUNTING|ROLE_ADMIN` + `dispatch.confirm`

## Workflow Summary

1. Sales creates order (proforma intent).
2. Inventory/factory prepares packaging slip.
3. Factory confirms dispatch via sales dispatch endpoint.
4. Dispatch finalizes invoice/AR/COGS and order markers.
5. Accounting consumes finalized postings.

## Invariants

- Tenant isolation must be enforced by company context on every request.
- Credit approvals are fail-closed (no dealer => no approval).
- Dispatch endpoint remains factory/admin-only.
- Sales dashboard is aggregation-only; no state mutation.

## Related Docs

- `erp-domain/docs/ORDER_TO_CASH_STATE_MACHINES.md`
- `erp-domain/docs/MODULE_FLOW_MAP.md`
- `tickets/TKT-ERP-STAGE-116/IMPLEMENTATION_NOTES.md`
