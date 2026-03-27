# Purchasing Module Overview

## Purpose

The Purchasing module implements the complete **Procure-to-Pay (P2P)** workflow for BigBright ERP, managing supplier relationships, purchase orders, goods receipts, purchase invoices, and purchase returns. It provides full integration with the Inventory and Accounting modules.

## Location

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/`

## Architecture

```
purchasing/
├── controller/          # REST API endpoints
├── service/             # Business logic layer
├── domain/              # Entities, enums, and repositories
└── dto/                 # Request/Response data transfer objects
```

## Module Statistics

| Stereotype       | Count |
|------------------|-------|
| Controllers      | 3     |
| Services         | 11    |
| Entities         | 7     |
| Enums            | 3     |
| Repositories     | 5     |
| DTOs             | 17    |
| **Total Java Files** | **48** |

## Core Components

### Controllers (3)
- `PurchasingWorkflowController` - Main P2P workflow orchestration
- `RawMaterialPurchaseController` - Purchase invoice and return operations
- `SupplierController` - Supplier master data management

### Services (11)
- `PurchasingService` - Facade for all purchasing operations
- `PurchaseOrderService` - PO lifecycle management
- `GoodsReceiptService` - Goods receipt processing
- `SupplierService` - Supplier CRUD and lifecycle
- `PurchaseInvoiceService` - Invoice processing facade
- `PurchaseInvoiceEngine` - Invoice posting with journal integration
- `PurchaseReturnService` - Purchase return processing
- `PurchaseReturnAllocationService` - Return quantity allocation
- `PurchaseResponseMapper` - Entity to DTO mapping
- `PurchaseTaxPolicy` - GST/tax calculation rules
- `SupplierApprovalPolicy` - Supplier approval validation

### Domain Entities (7)
- `Supplier` - Supplier master data
- `PurchaseOrder` - Purchase order header
- `PurchaseOrderLine` - Purchase order line items
- `PurchaseOrderStatusHistory` - PO status change audit trail
- `GoodsReceipt` - Goods receipt header
- `GoodsReceiptLine` - Goods receipt line items
- `RawMaterialPurchase` - Purchase invoice header
- `RawMaterialPurchaseLine` - Purchase invoice line items

### Enums (3)
- `SupplierStatus` - PENDING, APPROVED, ACTIVE, SUSPENDED
- `SupplierPaymentTerms` - NET_30, NET_60, NET_90
- `PurchaseOrderStatus` - DRAFT, APPROVED, PARTIALLY_RECEIVED, FULLY_RECEIVED, INVOICED, CLOSED, VOID
- `GoodsReceiptStatus` - PARTIAL, RECEIVED, INVOICED

## Key Business Rules

### Supplier Lifecycle
1. **PENDING** → Supplier created, awaiting approval
2. **APPROVED** → Approved but not yet active
3. **ACTIVE** → Can transact (create POs, post invoices)
4. **SUSPENDED** → Temporarily blocked from transactions

### Purchase Order Status Transitions
```
DRAFT → APPROVED → PARTIALLY_RECEIVED → FULLY_RECEIVED → INVOICED → CLOSED
                    ↓                    ↓                ↓
                   VOID                 VOID             VOID
```

### Goods Receipt Requirements
- Requires an approved or partially-received purchase order
- Cannot exceed ordered quantities
- Idempotency key mandatory
- Automatically creates inventory batches

### Purchase Invoice Requirements
- Requires a goods receipt
- Quantities must match goods receipt exactly
- Posts journal entry to accounting
- Supports GST (CGST/SGST/IGST) or non-GST materials

## Cross-Module Integration

| Module     | Integration Points                                    |
|------------|------------------------------------------------------|
| Inventory  | RawMaterial, RawMaterialBatch, RawMaterialMovement   |
| Accounting | JournalEntry, Account, GstService, AccountingFacade  |
| Company    | Company, CompanyContextService                       |

## Security

All endpoints require authentication with specific role authorities:
- `ROLE_ADMIN` - Full access
- `ROLE_ACCOUNTING` - Purchasing operations
- `ROLE_FACTORY` - Read-only supplier access

## API Base Paths

| Controller                      | Base Path                           |
|---------------------------------|-------------------------------------|
| PurchasingWorkflowController    | `/api/v1/purchasing`                |
| RawMaterialPurchaseController   | `/api/v1/purchasing/raw-material-purchases` |
| SupplierController              | `/api/v1/suppliers`                 |

## Related Documentation

- [Controllers](./CONTROLLERS.md)
- [Services](./SERVICES.md)
- [Entities](./ENTITIES.md)
- [DTOs](./DTOS.md)
- [Canonical Flows (P2P)](./CANONICAL_FLOWS.md)
