# FACTORY Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/FactoryController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/PackagingMappingController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/PackingController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/ProductionLogController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/FactoryTask.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/FactoryTaskRepository.java | JPA repository for FactoryTask persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackagingSizeMapping.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackagingSizeMappingRepository.java | JPA repository for PackagingSizeMapping persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackingRecord.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackingRecordRepository.java | JPA repository for PackingRecord persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackingRequestRecord.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/PackingRequestRecordRepository.java | JPA repository for PackingRequestRecord persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionBatch.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionBatchRepository.java | JPA repository for ProductionBatch persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionLog.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionLogMaterial.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionLogRepository.java | JPA repository for ProductionLog persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionLogStatus.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionPlan.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/domain/ProductionPlanRepository.java | JPA repository for ProductionPlan persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/BulkPackRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/BulkPackResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/CostAllocationRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/CostAllocationResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/CostBreakdownDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/FactoryDashboardDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/FactoryTaskDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/FactoryTaskRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/MonthlyProductionCostDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackagingConsumptionResult.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackagingSizeMappingDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackagingSizeMappingRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackingLineRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackingRecordDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/PackingRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionBatchDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionBatchRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionLogDetailDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionLogDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionLogMaterialDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionLogRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionPlanDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/ProductionPlanRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/UnpackedBatchDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/dto/WastageReportDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/event/PackagingSlipEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/CostAllocationService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/FactoryService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/FactorySlipEventListener.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackagingMaterialService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackagingSizeParser.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackingService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V16__factory_sales_dealer_roles.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V6__factory_tables.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V85__factory_task_sales_order_links.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/PackingControllerSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/controller/PackingControllerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingServiceIdempotencyTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/PackagingMaterialServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/PackagingSizeParserTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/PackingServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogServiceCostingFallbackTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/ProductionLogController.java | Production log endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/PackingController.java | Packing and conversion endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/controller/FactoryController.java | Factory task control endpoints

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackingService.java | Packing and conversion orchestration
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingService.java | Bulk split and child-batch conversion
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/CostAllocationService.java | Cost attribution
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogService.java | Production log lifecycle
