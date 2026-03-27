# Factory Services

Business logic layer for the Factory module.

## Core Services

### FactoryService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Primary service for production planning and task management.

| Method | Description |
|--------|-------------|
| `listPlans()` | List all production plans for current company |
| `createPlan(ProductionPlanRequest)` | Create a production plan with idempotency |
| `updatePlan(Long id, ProductionPlanRequest)` | Update plan details |
| `updatePlanStatus(Long id, String status)` | Update plan status |
| `deletePlan(Long id)` | Delete a plan |
| `listTasks()` | List factory tasks |
| `createTask(FactoryTaskRequest)` | Create a task with idempotency |
| `updateTask(Long id, FactoryTaskRequest)` | Update task details |
| `dashboard()` | Get dashboard metrics (efficiency, completed plans, batches) |

**Dependencies**: `CompanyContextService`, `ProductionPlanRepository`, `FactoryTaskRepository`, `CompanyEntityLookup`

---

### ProductionLogService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Manages production log creation and retrieval. Implements M2S (Manufacturing-to-Stock) flow.

| Method | Description |
|--------|-------------|
| `createLog(ProductionLogRequest)` | Create production log with material consumption |
| `recentLogs()` | List recent production logs |
| `getLog(Long id)` | Get log details with materials and packing records |

**Key Operations in createLog()**:
1. Lock company row for sequential code generation
2. Validate brand and product ownership
3. Issue raw materials using FIFO batch selection
4. Calculate material, labor, and overhead costs
5. Create semi-finished batch and inventory movement
6. Post material journal entry
7. Post labor/overhead journal entry

**Dependencies**: `CompanyContextService`, `ProductionLogRepository`, `RawMaterialBatchRepository`, `AccountingFacade`, `FinishedGoodBatchRepository`, `PackingAllowedSizeService`

---

### PackingService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Handles standard packing operations for production batches.

| Method | Description |
|--------|-------------|
| `recordPacking(PackingRequest)` | Record packing with idempotency support |
| `listUnpackedBatches()` | List batches ready for packing |
| `packingHistory(Long productionLogId)` | Get packing history for a batch |

**Packing Flow (recordPacking)**:
1. Lock production log row
2. Reserve idempotency key if provided
3. For each packing line:
   - Resolve size variant and target finished good
   - Consume packaging material via `PackagingMaterialService`
   - Consume semi-finished inventory via `PackingInventoryService`
   - Register finished good batch via `PackingBatchService`
   - Post packaging material journal
4. Update production log packed quantity and status
5. Handle residual wastage if requested

**Dependencies**: `CompanyContextService`, `ProductionLogRepository`, `PackingRecordRepository`, `PackagingMaterialService`, `PackingProductSupport`, `PackingAllowedSizeService`, `PackingLineResolver`, `PackingIdempotencyService`, `PackingInventoryService`, `PackingBatchService`, `PackingJournalBuilder`, `PackingReadService`

---

### BulkPackingService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Handles bulk-to-size packing operations (repacking bulk batches into smaller SKUs).

| Method | Description |
|--------|-------------|
| `pack(BulkPackRequest)` | Pack bulk batch into sized child SKUs |
| `listBulkBatches(Long finishedGoodId)` | List available bulk batches |
| `listChildBatches(Long parentBatchId)` | List child batches from a parent |

**Bulk Pack Flow**:
1. Validate pack lines (no duplicates, valid quantities)
2. Lock bulk batch row
3. Check idempotency (return existing if found)
4. Verify sufficient bulk stock
5. Consume packaging materials via `BulkPackingCostService`
6. Create child batches via `BulkPackingOrchestrator`
7. Consume bulk inventory via `BulkPackingInventoryService`
8. Post packaging journal entry

**Dependencies**: `CompanyContextService`, `FinishedGoodBatchRepository`, `AccountingFacade`, `BatchNumberService`, `BulkPackingOrchestrator`, `BulkPackingCostService`, `BulkPackingInventoryService`, `BulkPackingReadService`, `PackingJournalLinkHelper`

---

## Supporting Services

### PackagingMaterialService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Manages packaging size mappings and automatic material consumption.

