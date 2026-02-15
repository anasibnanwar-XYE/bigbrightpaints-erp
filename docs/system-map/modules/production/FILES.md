# PRODUCTION Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/controller/ProductionCatalogController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/CatalogImport.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/CatalogImportRepository.java | JPA repository for CatalogImport persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/ProductionBrand.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/ProductionBrandRepository.java | JPA repository for ProductionBrand persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/ProductionProduct.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/ProductionProductRepository.java | JPA repository for ProductionProduct persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/BulkVariantRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/BulkVariantResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/CatalogImportResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/ProductCreateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/ProductUpdateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/ProductionBrandDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/ProductionProductDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V111__backfill_production_product_metadata.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V18__production_catalog.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V19__production_logs.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V20__production_product_pricing.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V41__production_log_schema_backfill.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V45__production_logs_alignment.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogServiceRetryPolicyTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/controller/ProductionCatalogController.java | Production catalog API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogService.java | Catalog mutation API entry

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/service/ProductionCatalogService.java | Catalog write logic
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/ProductionProductRepository.java | Product catalog persistence boundaries
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/domain/CatalogImportRepository.java | Product import/backfill process
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/ProductCreateRequest.java | Product creation payload correctness
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/production/dto/BulkVariantRequest.java | Bulk import shape and validation
