# Inventory Module - Entities

## Overview

The Inventory module contains 15 domain entities for managing inventory, batches, movements, reservations, and dispatch operations.

---

## Core Inventory Entities

### FinishedGood

**Path:** `modules/inventory/domain/FinishedGood.java`
**Table:** `finished_goods`
**Purpose:** Represents a finished product in inventory

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| company | Company | Owning company (FK) |
| productCode | String | Unique product code |
| name | String | Product name |
| unit | String | Unit of measure (default: "UNIT") |
| currentStock | BigDecimal | Current stock quantity |
| reservedStock | BigDecimal | Reserved quantity |
| costingMethod | String | FIFO, LIFO, or WAC |
| valuationAccountId | Long | Inventory valuation account (FK) |
| cogsAccountId | Long | COGS account (FK) |
| revenueAccountId | Long | Revenue account (FK) |
| discountAccountId | Long | Discount account (FK) |
| taxAccountId | Long | Tax account (FK) |
| lowStockThreshold | BigDecimal | Low stock alert threshold |
| inventoryType | InventoryType | STANDARD or EXEMPT |
| createdAt | Instant | Creation timestamp |
| updatedAt | Instant | Last update timestamp |

**Constraints:**
- Unique: (company_id, product_code)
- Non-negative stock and reserved quantities

---

### RawMaterial

**Path:** `modules/inventory/domain/RawMaterial.java`
**Table:** `raw_materials`
**Purpose:** Represents a raw material used in production

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| company | Company | Owning company (FK) |
| name | String | Material name |
| sku | String | Stock keeping unit |
| unitType | String | Unit of measure |
| reorderLevel | BigDecimal | Reorder threshold |
| currentStock | BigDecimal | Current stock quantity |
| minStock | BigDecimal | Minimum stock level |
| maxStock | BigDecimal | Maximum stock level |
| inventoryAccountId | Long | Inventory account (FK) |
| inventoryType | InventoryType | STANDARD or EXEMPT |
| materialType | MaterialType | PRODUCTION or PACKAGING |
| costingMethod | String | FIFO, LIFO, or WAC |
| gstRate | BigDecimal | GST rate |
| privateStock | BigDecimal | Private stock quantity |
| createdAt | Instant | Creation timestamp |
| updatedAt | Instant | Last update timestamp |

**Constraints:**
- Unique: (company_id, sku)
- Non-negative stock quantities

---

### FinishedGoodBatch

**Path:** `modules/inventory/domain/FinishedGoodBatch.java`
**Table:** `finished_good_batches`
**Purpose:** Batch tracking for finished goods

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| finishedGood | FinishedGood | Parent finished good (FK) |
| batchCode | String | Unique batch code |
| quantityTotal | BigDecimal | Total quantity |
| quantityAvailable | BigDecimal | Available quantity |
| unitCost | BigDecimal | Unit cost |
| manufacturedAt | Instant | Manufacturing timestamp |
| expiryDate | LocalDate | Expiry date |
| inventoryType | InventoryType | STANDARD or EXEMPT |
| source | InventoryBatchSource | PRODUCTION, PURCHASE, ADJUSTMENT |
| parentBatch | FinishedGoodBatch | Parent bulk batch (FK) |
| bulk | boolean | Is bulk batch |
| sizeLabel | String | Size label (e.g., "1L", "4L") |
| createdAt | Instant | Creation timestamp |

**Constraints:**
- Unique: (finished_good_id, batch_code)
- Non-negative quantities

---

### RawMaterialBatch

**Path:** `modules/inventory/domain/RawMaterialBatch.java`
**Table:** `raw_material_batches`
**Purpose:** Batch tracking for raw materials

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| rawMaterial | RawMaterial | Parent raw material (FK) |
| batchCode | String | Unique batch code |
| quantity | BigDecimal | Quantity in stock |
| unit | String | Unit of measure |
| costPerUnit | BigDecimal | Cost per unit |
| supplierName | String | Supplier name |
| supplier | Supplier | Supplier reference (FK) |
| receivedAt | Instant | Receipt timestamp |
| manufacturedAt | Instant | Manufacturing timestamp |
| expiryDate | LocalDate | Expiry date |
| notes | String | Additional notes |
| inventoryType | InventoryType | STANDARD or EXEMPT |
| source | InventoryBatchSource | PURCHASE, PRODUCTION, ADJUSTMENT |

**Constraints:**
- Non-negative quantity

---

## Movement Entities

### InventoryMovement

