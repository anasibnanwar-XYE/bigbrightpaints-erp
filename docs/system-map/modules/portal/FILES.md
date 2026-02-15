# PORTAL Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/controller/PortalInsightsController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/DashboardInsights.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/EnterpriseDashboardSnapshot.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/OperationsInsights.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/WorkforceInsights.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/EnterpriseDashboardService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/PortalInsightsService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V17__dealer_portal_support.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V9__dealer_portal_user_binding_hardening.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/portal/PortalInsightsControllerIT.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/controller/PortalInsightsController.java | Dashboard and insights API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/PortalInsightsService.java | Insights aggregation API service
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/EnterpriseDashboardService.java | Enterprise dashboard orchestration

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/PortalInsightsService.java | Cross-module read aggregation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/service/EnterpriseDashboardService.java | Enterprise-wide snapshot integrity
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/EnterpriseDashboardSnapshot.java | Snapshot schema and field evolution
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/dto/OperationsInsights.java | Operations metrics contract
