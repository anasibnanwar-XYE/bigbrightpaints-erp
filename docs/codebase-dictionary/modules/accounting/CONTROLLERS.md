# Accounting Controllers

## Overview

| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| AccountingController | 60+ | Main accounting operations |
| AccountingConfigurationController | 1 | Configuration health check |
| AccountingCatalogController | 0 | Placeholder (empty) |
| AccountingAuditTrailController | 1 | Audit trail queries |
| PayrollController | 1 | Payroll batch payments |
| TallyImportController | 1 | Tally XML import |
| OpeningBalanceImportController | 1 | Opening balance CSV import |

---

## AccountingController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/accounting`

**Dependencies**:
- AccountingService
- JournalEntryService
- DealerReceiptService
- SettlementService
- CreditDebitNoteService
- AccountingAuditService
- InventoryAccountingService
- AccountingFacade
- SalesReturnService
- AccountingPeriodService
- ReconciliationService
- StatementService
- TaxService
- TemporalBalanceService
- AccountHierarchyService
- AgingReportService
- CompanyDefaultAccountsService
- AccountingAuditTrailService
- CompanyContextService
- CompanyClock
- BankReconciliationSessionService
- AuditService

### Endpoints

#### Account Management

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/accounts` | `ResponseEntity<ApiResponse<List<AccountDto>>> accounts()` | List all accounts |
| POST | `/accounts` | `ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody AccountRequest request)` | Create new account |
| GET | `/default-accounts` | `ResponseEntity<ApiResponse<CompanyDefaultAccountsResponse>> defaultAccounts()` | Get company default accounts |
| PUT | `/default-accounts` | `ResponseEntity<ApiResponse<CompanyDefaultAccountsResponse>> updateDefaultAccounts(@Valid @RequestBody CompanyDefaultAccountsRequest request)` | Update default accounts |
| GET | `/accounts/tree` | `ResponseEntity<ApiResponse<List<AccountNode>>> getChartOfAccountsTree()` | Get account hierarchy |
| GET | `/accounts/tree/{type}` | `ResponseEntity<ApiResponse<List<AccountNode>>> getAccountTreeByType(@PathVariable String type)` | Get hierarchy by account type |
| GET | `/accounts/{accountId}/balance/as-of` | `ResponseEntity<ApiResponse<BigDecimal>> getBalanceAsOf(@PathVariable Long accountId, @RequestParam String date)` | Balance as of date |
| GET | `/accounts/{accountId}/activity` | `ResponseEntity<ApiResponse<AccountActivityReport>> getAccountActivity(@PathVariable Long accountId, @RequestParam(required=false) String startDate, @RequestParam(required=false) String endDate, @RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Account activity report |
| GET | `/accounts/{accountId}/balance/compare` | `ResponseEntity<ApiResponse<BalanceComparison>> compareBalances(@PathVariable Long accountId, @RequestParam String date1, @RequestParam String date2)` | Compare balances |

#### Journal Entries

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/journal-entries` | `ResponseEntity<ApiResponse<List<JournalEntryDto>>> journalEntries(@RequestParam(required=false) Long dealerId, @RequestParam(required=false) Long supplierId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="100") int size)` | List journal entries |
| POST | `/journal-entries` | `ResponseEntity<ApiResponse<JournalEntryDto>> createJournalEntry(@Valid @RequestBody JournalEntryRequest request)` | Create journal entry |
| POST | `/journal-entries/{entryId}/reverse` | `ResponseEntity<ApiResponse<JournalEntryDto>> reverseJournalEntry(@PathVariable Long entryId, @RequestBody(required=false) JournalEntryReversalRequest request)` | Reverse journal entry |
| POST | `/journal-entries/{entryId}/cascade-reverse` | `ResponseEntity<ApiResponse<List<JournalEntryDto>>> cascadeReverseJournalEntry(@PathVariable Long entryId, @RequestBody JournalEntryReversalRequest request)` | Cascade reverse related entries |
| GET | `/journals` | `ResponseEntity<ApiResponse<List<JournalListItemDto>>> listJournals(@RequestParam(required=false) LocalDate fromDate, @RequestParam(required=false) LocalDate toDate, @RequestParam(required=false) String type, @RequestParam(required=false) String sourceModule)` | List journals with filters |
| POST | `/journals/manual` | `ResponseEntity<ApiResponse<JournalEntryDto>> createManualJournal(@Valid @RequestBody ManualJournalRequest request)` | Create manual journal |

#### Dealer Receipts & Settlements

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/receipts/dealer` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerReceipt(@Valid @RequestBody DealerReceiptRequest request, @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey, @RequestHeader(value="X-Idempotency-Key", required=false) String legacyIdempotencyKey)` | Record dealer receipt |
| POST | `/receipts/dealer/hybrid` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordDealerHybridReceipt(@Valid @RequestBody DealerReceiptSplitRequest request, ...)` | Record split receipt |
| POST | `/settlements/dealers` | `ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleDealer(@Valid @RequestBody DealerSettlementRequest request, ...)` | Settle dealer invoices |
| POST | `/dealers/{dealerId}/auto-settle` | `ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleDealer(@PathVariable Long dealerId, @Valid @RequestBody AutoSettlementRequest request, ...)` | Auto-settle dealer |

