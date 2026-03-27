# Accounting Services

## Service Hierarchy

```
AccountingCoreLogic (Abstract Base)
└── AccountingCoreService
    └── AccountingCoreEngine
        ├── AccountingService (Facade)
        ├── JournalEntryService
        ├── SettlementService
        ├── DealerReceiptService
        ├── CreditDebitNoteService
        ├── InventoryAccountingService
        └── AccountingIdempotencyService

AccountingFacadeCore (Abstract)
└── AccountingFacade

AccountingPeriodServiceCore (Abstract)
└── AccountingPeriodService

ReconciliationServiceCore (Abstract)
└── ReconciliationService

AccountingAuditTrailServiceCore (Abstract)
└── AccountingAuditTrailService
```

---

## Core Services

### AccountingService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service (Facade)

**Extends**: AccountingCoreService

**Responsibility**: Main orchestrating service for accounting operations; delegates to specialized services

**Use when**: Need to perform any accounting operation that requires coordination across multiple services

**Do not use when**: Direct access to a specific service (e.g., JournalEntryService) is sufficient

**Dependencies**:
- JournalEntryService
- DealerReceiptService
- SettlementService
- CreditDebitNoteService
- AccountingAuditService
- InventoryAccountingService
- AccountingFacade
- DealerLedgerService
- SupplierLedgerService
- AccountRepository
- JournalEntryRepository
- AccountingPeriodService
- ReferenceNumberService
- ApplicationEventPublisher
- CompanyClock
- CompanyEntityLookup
- PartnerSettlementAllocationRepository
- RawMaterialPurchaseRepository
- InvoiceRepository
- RawMaterialMovementRepository
- RawMaterialBatchRepository
- FinishedGoodBatchRepository
- DealerRepository
- SupplierRepository
- InvoiceSettlementPolicy
- JournalReferenceResolver
- JournalReferenceMappingRepository
- EntityManager
- SystemSettingsService
- AuditService
- AccountingEventStore

**Public Methods**:
```java
List<JournalEntryDto> listJournalEntries(Long dealerId, Long supplierId, int page, int size)
List<JournalEntryDto> listJournalEntries(Long dealerId)
List<JournalEntryDto> listJournalEntriesByReferencePrefix(String prefix)
JournalEntryDto createJournalEntry(JournalEntryRequest request)
JournalEntryDto createStandardJournal(JournalCreationRequest request)
JournalEntryDto createManualJournal(ManualJournalRequest request)
List<JournalListItemDto> listJournals(LocalDate fromDate, LocalDate toDate, String journalType, String sourceModule)
JournalEntryDto reverseJournalEntry(Long entryId, JournalEntryReversalRequest request)
JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey)
List<JournalEntryDto> cascadeReverseRelatedEntries(Long primaryEntryId, JournalEntryReversalRequest request)
JournalEntryDto recordDealerReceipt(DealerReceiptRequest request)
JournalEntryDto recordDealerReceiptSplit(DealerReceiptSplitRequest request)
JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request)
PartnerSettlementResponse settleDealerInvoices(DealerSettlementRequest request)
PartnerSettlementResponse autoSettleDealer(Long dealerId, AutoSettlementRequest request)
PartnerSettlementResponse settleSupplierInvoices(SupplierSettlementRequest request)
PartnerSettlementResponse autoSettleSupplier(Long supplierId, AutoSettlementRequest request)
JournalEntryDto postCreditNote(CreditNoteRequest request)
JournalEntryDto postDebitNote(DebitNoteRequest request)
JournalEntryDto postAccrual(AccrualRequest request)
JournalEntryDto writeOffBadDebt(BadDebtWriteOffRequest request)
JournalEntryDto recordLandedCost(LandedCostRequest request)
JournalEntryDto revalueInventory(InventoryRevaluationRequest request)
JournalEntryDto adjustWip(WipAdjustmentRequest request)
AuditDigestResponse auditDigest(LocalDate from, LocalDate to)
String auditDigestCsv(LocalDate from, LocalDate to)
```

**Side Effects**: Database writes, event publishing, audit logging

**Status**: Canonical

