# REPORTS Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/controller/ReportController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/AccountStatementEntryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/AgedDebtorDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/BalanceSheetDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/BalanceWarningDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/CashFlowDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/InventoryValuationDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ProfitLossDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ReconciliationDashboardDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ReconciliationSummaryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ReportMetadata.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ReportSource.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/TrialBalanceDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/InventoryValuationService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/ReportService.java | business service logic for module workflows
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/ReportControllerSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/ReportInventoryParityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/controller/ReportControllerContractTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/service/InventoryValuationServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/service/ReportServiceAccountStatementTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/reports/service/ReportServiceCostBreakdownTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/controller/ReportController.java | Reporting API entrypoint

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/ReportService.java | Core report generation logic
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/service/InventoryValuationService.java | Inventory valuation computation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/TrialBalanceDto.java | Trial balance schema integrity
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/dto/ReportMetadata.java | Report metadata and filter semantics
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/reports/controller/ReportController.java | Report visibility and RBAC filters
