# Purchasing Module Services

## Overview

The Purchasing module contains 11 services implementing the P2P workflow. Services follow a layered architecture with `PurchasingService` as the facade.

---

## Service Hierarchy

```
PurchasingService (Facade)
├── PurchaseOrderService
├── GoodsReceiptService
├── PurchaseInvoiceService
│   └── PurchaseInvoiceEngine
├── PurchaseReturnService
│   └── PurchaseReturnAllocationService
├── SupplierService
├── PurchaseResponseMapper
├── PurchaseTaxPolicy
└── SupplierApprovalPolicy
```

---

## PurchasingService (Facade)

**File:** `service/PurchasingService.java`

Primary facade that coordinates all purchasing operations. Exposes a unified API for controllers.

### Dependencies
- `PurchaseOrderService`
- `GoodsReceiptService`
- `PurchaseInvoiceService`
- `PurchaseReturnService`

### Public Methods

| Method | Description |
|--------|-------------|
| `listPurchases()` / `listPurchases(Long supplierId)` | List purchase invoices |
| `listPurchaseOrders()` / `listPurchaseOrders(Long supplierId)` | List purchase orders |
| `getPurchaseOrder(Long id)` | Get single purchase order |
| `createPurchaseOrder(PurchaseOrderRequest)` | Create purchase order |
| `approvePurchaseOrder(Long id)` | Approve purchase order |
| `voidPurchaseOrder(Long id, PurchaseOrderVoidRequest)` | Void purchase order |
| `closePurchaseOrder(Long id)` | Close purchase order |
| `getPurchaseOrderTimeline(Long id)` | Get status history |
| `listGoodsReceipts()` / `listGoodsReceipts(Long supplierId)` | List goods receipts |
| `getGoodsReceipt(Long id)` | Get single goods receipt |
| `createGoodsReceipt(GoodsReceiptRequest)` | Create goods receipt |
| `getPurchase(Long id)` | Get purchase invoice |
| `createPurchase(RawMaterialPurchaseRequest)` | Create purchase invoice |
| `recordPurchaseReturn(PurchaseReturnRequest)` | Execute purchase return |
| `previewPurchaseReturn(PurchaseReturnRequest)` | Preview purchase return |

---

## PurchaseOrderService

**File:** `service/PurchaseOrderService.java`

Manages the complete lifecycle of purchase orders.

### Dependencies
- `CompanyContextService`
- `PurchaseOrderRepository`
- `RawMaterialRepository`
- `CompanyEntityLookup`
- `PurchaseResponseMapper`
- `PurchaseOrderStatusHistoryRepository`

### Public Methods

| Method | Description |
|--------|-------------|
| `listPurchaseOrders()` / `listPurchaseOrders(Long supplierId)` | List POs with optional supplier filter |
| `getPurchaseOrder(Long id)` | Get single PO by ID |
| `createPurchaseOrder(PurchaseOrderRequest)` | Create PO in DRAFT status |
| `approvePurchaseOrder(Long id)` | Transition DRAFT → APPROVED |
| `voidPurchaseOrder(Long id, PurchaseOrderVoidRequest)` | Transition → VOID with reason |
| `closePurchaseOrder(Long id)` | Transition → CLOSED |
| `getPurchaseOrderTimeline(Long id)` | Get status history timeline |
| `transitionStatus(PurchaseOrder, PurchaseOrderStatus, String reasonCode, String reason)` | Generic status transition (public) |

### Status Transition Matrix

```
DRAFT → APPROVED | VOID
APPROVED → PARTIALLY_RECEIVED | FULLY_RECEIVED | VOID
PARTIALLY_RECEIVED → FULLY_RECEIVED
FULLY_RECEIVED → INVOICED
INVOICED → CLOSED
CLOSED → (terminal)
VOID → (terminal)
```

### Business Rules
- Supplier must be transactional (ACTIVE status)
- Order number must be unique per company
- No duplicate raw material lines allowed
- All line quantities and costs must be positive

---

## GoodsReceiptService

**File:** `service/GoodsReceiptService.java`

Processes goods receipts against approved purchase orders with idempotency support.

### Dependencies
- `CompanyContextService`
- `PurchaseOrderRepository`
- `GoodsReceiptRepository`
- `RawMaterialRepository`
- `RawMaterialService`
- `CompanyEntityLookup`
- `AccountingPeriodService`
- `PurchaseResponseMapper`
- `PurchaseOrderService`
- `IdempotencyReservationService`
- `TransactionTemplate`

### Public Methods

| Method | Description |
|--------|-------------|
| `listGoodsReceipts()` / `listGoodsReceipts(Long supplierId)` | List receipts with optional supplier filter |
| `getGoodsReceipt(Long id)` | Get single receipt |
| `createGoodsReceipt(GoodsReceiptRequest)` | Create receipt with idempotency |

