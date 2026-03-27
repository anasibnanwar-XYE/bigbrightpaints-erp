# Factory Entities

Domain model for the Factory module.

## Core Entities

### ProductionLog
**Table**: `production_logs`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Central entity representing a manufacturing batch. Tracks the complete M2S flow from raw material consumption to finished goods packing.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `brand` | ProductionBrand | Product brand |
| `product` | ProductionProduct | Product being manufactured |
| `productionCode` | String | Unique code (e.g., "PROD-20240115-001") |
| `batchColour` | String | Colour/variant identifier |
| `batchSize` | BigDecimal | Batch size |
| `unitOfMeasure` | String | Unit (LITER, UNIT, etc.) |
| `mixedQuantity` | BigDecimal | Total quantity mixed (liters) |
| `status` | ProductionLogStatus | Current status |
| `totalPackedQuantity` | BigDecimal | Quantity packed so far |
| `wastageQuantity` | BigDecimal | Unpacked/wastage quantity |
| `wastageReasonCode` | String | Reason for wastage |
| `materialCostTotal` | BigDecimal | Total material cost |
| `laborCostTotal` | BigDecimal | Total labor cost |
| `overheadCostTotal` | BigDecimal | Total overhead cost |
| `unitCost` | BigDecimal | Cost per unit |
| `producedAt` | Instant | Production timestamp |
| `notes` | String | Notes |
| `createdBy` | String | Creator |
| `salesOrderId` | Long | Linked sales order |
| `salesOrderNumber` | String | Sales order number |
| `materials` | List<ProductionLogMaterial> | Materials consumed |
| `packingRecords` | List<PackingRecord> | Packing records |

**Unique Constraint**: `(company_id, production_code)`

---

### ProductionPlan
**Table**: `production_plans`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Represents a planned production run.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `planNumber` | String | Plan number |
| `productName` | String | Product name |
| `quantity` | double | Planned quantity |
| `plannedDate` | LocalDate | Planned date |
| `status` | String | Status (PLANNED, IN_PROGRESS, COMPLETED) |
| `notes` | String | Notes |

**Unique Constraint**: `(company_id, plan_number)`

---

### PackingRecord
**Table**: `packing_records`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Records a packing operation for a production batch.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `productionLog` | ProductionLog | Parent production log |
| `finishedGood` | FinishedGood | Target finished good |
| `finishedGoodBatch` | FinishedGoodBatch | Created batch |
| `packagingSize` | String | Size packed (1L, 5L, etc.) |
| `quantityPacked` | BigDecimal | Quantity packed |
| `piecesCount` | Integer | Number of pieces |
| `boxesCount` | Integer | Number of boxes |
| `piecesPerBox` | Integer | Pieces per box |
| `packedDate` | LocalDate | Packing date |
| `packedBy` | String | Packed by |
| `packagingCost` | BigDecimal | Packaging material cost |
| `packagingMaterial` | RawMaterial | Packaging material used |
| `sizeVariant` | SizeVariant | Size variant |
| `packagingQuantity` | BigDecimal | Packaging quantity consumed |
| `childBatchCount` | Integer | Number of child batches |

---

### ProductionLogMaterial
**Table**: `production_log_materials`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Records material consumption for a production log.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `log` | ProductionLog | Parent log |
| `rawMaterial` | RawMaterial | Material consumed |
| `rawMaterialBatch` | RawMaterialBatch | Batch consumed |
| `rawMaterialMovementId` | Long | Movement reference |
| `materialName` | String | Material name |
| `quantity` | BigDecimal | Quantity consumed |
| `unitOfMeasure` | String | Unit |
| `costPerUnit` | BigDecimal | Cost per unit |
| `totalCost` | BigDecimal | Total cost |

---

### FactoryTask
**Table**: `factory_tasks`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Represents a factory task.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `title` | String | Task title |
| `description` | String | Description |
| `assignee` | String | Assigned to |
| `status` | String | Status (PENDING, IN_PROGRESS, COMPLETED) |
| `dueDate` | LocalDate | Due date |
| `salesOrderId` | Long | Linked sales order |
| `packagingSlipId` | Long | Linked packaging slip |

---

### ProductionBatch
**Table**: `production_batches`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Represents a production batch execution.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `plan` | ProductionPlan | Linked plan |
| `batchNumber` | String | Batch number |
| `quantityProduced` | double | Quantity produced |
| `producedAt` | Instant | Production timestamp |
| `loggedBy` | String | Logged by |
| `notes` | String | Notes |

