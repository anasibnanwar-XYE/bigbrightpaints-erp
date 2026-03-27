# Inventory Module - Services
## Overview
The Inventory module provides 12 services handling finished goods, raw materials, dispatch, reservations, adjustments, batch tracking, opening stock imports, and valuation.
---
## FinishedGoodsService
**Path:** `modules/inventory/service/FinishedGoodsService.java`
**Purpose:** Facade service for finished goods operations
**Pattern:** Delegates to FinishedGoodsWorkflowEngineService for all business logic
**Key Methods:**
- `listFinishedGoods()` - List all finished goods
- `getFinishedGood(id)` - Get finished good by ID
- `lockFinishedGoodByProductCode(code)` - Lock and get by product code
- `currentWeightedAverageCost(fg)` - Get WAC
- `updateFinishedGood(id, request)` - Update finished good
- `listBatchesForFinishedGood(id)` - List batches for a finished good
- `registerBatch(request)` - Register a new batch
- `getStockSummary()` - Get stock summary
- `getLowStockItems(threshold)` - Get low stock items
- `getLowStockThreshold(id)` - Get low stock threshold
- `updateLowStockThreshold(id, threshold)` - Update threshold
- `listPackagingSlips()` - List packaging slips
- `reserveForOrder(order)` - Reserve inventory for sales order
- `releaseReservationsForOrder(orderId)` - Release reservations
- `accountingProfiles(productCodes)` - Get accounting profiles
- `markSlipDispatched(orderId)` - Mark slip as dispatched
- `getDispatchPreview(slipId)` - Get dispatch preview
- `confirmDispatch(request, username)` - Confirm dispatch
- `getDispatchConfirmation(slipId)` - Get existing confirmation
- `getPackagingSlip(slipId)` - Get slip details
- `getPackagingSlipByOrder(orderId)` - Get slip by order
- `updateSlipStatus(slipId, status)` - Update slip status
- `cancelBackorderSlip(slipId, username, reason)` - Cancel backorder slip
- `linkDispatchMovementsToJournal(slipId, journalId)` - Link movements to journal
- `invalidateWeightedAverageCost(fgId)` - Invalidate WAC cache