### Business Rules
- Purchase order must be APPROVED or PARTIALLY_RECEIVED
- Receipt quantities cannot exceed remaining ordered quantities
- Receipt number must be unique per company
- Idempotency key is mandatory
- Accounting period must be open for receipt date
- Creates inventory batches automatically
- Updates PO status to PARTIALLY_RECEIVED or FULLY_RECEIVED

### Idempotency
- Uses `IdempotencyReservationService` for duplicate detection
- Stores idempotency hash for payload validation
- Handles concurrent requests with database-level conflict detection

---

## PurchaseInvoiceService

**File:** `service/PurchaseInvoiceService.java`

Thin facade for purchase invoice operations, delegating to `PurchaseInvoiceEngine`.

### Dependencies
- `PurchaseInvoiceEngine`
- `PurchaseOrderService`

### Public Methods

| Method | Description |
|--------|-------------|
| `listPurchases()` / `listPurchases(Long supplierId)` | List purchase invoices |
| `getPurchase(Long id)` | Get single invoice |
| `createPurchase(RawMaterialPurchaseRequest)` | Create invoice from goods receipt |

---

## PurchaseInvoiceEngine

**File:** `service/PurchaseInvoiceEngine.java`

Core engine for purchase invoice processing with journal integration.

### Dependencies
- `CompanyContextService`
- `RawMaterialPurchaseRepository`
- `PurchaseOrderRepository`
- `GoodsReceiptRepository`
- `RawMaterialRepository`
- `RawMaterialBatchRepository`
- `RawMaterialService`
- `RawMaterialMovementRepository`
- `AccountingFacade`
- `CompanyEntityLookup`
- `ReferenceNumberService`
- `CompanyClock`
- `GstService`
- `PurchaseResponseMapper`
- `PurchaseTaxPolicy`

### Public Methods

| Method | Description |
|--------|-------------|
| `listPurchases()` / `listPurchases(Long supplierId)` | List invoices |
| `getPurchase(Long id)` | Get single invoice |
| `createPurchase(RawMaterialPurchaseRequest)` | Create invoice and post journal |

### Invoice Creation Process
1. Lock and validate supplier (must be ACTIVE)
2. Lock goods receipt and verify not already invoiced
3. Validate invoice lines match goods receipt exactly
4. Resolve tax mode (GST vs non-GST)
5. Calculate line taxes and GST breakdown (CGST/SGST/IGST)
6. **Post journal entry first** (fail-fast for accounting errors)
7. Create purchase invoice entity with journal linkage
8. Link goods receipt movements to journal
9. Update goods receipt status to INVOICED
10. Update purchase order status to INVOICED/CLOSED

### Tax Calculation
- Supports GST (intra-state: CGST+SGST, inter-state: IGST)
- Supports non-GST materials
- Cannot mix GST and non-GST materials in single invoice
- Tax-inclusive pricing supported with reverse calculation

---

## PurchaseReturnService

**File:** `service/PurchaseReturnService.java`

Handles purchase returns with stock reversal and journal correction.

### Dependencies
- `CompanyContextService`
- `RawMaterialPurchaseRepository`
- `RawMaterialRepository`
- `RawMaterialBatchRepository`
- `RawMaterialMovementRepository`
- `AccountingFacade`
- `JournalEntryRepository`
- `CompanyEntityLookup`
- `ReferenceNumberService`
- `CompanyClock`
- `GstService`
- `PurchaseReturnAllocationService`

### Public Methods

| Method | Description |
|--------|-------------|
| `previewPurchaseReturn(PurchaseReturnRequest)` | Preview return without execution |
| `recordPurchaseReturn(PurchaseReturnReturnRequest)` | Execute return |

### Return Process
1. Validate purchase exists and is posted
2. Validate returnable quantity via allocation service
3. Calculate proportional tax credit reversal
4. Post reversal journal entry
5. Link correction journal to original purchase journal
6. Deduct stock from material and batches (FIFO)
7. Create return movement records
8. Update purchase line returned quantities
9. Update purchase outstanding amount

---

## PurchaseReturnAllocationService

**File:** `service/PurchaseReturnAllocationService.java`

Manages return quantity allocation across purchase lines.

### Public Methods

| Method | Description |
|--------|-------------|
| `remainingReturnableQuantity(RawMaterialPurchase, RawMaterial)` | Calculate remaining qty that can be returned |
| `applyPurchaseReturnQuantity(RawMaterialPurchase, RawMaterial, BigDecimal)` | Apply return to purchase lines |
| `applyPurchaseReturnToOutstanding(RawMaterialPurchase, BigDecimal)` | Reduce outstanding amount |

---

## SupplierService

**File:** `service/SupplierService.java`

Manages supplier master data with lifecycle state management.

