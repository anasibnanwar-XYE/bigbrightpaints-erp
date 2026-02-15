# SALES Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/AGENTS.md | module governance notes and constraints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/config/SalesAccountConfigurationValidator.java | configuration and feature wiring for module
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/CreditLimitOverrideController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerPortalController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/SalesController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/CreditLimitOverrideRequest.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/CreditLimitOverrideRequestRepository.java | JPA repository for CreditLimitOverrideRequest persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/CreditRequest.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/CreditRequestRepository.java | JPA repository for CreditRequest persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/Dealer.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/DealerRepository.java | JPA repository for Dealer persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/OrderSequence.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/OrderSequenceRepository.java | JPA repository for OrderSequence persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/Promotion.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/PromotionRepository.java | JPA repository for Promotion persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrder.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrderItem.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrderItemRepository.java | JPA repository for SalesOrderItem persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrderRepository.java | JPA repository for SalesOrder persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesTarget.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesTargetRepository.java | JPA repository for SalesTarget persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreateDealerRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreditLimitOverrideDecisionRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreditLimitOverrideRequestCreateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreditLimitOverrideRequestDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreditRequestDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/CreditRequestRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DealerDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DealerLookupResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DealerPortalCreditRequestCreateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DealerRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DealerResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DispatchConfirmRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DispatchConfirmResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/DispatchMarkerReconciliationResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/PromotionDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/PromotionRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesOrderDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesOrderItemDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesOrderItemRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesOrderRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesTargetDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/dto/SalesTargetRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/event/SalesOrderCreatedEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/CreditLimitOverrideService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerPortalService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DunningService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/OrderNumberService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesFulfillmentService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesOrderCreditExposurePolicy.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/util/SalesOrderReference.java | helper utilities for module logic
erp-domain/src/main/resources/db/migration/V108__sales_order_gst_inclusive.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V10__sales_order_items.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V16__factory_sales_dealer_roles.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V21__sales_gst_and_accounts.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V4__sales_tables.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V55__sales_order_idempotency.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V75__sales_order_idempotency_markers.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V85__factory_task_sales_order_links.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V8__sales_fulfillment_extensions.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V3__sales_invoice.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/DealerControllerSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/DealerPortalControllerSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/SalesControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/controller/SalesControllerIdempotencyHeaderTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/dto/SalesOrderRequestTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/CreditLimitOverrideServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/DealerPortalServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/DealerServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/OrderNumberServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesFulfillmentServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/service/SalesServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/util/SalesOrderReferenceTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/SalesController.java | Canonical sales order API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerController.java | Dealer API and onboarding
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/DealerPortalController.java | Dealer portal API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/CreditLimitOverrideController.java | Credit limit override API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/controller/CreditLimitOverrideController.java | Credit override API

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java | Canonical sales workflow orchestration
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesFulfillmentService.java | Fulfillment state transitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerService.java | Dealer onboarding/binding service
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesJournalService.java | Sales journal request creation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java | Return handling and reversals
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/util/SalesOrderReference.java | Canonical reference mapping
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java | Dispatch order command path