#### Supplier Payments & Settlements

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/suppliers/payments` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordSupplierPayment(@Valid @RequestBody SupplierPaymentRequest request, ...)` | Record supplier payment |
| POST | `/settlements/suppliers` | `ResponseEntity<ApiResponse<PartnerSettlementResponse>> settleSupplier(@Valid @RequestBody SupplierSettlementRequest request, ...)` | Settle supplier invoices |
| POST | `/suppliers/{supplierId}/auto-settle` | `ResponseEntity<ApiResponse<PartnerSettlementResponse>> autoSettleSupplier(@PathVariable Long supplierId, @Valid @RequestBody AutoSettlementRequest request, ...)` | Auto-settle supplier |

#### Credit/Debit Notes

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/credit-notes` | `ResponseEntity<ApiResponse<JournalEntryDto>> postCreditNote(@Valid @RequestBody CreditNoteRequest request)` | Post credit note |
| POST | `/debit-notes` | `ResponseEntity<ApiResponse<JournalEntryDto>> postDebitNote(@Valid @RequestBody DebitNoteRequest request)` | Post debit note |
| POST | `/accruals` | `ResponseEntity<ApiResponse<JournalEntryDto>> postAccrual(@Valid @RequestBody AccrualRequest request)` | Post accrual |
| POST | `/bad-debts/write-off` | `ResponseEntity<ApiResponse<JournalEntryDto>> writeOffBadDebt(@Valid @RequestBody BadDebtWriteOffRequest request)` | Write off bad debt |

#### Sales Returns

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/sales/returns` | `ResponseEntity<ApiResponse<List<JournalEntryDto>>> listSalesReturns()` | List sales returns |
| POST | `/sales/returns/preview` | `ResponseEntity<ApiResponse<SalesReturnPreviewDto>> previewSalesReturn(@Valid @RequestBody SalesReturnRequest request)` | Preview sales return |
| POST | `/sales/returns` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordSalesReturn(@Valid @RequestBody SalesReturnRequest request)` | Record sales return |

#### Period Management

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/periods` | `ResponseEntity<ApiResponse<List<AccountingPeriodDto>>> listPeriods()` | List accounting periods |
| POST | `/periods` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> createOrUpdatePeriod(@Valid @RequestBody AccountingPeriodUpsertRequest request)` | Create/update period |
| PUT | `/periods/{periodId}` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> updatePeriod(@PathVariable Long periodId, @Valid @RequestBody AccountingPeriodUpdateRequest request)` | Update period |
| POST | `/periods/{periodId}/close` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> closePeriod(@PathVariable Long periodId, @RequestBody(required=false) AccountingPeriodCloseRequest request)` | Close period (disabled) |
| POST | `/periods/{periodId}/request-close` | `ResponseEntity<ApiResponse<PeriodCloseRequestDto>> requestPeriodClose(@PathVariable Long periodId, @RequestBody(required=false) PeriodCloseRequestActionRequest request)` | Request period close |
| POST | `/periods/{periodId}/approve-close` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> approvePeriodClose(@PathVariable Long periodId, @RequestBody(required=false) PeriodCloseRequestActionRequest request)` | Approve close (ADMIN) |
| POST | `/periods/{periodId}/reject-close` | `ResponseEntity<ApiResponse<PeriodCloseRequestDto>> rejectPeriodClose(@PathVariable Long periodId, @RequestBody(required=false) PeriodCloseRequestActionRequest request)` | Reject close (ADMIN) |
| POST | `/periods/{periodId}/lock` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> lockPeriod(@PathVariable Long periodId, @RequestBody(required=false) AccountingPeriodLockRequest request)` | Lock period |
| POST | `/periods/{periodId}/reopen` | `ResponseEntity<ApiResponse<AccountingPeriodDto>> reopenPeriod(@PathVariable Long periodId, @RequestBody(required=false) AccountingPeriodReopenRequest request)` | Reopen period (SUPER_ADMIN) |
| GET | `/month-end/checklist` | `ResponseEntity<ApiResponse<MonthEndChecklistDto>> checklist(@RequestParam(required=false) Long periodId)` | Get month-end checklist |
| POST | `/month-end/checklist/{periodId}` | `ResponseEntity<ApiResponse<MonthEndChecklistDto>> updateChecklist(@PathVariable Long periodId, @RequestBody MonthEndChecklistUpdateRequest request)` | Update checklist |

#### Bank Reconciliation

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/reconciliation/bank` | `ResponseEntity<ApiResponse<BankReconciliationSummaryDto>> reconcileBank(@Valid @RequestBody BankReconciliationRequest request)` | Legacy bank reconciliation |
| POST | `/reconciliation/bank/sessions` | `ResponseEntity<ApiResponse<BankReconciliationSessionSummaryDto>> startBankReconciliationSession(@Valid @RequestBody BankReconciliationSessionCreateRequest request)` | Start reconciliation session |
| PUT | `/reconciliation/bank/sessions/{sessionId}/items` | `ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>> updateBankReconciliationSessionItems(@PathVariable Long sessionId, @RequestBody BankReconciliationSessionItemsUpdateRequest request)` | Update session items |
| POST | `/reconciliation/bank/sessions/{sessionId}/complete` | `ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>> completeBankReconciliationSession(@PathVariable Long sessionId, @RequestBody(required=false) BankReconciliationSessionCompletionRequest request)` | Complete session |
| GET | `/reconciliation/bank/sessions` | `ResponseEntity<ApiResponse<PageResponse<BankReconciliationSessionSummaryDto>>> listBankReconciliationSessions(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size)` | List sessions |
| GET | `/reconciliation/bank/sessions/{sessionId}` | `ResponseEntity<ApiResponse<BankReconciliationSessionDetailDto>> getBankReconciliationSession(@PathVariable Long sessionId)` | Get session detail |
| GET | `/reconciliation/subledger` | `ResponseEntity<ApiResponse<SubledgerReconciliationReport>> reconcileSubledger()` | Sub-ledger reconciliation |
| GET | `/reconciliation/discrepancies` | `ResponseEntity<ApiResponse<ReconciliationDiscrepancyListResponse>> listReconciliationDiscrepancies(@RequestParam(required=false) String status, @RequestParam(required=false) String type)` | List discrepancies |
| POST | `/reconciliation/discrepancies/{discrepancyId}/resolve` | `ResponseEntity<ApiResponse<ReconciliationDiscrepancyDto>> resolveReconciliationDiscrepancy(@PathVariable Long discrepancyId, @Valid @RequestBody ReconciliationDiscrepancyResolveRequest request)` | Resolve discrepancy |
| GET | `/reconciliation/inter-company` | `ResponseEntity<ApiResponse<InterCompanyReconciliationReport>> reconcileInterCompany(@RequestParam("companyA") Long companyA, @RequestParam("companyB") Long companyB)` | Inter-company reconciliation |