### Dependencies
- `FinishedGoodsWorkflowEngineService` - Workflow engine
### Records
- `FinishedGoodAccountingProfile` - Accounting profile for product
- `DispatchPosting` - Posting details for accounting
- `InventoryReservationResult` - Reservation result with shortages
- `InventoryShortage` - Shortage details
---
## RawMaterialService
**Path:** `modules/inventory/service/RawMaterialService.java`
**Purpose:** Manages raw material inventory, receipts, adjustments, and stock levels
### Key Methods:**
- `listRawMaterials()` - List raw materials
- `createRawMaterial(request)` - Create raw material
- `updateRawMaterial(id, request)` - Update raw material
- `deleteRawMaterial(id)` - Delete raw material
- `summarizeStock()` - Stock summary
- `listInventory()` - List inventory snapshots
- `listLowStock()` - List low stock items
- `listBatches(id)` - List batches for raw material
- `createBatch(id, request, key)` - Create batch (if intake enabled)
- `recordReceipt(id, request, context)` - Record receipt from supplier
- `intake(request, key)` - Intake raw material (if intake enabled)
- `adjustStock(request)` - Adjust stock
### Dependencies
- `RawMaterialRepository` - Raw material repo
- `RawMaterialBatchRepository` - Batch repo
- `RawMaterialMovementRepository` - Movement repo
- `RawMaterialAdjustmentRepository` - Adjustment repo
- `RawMaterialIntakeRepository` - Intake record repo
- `CompanyContextService` - Company context
- `ProductionProductRepository` - Product repo
- `ProductionBrandRepository` - Brand repo
- `AccountingFacade` - Accounting integration
- `BatchNumberService` - Batch numbering
- `ReferenceNumberService` - Reference numbering
- `CompanyClock` - Company clock
- `CompanyEntityLookup` - Entity lookup
- `AuditService` - Audit logging
- `Environment` - Spring environment
- `TransactionManager` - Transaction management
### Configuration
- `erp.raw-material.intake.enabled` (default: false) - Enable/disable manual intake
### Records
- `ReceiptContext` - Receipt context with reference info
- `ReceiptResult` - Receipt result with batch, movement, journal
---
## InventoryValuationService
**Path:** `modules/inventory/service/InventoryValuationService.java`
**Purpose:** Inventory valuation with costing method support (FIFO, LIFO, WAC)
### Key Methods:**
- `currentWeightedAverageCost(fg)` - Calculate WAC
- `stockSummaryUnitCost(fg)` - Get unit cost for stock summary
- `resolveDispatchUnitCost(fg, batch)` - Get unit cost for dispatch
- `requireNonZeroDispatchCost(fg, cost, qty)` - Validate non-zero dispatch cost
- `invalidateWeightedAverageCost(fgId)` - Invalidate WAC cache
### Caching
- WAC cached for 5 minutes
### Dependencies
- `FinishedGoodBatchRepository` - Batch repo
- `CostingMethodService` - Costing method resolution
- `CompanyClock` - Company clock
---
## InventoryBatchQueryService
**Path:** `modules/inventory/service/InventoryBatchQueryService.java`
**Purpose:** Query service for batch data
### Key Methods:**
- `listExpiringSoonBatches(days)` - List batches expiring within N days
### Dependencies
- `CompanyContextService` - Company context
- `CompanyClock` - Company clock
- `RawMaterialBatchRepository` - Raw material batch repo
- `FinishedGoodBatchRepository` - Finished good batch repo
---
## InventoryBatchTraceabilityService
**Path:** `modules/inventory/service/InventoryBatchTraceabilityService.java`
**Purpose:** Batch traceability with full movement history
### Key Methods:**
- `getBatchMovementHistory(batchId, batchType)` - Get complete movement history for a batch
### Dependencies
- `CompanyContextService` - Company context
- `FinishedGoodBatchRepository` - Batch repo
- `RawMaterialBatchRepository` - Raw material batch repo
- `InventoryMovementRepository` - Movement repo
- `RawMaterialMovementRepository` - Raw material movement repo
---
## BatchNumberService
**Path:** `modules/inventory/service/BatchNumberService.java`
**Purpose:** Generate unique batch codes and slip numbers
### Key Methods:**
- `nextRawMaterialBatchCode(material)` - Generate next RM batch code
- `nextFinishedGoodBatchCode(fg, date)` - Generate next FG batch code
- `nextPackagingSlipNumber(company)` - Generate next slip number
### Dependencies
- `NumberSequenceService` - Number sequence service
---
## InventoryAdjustmentService
**Path:** `modules/inventory/service/InventoryAdjustmentService.java`
**Purpose:** Finished goods adjustments (damage, recount, etc.)
### Key Methods:**
- `listAdjustments()` - List all adjustments
- `createAdjustment(request)` - Create adjustment with journal posting
### Idempotency
- Uses idempotency key with signature hash
### Costing Method Support
- FIFO, LIFO, WAC based on company settings
### Dependencies
- `CompanyContextService` - Company context
- `FinishedGoodRepository` - Finished good repo
- `InventoryAdjustmentRepository` - Adjustment repo
- `InventoryMovementRepository` - Movement repo
- `FinishedGoodBatchRepository` - Batch repo
- `AccountingFacade` - Accounting integration
- `ReferenceNumberService` - Reference numbering
- `CompanyClock` - Company clock
- `FinishedGoodsService` - Finished goods service
- `CostingMethodService` - Costing method resolution
---
## OpeningStockImportService
**Path:** `modules/inventory/service/OpeningStockImportService.java`
**Purpose:** CSV-based opening stock import for migration
### Key Methods:**
- `importOpeningStock(file, key, batchKey)` - Import opening stock from CSV
- `listImportHistory(page, size)` - Get import history
### CSV Processing
- Parses: type, sku, unit, batch_code, quantity, unit_cost, manufactured_at, expiry_date
- Validates SKU readiness (catalog, inventory, production, sales)
- Posts to inventory and opening balance accounts
### Configuration
- `erp.inventory.opening-stock.enabled` (default: false) - Enable opening stock imports
### Dependencies
- `CompanyContextService` - Company context
- `RawMaterialRepository` - Raw material repo
- `RawMaterialBatchRepository` - Batch repo
- `RawMaterialMovementRepository` - Movement repo
- `FinishedGoodRepository` - Finished good repo
- `FinishedGoodBatchRepository` - Batch repo
- `InventoryMovementRepository` - Movement repo
- `SkuReadinessService` - SKU readiness validation
- `BatchNumberService` - Batch numbering
- `AccountingFacade` - Accounting integration
- `AccountRepository` - Account repo
- `JournalEntryRepository` - Journal entry repo
- `OpeningStockImportRepository` - Import record repo
- `AuditService` - Audit logging
- `ObjectMapper` - JSON processing
- `CompanyClock` - Company clock
---
## PackagingSlipService
**Path:** `modules/inventory/service/PackagingSlipService.java`
**Purpose:** Packaging slip management
### Key Methods:**
- `listPackagingSlips()` - List all packaging slips
- `getPackagingSlip(slipId)` - Get slip by ID
- `getPackagingSlipByOrder(orderId)` - Get slip by order ID
- `updateSlipStatus(slipId, status)` - Update slip status
- `cancelBackorderSlip(slipId, username, reason)` - Cancel backorder slip
- `createBackorderSlip(slip)` - Create backorder slip
- `resolveBackorderSlipIdForResponse(slip, company, hasBackorder)` - Resolve backorder slip ID
### Dependencies
- `CompanyContextService` - Company context
- `PackagingSlipRepository` - Slip repo
- `InventoryReservationRepository` - Reservation repo
- `FinishedGoodRepository` - Finished good repo
- `FinishedGoodBatchRepository` - Batch repo
- `SalesOrderRepository` - Sales order repo
- `InventoryValuationService` - Valuation service
- `BatchNumberService` - Batch numbering
---
## DeliveryChallanPdfService
**Path:** `modules/inventory/service/DeliveryChallanPdfService.java`
**Purpose:** Generate delivery challan PDFs
### Key Methods:**
- `renderDeliveryChallanPdf(slipId)` - Render PDF for packaging slip
### Dependencies
- `CompanyContextService` - Company context
- `PackagingSlipRepository` - Slip repo
- `TemplateEngine` - Thymeleaf template engine
### Output
- PDF bytes with filename
---
## InventoryMovementRecorder
**Path:** `modules/inventory/service/InventoryMovementRecorder.java`
**Purpose:** Record inventory movements and publish events
### Key Methods:**
- `recordFinishedGoodMovement(...)` - Record movement and publish event
### Dependencies
- `InventoryMovementRepository` - Movement repo
- `ApplicationEventPublisher` - Event publisher
- `CompanyClock` - Company clock
---
## FinishedGoodsReservationEngine
**Path:** `modules/inventory/service/FinishedGoodsReservationEngine.java`
**Purpose:** Handle inventory reservations for sales orders
### Key Methods:**
- `reserveForOrder(order)` - Reserve inventory for order
- `releaseReservationsForOrder(orderId)` - Release reservations
- `rebuildReservationsFromSlip(slip, orderId)` - Rebuild from slip lines
- `slipLinesMatchOrder(slip, order)` - Validate slip matches order
- `resolveReservedQuantity(reservation)` - Get reserved quantity
### Dependencies
- `CompanyContextService` - Company context
- `FinishedGoodRepository` - Finished good repo
- `FinishedGoodBatchRepository` - Batch repo
- `PackagingSlipRepository` - Slip repo
- `InventoryMovementRepository` - Movement repo
- `InventoryReservationRepository` - Reservation repo
- `SalesOrderRepository` - Sales order repo
- `BatchNumberService` - Batch numbering
- `CostingMethodService` - Costing method resolution
- `CompanyClock` - Company clock
- `InventoryMovementRecorder` - Movement recorder
- `InventoryValuationService` - Valuation service
---
## FinishedGoodsDispatchEngine
**Path:** `modules/inventory/service/FinishedGoodsDispatchEngine.java`
**Purpose:** Handle dispatch workflow for sales orders
### Key Methods:**
- `markSlipDispatched(orderId)` - Mark slip as dispatched
- `getDispatchPreview(slipId)` - Get dispatch preview
- `confirmDispatch(request, username)` - Confirm dispatch
- `getDispatchConfirmation(slipId)` - Get confirmation details
- `linkDispatchMovementsToJournal(slipId, journalId)` - Link movements to journal
### Dependencies
- `CompanyContextService` - Company context
- `FinishedGoodRepository` - Finished good repo
- `FinishedGoodBatchRepository` - Batch repo
- `PackagingSlipRepository` - Slip repo
- `InventoryMovementRepository` - Movement repo
- `InventoryReservationRepository` - Reservation repo
 - `SalesOrderRepository` - Sales order repo