---

### AccountingFacade

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacade.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingFacadeCore

**Responsibility**: Entry point for manual journal operations with validation

**Use when**: Creating manual journals or payroll payments

**Do not use when**: Automated journal posting (use module-specific services)

**Dependencies**:
- AccountingService
- AccountRepository
- JournalEntryRepository
- ReferenceNumberService
- DealerRepository
- SupplierRepository
- CompanyContextService
- CompanyClock
- CompanyEntityLookup
- CompanyAccountingSettingsService
- JournalReferenceResolver
- JournalReferenceMappingRepository

**Public Methods**:
```java
static boolean isReservedReferenceNamespace(String referenceNumber)
JournalEntryDto createManualJournal(ManualJournalRequest request)
JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey)
JournalEntryDto recordPayrollPayment(PayrollPaymentRequest request)
```

**Invariants**: Manual journals must balance; reserved reference namespaces blocked

**Status**: Canonical

---

### JournalEntryService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/JournalEntryService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Journal entry CRUD, listing, reversal

**Use when**: Managing journal entries directly

**Dependencies**:
- AccountingIdempotencyService
- CompanyContextService
- CompanyClock
- (plus all AccountingCoreEngine dependencies)

**Public Methods**:
```java
List<JournalEntryDto> listJournalEntries(Long dealerId, Long supplierId, int page, int size)
List<JournalEntryDto> listJournalEntries(Long dealerId)
List<JournalEntryDto> listJournalEntriesByReferencePrefix(String prefix)
JournalEntryDto createJournalEntry(JournalEntryRequest request)
JournalEntryDto createStandardJournal(JournalCreationRequest request)
List<JournalListItemDto> listJournals(LocalDate fromDate, LocalDate toDate, String journalType, String sourceModule)
JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey)
JournalEntryDto reverseJournalEntry(Long entryId, JournalEntryReversalRequest request)
List<JournalEntryDto> cascadeReverseRelatedEntries(Long primaryEntryId, JournalEntryReversalRequest request)
```

**Invariants**:
- Journal entry must balance (totalDebit == totalCredit)
- No line can have both debit and credit > 0
- Amounts must be non-negative

**Status**: Canonical

---

### SettlementService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/SettlementService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Partner (dealer/supplier) settlements with idempotency

**Use when**: Settling invoices with payments, discounts, write-offs

**Dependencies**:
- AccountingIdempotencyService
- (plus all AccountingCoreEngine dependencies)

**Public Methods**:
```java
JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request)
PartnerSettlementResponse settleDealerInvoices(DealerSettlementRequest request)
PartnerSettlementResponse autoSettleDealer(Long dealerId, AutoSettlementRequest request)
PartnerSettlementResponse settleSupplierInvoices(SupplierSettlementRequest request)
PartnerSettlementResponse autoSettleSupplier(Long supplierId, AutoSettlementRequest request)
```

**Side Effects**: Creates journal entries, partner ledger entries, settlement allocations

**Status**: Canonical

---

### DealerReceiptService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/DealerReceiptService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Dealer receipt recording with idempotency

**Use when**: Recording dealer payments/receipts

**Dependencies**:
- AccountingIdempotencyService
- (plus all AccountingCoreEngine dependencies)

**Public Methods**:
```java
JournalEntryDto recordDealerReceipt(DealerReceiptRequest request)
JournalEntryDto recordDealerReceiptSplit(DealerReceiptSplitRequest request)
List<JournalEntryDto> listDealerReceipts(Long dealerId, int page, int size)
```

**Status**: Canonical

---

### DealerLedgerService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/DealerLedgerService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AbstractPartnerLedgerService<Dealer, DealerLedgerEntry>

**Responsibility**: AR sub-ledger management for dealers

**Use when**: Managing dealer ledger entries and balances

**Dependencies**:
- DealerLedgerRepository
- CompanyContextService
- DealerRepository
- CompanyEntityLookup