| Method | Description |
|--------|-------------|
| `listMappings()` | List all packaging mappings |
| `listActiveMappings()` | List active mappings only |
| `createMapping(PackagingSizeMappingRequest)` | Create a packaging rule |
| `updateMapping(Long id, PackagingSizeMappingRequest)` | Update a rule |
| `deactivateMapping(Long id)` | Deactivate a rule |
| `consumePackagingMaterial(size, piecesCount, referenceId, ...)` | Consume packaging during packing |

**Dependencies**: `CompanyContextService`, `PackagingSizeMappingRepository`, `RawMaterialBatchRepository`, `RawMaterialMovementRepository`

---

### CostAllocationService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Allocates period labor and overhead costs to production batches.

| Method | Description |
|--------|-------------|
| `allocateCosts(CostAllocationRequest)` | Allocate month costs to fully packed batches |

**Allocation Flow**:
1. Find all fully packed batches in the period
2. Calculate total liters produced
3. Skip batches with existing cost variance journals
4. Calculate labor and overhead variance
5. Allocate variance proportionally by liters
6. Update batch costs and unit costs
7. Post cost variance journal entries

**Dependencies**: `CompanyContextService`, `ProductionLogRepository`, `FinishedGoodBatchRepository`, `AccountingFacade`, `CompanyEntityLookup`

---

### PackingIdempotencyService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Ensures idempotent packing operations.

| Method | Description |
|--------|-------------|
| `packingRequestHash(PackingRequest, LocalDate)` | Generate request hash |
| `reserveIdempotencyRecord(company, logId, key, hash)` | Reserve or retrieve existing |
| `markCompleted(reservation, packingRecordId)` | Mark as completed |

---

### PackingAllowedSizeService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Resolves allowed sellable size targets for production batches.

| Method | Description |
|--------|-------------|
| `listAllowedSellableSizes(Company, ProductionLog)` | List available size targets |
| `resolveAllowedSellableSizeTargets(...)` | Load and resolve targets |
| `requireAllowedSellableSize(...)` | Validate and return target |

---

### PackingReadService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Read-only service for packing queries.

| Method | Description |
|--------|-------------|
| `listUnpackedBatches()` | List batches ready for packing |
| `packingHistory(Long productionLogId)` | Get packing history |

---

### PackingInventoryService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Handles semi-finished inventory operations during packing.

| Method | Description |
|--------|-------------|
| `consumeSemiFinishedInventory(log, quantity, packingRecordId)` | Consume semi-finished stock |
| `consumeSemiFinishedWastage(log, wastageQty)` | Consume wastage |

---

### PackingBatchService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Registers finished good batches during packing.

| Method | Description |
|--------|-------------|
| `registerFinishedGoodBatch(...)` | Create FG batch with costs and journal |

---

### PackingLineResolver
**Package**: `com.bigbrightpaints.erp.modules.factory.service` (Component)

Resolves packing line details.

| Method | Description |
|--------|-------------|
| `normalizePackagingSize(size, lineNumber)` | Normalize size string |
| `resolveSizeVariant(company, log, packagingSize)` | Get or create size variant |
| `resolvePiecesPerBox(line, sizeVariant)` | Calculate pieces per box |
| `resolvePiecesCountForLine(...)` | Calculate total pieces |
| `resolveQuantity(...)` | Calculate quantity in liters |
| `resolveChildBatchCount(line, piecesCount)` | Get child batch count |

---

### PackingProductSupport
**Package**: `com.bigbrightpaints.erp.modules.factory.service` (Component)

Product-related utilities for packing.

| Method | Description |
|--------|-------------|
| `ensureFinishedGood(company, log)` | Get or create finished good |
| `resolveTargetFinishedGood(...)` | Resolve child FG |
| `isMatchingChildSku(candidate, parent)` | Validate SKU family |
| `requireWipAccountId(product)` | Get WIP account |
| `requireSemiFinishedAccountId(product)` | Get semi-finished account |
| `semiFinishedSku(product)` | Generate bulk SKU code |

---

### PackingJournalBuilder
**Package**: `com.bigbrightpaints.erp.modules.factory.service` (Component)

Builds journal line entries for packing operations.