**Path:** `modules/inventory/domain/InventoryMovement.java`
**Table:** `inventory_movements`
**Purpose:** Tracks finished goods movements (receipts, dispatches, adjustments)

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| finishedGood | FinishedGood | Finished good (FK) |
| finishedGoodBatch | FinishedGoodBatch | Batch (FK) |
| referenceType | String | Reference type (SALES_ORDER, PRODUCTION_LOG, etc.) |
| referenceId | String | Reference ID |
| packingSlipId | Long | Packaging slip ID |
| movementType | String | RECEIPT, DISPATCH, ADJUSTMENT_IN, etc. |
| quantity | BigDecimal | Movement quantity |
| unitCost | BigDecimal | Unit cost |
| createdAt | Instant | Creation timestamp |
| journalEntryId | Long | Journal entry ID (FK) |

---

### RawMaterialMovement

**Path:** `modules/inventory/domain/RawMaterialMovement.java`
**Table:** `raw_material_movements`
**Purpose:** Tracks raw material movements

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| rawMaterial | RawMaterial | Raw material (FK) |
| rawMaterialBatch | RawMaterialBatch | Batch (FK) |
| referenceType | String | Reference type |
| referenceId | String | Reference ID |
| movementType | String | RECEIPT, ADJUSTMENT_IN, ADJUSTMENT_OUT |
| quantity | BigDecimal | Movement quantity |
| unitCost | BigDecimal | Unit cost |
| createdAt | Instant | Creation timestamp |
| journalEntryId | Long | Journal entry ID (FK) |

---

## Dispatch Entities

### PackagingSlip

**Path:** `modules/inventory/domain/PackagingSlip.java`
**Table:** `packaging_slips`
**Purpose:** Packaging slip for sales order dispatch

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| company | Company | Owning company (FK) |
| salesOrder | SalesOrder | Sales order (FK) |
| slipNumber | String | Unique slip number |
| status | String | PENDING, RESERVED, DISPATCHED, etc. |
| isBackorder | boolean | Is backorder slip |
| createdAt | Instant | Creation timestamp |
| dispatchedAt | Instant | Dispatch timestamp |
| confirmedAt | Instant | Confirmation timestamp |
| confirmedBy | String | Confirmed by user |
| dispatchNotes | String | Dispatch notes |
| transporterName | String | Transporter name |
| driverName | String | Driver name |
| vehicleNumber | String | Vehicle number |
| challanReference | String | Challan reference |
| journalEntryId | Long | Inventory journal ID (FK) |
| cogsJournalEntryId | Long | COGS journal ID (FK) |
| invoiceId | Long | Invoice ID (FK) |
| lines | List<PackagingSlipLine> | Slip lines |

**Constraints:**
- Unique: (company_id, slip_number)

---

### PackagingSlipLine

**Path:** `modules/inventory/domain/PackagingSlipLine.java`
**Table:** `packaging_slip_lines`
**Purpose:** Line item on a packaging slip

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| packagingSlip | PackagingSlip | Parent slip (FK) |
| finishedGoodBatch | FinishedGoodBatch | Batch (FK) |
| orderedQuantity | BigDecimal | Ordered quantity |
| shippedQuantity | BigDecimal | Shipped quantity |
| backorderQuantity | BigDecimal | Backorder quantity |
| quantity | BigDecimal | Reserved quantity |
| unitCost | BigDecimal | Unit cost |
| notes | String | Notes |

---

## Reservation Entity

### InventoryReservation

**Path:** `modules/inventory/domain/InventoryReservation.java`
**Table:** `inventory_reservations`
**Purpose:** Reserves inventory for sales orders

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| rawMaterial | RawMaterial | Raw material (FK, optional) |
| finishedGood | FinishedGood | Finished good (FK) |
| finishedGoodBatch | FinishedGoodBatch | Batch (FK) |
| referenceType | String | Reference type |
| referenceId | String | Reference ID |
| quantity | BigDecimal | Total reserved quantity |
| reservedQuantity | BigDecimal | Current reserved quantity |
| fulfilledQuantity | BigDecimal | Fulfilled quantity |
| status | String | RESERVED, PARTIAL, FULFILLED, CANCELLED |
| createdAt | Instant | Creation timestamp |

---

## Adjustment Entities

### InventoryAdjustment

**Path:** `modules/inventory/domain/InventoryAdjustment.java`
**Table:** `inventory_adjustments`
**Purpose:** Finished goods adjustment record

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| company | Company | Owning company (FK) |
| referenceNumber | String | Unique reference number |
| adjustmentDate | LocalDate | Adjustment date |
| type | InventoryAdjustmentType | RECOUNT_UP, RECOUNT_DOWN, DAMAGED |
| reason | String | Adjustment reason |
| status | String | DRAFT, POSTED |
| journalEntryId | Long | Journal entry ID (FK) |
| totalAmount | BigDecimal | Total adjustment amount |
| idempotencyKey | String | Idempotency key |
| idempotencyHash | String | Idempotency hash |
| createdAt | Instant | Creation timestamp |
| createdBy | String | Created by user |
| lines | List<InventoryAdjustmentLine> | Adjustment lines |