**Public Methods**:
```java
void recordLedgerEntry(Dealer dealer, LedgerContext context)
Map<Long, BigDecimal> currentBalances(Collection<Long> dealerIds)
BigDecimal currentBalance(Long dealerId)
List<DealerLedgerEntry> entries(Dealer dealer)
void syncInvoiceLedger(Invoice invoice, LocalDate settlementDate)
```

**Status**: Canonical

---

### SupplierLedgerService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/SupplierLedgerService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AbstractPartnerLedgerService<Supplier, SupplierLedgerEntry>

**Responsibility**: AP sub-ledger management for suppliers

**Use when**: Managing supplier ledger entries and balances

**Dependencies**:
- SupplierLedgerRepository
- SupplierRepository
- CompanyContextService
- CompanyEntityLookup

**Public Methods**:
```java
void recordLedgerEntry(Supplier supplier, LedgerContext context)
Map<Long, BigDecimal> currentBalances(Collection<Long> supplierIds)
BigDecimal currentBalance(Long supplierId)
```

**Status**: Canonical

---

## Period Management

### AccountingPeriodService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingPeriodService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingPeriodServiceCore

**Responsibility**: Accounting period lifecycle management with maker-checker workflow

**Use when**: Managing period open/close/lock/reopen

**Dependencies**:
- AccountingPeriodRepository
- CompanyContextService
- JournalEntryRepository
- CompanyEntityLookup
- JournalLineRepository
- AccountRepository
- CompanyClock
- ReportService
- ReconciliationService
- InvoiceRepository
- GoodsReceiptRepository
- RawMaterialPurchaseRepository
- PayrollRunRepository
- ReconciliationDiscrepancyRepository
- PeriodCloseRequestRepository
- AccountingFacade
- PeriodCloseHook
- AccountingPeriodSnapshotService

**Public Methods**:
```java
AccountingPeriodDto reopenPeriod(Long periodId, AccountingPeriodReopenRequest request) // SUPER_ADMIN only
AccountingPeriodDto approvePeriodClose(Long periodId, PeriodCloseRequestActionRequest request) // ADMIN only
PeriodCloseRequestDto rejectPeriodClose(Long periodId, PeriodCloseRequestActionRequest request) // ADMIN only
AccountingPeriodDto closePeriod(Long periodId, AccountingPeriodCloseRequest request) // Disabled - throws error
// Plus inherited methods from AccountingPeriodServiceCore
```

**Invariants**:
- Direct close disabled; must use request-close + approve workflow
- SUPER_ADMIN required for reopen
- Month-end checklist must be complete before close

**Status**: Canonical

---

## Reconciliation

### ReconciliationService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReconciliationService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: ReconciliationServiceCore

**Responsibility**: Bank and sub-ledger reconciliation

**Use when**: Reconciling bank accounts, sub-ledgers, inter-company

**Dependencies**:
- CompanyContextService
- CompanyRepository
- AccountRepository
- DealerRepository
- DealerLedgerRepository
- SupplierRepository
- SupplierLedgerRepository
- JournalEntryRepository
- JournalLineRepository
- TemporalBalanceService
- ReconciliationDiscrepancyRepository
- AccountingPeriodRepository
- TaxService
- ReportService
- AccountingFacade

**Public Methods** (inherited from ReconciliationServiceCore):
```java
BankReconciliationSummaryDto reconcileBankAccount(Long bankAccountId, LocalDate statementDate, BigDecimal statementEndingBalance, LocalDate startDate, LocalDate endDate, Set<Long> clearedJournalLineIds, Set<Long> excludedJournalLineIds)
SubledgerReconciliationReport reconcileSubledgerBalances()
InterCompanyReconciliationReport interCompanyReconcile(Long companyA, Long companyB)
ReconciliationDiscrepancyListResponse listDiscrepancies(ReconciliationDiscrepancyStatus status, ReconciliationDiscrepancyType type)
ReconciliationDiscrepancyDto resolveDiscrepancy(Long discrepancyId, ReconciliationDiscrepancyResolveRequest request)
```

**Status**: Canonical

---

### BankReconciliationSessionService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/BankReconciliationSessionService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Interactive bank reconciliation sessions

**Use when**: Performing step-by-step bank reconciliation