- `GstService` - GST calculation
- `CompanyClock` - Company clock
- `InventoryMovementRecorder` - Movement recorder
- `FinishedGoodsReservationEngine` - Reservation engine
- `PackagingSlipService` - Packaging slip service
- `InventoryValuationService` - Valuation service
---
## FinishedGoodsWorkflowEngineService
**Path:** `modules/inventory/service/FinishedGoodsWorkflowEngineService.java`
**Purpose:** Orchestrate finished goods workflows
### Key Methods:**
- `listFinishedGoods()` - List all finished goods
- `getFinishedGood(id)` - Get finished good by ID
- `lockFinishedGoodByProductCode(code)` - Lock and get by product code
- `currentWeightedAverageCost(fg)` - Get WAC
- `updateFinishedGood(id, request)` - Update finished good
- `listBatchesForFinishedGood(id)` - List batches
- `registerBatch(request)` - Register batch
- `getStockSummary()` - Get stock summary
- `getLowStockItems(threshold)` - Get low stock items
- `getLowStockThreshold(id)` - Get low stock threshold
- `updateLowStockThreshold(id, threshold)` - Update threshold
- `listPackagingSlips()` - List packaging slips
- `reserveForOrder(order)` - Reserve inventory for order
- `releaseReservationsForOrder(orderId)` - Release reservations
- `accountingProfiles(productCodes)` - Get accounting profiles
- `markSlipDispatched(orderId)` - Mark slip as dispatched
- `getDispatchPreview(slipId)` - Get dispatch preview
- `confirmDispatch(request, username)` - Confirm dispatch
- `getDispatchConfirmation(slipId)` - Get confirmation
- `getPackagingSlip(slipId)` - Get slip details
- `getPackagingSlipByOrder(orderId)` - Get slip by order
- `updateSlipStatus(slipId, status)` - Update slip status
- `cancelBackorderSlip(slipId, username, reason)` - Cancel backorder slip
- `linkDispatchMovementsToJournal(slipId, journalId)` - Link movements to journal
- `invalidateWeightedAverageCost(fgId)` - Invalidate WAC cache
### Dependencies
- `CompanyContextService` - Company context
- `CompanyEntityLookup` - Entity lookup
- `FinishedGoodRepository` - Finished good repo
- `FinishedGoodBatchRepository` - Batch repo
- `PackagingSlipRepository` - Slip repo
- `InventoryMovementRepository` - Movement repo
- `InventoryReservationRepository` - Reservation repo
- `BatchNumberService` - Batch numbering
- `SalesOrderRepository` - Sales order repo
- `CompanyDefaultAccountsService` - Default accounts
- `CostingMethodService` - Costing method resolution
- `GstService` - GST calculation
- `ApplicationEventPublisher` - Event publisher
- `CompanyClock` - Company clock
- `Environment` - Spring environment
### Configuration
- `erp.inventory.finished-goods.batch.enabled` (default: false) - Enable manual batch registration
### Internal Services
- `InventoryValuationService`
- `PackagingSlipService`
- `FinishedGoodsReservationEngine`
- `FinishedGoodsDispatchEngine`
---
## DispatchArtifactPaths
**Path:** `modules/inventory/service/DispatchArtifactPaths.java`
**Purpose:** Utility for delivery challan paths
### Methods
- `deliveryChallanNumber(slipNumber)` - Generate DC number
- `deliveryChallanPdfPath(slipId)` - Generate PDF path
