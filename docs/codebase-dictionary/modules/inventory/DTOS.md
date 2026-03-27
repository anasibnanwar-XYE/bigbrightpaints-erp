# Inventory Module - DTOs

## Overview

The Inventory module defines 23 DTOs for API requests and responses.

---

## Request DTOs

### FinishedGoodRequest

**Path:** `modules/inventory/dto/FinishedGoodRequest.java`
**Purpose:** Create/update finished good

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| productCode | String | Yes | Product code |
| name | String | Yes | Product name |
| unit | String | No | Unit of measure |
| costingMethod | String | No | FIFO, LIFO, WAC |
| valuationAccountId | Long | No | Valuation account |
| cogsAccountId | Long | No | COGS account |
| revenueAccountId | Long | No | Revenue account |
| discountAccountId | Long | No | Discount account |
| taxAccountId | Long | No | Tax account |

---

### FinishedGoodBatchRequest

**Path:** `modules/inventory/dto/FinishedGoodBatchRequest.java`
**Purpose:** Register finished good batch

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| finishedGoodId | Long | Yes | Finished good ID |
| batchCode | String | No | Batch code (auto-generated if not provided) |
| quantity | BigDecimal | Yes | Quantity |
| unitCost | BigDecimal | Yes | Unit cost |
| manufacturedAt | Instant | No | Manufacturing timestamp |
| expiryDate | LocalDate | No | Expiry date |

---

### RawMaterialRequest

**Path:** `modules/inventory/dto/RawMaterialRequest.java`
**Purpose:** Create/update raw material

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Material name |
| sku | String | Yes | Stock keeping unit |
| unitType | String | Yes | Unit of measure |
| reorderLevel | BigDecimal | No | Reorder threshold |
| minStock | BigDecimal | No | Minimum stock |
| maxStock | BigDecimal | No | Maximum stock |
| inventoryAccountId | Long | No | Inventory account |
| costingMethod | String | No | FIFO, LIFO, WAC |
| materialType | String | No | PRODUCTION, PACKAGING |

---

### RawMaterialBatchRequest

**Path:** `modules/inventory/dto/RawMaterialBatchRequest.java`
**Purpose:** Create raw material batch

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| batchCode | String | No | Batch code |
| quantity | BigDecimal | Yes | Quantity |
| unit | String | No | Unit of measure |
| costPerUnit | BigDecimal | Yes | Cost per unit |
| supplierId | Long | No | Supplier ID |
| manufacturingDate | LocalDate | No | Manufacturing date |
| expiryDate | LocalDate | No | Expiry date |
| notes | String | No | Notes |

---

### RawMaterialIntakeRequest

**Path:** `modules/inventory/dto/RawMaterialIntakeRequest.java`
**Purpose:** Raw material intake

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| rawMaterialId | Long | Yes | Raw material ID |
| batchCode | String | No | Batch code |
| quantity | BigDecimal | Yes | Quantity |
| unit | String | No | Unit |
| costPerUnit | BigDecimal | Yes | Cost per unit |
| supplierId | Long | No | Supplier ID |
| manufacturingDate | LocalDate | No | Manufacturing date |
| expiryDate | LocalDate | No | Expiry date |
| notes | String | No | Notes |

---

### RawMaterialAdjustmentRequest

**Path:** `modules/inventory/dto/RawMaterialAdjustmentRequest.java`
**Purpose:** Raw material adjustment

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| adjustmentDate | LocalDate | No | Adjustment date |
| direction | AdjustmentDirection | Yes | INCREASE or DECREASE |
| adjustmentAccountId | Long | Yes | Adjustment account |
| reason | String | No | Reason |
| adminOverride | Boolean | No | Admin override |
| idempotencyKey | String | Yes | Idempotency key |
| lines | List<LineRequest> | Yes | Adjustment lines |

**LineRequest:**
| Field | Type | Required |
|-------|------|----------|
| rawMaterialId | Long | Yes |
| quantity | BigDecimal | Yes |
| unitCost | BigDecimal | Yes |
| note | String | No |

---

### InventoryAdjustmentRequest

**Path:** `modules/inventory/dto/InventoryAdjustmentRequest.java`
**Purpose:** Finished goods adjustment

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| adjustmentDate | LocalDate | No | Adjustment date |
| type | InventoryAdjustmentType | No | RECOUNT_UP, RECOUNT_DOWN, DAMAGED |
| adjustmentAccountId | Long | Yes | Adjustment account |
| reason | String | No | Reason |
| adminOverride | Boolean | No | Admin override |
| idempotencyKey | String | Yes | Idempotency key |
| lines | List<LineRequest> | Yes | Adjustment lines |

