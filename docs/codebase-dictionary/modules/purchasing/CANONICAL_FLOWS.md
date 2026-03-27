# Purchasing Module - Procure-to-Pay (P2P) Flow

## Overview

This document describes the canonical **Procure-to-Pay (P2P)** flow implemented in the purchasing module, covering the complete lifecycle from supplier onboarding to purchase invoice posting.

---

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          PROCUREMENT-TO-PAY FLOW                             │
└─────────────────────────────────────────────────────────────────────────────┘

    ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
    │ PENDING  │────▶│ APPROVED │────▶│  ACTIVE  │◀───▶│SUSPENDED │
    └──────────┘     └──────────┘     └──────────┘     └──────────┘
         │                │
         │                │ Supplier Onboarding
         ▼                ▼
    ┌──────────┐     ┌──────────┐
    │  Create  │     │ Approve  │
    │ Supplier │     │ Supplier │
    └──────────┘     └──────────┘

                           │
                           ▼
    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │   DRAFT  │────▶│ APPROVED │────▶│PARTIALLY │
    │          │     │    PO    │     │ RECEIVED │
    └──────────┘     └──────────┘     └──────────┘
         │                │                │
         │ void           │ void           │
         ▼                ▼                ▼
    ┌──────────┐     ┌──────────┐     ┌──────────┐
    │   VOID   │     │   VOID   │     │  FULLY   │
    │          │     │          │     │ RECEIVED │
    └──────────┘     └──────────┘     └──────────┘
                                             │
                                             ▼
                                        ┌──────────┐
                                        │ INVOICED │
                                        └──────────┘
                                             │
                                             ▼
                                        ┌──────────┐
                                        │  CLOSED  │
                                        └──────────┘
