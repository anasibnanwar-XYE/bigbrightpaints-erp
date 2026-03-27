# Factory DTOs

Data Transfer Objects for the Factory module.

## Request DTOs

### ProductionLogRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to create a production log entry (M2S).

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `brandId` | Long | @NotNull | Brand ID |
| `productId` | Long | @NotNull | Product ID |
| `batchColour` | String | - | Colour/variant |
| `batchSize` | BigDecimal | @NotNull | Batch size |
| `unitOfMeasure` | String | - | Unit (defaults to product UoM) |
| `mixedQuantity` | BigDecimal | @NotNull | Mixed quantity |
| `producedAt` | String | - | Production timestamp (flexible format) |
| `notes` | String | - | Notes |
| `createdBy` | String | - | Creator |
| `salesOrderId` | Long | - | Linked sales order |
| `laborCost` | BigDecimal | - | Labor cost |
| `overheadCost` | BigDecimal | - | Overhead cost |
| `materials` | List<MaterialUsageRequest> | @Valid @NotEmpty | Materials to consume |

**Nested: MaterialUsageRequest**
| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `rawMaterialId` | Long | @NotNull | Raw material ID |
| `quantity` | BigDecimal | @NotNull @Positive | Quantity to consume |
| `unitOfMeasure` | String | - | Unit |

---

### PackingRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to record a packing operation.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `productionLogId` | Long | @NotNull | Production log ID |
| `packedDate` | LocalDate | - | Packing date |
| `packedBy` | String | - | Packed by |
| `idempotencyKey` | String | @JsonIgnore | Idempotency key (from header) |
| `lines` | List<PackingLineRequest> | @Valid | Packing lines |
| `closeResidualWastage` | Boolean | - | Close remaining as wastage |

---

### PackingLineRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Single packing line for a specific size.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `childFinishedGoodId` | Long | @NotNull | Target sellable size FG |
| `childBatchCount` | Integer | - | Number of child batches |
| `packagingSize` | String | @NotBlank | Size label (1L, 5L, etc.) |
| `quantityLiters` | BigDecimal | @Positive | Quantity in liters |
| `piecesCount` | Integer | @Positive | Number of pieces |
| `boxesCount` | Integer | @Positive | Number of boxes |
| `piecesPerBox` | Integer | @Positive | Pieces per box |

---

### BulkPackRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to pack a bulk batch into sized child SKUs.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `bulkBatchId` | Long | @NotNull | Bulk batch ID |
| `packs` | List<PackLine> | @NotEmpty @Valid | Pack lines |
| `packDate` | LocalDate | - | Pack date |
| `packedBy` | String | - | Packed by |
| `notes` | String | - | Notes |
| `idempotencyKey` | String | - | Optional idempotency key |

**Nested: PackLine**
| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `childSkuId` | Long | @NotNull | Child SKU ID |
| `quantity` | BigDecimal | @NotNull @Positive | Quantity |
| `sizeLabel` | String | - | Size label |
| `unit` | String | - | Unit |

---

### CostAllocationRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to allocate period costs to batches.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `year` | Integer | @NotNull @Min(2000) | Year |
| `month` | Integer | @NotNull @Min(1) | Month (1-12) |
| `laborCost` | BigDecimal | @NotNull | Total labor cost |
| `overheadCost` | BigDecimal | @NotNull | Total overhead cost |
| `finishedGoodsAccountId` | Long | @NotNull | FG asset account |
| `laborExpenseAccountId` | Long | @NotNull | Labor expense account |
| `overheadExpenseAccountId` | Long | @NotNull | Overhead expense account |
| `notes` | String | - | Notes |

---

### ProductionPlanRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to create/update a production plan.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `planNumber` | String | @NotBlank | Plan number |
| `productName` | String | @NotBlank | Product name |
| `quantity` | Double | @NotNull | Quantity |
| `plannedDate` | LocalDate | @NotNull | Planned date |
| `notes` | String | - | Notes |

---

### FactoryTaskRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to create/update a factory task.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `title` | String | @NotBlank | Task title |
| `description` | String | - | Description |
| `assignee` | String | - | Assigned to |
| `status` | String | - | Status (defaults to PENDING) |
| `dueDate` | LocalDate | - | Due date |
| `salesOrderId` | Long | - | Linked sales order |
| `packagingSlipId` | Long | - | Linked packaging slip |

---

### PackagingSizeMappingRequest
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Request to create/update a packaging rule.

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| `packagingSize` | String | @NotBlank | Size label |
| `rawMaterialId` | Long | @NotNull | Packaging material ID |
| `unitsPerPack` | Integer | @NotNull @Positive | Units per pack |
| `cartonSize` | Integer | @Positive | Carton size |
| `litersPerUnit` | BigDecimal | @Positive | Liters per unit |

---

## Response DTOs

### ProductionLogDetailDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Detailed production log response.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `productionCode` | String | Unique code |
| `producedAt` | Instant | Production timestamp |
| `brandName` | String | Brand name |
| `productName` | String | Product name |
| `skuCode` | String | SKU code |
| `batchColour` | String | Colour |
| `batchSize` | BigDecimal | Batch size |
| `unitOfMeasure` | String | Unit |
| `mixedQuantity` | BigDecimal | Mixed quantity |
| `outputBatchCode` | String | Output batch code |
| `outputQuantity` | BigDecimal | Output quantity |
| `totalPackedQuantity` | BigDecimal | Packed quantity |
| `wastageQuantity` | BigDecimal | Wastage |
| `wastageReasonCode` | String | Wastage reason |
| `status` | String | Status |
| `materialCostTotal` | BigDecimal | Material cost |
| `laborCostTotal` | BigDecimal | Labor cost |
| `overheadCostTotal` | BigDecimal | Overhead cost |
| `unitCost` | BigDecimal | Unit cost |
| `salesOrderId` | Long | Sales order ID |
| `salesOrderNumber` | String | Sales order number |
| `notes` | String | Notes |
| `createdBy` | String | Creator |
| `materials` | List<ProductionLogMaterialDto> | Materials |
| `packingRecords` | List<ProductionLogPackingRecordDto> | Packing records |
| `productFamilyName` | String | Product family |
| `allowedSellableSizes` | List<AllowedSellableSizeDto> | Allowed sizes |

---

### ProductionLogDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Summary production log response.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `productionCode` | String | Unique code |
| `producedAt` | Instant | Production timestamp |
| `brandName` | String | Brand name |
| `productName` | String | Product name |
| `skuCode` | String | SKU code |
| `batchColour` | String | Colour |
| `batchSize` | BigDecimal | Batch size |
| `unitOfMeasure` | String | Unit |
| `mixedQuantity` | BigDecimal | Mixed quantity |
| `outputBatchCode` | String | Output batch code |
| `outputQuantity` | BigDecimal | Output quantity |
| `totalPackedQuantity` | BigDecimal | Packed quantity |
| `wastageQuantity` | BigDecimal | Wastage |
| `wastageReasonCode` | String | Wastage reason |
| `status` | String | Status |
| `createdBy` | String | Creator |
| `unitCost` | BigDecimal | Unit cost |
| `materialCostTotal` | BigDecimal | Material cost |
| `laborCostTotal` | BigDecimal | Labor cost |
| `overheadCostTotal` | BigDecimal | Overhead cost |
| `salesOrderId` | Long | Sales order ID |
| `salesOrderNumber` | String | Sales order number |

---

### BulkPackResponse
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Response from bulk-to-size packing.

| Field | Type | Description |
|-------|------|-------------|
| `bulkBatchId` | Long | Bulk batch ID |
| `bulkBatchCode` | String | Bulk batch code |
| `volumeDeducted` | BigDecimal | Volume deducted |
| `remainingBulkQuantity` | BigDecimal | Remaining bulk |
| `packagingCost` | BigDecimal | Packaging cost |
| `childBatches` | List<ChildBatchDto> | Child batches |
| `journalEntryId` | Long | Journal entry ID |
| `packedAt` | Instant | Pack timestamp |

**Nested: ChildBatchDto**
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Batch ID |
| `publicId` | UUID | Public ID |
| `batchCode` | String | Batch code |
| `finishedGoodId` | Long | FG ID |
| `finishedGoodCode` | String | FG code |
| `finishedGoodName` | String | FG name |
| `sizeLabel` | String | Size |
| `quantity` | BigDecimal | Quantity |
| `unitCost` | BigDecimal | Unit cost |
| `totalValue` | BigDecimal | Total value |

---

### PackingRecordDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Packing record summary.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Record ID |
| `productionLogId` | Long | Log ID |
| `productionCode` | String | Production code |
| `sizeVariantId` | Long | Size variant ID |
| `sizeVariantLabel` | String | Size label |
| `finishedGoodBatchId` | Long | FG batch ID |
| `finishedGoodBatchCode` | String | FG batch code |
| `childBatchCount` | Integer | Child batch count |
| `packagingSize` | String | Packaging size |
| `quantityPacked` | BigDecimal | Quantity packed |
| `piecesCount` | Integer | Pieces count |
| `boxesCount` | Integer | Boxes count |
| `piecesPerBox` | Integer | Pieces per box |
| `packedDate` | LocalDate | Pack date |
| `packedBy` | String | Packed by |

---

### UnpackedBatchDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Unpacked batch summary for packing list.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Log ID |
| `productionCode` | String | Production code |
| `productName` | String | Product name |
| `batchColour` | String | Colour |
| `mixedQuantity` | BigDecimal | Mixed quantity |
| `packedQuantity` | BigDecimal | Packed quantity |
| `remainingQuantity` | BigDecimal | Remaining |
| `status` | String | Status |
| `producedAt` | Instant | Production timestamp |
| `productFamilyName` | String | Product family |
| `allowedSellableSizes` | List<AllowedSellableSizeDto> | Allowed sizes |

---

### CostAllocationResponse
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Response from cost allocation.

