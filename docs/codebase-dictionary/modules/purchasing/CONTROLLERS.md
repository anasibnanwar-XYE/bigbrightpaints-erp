# Purchasing Module Controllers

## Overview

Three REST controllers expose endpoints for the Purchasing module. All controllers are secured with `@PreAuthorize` annotations enforcing role-based access control.

---

## PurchasingWorkflowController

**File:** `controller/PurchasingWorkflowController.java`  
**Base Path:** `/api/v1/purchasing`

Orchestrates the core P2P workflow: purchase orders and goods receipts.

### Endpoints

| Method | HTTP | Path | Description |
|--------|------|------|-------------|
| `listPurchaseOrders` | GET | `/purchase-orders` | List purchase orders (optional supplierId filter) |
| `getPurchaseOrder` | GET | `/purchase-orders/{id}` | Get single purchase order |
| `createPurchaseOrder` | POST | `/purchase-orders` | Create new purchase order |
| `approvePurchaseOrder` | POST | `/purchase-orders/{id}/approve` | Approve purchase order |
| `voidPurchaseOrder` | POST | `/purchase-orders/{id}/void` | Void purchase order |
| `closePurchaseOrder` | POST | `/purchase-orders/{id}/close` | Close purchase order |
| `purchaseOrderTimeline` | GET | `/purchase-orders/{id}/timeline` | Get status history timeline |
| `listGoodsReceipts` | GET | `/goods-receipts` | List goods receipts (optional supplierId filter) |
| `getGoodsReceipt` | GET | `/goods-receipts/{id}` | Get single goods receipt |
| `createGoodsReceipt` | POST | `/goods-receipts` | Create goods receipt (requires Idempotency-Key header) |

### Authorization
- `ROLE_ADMIN` or `ROLE_ACCOUNTING` required for all endpoints

### Dependencies
- `PurchasingService` - delegates all operations

### Idempotency
Goods receipt creation requires an `Idempotency-Key` header. The controller validates and normalizes the key before delegation.

---

## RawMaterialPurchaseController

**File:** `controller/RawMaterialPurchaseController.java`  
**Base Path:** `/api/v1/purchasing/raw-material-purchases`

Manages purchase invoices (invoicing of goods receipts) and purchase returns.

### Endpoints

| Method | HTTP | Path | Description |
|--------|------|------|-------------|
| `listPurchases` | GET | `/` | List purchase invoices (optional supplierId filter) |
| `getPurchase` | GET | `/{id}` | Get single purchase invoice |
| `createPurchase` | POST | `/` | Create purchase invoice from goods receipt |
| `recordPurchaseReturn` | POST | `/returns` | Record a purchase return (creates reversal journal) |
| `previewPurchaseReturn` | POST | `/returns/preview` | Preview return without executing |

### Authorization
- `ROLE_ADMIN` or `ROLE_ACCOUNTING` required for all endpoints

### Dependencies
- `PurchasingService` - delegates all operations

### Response Types
- `RawMaterialPurchaseResponse` - Purchase invoice data
- `JournalEntryDto` - Accounting journal entry (from returns)
- `PurchaseReturnPreviewDto` - Return preview data

---

## SupplierController

**File:** `controller/SupplierController.java`  
**Base Path:** `/api/v1/suppliers`

Manages supplier master data including lifecycle state transitions.

### Endpoints

| Method | HTTP | Path | Description |
|--------|------|------|-------------|
| `listSuppliers` | GET | `/` | List all suppliers |
| `getSupplier` | GET | `/{id}` | Get single supplier |
| `createSupplier` | POST | `/` | Create new supplier |
| `updateSupplier` | PUT | `/{id}` | Update supplier details |
| `approveSupplier` | POST | `/{id}/approve` | Approve pending supplier |
| `activateSupplier` | POST | `/{id}/activate` | Activate approved/suspended supplier |
| `suspendSupplier` | POST | `/{id}/suspend` | Suspend active supplier |

### Authorization
- `ROLE_ADMIN` or `ROLE_ACCOUNTING` required for write operations
- `ROLE_FACTORY` also has read access

### Dependencies
- `SupplierService` - delegates all operations

### Supplier Lifecycle States
```
PENDING → APPROVED → ACTIVE ↔ SUSPENDED
```

---

## Security Contract

All controllers enforce role-based access:

| Role | Permissions |
|------|-------------|
| `ROLE_ADMIN` | Full access to all purchasing endpoints |
| `ROLE_ACCOUNTING` | Full access to all purchasing endpoints |
| `ROLE_FACTORY` | Read-only access to suppliers |

---

## Common Patterns

### Request Validation
All POST/PUT endpoints use `@Valid` annotation with Jakarta Bean Validation on request DTOs.

### Response Wrapping
All responses are wrapped in `ApiResponse<T>` providing consistent structure:
```java
ApiResponse.success("message", data)
```

### Error Handling
Controllers delegate to services which throw `ApplicationException` with appropriate `ErrorCode` for business rule violations.