### Dependencies
- `SupplierRepository`
- `CompanyContextService`
- `AccountRepository`
- `SupplierLedgerService`
- `CompanyEntityLookup`
- `CryptoService`

### Public Methods

| Method | Description |
|--------|-------------|
| `listSuppliers()` | List all suppliers with current balances |
| `getSupplier(Long id)` | Get single supplier with balance |
| `createSupplier(SupplierRequest)` | Create supplier in PENDING status |
| `updateSupplier(Long id, SupplierRequest)` | Update supplier details |
| `approveSupplier(Long id)` | PENDING → APPROVED |
| `activateSupplier(Long id)` | APPROVED/SUSPENDED → ACTIVE |
| `suspendSupplier(Long id)` | ACTIVE → SUSPENDED |

### Business Rules
- Code auto-generated from name if not provided
- GST number validated against GSTIN pattern (15 chars)
- State code must be exactly 2 characters
- Bank details encrypted using CryptoService
- Auto-creates payable account (AP-{code})

---

## PurchaseResponseMapper

**File:** `service/PurchaseResponseMapper.java`

Maps domain entities to response DTOs with linked reference resolution.

### Dependencies
- `RawMaterialPurchaseRepository`
- `PartnerSettlementAllocationRepository`

### Public Methods

| Method | Input | Output |
|--------|-------|--------|
| `toPurchaseOrderResponse` | `PurchaseOrder` | `PurchaseOrderResponse` |
| `toPurchaseOrderLineResponse` | `PurchaseOrderLine` | `PurchaseOrderLineResponse` |
| `toGoodsReceiptResponse` | `GoodsReceipt` | `GoodsReceiptResponse` |
| `toGoodsReceiptLineResponse` | `GoodsReceiptLine` | `GoodsReceiptLineResponse` |
| `toPurchaseResponse` | `RawMaterialPurchase` | `RawMaterialPurchaseResponse` |
| `toPurchaseLineResponse` | `RawMaterialPurchaseLine` | `RawMaterialPurchaseLineResponse` |
| `toPurchaseResponses` | `List<RawMaterialPurchase>` | `List<RawMaterialPurchaseResponse>` |
| `toGoodsReceiptResponses` | `List<GoodsReceipt>` | `List<GoodsReceiptResponse>` |

### Features
- Resolves linked purchase invoices for goods receipts
- Resolves settlement allocations for purchase invoices
- Builds `DocumentLifecycleDto` for lifecycle status
- Builds `LinkedBusinessReferenceDto` for document chains

---

## PurchaseTaxPolicy

**File:** `service/PurchaseTaxPolicy.java`

Encapsulates GST tax calculation rules for purchases.

### Public Methods

| Method | Description |
|--------|-------------|
| `resolvePurchaseTaxMode(List<RawMaterialPurchaseLineRequest>, Map<Long, RawMaterial>)` | Determine GST or NON_GST mode |
| `resolveLineTaxRateForMode(RawMaterialPurchaseLineRequest, RawMaterial, Company, PurchaseTaxMode)` | Get effective tax rate for line |
| `enforcePurchaseTaxContract(PurchaseTaxMode, BigDecimal providedTaxAmount, boolean hasTaxableLines)` | Validate tax contract |

### Tax Rules
- GST rate resolved in order: line request → raw material → company default
- Maximum GST rate: 28%
- Cannot mix GST and non-GST materials in single invoice
- Non-GST lines cannot have positive tax rate or tax-inclusive flag

---

## SupplierApprovalPolicy

**File:** `service/SupplierApprovalPolicy.java`

Validates supplier approval decisions for exception handling.

### Public Methods

| Method | Description |
|--------|-------------|
| `requireSupplierExceptionApproval(SupplierApprovalDecision)` | Validate SUPPLIER_EXCEPTION approval |
| `requireSettlementOverrideApproval(SupplierApprovalDecision)` | Validate SETTLEMENT_OVERRIDE approval |

---

## Supporting Types

### SupplierApprovalDecision (Record)

**File:** `service/SupplierApprovalDecision.java`

Immutable record capturing approval metadata.

| Field | Type | Description |
|-------|------|-------------|
| `approvalId` | String | Unique approval identifier |
| `makerUserId` | String | User who initiated request |
| `checkerUserId` | String | User who approved (must differ from maker) |
| `reasonCode` | SupplierApprovalReasonCode | Enum: SUPPLIER_EXCEPTION, SETTLEMENT_OVERRIDE |
| `approvedAt` | Instant | Approval timestamp |
| `auditMetadata` | Map<String, String> | Required: ticket, approvalSource |

### SupplierApprovalReasonCode (Enum)

**File:** `service/SupplierApprovalReasonCode.java`

- `SUPPLIER_EXCEPTION`
- `SETTLEMENT_OVERRIDE`