**Dependencies**:
- CompanyContextService
- AccountRepository
- AccountingPeriodRepository
- BankReconciliationSessionRepository
- BankReconciliationItemRepository
- JournalLineRepository
- ReconciliationService
- AccountingPeriodService
- ReferenceNumberService

**Public Methods**:
```java
BankReconciliationSessionSummaryDto startSession(BankReconciliationSessionCreateRequest request)
BankReconciliationSessionDetailDto updateItems(Long sessionId, BankReconciliationSessionItemsUpdateRequest request)
BankReconciliationSessionDetailDto completeSession(Long sessionId, BankReconciliationSessionCompletionRequest request)
PageResponse<BankReconciliationSessionSummaryDto> listSessions(int page, int size)
BankReconciliationSessionDetailDto getSessionDetail(Long sessionId)
BankReconciliationSummaryDto reconcileLegacy(BankReconciliationRequest request)
```

**Status**: Canonical

---

## Statements & Aging

### StatementService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/StatementService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Partner statements and aging reports with PDF generation

**Use when**: Generating partner statements, aging reports, or overdue invoice lists

**Dependencies**:
- CompanyContextService
- DealerRepository
- SupplierRepository
- DealerLedgerRepository
- SupplierLedgerRepository
- PartnerSettlementAllocationRepository
- CompanyClock

**Public Methods**:
```java
PartnerStatementResponse dealerStatement(Long dealerId, LocalDate from, LocalDate to)
PartnerStatementResponse supplierStatement(Long supplierId, LocalDate from, LocalDate to)
AgingSummaryResponse dealerAging(Long dealerId, LocalDate asOf, String bucketParam)
AgingSummaryResponse supplierAging(Long supplierId, LocalDate asOf, String bucketParam)
List<OverdueInvoiceDto> dealerOverdueInvoices(Dealer dealer, LocalDate asOf)
long dealerOpenInvoiceCount(Dealer dealer, LocalDate asOf)
byte[] dealerStatementPdf(Long dealerId, LocalDate from, LocalDate to)
byte[] supplierStatementPdf(Long supplierId, LocalDate from, LocalDate to)
byte[] dealerAgingPdf(Long dealerId, LocalDate asOf, String buckets)
byte[] supplierAgingPdf(Long supplierId, LocalDate asOf, String buckets)
```

**Status**: Canonical

---

### AgingReportService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AgingReportService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Aged receivables reports and DSO calculations

**Use when**: Generating aged receivables reports, DSO metrics

**Dependencies**:
- DealerLedgerRepository
- DealerRepository
- CompanyContextService
- CompanyClock

**Public Methods**:
```java
AgedReceivablesReport getAgedReceivablesReport()
AgedReceivablesReport getAgedReceivablesReport(LocalDate asOfDate)
DealerAgingDetail getDealerAging(Long dealerId)
DealerAgingDetailedReport getDealerAgingDetailed(Long dealerId)
DSOReport getDealerDSO(Long dealerId)
```

**DTOs**:
- `AgingBuckets`: Current, 1-30, 31-60, 61-90, 90+ days
- `DealerAgingDetail`
- `AgedReceivablesReport`
- `DealerAgingDetailedReport`
- `DSOReport`

**Status**: Canonical

---

## Tax Services

### TaxService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TaxService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: GST return generation and reconciliation

**Use when**: Generating GST returns, GST reconciliation reports

**Dependencies**:
- CompanyContextService
- CompanyAccountingSettingsService
- CompanyClock
- JournalLineRepository
- GstService
- InvoiceRepository
- RawMaterialPurchaseRepository

**Public Methods**:
```java
GstReturnDto generateGstReturn(YearMonth period)
GstReconciliationDto generateGstReconciliation(YearMonth period)
```

**Status**: Canonical

---

### GstService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/GstService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: GST calculations (CGST, SGST, IGST breakdown)

**Use when**: Need to calculate/split GST amounts

**Public Methods**: (from usage patterns)
```java
GstBreakdown splitTaxAmount(BigDecimal taxableAmount, BigDecimal taxAmount, String companyStateCode, String partnerStateCode)
GstBreakdown resolveInvoiceLineBreakdown(...)
TaxType resolveTaxType(String companyStateCode, String partnerStateCode, boolean isSales)
```

