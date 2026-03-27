# Purchasing Module Entities

## Overview

The Purchasing module contains 7 domain entities, 3 enums, and 5 repository interfaces. All entities extend `VersionedEntity` for optimistic locking.

---

## Entity Overview

| Entity | Table | Description |
|--------|-------|-------------|
| `Supplier` | `suppliers` | Supplier master data |
| `PurchaseOrder` | `purchase_orders` | Purchase order header |
| `PurchaseOrderLine` | `purchase_order_items` | Purchase order line items |
| `PurchaseOrderStatusHistory` | `purchase_order_status_history` | PO status change audit |
| `GoodsReceipt` | `goods_receipts` | Goods receipt header |
| `GoodsReceiptLine` | `goods_receipt_items` | Goods receipt line items |
| `RawMaterialPurchase` | `raw_material_purchases` | Purchase invoice header |
| `RawMaterialPurchaseLine` | `raw_material_purchase_items` | Purchase invoice line items |

---

## Supplier

**File:** `domain/Supplier.java`  
**Table:** `suppliers`

Master data entity for suppliers with lifecycle management.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `publicId` | UUID | `public_id` | NOT NULL | Public identifier |
| `company` | Company | `company_id` | FK, NOT NULL | Owning company |
| `code` | String | `code` | NOT NULL, Unique (company) | Supplier code |
| `name` | String | `name` | NOT NULL | Supplier name |
| `status` | SupplierStatus | `status` | NOT NULL | Lifecycle status |
| `email` | String | `email` | - | Contact email |
| `phone` | String | `phone` | - | Contact phone |
| `address` | String | `address` | - | Address |
| `gstNumber` | String | `gst_number` | - | GSTIN (15 chars) |
| `stateCode` | String | `state_code` | - | State code (2 chars) |
| `gstRegistrationType` | GstRegistrationType | `gst_registration_type` | NOT NULL | GST registration type |
| `paymentTerms` | SupplierPaymentTerms | `payment_terms` | NOT NULL | Payment terms |
| `bankAccountNameEncrypted` | String | `bank_account_name_encrypted` | - | Encrypted bank name |
| `bankAccountNumberEncrypted` | String | `bank_account_number_encrypted` | - | Encrypted account number |
| `bankIfscEncrypted` | String | `bank_ifsc_encrypted` | - | Encrypted IFSC code |
| `bankBranchEncrypted` | String | `bank_branch_encrypted` | - | Encrypted branch |
| `creditLimit` | BigDecimal | `credit_limit` | NOT NULL, >= 0 | Credit limit |
| `outstandingBalance` | BigDecimal | `outstanding_balance` | NOT NULL | Current balance |
| `payableAccount` | Account | `payable_account_id` | FK | AP account |
| `createdAt` | Instant | `created_at` | NOT NULL | Creation timestamp |

### Unique Constraints
- `(company_id, code)` - Unique supplier code per company

### Business Methods

| Method | Description |
|--------|-------------|
| `isTransactionalUsageAllowed()` | Returns true if status is ACTIVE |
| `requireTransactionalUsage(String action)` | Throws if not ACTIVE |
| `getStatusEnum()` | Returns typed SupplierStatus |
| `setStatus(String)` | Normalizes status strings (INACTIVE→SUSPENDED, etc.) |

---

## PurchaseOrder

**File:** `domain/PurchaseOrder.java`  
**Table:** `purchase_orders`

Purchase order header entity.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `publicId` | UUID | `public_id` | NOT NULL | Public identifier |
| `company` | Company | `company_id` | FK, NOT NULL | Owning company |
| `supplier` | Supplier | `supplier_id` | FK, NOT NULL | Supplier |
| `orderNumber` | String | `order_number` | NOT NULL, Unique (company) | PO number |
| `orderDate` | LocalDate | `order_date` | NOT NULL | Order date |
| `status` | PurchaseOrderStatus | `status` | NOT NULL | PO status |
| `memo` | String | `memo` | - | Notes |
| `createdAt` | Instant | `created_at` | NOT NULL | Creation timestamp |
| `updatedAt` | Instant | `updated_at` | NOT NULL | Last update timestamp |
| `lines` | List<PurchaseOrderLine> | - |OneToMany, CASCADE ALL | Line items |

