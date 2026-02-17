# Dealer Portal Endpoint Map (V1)

Source of truth:
- `openapi.json`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerPortalController.java`

Portal: `DEALER`  
Scope: dealer self-service only (own orders, invoices, aging, ledger, credit request)

## 1) Core Dealer-Self Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/dealer-portal/dashboard` | dealer summary card data |
| GET | `/api/v1/dealer-portal/orders` | own order list |
| GET | `/api/v1/dealer-portal/invoices` | own invoice list |
| GET | `/api/v1/dealer-portal/invoices/{invoiceId}/pdf` | own invoice PDF |
| GET | `/api/v1/dealer-portal/aging` | own aging buckets |
| GET | `/api/v1/dealer-portal/ledger` | own ledger/running balance |
| POST | `/api/v1/dealer-portal/credit-requests` | own credit-limit request |

## 2) Security and Scope Contract
- Dealer portal requires `ROLE_DEALER`.
- Data is auto-scoped to authenticated dealer identity.
- No dealer should ever access another dealer's records; treat any leakage as P0 blocker.

## 3) Request Contracts
- Credit request payload:
  - `amountRequested` required and positive
  - `reason` optional but recommended in UI
- Invoice PDF download must remain dealer-scoped and binary-safe.

## 4) UX Contracts
- Navigation should be task-first: dashboard, orders, invoices, ledger, credit request.
- Keep forms minimal and action-oriented; dealer users are external and non-technical.
- Surface clear denial messaging when action exceeds dealer scope.
- For aging/ledger data, show read-only integrity badge to indicate backend-calculated values.

## 5) Error Contract
- `401`: session expired -> redirect login.
- `403`: dealer scope/authority violation.
- `404`: record not found within dealer scope.
- `409`: write conflict (rare; mostly credit request retries).

## 6) Frontend Implementation Checklist
- Never pass dealer IDs from UI for dealer-portal endpoints; backend derives dealer context.
- Use explicit loading/error states for financial views (aging/ledger).
- Keep invoice PDF fetch isolated from generic JSON fetch wrappers.
- Add session-timeout recovery and re-auth flow for portal continuity.
