# Sales Module - Controllers

## Overview

The Sales module exposes 5 REST controllers providing endpoints for sales order management, dealer management, dealer portal access, and credit limit workflows.

---

## SalesController

**Package:** `com.bigbrightpaints.erp.modules.sales.controller`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/SalesController.java`
**Base Path:** `/api/v1`

Primary REST controller for sales orders, promotions, targets, and dispatch confirmation.

### Dependencies

| Service | Purpose |
|---------|---------|
| `SalesService` | Orchestrates sales operations |
| `SalesOrderCrudService` | CRUD operations for sales orders |
| `SalesOrderLifecycleService` | Order status transitions |
| `SalesDispatchReconciliationService` | Dispatch confirmation |
| `SalesDashboardService` | Dashboard metrics |
| `DealerService` | Dealer operations |
| `FinishedGoodsService` | Inventory queries |

### Endpoints

#### Dealer Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/sales/dealers` | `ADMIN_SALES_ACCOUNTING` | List all dealers |
| GET | `/sales/dealers/search` | `ADMIN_SALES_ACCOUNTING` | Search dealers with filters |

#### Sales Order Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/sales/orders` | `ADMIN_SALES_FACTORY_ACCOUNTING` | List orders with pagination |
| GET | `/sales/orders/search` | `ADMIN_SALES_FACTORY_ACCOUNTING` | Search orders with filters |
| GET | `/sales/dashboard` | `ADMIN_SALES_FACTORY_ACCOUNTING` | Get sales dashboard metrics |
| POST | `/sales/orders` | `ROLE_SALES, ROLE_ADMIN` | Create sales order (supports idempotency) |
| PUT | `/sales/orders/{id}` | `ROLE_SALES, ROLE_ADMIN` | Update sales order |
| DELETE | `/sales/orders/{id}` | `ROLE_SALES, ROLE_ADMIN` | Delete sales order |
| POST | `/sales/orders/{id}/confirm` | `ROLE_SALES, ROLE_ADMIN` | Confirm order |
| POST | `/sales/orders/{id}/cancel` | `ROLE_SALES, ROLE_ADMIN` | Cancel order |
| PATCH | `/sales/orders/{id}/status` | `ROLE_SALES, ROLE_ADMIN` | Update order status |
| GET | `/sales/orders/{id}/timeline` | `ADMIN_SALES_FACTORY_ACCOUNTING` | Get order status history |

#### Promotion Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/sales/promotions` | `ROLE_ADMIN, ROLE_SALES` | List promotions |
| POST | `/sales/promotions` | `ROLE_SALES, ROLE_ADMIN` | Create promotion |
| PUT | `/sales/promotions/{id}` | `ROLE_SALES, ROLE_ADMIN` | Update promotion |
| DELETE | `/sales/promotions/{id}` | `ROLE_SALES, ROLE_ADMIN` | Delete promotion |

#### Sales Target Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/sales/targets` | `ROLE_ADMIN, ROLE_SALES` | List sales targets |
| POST | `/sales/targets` | `ROLE_ADMIN` | Create sales target |
| PUT | `/sales/targets/{id}` | `ROLE_ADMIN` | Update sales target |
| DELETE | `/sales/targets/{id}` | `ROLE_ADMIN` | Delete sales target |

#### Dispatch Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| POST | `/sales/dispatch/confirm` | `FINANCIAL_DISPATCH` | Confirm dispatch (invoice + AR) |
| POST | `/sales/dispatch/reconcile-order-markers` | `FINANCIAL_DISPATCH` | Reconcile stale order markers |

### Idempotency Support

Order creation supports idempotency via headers:
- `Idempotency-Key` - Primary header
- `X-Idempotency-Key` - Legacy header (deprecated)

---

## DealerController