---

### InventoryAdjustmentLine

**Path:** `modules/inventory/domain/InventoryAdjustmentLine.java`
**Table:** `inventory_adjustment_lines`
**Purpose:** Line item for inventory adjustment

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| adjustment | InventoryAdjustment | Parent adjustment (FK) |
| finishedGood | FinishedGood | Finished good (FK) |
| quantity | BigDecimal | Adjusted quantity |
| unitCost | BigDecimal | Unit cost |
| amount | BigDecimal | Total amount |
| note | String | Line note |

---

### RawMaterialAdjustment

**Path:** `modules/inventory/domain/RawMaterialAdjustment.java`
**Table:** `raw_material_adjustments`
**Purpose:** Raw material adjustment record

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| publicId | UUID | Public identifier |
| company | Company | Owning company (FK) |
| referenceNumber | String | Unique reference number |
| adjustmentDate | LocalDate | Adjustment date |
| reason | String | Adjustment reason |
| status | String | DRAFT, POSTED |
| journalEntryId | Long | Journal entry ID (FK) |
| totalAmount | BigDecimal | Total adjustment amount |
| idempotencyKey | String | Idempotency key |
| idempotencyHash | String | Idempotency hash |
| createdAt | Instant | Creation timestamp |
| createdBy | String | Created by user |
| lines | List<RawMaterialAdjustmentLine> | Adjustment lines |

---

### RawMaterialAdjustmentLine

**Path:** `modules/inventory/domain/RawMaterialAdjustmentLine.java`
**Table:** `raw_material_adjustment_lines`
**Purpose:** Line item for raw material adjustment

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| adjustment | RawMaterialAdjustment | Parent adjustment (FK) |
| rawMaterial | RawMaterial | Raw material (FK) |
| quantity | BigDecimal | Adjusted quantity (+/-) |
| unitCost | BigDecimal | Unit cost |
| amount | BigDecimal | Total amount |
| note | String | Line note |

---

## Import Entity

### OpeningStockImport

**Path:** `modules/inventory/domain/OpeningStockImport.java`
**Table:** `opening_stock_imports`
**Purpose:** Tracks opening stock import records

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| company | Company | Owning company (FK) |
| idempotencyKey | String | Idempotency key |
| referenceNumber | String | Journal reference |
| openingStockBatchKey | String | Batch key |
| fileName | String | Import file name |
| rowsProcessed | int | Rows processed |
| rawMaterialBatchesCreated | int | RM batches created |
| finishedGoodBatchesCreated | int | FG batches created |
| resultsJson | String | JSON results |
| errorsJson | String | JSON errors |
| journalEntryId | Long | Journal entry ID (FK) |
| createdAt | Instant | Creation timestamp |

---

### RawMaterialIntakeRecord

**Path:** `modules/inventory/domain/RawMaterialIntakeRecord.java`
**Table:** `raw_material_intake_records`
**Purpose:** Tracks manual raw material intake records

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key |
| company | Company | Owning company (FK) |
| idempotencyKey | String | Idempotency key |
| idempotencyHash | String | Idempotency hash |
| rawMaterialId | Long | Raw material ID |
| rawMaterialBatchId | Long | Batch ID |
| rawMaterialMovementId | Long | Movement ID |
| journalEntryId | Long | Journal entry ID |
| createdAt | Instant | Creation timestamp |

---

## Enums

### InventoryType

**Path:** `modules/inventory/domain/InventoryType.java`
**Values:** STANDARD, EXEMPT

### MaterialType

**Path:** `modules/inventory/domain/MaterialType.java`
**Values:** PRODUCTION, PACKAGING

### InventoryBatchSource

**Path:** `modules/inventory/domain/InventoryBatchSource.java`
**Values:** PRODUCTION, PURCHASE, ADJUSTMENT

### InventoryAdjustmentType

**Path:** `modules/inventory/domain/InventoryAdjustmentType.java`
**Values:** RECOUNT_UP, RECOUNT_DOWN, DAMAGED

### InventoryReference

**Path:** `modules/inventory/domain/InventoryReference.java`
**Constants:**
- `PRODUCTION_LOG`
- `RAW_MATERIAL_PURCHASE`
- `OPENING_STOCK`
- `SALES_ORDER`
- `MANUFACTURING_ORDER`
- `PURCHASE_RETURN`
- `PACKING_RECORD`
- `GOODS_RECEIPT`
- `RAW_MATERIAL_ADJUSTMENT`