### Unique Constraints
- `(company_id, order_number)` - Unique order number per company

### Lifecycle Callbacks
- `@PrePersist` - Sets publicId, createdAt, updatedAt
- `@PreUpdate` - Updates updatedAt

---

## PurchaseOrderLine

**File:** `domain/PurchaseOrderLine.java`  
**Table:** `purchase_order_items`

Purchase order line item entity.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `purchaseOrder` | PurchaseOrder | `purchase_order_id` | FK, NOT NULL | Parent PO |
| `rawMaterial` | RawMaterial | `raw_material_id` | FK, NOT NULL | Material |
| `quantity` | BigDecimal | `quantity` | NOT NULL | Ordered quantity |
| `unit` | String | `unit` | NOT NULL | Unit of measure |
| `costPerUnit` | BigDecimal | `cost_per_unit` | NOT NULL | Unit cost |
| `lineTotal` | BigDecimal | `line_total` | NOT NULL | Line total (qty × cost) |
| `notes` | String | `notes` | - | Line notes |

---

## PurchaseOrderStatusHistory

**File:** `domain/PurchaseOrderStatusHistory.java`  
**Table:** `purchase_order_status_history`

Audit trail for purchase order status changes.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `company` | Company | `company_id` | FK, NOT NULL | Owning company |
| `purchaseOrder` | PurchaseOrder | `purchase_order_id` | FK, NOT NULL | Related PO |
| `fromStatus` | String | `from_status` | - | Previous status |
| `toStatus` | String | `to_status` | NOT NULL | New status |
| `reasonCode` | String | `reason_code` | - | Reason code |
| `reason` | String | `reason` | - | Reason description |
| `changedBy` | String | `changed_by` | NOT NULL | Actor (user/system) |
| `changedAt` | Instant | `changed_at` | NOT NULL | Change timestamp |

---

## GoodsReceipt

**File:** `domain/GoodsReceipt.java`  
**Table:** `goods_receipts`

Goods receipt header entity.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `publicId` | UUID | `public_id` | NOT NULL | Public identifier |
| `company` | Company | `company_id` | FK, NOT NULL | Owning company |
| `supplier` | Supplier | `supplier_id` | FK, NOT NULL | Supplier |
| `purchaseOrder` | PurchaseOrder | `purchase_order_id` | FK, NOT NULL | Related PO |
| `receiptNumber` | String | `receipt_number` | NOT NULL, Unique (company) | Receipt number |
| `receiptDate` | LocalDate | `receipt_date` | NOT NULL | Receipt date |
| `idempotencyKey` | String | `idempotency_key` | Length 128 | Idempotency key |
| `idempotencyHash` | String | `idempotency_hash` | Length 64 | Payload hash |
| `status` | GoodsReceiptStatus | `status` | NOT NULL | Receipt status |
| `memo` | String | `memo` | - | Notes |
| `createdAt` | Instant | `created_at` | NOT NULL | Creation timestamp |
| `updatedAt` | Instant | `updated_at` | NOT NULL | Last update timestamp |
| `lines` | List<GoodsReceiptLine> | - | OneToMany, CASCADE ALL | Line items |

### Unique Constraints
- `(company_id, receipt_number)` - Unique receipt number per company

---

## GoodsReceiptLine

**File:** `domain/GoodsReceiptLine.java`  
**Table:** `goods_receipt_items`

Goods receipt line item entity.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `goodsReceipt` | GoodsReceipt | `goods_receipt_id` | FK, NOT NULL | Parent receipt |
| `rawMaterial` | RawMaterial | `raw_material_id` | FK, NOT NULL | Material |
| `rawMaterialBatch` | RawMaterialBatch | `raw_material_batch_id` | FK | Created batch |
| `batchCode` | String | `batch_code` | NOT NULL | Batch code |
| `quantity` | BigDecimal | `quantity` | NOT NULL | Received quantity |
| `unit` | String | `unit` | NOT NULL | Unit of measure |
| `costPerUnit` | BigDecimal | `cost_per_unit` | NOT NULL | Unit cost |
| `lineTotal` | BigDecimal | `line_total` | NOT NULL | Line total |
| `notes` | String | `notes` | - | Line notes |