**Package:** `com.bigbrightpaints.erp.modules.sales.controller`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerController.java`
**Base Path:** `/api/v1/dealers`

Manages dealer CRUD, ledger, invoices, credit utilization, aging, and dunning.

### Dependencies

| Service | Purpose |
|---------|---------|
| `DealerService` | Dealer operations |
| `DunningService` | Dunning automation |
| `DealerPortalService` | Portal data aggregation |

### Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| POST | `/` | `ADMIN_SALES_ACCOUNTING` | Create dealer |
| GET | `/` | `ADMIN_SALES_ACCOUNTING` | List all dealers |
| GET | `/search` | `ADMIN_SALES_ACCOUNTING` | Search dealers with filters |
| PUT | `/{dealerId}` | `ADMIN_SALES_ACCOUNTING` | Update dealer |
| GET | `/{dealerId}/ledger` | `ADMIN_SALES_ACCOUNTING` | Get dealer ledger |
| GET | `/{dealerId}/invoices` | `ADMIN_SALES_ACCOUNTING` | Get dealer invoices |
| GET | `/{dealerId}/credit-utilization` | `ADMIN_SALES_ACCOUNTING` | Get credit utilization |
| GET | `/{dealerId}/aging` | `ADMIN_SALES_ACCOUNTING` | Get aging summary |
| POST | `/{dealerId}/dunning/hold` | `ADMIN_SALES_ACCOUNTING` | Evaluate dunning hold |

---

## DealerPortalController

**Package:** `com.bigbrightpaints.erp.modules.sales.controller`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerPortalController.java`
**Base Path:** `/api/v1/dealer-portal`

Provides dealer-scoped endpoints for authenticated dealer users to view their own data.

### Security

All endpoints require `ROLE_DEALER`. Data is automatically scoped to the logged-in dealer.

### Dependencies

| Service | Purpose |
|---------|---------|
| `DealerPortalService` | Portal data aggregation |
| `CreditLimitRequestService` | Credit limit requests |
| `AuditService` | Audit logging |

### Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/dashboard` | `ROLE_DEALER` | Get dealer dashboard |
| GET | `/ledger` | `ROLE_DEALER` | Get my ledger |
| GET | `/invoices` | `ROLE_DEALER` | Get my invoices |
| GET | `/aging` | `ROLE_DEALER` | Get my aging/outstanding |
| GET | `/orders` | `ROLE_DEALER` | Get my orders |
| POST | `/credit-limit-requests` | `ROLE_DEALER` | Submit credit limit request |
| GET | `/invoices/{invoiceId}/pdf` | `ROLE_DEALER` | Download invoice PDF |

### Key Features

- Automatic dealer scoping via authentication
- Invoice PDF generation with audit trail
- Credit limit increase requests from dealers

---

## CreditLimitRequestController

**Package:** `com.bigbrightpaints.erp.modules.sales.controller`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/CreditLimitRequestController.java`
**Base Path:** `/api/v1/credit/limit-requests`

Manages dealer credit limit increase requests (permanent credit limit changes).

### Dependencies

| Service | Purpose |
|---------|---------|
| `CreditLimitRequestService` | Credit request processing |

### Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| GET | `/` | `ROLE_ADMIN, ROLE_SALES` | List all credit limit requests |
| POST | `/` | `ROLE_SALES, ROLE_ADMIN` | Create credit limit request |
| POST | `/{id}/approve` | `ADMIN_OR_ACCOUNTING` | Approve request |
| POST | `/{id}/reject` | `ADMIN_OR_ACCOUNTING` | Reject request |

### Workflow

1. Sales creates request with dealer ID, amount, and reason
2. Admin/Accounting reviews and approves/rejects
3. On approval: Dealer's credit limit is increased
4. All decisions are audit logged

---

## CreditLimitOverrideController

**Package:** `com.bigbrightpaints.erp.modules.sales.controller`
**File:** `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/CreditLimitOverrideController.java`
**Base Path:** `/api/v1/credit/override-requests`

Manages one-time credit limit overrides for dispatch (temporary exceptions).

### Dependencies

| Service | Purpose |
|---------|---------|
| `CreditLimitOverrideService` | Override request processing |

### Endpoints

| Method | Path | Authority | Description |
|--------|------|-----------|-------------|
| POST | `/` | `ADMIN_FACTORY_SALES` | Create override request |
| GET | `/` | `ADMIN_OR_ACCOUNTING` | List override requests |
| POST | `/{id}/approve` | `ADMIN_OR_ACCOUNTING` | Approve override |
| POST | `/{id}/reject` | `ADMIN_OR_ACCOUNTING` | Reject override |

### Workflow

1. System creates override request when dispatch exceeds credit limit
2. Admin/Accounting approves with optional expiration
3. Maker-checker enforced: requester cannot approve own request
4. On approval: Dispatch proceeds with temporary headroom

### Key Constraints

- Maker-checker boundary enforced
- Expiration time can be set (defaults to 24 hours)
- Linked to packaging slip or sales order