**Status**: Canonical

---

## Temporal Queries

### TemporalBalanceService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TemporalBalanceService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Point-in-time balance queries using snapshots for closed periods

**Use when**: Getting balance as of a specific date, trial balance snapshots

**Dependencies**:
- AccountRepository
- CompanyContextService
- AccountingPeriodRepository
- AccountingPeriodSnapshotRepository
- AccountingPeriodTrialBalanceLineRepository
- JournalLineRepository
- CompanyClock

**Public Methods**:
```java
BigDecimal getBalanceAsOfDate(Long accountId, LocalDate asOfDate)
BigDecimal getBalanceAsOfTimestamp(Long accountId, Instant asOf)
Map<Long, BigDecimal> getBalancesAsOfDate(List<Long> accountIds, LocalDate asOfDate)
TrialBalanceSnapshot getTrialBalanceAsOf(LocalDate asOfDate)
AccountActivityReport getAccountActivity(Long accountId, LocalDate startDate, LocalDate endDate)
BalanceComparison compareBalances(Long accountId, LocalDate date1, LocalDate date2)
```

**DTOs**:
- `TrialBalanceSnapshot`: asOfDate, entries, totalDebits, totalCredits
- `TrialBalanceEntry`: accountId, accountCode, accountName, accountType, debit, credit
- `AccountActivityReport`: openingBalance, closingBalance, movements
- `BalanceComparison`: balance at two dates with change

**Invariants**:
- Closed periods use snapshots
- Open periods use journal line aggregation

**Status**: Canonical

---

## Supporting Services

### ReferenceNumberService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/ReferenceNumberService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Generate unique journal reference numbers

**Use when**: Need to generate reference numbers for journals

**Public Methods**:
```java
String nextJournalReference(Company company)
```

**Status**: Canonical

---

### AccountHierarchyService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountHierarchyService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Chart of accounts hierarchy operations

**Use when**: Building account tree views

**Public Methods**:
```java
List<AccountNode> getChartOfAccountsTree()
List<AccountNode> getTreeByType(AccountType type)
```

**Status**: Canonical

---

### CompanyDefaultAccountsService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Manage company default accounts (inventory, COGS, revenue, etc.)

**Public Methods**:
```java
DefaultAccounts getDefaults()
DefaultAccounts updateDefaults(Long inventoryAccountId, Long cogsAccountId, Long revenueAccountId, Long discountAccountId, Long taxAccountId)
```

**Status**: Canonical

---

### AccountingIdempotencyService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingIdempotencyService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Idempotent wrapper for accounting operations

**Use when**: Need idempotency guarantees for settlement/receipt operations

**Public Methods**:
```java
JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey)
JournalEntryDto recordDealerReceipt(DealerReceiptRequest request)
JournalEntryDto recordDealerReceiptSplit(DealerReceiptSplitRequest request)
JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request)
PartnerSettlementResponse settleDealerInvoices(DealerSettlementRequest request)
PartnerSettlementResponse autoSettleDealer(Long dealerId, AutoSettlementRequest request)
PartnerSettlementResponse settleSupplierInvoices(SupplierSettlementRequest request)
PartnerSettlementResponse autoSettleSupplier(Long supplierId, AutoSettlementRequest request)
```

**Status**: Canonical

---

### CreditDebitNoteService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CreditDebitNoteService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Credit notes, debit notes, accruals, bad debt write-offs

**Public Methods**:
```java
JournalEntryDto postCreditNote(CreditNoteRequest request)
JournalEntryDto postDebitNote(DebitNoteRequest request)
JournalEntryDto postAccrual(AccrualRequest request)
JournalEntryDto writeOffBadDebt(BadDebtWriteOffRequest request)
```

**Status**: Canonical

---

### InventoryAccountingService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/InventoryAccountingService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingCoreEngine

**Responsibility**: Inventory-related accounting (landed cost, revaluation, WIP)