**LineRequest:**
| Field | Type | Required |
|-------|------|----------|
| finishedGoodId | Long | Yes |
| quantity | BigDecimal | Yes |
| unitCost | BigDecimal | Yes |
| note | String | No |

---

### DispatchConfirmationRequest

**Path:** `modules/inventory/dto/DispatchConfirmationRequest.java`
**Purpose:** Confirm dispatch

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| packagingSlipId | Long | Yes | Packaging slip ID |
| lines | List<LineConfirmation> | Yes | Line confirmations |
| notes | String | No | Notes |
| confirmedBy | String | No | Confirmed by |
| overrideRequestId | Long | No | Override request ID |
| transporterName | String | No | Transporter name |
| driverName | String | No | Driver name |
| vehicleNumber | String | No | Vehicle number |
| challanReference | String | No | Challan reference |

**LineConfirmation:**
| Field | Type | Required |
|-------|------|----------|
| lineId | Long | Yes |
| shippedQuantity | BigDecimal | Yes |
| notes | String | No |

---

### FinishedGoodLowStockThresholdRequest

**Path:** `modules/inventory/dto/FinishedGoodLowStockThresholdRequest.java`
**Purpose:** Update low stock threshold

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| threshold | BigDecimal | Yes | Low stock threshold |

---

## Response DTOs

### FinishedGoodDto

**Path:** `modules/inventory/dto/FinishedGoodDto.java`
**Record Fields:**
- `id`, `publicId`, `productCode`, `name`, `unit`, `currentStock`, `reservedStock`, `costingMethod`, `valuationAccountId`, `cogsAccountId`, `revenueAccountId`, `discountAccountId`, `taxAccountId`

---

### RawMaterialDto

**Path:** `modules/inventory/dto/RawMaterialDto.java`
**Record Fields:**
- `id`, `publicId`, `name`, `sku`, `unitType`, `reorderLevel`, `currentStock`, `minStock`, `maxStock`, `stockStatus`, `inventoryAccountId`, `costingMethod`, `materialType`

---

### FinishedGoodBatchDto

**Path:** `modules/inventory/dto/FinishedGoodBatchDto.java`
**Record Fields:**
- `id`, `publicId`, `batchCode`, `quantityTotal`, `quantityAvailable`, `unitCost`, `manufacturedAt`, `expiryDate`

---

### RawMaterialBatchDto

**Path:** `modules/inventory/dto/RawMaterialBatchDto.java`
**Record Fields:**
- `id`, `publicId`, `batchCode`, `quantity`, `unit`, `costPerUnit`, `supplierId`, `supplierName`, `receivedAt`, `notes`

---

### StockSummaryDto

**Path:** `modules/inventory/dto/StockSummaryDto.java`
**Record Fields:**
- `id`, `publicId`, `code`, `name`, `currentStock`, `reservedStock`, `availableStock`, `unitCost`, `totalItems`, `lowStockCount`, `criticalStockCount`, `batchCount`

---

### InventoryStockSnapshot

**Path:** `modules/inventory/dto/InventoryStockSnapshot.java`
**Record Fields:**
- `name`, `sku`, `currentStock`, `reorderLevel`, `status`

---

### FinishedGoodLowStockThresholdDto

**Path:** `modules/inventory/dto/FinishedGoodLowStockThresholdDto.java`
**Record Fields:**
- `id`, `productCode`, `threshold`

---

### PackagingSlipDto

**Path:** `modules/inventory/dto/PackagingSlipDto.java`
**Record Fields:**
- `id`, `publicId`, `salesOrderId`, `orderNumber`, `dealerName`, `slipNumber`, `status`, `createdAt`, `confirmedAt`, `confirmedBy`, `dispatchedAt`, `dispatchNotes`, `journalEntryId`, `cogsJournalEntryId`, `lines`, `transporterName`, `driverName`, `vehicleNumber`, `challanReference`, `deliveryChallanNumber`, `deliveryChallanPdfPath`

---

### PackagingSlipLineDto

**Path:** `modules/inventory/dto/PackagingSlipLineDto.java`
**Record Fields:**
- `id`, `batchPublicId`, `batchCode`, `productCode`, `productName`, `orderedQuantity`, `shippedQuantity`, `backorderQuantity`, `quantity`, `unitCost`, `notes`

---

### DispatchPreviewDto