| Field | Type | Description |
|-------|------|-------------|
| `year` | Integer | Year |
| `month` | Integer | Month |
| `batchesProcessed` | int | Batches processed |
| `totalLitersProduced` | BigDecimal | Total liters |
| `totalLaborAllocated` | BigDecimal | Labor allocated |
| `totalOverheadAllocated` | BigDecimal | Overhead allocated |
| `avgCostPerLiter` | BigDecimal | Average cost per liter |
| `journalEntryIds` | List<Long> | Journal IDs |
| `summary` | String | Summary message |

---

### AllowedSellableSizeDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Allowed sellable size for packing.

| Field | Type | Description |
|-------|------|-------------|
| `finishedGoodId` | Long | FG ID |
| `finishedGoodCode` | String | FG code |
| `finishedGoodName` | String | FG name |
| `sizeVariantId` | Long | Size variant ID |
| `sizeLabel` | String | Size label |
| `cartonQuantity` | Integer | Carton quantity |
| `litersPerUnit` | BigDecimal | Liters per unit |
| `productFamilyName` | String | Product family |

---

### FactoryDashboardDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Factory dashboard metrics.

| Field | Type | Description |
|-------|------|-------------|
| `productionEfficiency` | double | Efficiency ratio |
| `completedPlans` | long | Completed plans count |
| `batchesLogged` | long | Batches logged count |
| `alerts` | List<String> | Alert messages |

---

### ProductionPlanDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Production plan summary.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `planNumber` | String | Plan number |
| `productName` | String | Product name |
| `quantity` | double | Quantity |
| `plannedDate` | LocalDate | Planned date |
| `status` | String | Status |
| `notes` | String | Notes |

---

### FactoryTaskDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Factory task summary.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `title` | String | Title |
| `description` | String | Description |
| `assignee` | String | Assigned to |
| `status` | String | Status |
| `dueDate` | LocalDate | Due date |
| `salesOrderId` | Long | Sales order ID |
| `packagingSlipId` | Long | Packaging slip ID |

---

### PackagingSizeMappingDto
**Package**: `com.bigbrightpaints.erp.modules.factory.dto`

Packaging mapping summary.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `packagingSize` | String | Size label |
| `rawMaterialId` | Long | Material ID |
| `rawMaterialSku` | String | Material SKU |
| `rawMaterialName` | String | Material name |
| `unitsPerPack` | Integer | Units per pack |
| `cartonSize` | Integer | Carton size |
| `litersPerUnit` | BigDecimal | Liters per unit |
| `active` | boolean | Active flag |

---

### Supporting DTOs

#### ProductionLogMaterialDto
| Field | Type |
|-------|------|
| `rawMaterialId` | Long |
| `rawMaterialBatchId` | Long |
| `rawMaterialBatchCode` | String |
| `rawMaterialMovementId` | Long |
| `materialName` | String |
| `quantity` | BigDecimal |
| `unitOfMeasure` | String |
| `costPerUnit` | BigDecimal |
| `totalCost` | BigDecimal |

#### ProductionLogPackingRecordDto
| Field | Type |
|-------|------|
| `id` | Long |
| `sizeVariantId` | Long |
| `sizeVariantLabel` | String |
| `childBatchCount` | Long |
| `finishedGoodId` | Long |
| `finishedGoodCode` | String |
| `finishedGoodName` | String |
| `finishedGoodBatchId` | Long |
| `finishedGoodBatchPublicId` | UUID |
| `finishedGoodBatchCode` | String |
| `packagingSize` | String |
| `quantityPacked` | BigDecimal |
| `packedDate` | LocalDate |
| `packedBy` | String |

#### PackagingConsumptionResult
| Field | Type |
|-------|------|
| `consumed` | boolean |
| `totalCost` | BigDecimal |
| `quantity` | BigDecimal |
| `accountTotals` | Map<Long, BigDecimal> |
| `errorMessage` | String |

#### WastageReportDto
| Field | Type |
|-------|------|
| `productionLogId` | Long |
| `wastageQuantity` | BigDecimal |
| `wastageReasonCode` | String |
| `wastageDate` | LocalDate |

#### CostBreakdownDto
| Field | Type |
|-------|------|
| `materialCost` | BigDecimal |
| `laborCost` | BigDecimal |
| `overheadCost` | BigDecimal |
| `packagingCost` | BigDecimal |
| `totalCost` | BigDecimal |

#### MonthlyProductionCostDto
| Field | Type |
|-------|------|
| `yearMonth` | String |
| `totalMaterialCost` | BigDecimal |
| `totalLaborCost` | BigDecimal |
| `totalOverheadCost` | BigDecimal |
| `batchCount` | int |

#### PackedBatchTraceDto
| Field | Type |
|-------|------|
| `packingRecordId` | Long |
| `finishedGoodBatchId` | Long |
| `finishedGoodCode` | String |
| `quantityPacked` | BigDecimal |
| `packedDate` | LocalDate |

#### RawMaterialTraceDto
| Field | Type |
|-------|------|
| `rawMaterialId` | Long |
| `rawMaterialName` | String |
| `quantityConsumed` | BigDecimal |
| `unitCost` | BigDecimal |
| `totalCost` | BigDecimal |

#### CostComponentTraceDto
| Field | Type |
|-------|------|
| `componentName` | String |
| `amount` | BigDecimal |
| `percentage` | BigDecimal |