| Method | Description |
|--------|-------------|
| `buildWipPackagingConsumptionLines(...)` | Build packaging consumption lines |
| `buildBulkToSizePackingLines(...)` | Build bulk-to-size lines |

---

### PackingJournalLinkHelper
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Links inventory movements to journal entries.

| Method | Description |
|--------|-------------|
| `linkPackagingMovementsToJournal(company, referenceId, journalEntryId)` | Link movements |

---

### BulkPackingOrchestrator
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Orchestrates bulk-to-size packing operations.

| Method | Description |
|--------|-------------|
| `validatePackLines(request)` | Validate pack lines |
| `calculateTotalVolume(packs)` | Calculate total volume |
| `resolveTotalPacks(packs)` | Get total pack count |
| `createChildBatch(...)` | Create child batch |
| `buildBulkToSizeJournalLines(...)` | Build journal lines |
| `extractSizeInLiters(sizeLabel, unit)` | Parse size |

---

### BulkPackingCostService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Handles packaging costs for bulk packing.

| Method | Description |
|--------|-------------|
| `consumePackagingIfRequired(company, request, reference)` | Consume packaging |
| `createCostingContext(...)` | Create cost context |
| `resolveLinePackagingCostPerUnit(...)` | Get per-unit cost |

---

### BulkPackingInventoryService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Handles inventory for bulk packing.

| Method | Description |
|--------|-------------|
| `consumeBulkInventory(bulkBatch, totalVolume, packReference)` | Consume bulk stock |

---

### BulkPackingReadService
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Read service for bulk packing.

| Method | Description |
|--------|-------------|
| `resolveIdempotentPack(company, bulkBatch, reference)` | Check existing pack |
| `listBulkBatches(company, finishedGoodId)` | List bulk batches |
| `listChildBatches(company, parentBatchId)` | List child batches |
| `toChildBatchDto(...)` | Convert to DTO |

---

### FinishedGoodBatchRegistrar
**Package**: `com.bigbrightpaints.erp.modules.factory.service`

Registers finished good batch receipts.

| Method | Description |
|--------|-------------|
| `registerReceipt(ReceiptRegistrationRequest)` | Create batch, update stock, record movement |

---

### PackagingSizeParser
**Package**: `com.bigbrightpaints.erp.modules.factory.service` (Package-private)

Parses packaging size strings.

| Method | Description |
|--------|-------------|
| `parseSizeInLiters(String label)` | Parse size with unit (e.g., "5L", "500ML") |
| `parseSizeInLitersAllowBareNumber(String label)` | Parse size allowing bare numbers |

---

### FactorySlipEventListener
**Package**: `com.bigbrightpaints.erp.modules.factory.service` (Component)

Event listener for packaging slip events.

| Method | Description |
|--------|-------------|
| `onSlipEvent(PackagingSlipEvent)` | Log slip lifecycle events |

---

## Service Summary

| Service | Type | Primary Responsibility |
|---------|------|----------------------|
| FactoryService | @Service | Plans, tasks, dashboard |
| ProductionLogService | @Service | M2S production logging |
| PackingService | @Service | Standard packing operations |
| BulkPackingService | @Service | Bulk-to-size packing |
| PackagingMaterialService | @Service | Packaging rules and consumption |
| CostAllocationService | @Service | Period cost allocation |
| PackingIdempotencyService | @Service | Packing idempotency |
| PackingAllowedSizeService | @Service | Size target resolution |
| PackingReadService | @Service | Packing queries |
| PackingInventoryService | @Service | Semi-finished inventory |
| PackingBatchService | @Service | FG batch registration |
| PackingLineResolver | @Component | Line resolution |
| PackingProductSupport | @Component | Product utilities |
| PackingJournalBuilder | @Component | Journal line building |
| PackingJournalLinkHelper | @Service | Movement-journal linking |
| BulkPackingOrchestrator | @Service | Bulk packing orchestration |
| BulkPackingCostService | @Service | Bulk packaging costs |
| BulkPackingInventoryService | @Service | Bulk inventory |
| BulkPackingReadService | @Service | Bulk packing queries |
| FinishedGoodBatchRegistrar | @Service | FG batch registration |
| FactorySlipEventListener | @Component | Event handling |