#### Statements & Aging

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/statements/dealers/{dealerId}` | `ResponseEntity<ApiResponse<PartnerStatementResponse>> dealerStatement(@PathVariable Long dealerId, @RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Dealer statement |
| GET | `/statements/suppliers/{supplierId}` | `ResponseEntity<ApiResponse<PartnerStatementResponse>> supplierStatement(@PathVariable Long supplierId, @RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Supplier statement |
| GET | `/aging/dealers/{dealerId}` | `ResponseEntity<ApiResponse<AgingSummaryResponse>> dealerAging(@PathVariable Long dealerId, @RequestParam(required=false) String asOf, @RequestParam(required=false) String buckets)` | Dealer aging |
| GET | `/aging/suppliers/{supplierId}` | `ResponseEntity<ApiResponse<AgingSummaryResponse>> supplierAging(@PathVariable Long supplierId, @RequestParam(required=false) String asOf, @RequestParam(required=false) String buckets)` | Supplier aging |
| GET | `/statements/dealers/{dealerId}/pdf` | `ResponseEntity<byte[]> dealerStatementPdf(@PathVariable Long dealerId, @RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Dealer statement PDF |
| GET | `/statements/suppliers/{supplierId}/pdf` | `ResponseEntity<byte[]> supplierStatementPdf(@PathVariable Long supplierId, @RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Supplier statement PDF |
| GET | `/aging/dealers/{dealerId}/pdf` | `ResponseEntity<byte[]> dealerAgingPdf(@PathVariable Long dealerId, @RequestParam(required=false) String asOf, @RequestParam(required=false) String buckets)` | Dealer aging PDF |
| GET | `/aging/suppliers/{supplierId}/pdf` | `ResponseEntity<byte[]> supplierAgingPdf(@PathVariable Long supplierId, @RequestParam(required=false) String asOf, @RequestParam(required=false) String buckets)` | Supplier aging PDF |

#### Inventory Accounting

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/inventory/landed-cost` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordLandedCost(@Valid @RequestBody LandedCostRequest request)` | Record landed cost |
| POST | `/inventory/revaluation` | `ResponseEntity<ApiResponse<JournalEntryDto>> revalueInventory(@Valid @RequestBody InventoryRevaluationRequest request)` | Inventory revaluation |
| POST | `/inventory/wip-adjustment` | `ResponseEntity<ApiResponse<JournalEntryDto>> adjustWip(@Valid @RequestBody WipAdjustmentRequest request)` | WIP adjustment |

#### Tax (GST)

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/gst/return` | `ResponseEntity<ApiResponse<GstReturnDto>> generateGstReturn(@RequestParam(required=false) String period)` | Generate GST return |
| GET | `/gst/reconciliation` | `ResponseEntity<ApiResponse<GstReconciliationDto>> getGstReconciliation(@RequestParam(required=false) String period)` | GST reconciliation |

#### Audit & Reporting

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/audit/digest` | `ResponseEntity<ApiResponse<AuditDigestResponse>> auditDigest(@RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Audit digest (deprecated) |
| GET | `/audit/digest.csv` | `ResponseEntity<String> auditDigestCsv(@RequestParam(required=false) String from, @RequestParam(required=false) String to)` | Audit digest CSV (deprecated) |
| GET | `/audit/transactions` | `ResponseEntity<ApiResponse<PageResponse<AccountingTransactionAuditListItemDto>>> transactionAudit(@RequestParam(required=false) String from, @RequestParam(required=false) String to, @RequestParam(required=false) String module, @RequestParam(required=false) String status, @RequestParam(required=false) String reference, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size)` | Transaction audit |
| GET | `/audit/transactions/{journalEntryId}` | `ResponseEntity<ApiResponse<AccountingTransactionAuditDetailDto>> transactionAuditDetail(@PathVariable Long journalEntryId)` | Transaction audit detail |

#### Temporal Queries

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/trial-balance/as-of` | `ResponseEntity<ApiResponse<TrialBalanceSnapshot>> getTrialBalanceAsOf(@RequestParam String date)` | Trial balance as of date |
| GET | `/date-context` | `ResponseEntity<ApiResponse<Map<String, Object>>> getAccountingDateContext()` | Get accounting date context |

#### Payroll

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/payroll/payments` | `ResponseEntity<ApiResponse<JournalEntryDto>> recordPayrollPayment(@Valid @RequestBody PayrollPaymentRequest request)` | Record payroll payment |

**Side Effects**: All POST/PUT operations write to database, publish events, update audit logs

**Status**: Canonical

---

## AccountingConfigurationController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingConfigurationController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/accounting/configuration`

**Dependencies**:
- ConfigurationHealthService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/health` | `ResponseEntity<ApiResponse<ConfigurationHealthReport>> health()` | Configuration health check |

**Status**: Canonical

---

## AccountingAuditTrailController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingAuditTrailController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/accounting`

**Dependencies**:
- AuditTrailQueryService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| GET | `/audit-trail` | `ResponseEntity<ApiResponse<PageResponse<AccountingAuditTrailEntryDto>>> listAuditTrail(@RequestParam(required=false) LocalDate from, @RequestParam(required=false) LocalDate to, @RequestParam(required=false) String user, @RequestParam(required=false) String actionType, @RequestParam(required=false) String entityType, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size)` | Query audit trail |

**Status**: Canonical

---

## PayrollController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/PayrollController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/accounting/payroll`

**Dependencies**:
- AccountingService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/payments/batch` | `ResponseEntity<ApiResponse<PayrollBatchPaymentResponse>> processBatchPayment(@Valid @RequestBody PayrollBatchPaymentRequest request)` | Process payroll batch |

**Status**: Canonical

---

## TallyImportController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/TallyImportController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/migration`

**Dependencies**:
- TallyImportService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/tally-import` | `ResponseEntity<ApiResponse<TallyImportResponse>> importTally(@RequestPart("file") MultipartFile file)` | Import Tally XML |

**Status**: Canonical

---

## OpeningBalanceImportController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/OpeningBalanceImportController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Base Path**: `/api/v1/accounting`

**Dependencies**:
- OpeningBalanceImportService

### Endpoints

| Method | Path | Signature | Description |
|--------|------|-----------|-------------|
| POST | `/opening-balances` | `ResponseEntity<ApiResponse<OpeningBalanceImportResponse>> importOpeningBalances(@RequestPart("file") MultipartFile file)` | Import opening balances CSV |

**Status**: Canonical

---

## AccountingCatalogController

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountingCatalogController.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.controller`

**Status**: Scoped (empty class - placeholder)