**Unique Constraint**: `(company_id, batch_number)`

---

### PackagingSizeMapping
**Table**: `packaging_size_mappings`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Maps packaging sizes to raw materials for automatic consumption.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `packagingSize` | String | Size label (1L, 5L, etc.) |
| `rawMaterial` | RawMaterial | Packaging material (bucket) |
| `unitsPerPack` | Integer | Units per pack |
| `cartonSize` | Integer | Carton size |
| `litersPerUnit` | BigDecimal | Liters per unit |
| `active` | boolean | Active flag |

**Unique Constraint**: `(company_id, packaging_size, raw_material_id)`

---

### SizeVariant
**Table**: `size_variants`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Defines size variants for products.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Owning company |
| `product` | ProductionProduct | Product |
| `sizeLabel` | String | Size label (1L, 5L, etc.) |
| `cartonQuantity` | Integer | Items per carton |
| `litersPerUnit` | BigDecimal | Liters per unit |
| `active` | boolean | Active flag |

**Unique Constraint**: `(company_id, product_id, size_label)`

---

### ProductionLogStatus
**Type**: Enum
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Value | Description |
|-------|-------------|
| `MIXED` | Initial state after mixing |
| `READY_TO_PACK` | Ready for packing operations |
| `PARTIAL_PACKED` | Partially packed |
| `FULLY_PACKED` | Fully packed, complete |

---

### PackingRequestRecord
**Table**: `packing_request_records`
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Stores idempotency keys for packing requests.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `company` | Company | Owning company |
| `idempotencyKey` | String | Idempotency key |
| `idempotencyHash` | String | Request payload hash |
| `productionLogId` | Long | Production log ID |
| `packingRecordId` | Long | Created packing record ID |

---

## Repositories

### ProductionLogRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyOrderByPlannedDateDesc(company)` | List plans |
| `findTop25ByCompanyOrderByProducedAtDesc(company)` | Recent logs |
| `findByCompanyAndProductionCodeStartingWithOrderByProductionCodeDesc(company, prefix)` | Find by code prefix |
| `countByCompany(company)` | Count batches |
| `incrementPackedQuantityAtomic(logId, quantity)` | Atomic increment |
| `findFullyPackedBatchesByMonth(company, start, end)` | Find by period |

### PackingRecordRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyAndProductionLogOrderByPackedDateAscIdAsc(company, log)` | Packing history |

### FactoryTaskRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyOrderByCreatedAtDesc(company)` | List tasks |
| `findByCompanyAndSalesOrderIdAndTitleIgnoreCase(company, salesOrderId, title)` | Find by key |

### ProductionPlanRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyOrderByPlannedDateDesc(company)` | List plans |
| `findByCompanyAndPlanNumber(company, planNumber)` | Find by number |
| `insertIfAbsent(...)` | Atomic insert |

### PackagingSizeMappingRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyOrderByPackagingSizeAsc(company)` | List mappings |
| `findByCompanyAndActiveOrderByPackagingSizeAsc(company, active)` | Active mappings |
| `findActiveByCompanyAndPackagingSizeIgnoreCase(company, size)` | Find by size |
| `existsByCompanyAndPackagingSizeIgnoreCaseAndRawMaterial(...)` | Check existence |

### SizeVariantRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyAndProductOrderBySizeLabelAsc(company, product)` | List variants |
| `findByCompanyAndProductAndSizeLabelIgnoreCase(company, product, label)` | Find by label |

### PackingRequestRecordRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

| Method | Description |
|--------|-------------|
| `findByCompanyAndIdempotencyKey(company, key)` | Find by key |

### ProductionLogMaterialRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Standard JPA repository for production log materials.

### ProductionBatchRepository
**Package**: `com.bigbrightpaints.erp.modules.factory.domain`

Standard JPA repository for production batches.

---

## Entity Relationships

```
Company
  └── ProductionPlan
        └── ProductionBatch
  └── ProductionLog
        ├── ProductionLogMaterial ──> RawMaterial
        │                            └── RawMaterialBatch
        └── PackingRecord
              ├── FinishedGood
              ├── FinishedGoodBatch
              ├── SizeVariant ──> ProductionProduct
              └── RawMaterial (packaging)
  └── FactoryTask
  └── PackagingSizeMapping ──> RawMaterial
  └── SizeVariant ──> ProductionProduct
  └── PackingRequestRecord
```