---

## RawMaterialPurchase

**File:** `domain/RawMaterialPurchase.java`  
**Table:** `raw_material_purchases`

Purchase invoice header entity.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `publicId` | UUID | `public_id` | NOT NULL | Public identifier |
| `company` | Company | `company_id` | FK, NOT NULL | Owning company |
| `supplier` | Supplier | `supplier_id` | FK, NOT NULL | Supplier |
| `journalEntry` | JournalEntry | `journal_entry_id` | FK | Posted journal |
| `purchaseOrder` | PurchaseOrder | `purchase_order_id` | FK | Related PO |
| `goodsReceipt` | GoodsReceipt | `goods_receipt_id` | FK | Related receipt |
| `invoiceNumber` | String | `invoice_number` | NOT NULL, Unique (company) | Invoice number |
| `invoiceDate` | LocalDate | `invoice_date` | NOT NULL | Invoice date |
| `totalAmount` | BigDecimal | `total_amount` | NOT NULL | Total amount |
| `taxAmount` | BigDecimal | `tax_amount` | NOT NULL | Tax amount |
| `outstandingAmount` | BigDecimal | `outstanding_amount` | NOT NULL | Outstanding balance |
| `status` | String | `status` | NOT NULL | Invoice status |
| `memo` | String | `memo` | - | Notes |
| `createdAt` | Instant | `created_at` | NOT NULL | Creation timestamp |
| `updatedAt` | Instant | `updated_at` | NOT NULL | Last update timestamp |
| `lines` | List<RawMaterialPurchaseLine> | - | OneToMany, CASCADE ALL | Line items |

### Unique Constraints
- `(company_id, invoice_number)` - Unique invoice number per company

### Status Values
- `POSTED` - Invoice posted to accounting
- `PARTIAL` - Partially paid
- `PAID` - Fully paid
- `VOID` - Voided (fully returned)

---

## RawMaterialPurchaseLine

**File:** `domain/RawMaterialPurchaseLine.java`  
**Table:** `raw_material_purchase_items`

Purchase invoice line item entity with tax breakdown.

### Fields

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| `id` | Long | `id` | PK, Generated | Primary key |
| `purchase` | RawMaterialPurchase | `purchase_id` | FK, NOT NULL | Parent invoice |
| `rawMaterial` | RawMaterial | `raw_material_id` | FK, NOT NULL | Material |
| `rawMaterialBatch` | RawMaterialBatch | `raw_material_batch_id` | FK | Linked batch |
| `batchCode` | String | `batch_code` | NOT NULL | Batch code |
| `quantity` | BigDecimal | `quantity` | NOT NULL | Invoiced quantity |
| `returnedQuantity` | BigDecimal | `returned_quantity` | NOT NULL | Returned quantity |
| `unit` | String | `unit` | NOT NULL | Unit of measure |
| `costPerUnit` | BigDecimal | `cost_per_unit` | NOT NULL | Unit cost |
| `lineTotal` | BigDecimal | `line_total` | NOT NULL | Line total |
| `taxRate` | BigDecimal | `tax_rate` | - | Tax rate (%) |
| `taxAmount` | BigDecimal | `tax_amount` | - | Tax amount |
| `cgstAmount` | BigDecimal | `cgst_amount` | - | CGST amount |
| `sgstAmount` | BigDecimal | `sgst_amount` | - | SGST amount |
| `igstAmount` | BigDecimal | `igst_amount` | - | IGST amount |
| `notes` | String | `notes` | - | Line notes |

---

## Enums

### SupplierStatus

**File:** `domain/SupplierStatus.java`

| Value | Description |
|-------|-------------|
| `PENDING` | Created, awaiting approval |
| `APPROVED` | Approved, not yet active |
| `ACTIVE` | Active, can transact |
| `SUSPENDED` | Suspended, cannot transact |

### SupplierPaymentTerms

**File:** `domain/SupplierPaymentTerms.java`