```

---

## Stage 1: Supplier Onboarding

### Flow
```
POST /api/v1/suppliers → PENDING
POST /api/v1/suppliers/{id}/approve → APPROVED
POST /api/v1/suppliers/{id}/activate → ACTIVE
```

### Service Layer
- **Service:** `SupplierService`
- **Entity:** `Supplier`

### Business Rules
| Rule | Description |
|------|-------------|
| GSTIN Validation | GST number must match pattern `^[0-9]{2}[A-Z0-9]{13}$` |
| State Code | Must be exactly 2 characters |
| Code Generation | Auto-generated from name if not provided |
| Payable Account | Auto-created as `AP-{code}` |
| Bank Details | Encrypted using `CryptoService` |
| Transactional Check | Only ACTIVE suppliers can be used in POs and invoices |

### Entity State Transitions
```
PENDING → APPROVED → ACTIVE ↔ SUSPENDED
```

---

## Stage 2: Purchase Order Creation

### Flow
```
POST /api/v1/purchasing/purchase-orders → DRAFT
```

### Request Payload
```json
{
  "supplierId": 1,
  "orderNumber": "PO-2024-001",
  "orderDate": "2024-01-15",
  "memo": "Urgent order for paint production",
  "lines": [
    {
      "rawMaterialId": 10,
      "quantity": 100.00,
      "unit": "KG",
      "costPerUnit": 50.00,
      "notes": "Primary pigment"
    }
  ]
}
```

### Service Layer
- **Controller:** `PurchasingWorkflowController`
- **Service:** `PurchasingService` → `PurchaseOrderService`
- **Entity:** `PurchaseOrder`, `PurchaseOrderLine`

### Business Rules
| Rule | Description |
|------|-------------|
| Supplier Status | Must be ACTIVE (transactional) |
| Order Number | Unique per company (case-insensitive) |
| Line Materials | No duplicate raw materials in single PO |
| Quantities | Must be positive |
| Costs | Must be positive |

### Status History
Automatically records `PURCHASE_ORDER_CREATED` with actor identity.

---

## Stage 3: Purchase Order Approval

### Flow
```
POST /api/v1/purchasing/purchase-orders/{id}/approve → APPROVED
```

### Service Layer
- **Service:** `PurchaseOrderService.approvePurchaseOrder()`

### Status Transition
```
DRAFT → APPROVED
```

### Status History
Records `PURCHASE_ORDER_APPROVED` with actor identity.

### Alternative: Void
```
POST /api/v1/purchasing/purchase-orders/{id}/void
{
  "reasonCode": "CANCELLED_BY_REQUEST",
  "reason": "Customer cancelled order"
}
```

---

## Stage 4: Goods Receipt

### Flow
```
POST /api/v1/purchasing/goods-receipts
Headers: Idempotency-Key: <unique-key>
```

### Request Payload
```json
{
  "purchaseOrderId": 1,
  "receiptNumber": "GR-2024-001",
  "receiptDate": "2024-01-20",
  "memo": "Partial delivery received",
  "idempotencyKey": "uuid-or-client-generated-key",
  "lines": [
    {
      "rawMaterialId": 10,
      "batchCode": "BATCH-001",
      "quantity": 50.00,
      "unit": "KG",
      "costPerUnit": 50.00,
      "manufacturingDate": "2024-01-01",
      "expiryDate": "2025-01-01",
      "notes": "First batch"
    }
  ]
}
```

### Service Layer
- **Controller:** `PurchasingWorkflowController`
- **Service:** `PurchasingService` → `GoodsReceiptService`
- **Entity:** `GoodsReceipt`, `GoodsReceiptLine`

### Business Rules
| Rule | Description |
|------|-------------|
| PO Status | Must be APPROVED or PARTIALLY_RECEIVED |
| Receipt Number | Unique per company |
| Idempotency Key | **MANDATORY** - prevents duplicate receipts |
| Quantity Validation | Cannot exceed remaining ordered quantity |
| Unit Matching | Must match purchase order line unit |
| Accounting Period | Must be open for receipt date |

### Side Effects
1. Creates `RawMaterialBatch` for each line
2. Creates `RawMaterialMovement` for inventory tracking
3. Updates PO status to PARTIALLY_RECEIVED or FULLY_RECEIVED

### Idempotency Mechanism
```
1. Normalize idempotency key
2. Check for existing receipt with same key
3. If exists, validate payload hash matches
4. If hash mismatch → CONCURRENCY_CONFLICT error
5. If hash matches → return existing receipt
6. On concurrent insert, re-query and return
```

---

## Stage 5: Purchase Invoice

### Flow
```
POST /api/v1/purchasing/raw-material-purchases
```

### Request Payload
```json
{
  "supplierId": 1,
  "invoiceNumber": "INV-2024-001",
  "invoiceDate": "2024-01-25",
  "memo": "Invoice for GR-2024-001",
  "purchaseOrderId": 1,
  "goodsReceiptId": 1,
  "taxAmount": 450.00,
  "lines": [
    {
      "rawMaterialId": 10,
      "batchCode": "BATCH-001",
      "quantity": 50.00,
      "unit": "KG",
      "costPerUnit": 50.00,
      "taxRate": 18.00,
      "taxInclusive": false,
      "notes": "Tax exclusive line"
    }
  ]
}
```

### Service Layer
- **Controller:** `RawMaterialPurchaseController`
- **Service:** `PurchasingService` → `PurchaseInvoiceService` → `PurchaseInvoiceEngine`
- **Entity:** `RawMaterialPurchase`, `RawMaterialPurchaseLine`

### Invoice Creation Process
```
1. Lock supplier (validate ACTIVE)
2. Lock goods receipt (validate not already invoiced)
3. Validate invoice lines match goods receipt exactly
   - Same materials
   - Same quantities
   - Same units
   - Matching costs (within tolerance)
4. Resolve tax mode (GST vs NON_GST)
5. Calculate line taxes
6. Split GST into CGST/SGST (intra-state) or IGST (inter-state)
7. POST JOURNAL ENTRY FIRST (fail-fast principle)
8. Create RawMaterialPurchase entity
9. Link journal entry to purchase
10. Link goods receipt movements to journal
11. Update goods receipt status → INVOICED
12. Update purchase order status → INVOICED/CLOSED
```

### Tax Calculation Rules
| Scenario | Treatment |
|----------|-----------|
| Same state | CGST + SGST (50/50 split) |
| Different state | IGST (100%) |
| Non-GST material | No tax |
| Tax-inclusive | Reverse calculate net from gross |
| Mixed GST/Non-GST | **NOT ALLOWED** - single mode per invoice |

### Journal Entry Structure
```
Dr. Inventory Account (Raw Material)     ₹2,500
Dr. GST Credit (CGST/SGST/IGST)            ₹450
    Cr. Accounts Payable (Supplier)              ₹2,950
