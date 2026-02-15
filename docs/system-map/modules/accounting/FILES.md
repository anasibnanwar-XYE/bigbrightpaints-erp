# ACCOUNTING Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/AGENTS.md | module governance notes and constraints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingCatalogController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingConfigurationController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/PayrollController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/Account.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountRepository.java | JPA repository for Account persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriod.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodRepository.java | JPA repository for AccountingPeriod persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodSnapshot.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodSnapshotRepository.java | JPA repository for AccountingPeriodSnapshot persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodStatus.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodTrialBalanceLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodTrialBalanceLineRepository.java | JPA repository for AccountingPeriodTrialBalanceLine persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/DealerLedgerEntry.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/DealerLedgerRepository.java | JPA repository for DealerLedger persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalCorrectionType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntry.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntryRepository.java | JPA repository for JournalEntry persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalLineRepository.java | JPA repository for JournalLine persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalReferenceMapping.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalReferenceMappingRepository.java | JPA repository for JournalReferenceMapping persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/PartnerSettlementAllocation.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/PartnerSettlementAllocationRepository.java | JPA repository for PartnerSettlementAllocation persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/PartnerType.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/SupplierLedgerEntry.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/SupplierLedgerRepository.java | JPA repository for SupplierLedger persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountBalanceView.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingPeriodCloseRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingPeriodDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingPeriodLockRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingPeriodReopenRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingTransactionAuditDetailDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccountingTransactionAuditListItemDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AccrualRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AgingBucketDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AgingSummaryResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/AuditDigestResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/BadDebtWriteOffRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/BankReconciliationRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/BankReconciliationSummaryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/CompanyDefaultAccountsRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/CompanyDefaultAccountsResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/CreditNoteRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/DealerBalanceView.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/DealerReceiptRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/DealerReceiptSplitRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/DealerSettlementRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/DebitNoteRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/GstReturnDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/InventoryCountRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/InventoryCountResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/InventoryCountTarget.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/InventoryRevaluationRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/JournalEntryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/JournalEntryRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/JournalEntryReversalRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/JournalLineDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/LandedCostRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/MonthEndChecklistDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/MonthEndChecklistItemDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/MonthEndChecklistUpdateRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/PartnerSettlementResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/PartnerStatementResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/PayrollBatchPaymentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/PayrollBatchPaymentResponse.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/PayrollPaymentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SalesReturnRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SettlementAllocationRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SettlementPaymentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/StatementTransactionDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SupplierBalanceView.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SupplierPaymentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/SupplierSettlementRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/WipAdjustmentRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountCacheInvalidatedEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEvent.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventRepository.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventStore.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventType.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/InventoryAccountingEventListener.java | event contracts and event persistence models
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AbstractPartnerLedgerService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountHierarchyService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingAuditTrailService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingEventTrailAlertRoutingPolicy.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacade.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodSnapshotService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AgingReportService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AuditDigestScheduler.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyAccountingSettingsService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/DealerLedgerService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/JournalReferenceResolver.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/NoopPeriodCloseHook.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/PeriodCloseHook.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReconciliationService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReferenceNumberService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/StatementService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/SupplierLedgerService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TaxService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TemporalBalanceService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V115__accounting_event_sequence_unique.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V5__accounting_tables.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V66__accounting_journal_unique_constraint.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V67__accounting_journal_indexes.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V70__accounting_event_store.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V93__accounting_performance_indexes.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V10__accounting_replay_hotspot_indexes.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V11__accounting_replay_index_cleanup.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V15__accounting_audit_read_model_hotspot_indexes.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V16__accounting_audit_read_model_hotspot_indexes_journal_lines.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V17__accounting_audit_read_model_hotspot_indexes_invoices.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V18__accounting_audit_read_model_hotspot_indexes_purchases.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V2__accounting_core.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingCatalogControllerIdempotencyHeaderTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingCatalogControllerSecurityIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingControllerActivityContractTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingControllerExceptionHandlerTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingControllerIdempotencyHeaderParityTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountTypeTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntryTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalLineRepositoryIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/domain/PartnerSettlementAllocationTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/dto/SupplierSettlementRequestJsonCompatTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingAuditTrailServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingEventTrailAlertRoutingPolicyTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacadeTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceBenchmarkTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/AgingReportServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/ReferenceNumberServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/StatementServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/TaxServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/accounting/service/TemporalBalanceServiceTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java | Core accounting API entrypoint for journals, settlements, and vouchers
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/PayrollController.java | Payroll accounting exposure endpoint
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingCatalogController.java | Accounting catalog and reference lookups

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java | Posting and settlement orchestration
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacade.java | Canonical posting path for accounting effects
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/JournalReferenceResolver.java | Reference and dedupe control
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java | Period lock/reopen control
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReconciliationService.java | Reconciliation invariants
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReferenceNumberService.java | Stable reference generation