**Path:** `modules/inventory/dto/DispatchPreviewDto.java`
**Record Fields:**
- `packagingSlipId`, `slipNumber`, `status`, `salesOrderId`, `salesOrderNumber`, `dealerName`, `dealerCode`, `createdAt`, `totalOrdered`, `totalAfterTax`, `gstBreakdown`, `lines`

**GstBreakdown:**
- `taxableAmount`, `cgst`, `sgst`, `igst`, `totalTax`, `grandTotal`

**LinePreview:**
- `lineId`, `finishedGoodId`, `productCode`, `productName`, `batchCode`, `orderedQuantity`, `availableQuantity`, `suggestedShipQuantity`, `unitPrice`, `subtotal`, `tax`, `total`, `hasShortage`

---

### DispatchConfirmationResponse

**Path:** `modules/inventory/dto/DispatchConfirmationResponse.java`
**Record Fields:**
- `packagingSlipId`, `slipNumber`, `status`, `confirmedAt`, `confirmedBy`, `totalOrderedAmount`, `totalShippedAmount`, `totalBackorderAmount`, `journalEntryId`, `cogsJournalEntryId`, `lines`, `backorderSlipId`, `transporterName`, `driverName`, `vehicleNumber`, `challanReference`, `deliveryChallanNumber`, `deliveryChallanPdfPath`

**LineResult:**
- `lineId`, `productCode`, `productName`, `orderedQuantity`, `shippedQuantity`, `backorderQuantity`, `unitCost`, `lineTotal`, `notes`

---

### InventoryBatchTraceabilityDto

**Path:** `modules/inventory/dto/InventoryBatchTraceabilityDto.java`
**Record Fields:**
- `batchId`, `publicId`, `batchType`, `productCode`, `productName`, `batchCode`, `manufacturedAt`, `expiryDate`, `quantityTotal`, `quantityAvailable`, `unitCost`, `source`, `movements`

---

### InventoryBatchMovementDto

**Path:** `modules/inventory/dto/InventoryBatchMovementDto.java`
**Record Fields:**
- `id`, `movementType`, `quantity`, `unitCost`, `totalCost`, `createdAt`, `source`, `referenceType`, `referenceId`, `journalEntryId`, `packingSlipId`

---

### InventoryExpiringBatchDto

**Path:** `modules/inventory/dto/InventoryExpiringBatchDto.java`
**Record Fields:**
- `batchType`, `batchId`, `publicId`, `productCode`, `productName`, `batchCode`, `quantity`, `unitCost`, `manufacturedAt`, `expiryDate`, `daysUntilExpiry`

---

### InventoryAdjustmentDto

**Path:** `modules/inventory/dto/InventoryAdjustmentDto.java`
**Record Fields:**
- `id`, `publicId`, `referenceNumber`, `adjustmentDate`, `type`, `status`, `reason`, `totalAmount`, `journalEntryId`, `lines`

---

### InventoryAdjustmentLineDto

**Path:** `modules/inventory/dto/InventoryAdjustmentLineDto.java`
**Record Fields:**
- `finishedGoodId`, `finishedGoodName`, `quantity`, `unitCost`, `amount`, `note`

---

### RawMaterialAdjustmentDto

**Path:** `modules/inventory/dto/RawMaterialAdjustmentDto.java`
**Record Fields:**
- `id`, `publicId`, `referenceNumber`, `adjustmentDate`, `status`, `reason`, `totalAmount`, `journalEntryId`, `lines`

---

### RawMaterialAdjustmentLineDto

**Path:** `modules/inventory/dto/RawMaterialAdjustmentLineDto.java`
**Record Fields:**
- `rawMaterialId`, `rawMaterialName`, `quantity`, `unitCost`, `amount`, `note`

---

### OpeningStockImportResponse

**Path:** `modules/inventory/dto/OpeningStockImportResponse.java`
**Record Fields:**
- `openingStockBatchKey`, `rowsProcessed`, `rawMaterialBatchesCreated`, `finishedGoodBatchesCreated`, `results`, `errors`

**ImportRowResult:**
- `rowNumber`, `sku`, `stockType`, `readiness`

**ImportError:**
- `rowNumber`, `message`, `sku`, `stockType`, `readiness`

---

### OpeningStockImportHistoryItem

**Path:** `modules/inventory/dto/OpeningStockImportHistoryItem.java`
**Record Fields:**
- `id`, `idempotencyKey`, `openingStockBatchKey`, `referenceNumber`, `fileName`, `journalEntryId`, `rowsProcessed`, `rawMaterialBatchesCreated`, `finishedGoodBatchesCreated`, `errorCount`, `createdAt`