**Public Methods**:
```java
JournalEntryDto recordLandedCost(LandedCostRequest request)
JournalEntryDto revalueInventory(InventoryRevaluationRequest request)
JournalEntryDto adjustWip(WipAdjustmentRequest request)
```

**Status**: Canonical

---

## Import Services

### TallyImportService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/TallyImportService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Import chart of accounts and opening balances from Tally XML

**Use when**: Migrating from Tally accounting software

**Dependencies**:
- CompanyContextService
- AccountRepository
- OpeningBalanceImportService
- JournalEntryRepository
- TallyImportRepository
- AuditService
- ObjectMapper
- PlatformTransactionManager
- IdempotencyReservationService

**Public Methods**:
```java
TallyImportResponse importTallyXml(MultipartFile file)
```

**Side Effects**: Creates accounts, posts opening balance journal

**Status**: Canonical

---

### OpeningBalanceImportService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/OpeningBalanceImportService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Import opening balances from CSV

**Use when**: Setting up initial company balances

**Public Methods**:
```java
OpeningBalanceImportResponse importOpeningBalances(MultipartFile file)
OpeningBalanceImportResponse importFromParsedRows(List<ParsedOpeningBalanceRow> parsedRows)
OpeningBalanceImportResponse importFromParsedRows(List<ParsedOpeningBalanceRow> parsedRows, String referenceNumber)
```

**CSV Headers**: account_code, account_name, account_type, debit_amount, credit_amount, narration

**Status**: Canonical

---

## Audit Services

### AccountingAuditTrailService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingAuditTrailService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Extends**: AccountingAuditTrailServiceCore

**Responsibility**: Transaction audit queries with linked document resolution

**Public Methods**:
```java
PageResponse<AccountingTransactionAuditListItemDto> listTransactions(LocalDate from, LocalDate to, String module, String status, String reference, int page, int size)
AccountingTransactionAuditDetailDto transactionDetail(Long journalEntryId)
```

**Status**: Canonical

---

### AuditTrailQueryService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AuditTrailQueryService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Generic audit trail query service

**Public Methods**:
```java
PageResponse<AccountingAuditTrailEntryDto> queryAuditTrail(LocalDate from, LocalDate to, String user, String actionType, String entityType, int page, int size)
```

**Status**: Canonical

---

### AccountingAuditService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingAuditService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Audit digest generation (deprecated - use AccountingAuditTrailService)

**Status**: Legacy

---

### AccountingComplianceAuditService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingComplianceAuditService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Compliance audit checks

**Status**: Canonical

---

## Event Services

### AccountingEventStore

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventStore.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.event`

**Type**: Service

**Responsibility**: Event-sourced audit log for accounting operations

**Use when**: Recording journal events, replaying balance history

**Public Methods**:
```java
List<AccountingEvent> recordJournalEntryPosted(JournalEntry entry, Map<Long, BigDecimal> balancesBefore)
AccountingEvent recordJournalEntryReversed(JournalEntry original, JournalEntry reversal, String reason)
AccountingEvent recordBalanceAdjustment(Account account, BigDecimal oldBalance, BigDecimal newBalance, String reason)
BigDecimal replayBalanceAsOf(Company company, Long accountId, Instant asOf)
BigDecimal replayBalanceAsOfDate(Company company, Long accountId, LocalDate asOfDate)
List<AccountingEvent> getAccountHistory(Company company, Long accountId)
List<AccountingEvent> getJournalEntryAuditTrail(Long journalEntryId)
List<AccountingEvent> getCorrelatedEvents(UUID correlationId)
```

**Status**: Canonical

---

## Costing Services

### CostingMethodService

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CostingMethodService.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Responsibility**: Inventory costing method operations (FIFO, Weighted Average, etc.)

**Status**: Canonical

---

## Hooks

### PeriodCloseHook

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/PeriodCloseHook.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Interface

**Responsibility**: Extension point for period close operations

**Status**: Canonical

---

### NoopPeriodCloseHook

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/NoopPeriodCloseHook.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.service`

**Type**: Service

**Implements**: PeriodCloseHook

**Responsibility**: Default no-op implementation

**Status**: Canonical