| Value | Due Days |
|-------|----------|
| `NET_30` | 30 |
| `NET_60` | 60 |
| `NET_90` | 90 |

### PurchaseOrderStatus

**File:** `domain/PurchaseOrderStatus.java`

| Value | Description |
|-------|-------------|
| `DRAFT` | Initial state |
| `APPROVED` | Approved for receiving |
| `PARTIALLY_RECEIVED` | Partially received |
| `FULLY_RECEIVED` | All items received |
| `INVOICED` | Invoice posted |
| `CLOSED` | Completed |
| `VOID` | Cancelled |

### GoodsReceiptStatus

**File:** `domain/GoodsReceiptStatus.java`

| Value | Description |
|-------|-------------|
| `PARTIAL` | Partial receipt |
| `RECEIVED` | Fully received |
| `INVOICED` | Invoice posted |

---

## Repositories

### SupplierRepository

**File:** `domain/SupplierRepository.java`

| Method | Description |
|--------|-------------|
| `findByCompanyOrderByNameAsc(Company)` | List suppliers by company |
| `findByCompanyWithPayableAccountOrderByNameAsc(Company)` | List with payable account |
| `findByCompanyAndId(Company, Long)` | Find by ID |
| `findByCompanyAndCodeIgnoreCase(Company, String)` | Find by code |
| `findByCompanyAndPayableAccount(Company, Account)` | Find by payable account |
| `lockByCompanyAndId(Company, Long)` | Pessimistic lock by ID |

### PurchaseOrderRepository

**File:** `domain/PurchaseOrderRepository.java`

| Method | Description |
|--------|-------------|
| `findByCompanyWithLinesOrderByOrderDateDesc(Company)` | List with lines |
| `findByCompanyAndSupplierWithLinesOrderByOrderDateDesc(Company, Supplier)` | List by supplier |
| `existsByCompanyAndLinesRawMaterial(Company, RawMaterial)` | Check material usage |
| `findByCompanyAndId(Company, Long)` | Find by ID |
| `lockByCompanyAndId(Company, Long)` | Pessimistic lock |
| `lockByCompanyAndOrderNumberIgnoreCase(Company, String)` | Lock by order number |

### PurchaseOrderStatusHistoryRepository

**File:** `domain/PurchaseOrderStatusHistoryRepository.java`

| Method | Description |
|--------|-------------|
| `findTimeline(Company, PurchaseOrder)` | Get status history ordered by time |

### GoodsReceiptRepository

**File:** `domain/GoodsReceiptRepository.java`

| Method | Description |
|--------|-------------|
| `findByCompanyWithLinesOrderByReceiptDateDesc(Company)` | List with lines |
| `findByCompanyAndSupplierWithLinesOrderByReceiptDateDesc(Company, Supplier)` | List by supplier |
| `findByCompanyAndId(Company, Long)` | Find by ID |
| `findByPurchaseOrder(PurchaseOrder)` | Find by PO |
| `findWithLinesByCompanyAndIdempotencyKey(Company, String)` | Find by idempotency key |
| `lockByCompanyAndId(Company, Long)` | Pessimistic lock |
| `lockByCompanyAndReceiptNumberIgnoreCase(Company, String)` | Lock by receipt number |

### RawMaterialPurchaseRepository

**File:** `domain/RawMaterialPurchaseRepository.java`

| Method | Description |
|--------|-------------|
| `findByCompanyWithLinesOrderByInvoiceDateDesc(Company)` | List with lines |
| `findByCompanyAndSupplierWithLinesOrderByInvoiceDateDesc(Company, Supplier)` | List by supplier |
| `findByCompanyAndId(Company, Long)` | Find by ID |
| `findByCompanyAndGoodsReceipt(Company, GoodsReceipt)` | Find by receipt |
| `findByCompanyAndJournalEntry(Company, JournalEntry)` | Find by journal |
| `lockByCompanyAndId(Company, Long)` | Pessimistic lock |
| `lockByCompanyAndInvoiceNumberIgnoreCase(Company, String)` | Lock by invoice number |
| `lockOpenPurchasesForSettlement(Company, Supplier)` | Lock open invoices for payment |
