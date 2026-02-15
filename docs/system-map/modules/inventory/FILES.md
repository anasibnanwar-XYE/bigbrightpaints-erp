# INVENTORY Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/AGENTS.md | module governance notes and constraints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/DispatchController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/FinishedGoodController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/InventoryAdjustmentController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/OpeningStockImportController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/RawMaterialController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGood.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodBatch.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodBatchRepository.java | JPA repository for FinishedGoodBatch persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodRepository.java | JPA repository for FinishedGood persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryAdjustment.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryAdjustmentLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryAdjustmentRepository.java | JPA repository for InventoryAdjustment persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryAdjustmentType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryMovement.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryMovementRepository.java | JPA repository for InventoryMovement persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryReference.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryReservation.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryReservationRepository.java | JPA repository for InventoryReservation persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/MaterialType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/OpeningStockImport.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/OpeningStockImportRepository.java | JPA repository for OpeningStockImport persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/PackagingSlip.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/PackagingSlipLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/PackagingSlipLineRepository.java | JPA repository for PackagingSlipLine persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/PackagingSlipRepository.java | JPA repository for PackagingSlip persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterial.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialBatch.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialBatchRepository.java | JPA repository for RawMaterialBatch persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialIntakeRecord.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialIntakeRepository.java | JPA repository for RawMaterialIntake persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialMovement.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialMovementRepository.java | JPA repository for RawMaterialMovement persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/RawMaterialRepository.java | JPA repository for RawMaterial persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/DispatchConfirmationRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/DispatchConfirmationResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/DispatchPreviewDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/FinishedGoodBatchDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/FinishedGoodBatchRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/FinishedGoodDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/FinishedGoodRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/InventoryAdjustmentDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/InventoryAdjustmentLineDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/InventoryAdjustmentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/InventoryStockSnapshot.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/OpeningStockImportResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/PackagingSlipDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/PackagingSlipLineDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/RawMaterialBatchDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/RawMaterialBatchRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/RawMaterialDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/RawMaterialIntakeRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/RawMaterialRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/dto/StockSummaryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/event/InventoryMovementEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/event/InventoryValuationChangedEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/BatchNumberService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/InventoryAdjustmentService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/OpeningStockImportService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/RawMaterialService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V120__inventory_movements_packaging_slip_id.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V122__inventory_adjustment_idempotency.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V123__inventory_opening_stock_intake_idempotency.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V29__inventory_movement_journal_link.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V31__raw_material_inventory_account.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V3__inventory_tables.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V52__inventory_non_negative_guards.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V58__inventory_reservation_nullable.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V60__inventory_adjustments.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V65__performance_accounting_inventory_indexes.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V69__packaging_size_mappings_and_gst_inventory.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V9__finished_goods_inventory.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V4__inventory_production.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/InventorySmokeIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/RawMaterialAndProductUpdateIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/controller/DispatchControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/controller/InventoryAdjustmentControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/controller/OpeningStockImportControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/controller/RawMaterialControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/domain/InventoryReferenceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/service/AuditFixesIntegrationTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/service/BatchNumberServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsServiceTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/DispatchController.java | Dispatch endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/FinishedGoodController.java | Finished goods APIs
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/controller/InventoryAdjustmentController.java | Stock adjustment APIs

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java | Dispatch and finished-goods linkage logic
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/RawMaterialService.java | Raw material movements and valuation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/InventoryAdjustmentService.java | Adjustment correctness and audit
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/BatchNumberService.java | Batch generation idempotency
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/OpeningStockImportService.java | Opening stock replay and reconciliation