```

---

## Stage 6: Purchase Return (Optional)

### Flow
```
POST /api/v1/purchasing/raw-material-purchases/returns/preview
POST /api/v1/purchasing/raw-material-purchases/returns
```

### Request Payload
```json
{
  "supplierId": 1,
  "purchaseId": 1,
  "rawMaterialId": 10,
  "quantity": 10.00,
  "unitCost": 50.00,
  "referenceNumber": "RET-2024-001",
  "returnDate": "2024-02-01",
  "reason": "Defective material"
}
```

### Service Layer
- **Service:** `PurchaseReturnService`
- **Supporting:** `PurchaseReturnAllocationService`

### Return Process
```
1. Validate purchase is posted (has journal entry)
2. Calculate remaining returnable quantity
3. Validate return quantity ≤ remaining
4. Calculate proportional tax credit reversal
5. Post reversal journal entry
6. Link as correction to original journal
7. Deduct stock from raw material
8. Deduct from batches (FIFO)
9. Create return movement records
10. Update purchase line returned quantities
11. Reduce purchase outstanding amount
12. Update purchase status if fully returned
```

### Journal Entry Structure (Return)
```
Dr. Accounts Payable (Supplier)          ₹590
    Cr. Inventory Account                     ₹500
    Cr. GST Credit (Reversal)                 ₹90
```

---

## Status Transition Matrix

### Purchase Order
| From | To |
|------|-----|
| DRAFT | APPROVED, VOID |
| APPROVED | PARTIALLY_RECEIVED, FULLY_RECEIVED, VOID |
| PARTIALLY_RECEIVED | FULLY_RECEIVED |
| FULLY_RECEIVED | INVOICED |
| INVOICED | CLOSED |
| CLOSED | *(terminal)* |
| VOID | *(terminal)* |

### Goods Receipt
| From | To |
|------|-----|
| PARTIAL | RECEIVED, INVOICED |
| RECEIVED | INVOICED |
| INVOICED | *(terminal)* |

### Supplier
| From | To |
|------|-----|
| PENDING | APPROVED |
| APPROVED | ACTIVE |
| ACTIVE | SUSPENDED |
| SUSPENDED | ACTIVE |

---

## API Endpoints Summary

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/suppliers` | GET | List suppliers |
| `/api/v1/suppliers` | POST | Create supplier |
| `/api/v1/suppliers/{id}` | PUT | Update supplier |
| `/api/v1/suppliers/{id}/approve` | POST | Approve supplier |
| `/api/v1/suppliers/{id}/activate` | POST | Activate supplier |
| `/api/v1/suppliers/{id}/suspend` | POST | Suspend supplier |
| `/api/v1/purchasing/purchase-orders` | GET | List POs |
| `/api/v1/purchasing/purchase-orders` | POST | Create PO |
| `/api/v1/purchasing/purchase-orders/{id}` | GET | Get PO |
| `/api/v1/purchasing/purchase-orders/{id}/approve` | POST | Approve PO |
| `/api/v1/purchasing/purchase-orders/{id}/void` | POST | Void PO |
| `/api/v1/purchasing/purchase-orders/{id}/close` | POST | Close PO |
| `/api/v1/purchasing/purchase-orders/{id}/timeline` | GET | Get status history |
| `/api/v1/purchasing/goods-receipts` | GET | List receipts |
| `/api/v1/purchasing/goods-receipts` | POST | Create receipt |
| `/api/v1/purchasing/goods-receipts/{id}` | GET | Get receipt |
| `/api/v1/purchasing/raw-material-purchases` | GET | List invoices |
| `/api/v1/purchasing/raw-material-purchases` | POST | Create invoice |
| `/api/v1/purchasing/raw-material-purchases/{id}` | GET | Get invoice |
| `/api/v1/purchasing/raw-material-purchases/returns` | POST | Record return |
| `/api/v1/purchasing/raw-material-purchases/returns/preview` | POST | Preview return |

---

## Cross-Module Integration

| Module | Integration Point | Purpose |
|--------|------------------|---------|
| Inventory | `RawMaterial` | Material master data |
| Inventory | `RawMaterialBatch` | Batch tracking |
| Inventory | `RawMaterialMovement` | Stock movements |
| Inventory | `RawMaterialService` | Receipt processing |
| Accounting | `JournalEntry` | AP posting |
| Accounting | `Account` | Payable accounts |
| Accounting | `AccountingFacade` | Journal creation |
| Accounting | `GstService` | Tax breakdown |
| Accounting | `SupplierLedgerService` | Balance tracking |
| Company | `Company` | Multi-tenancy |
| Company | `CompanyContextService` | Current company |

---

## Error Handling

| ErrorCode | Description |
|-----------|-------------|
| `VALIDATION_INVALID_INPUT` | Invalid request data |
| `VALIDATION_MISSING_REQUIRED_FIELD` | Missing required field |
| `BUSINESS_INVALID_STATE` | Invalid state for operation |
| `BUSINESS_CONSTRAINT_VIOLATION` | Business rule violation |
| `CONCURRENCY_CONFLICT` | Idempotency conflict |
| `VALIDATION_INVALID_REFERENCE` | Invalid entity reference |
| `RETURN_EXCEEDS_OUTSTANDING` | Return amount exceeds balance |
