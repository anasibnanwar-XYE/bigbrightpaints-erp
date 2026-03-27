# Accounting Module Overview

**Module Path:** `modules/accounting/`  
**Package Root:** `com.bigbrightpaints.erp.modules.accounting`

## Purpose

The Accounting module is the financial ledger system for BigBright ERP. It provides:
- Double-entry bookkeeping with journal entries
- Chart of accounts management with hierarchical structure
- Partner (dealer/supplier) ledger tracking for AR/AP
- Bank reconciliation workflow
- Period close with checklist validation
- GST and tax calculations
- Opening balance and Tally import migrations

## Module Boundaries

### Inbound Dependencies
- **Company Context** (`modules/company`) - Tenant isolation via `CompanyContextHolder`
- **Dealer** (`modules/sales`) - AR subledger entries
- **Supplier** (`modules/purchasing`) - AP subledger entries
- **Invoice** (`modules/invoice`) - Settlement allocation references
- **Inventory Events** (`modules/inventory`) - Automated GL postings via `InventoryAccountingEventListener`
- **Payroll** (`modules/hr`) - Payroll journal posting

### Outbound Dependencies
- **Reports** (`modules/reports`) - Trial balance, P&L, balance sheet queries
- **Events** - `AccountCacheInvalidatedEvent` for cache invalidation
- **Audit Trail** - `AccountingEventStore` for event sourcing

### Domain Events Published
- `AccountCacheInvalidatedEvent` - Published on account changes
- `JournalEntryPostedEvent` - Internal event for audit trail

## Architecture Layers

```
modules/accounting/
├── controller/          # REST endpoints (7 classes)
├── service/             # Business logic (35 classes)
├── domain/              # Entities & repositories (46 classes)
├── dto/                 # Data transfer objects (56 classes)
├── event/               # Domain events (6 classes)
└── internal/            # Core engine implementations (5 classes)
```

## Key Design Patterns

### 1. Facade Pattern
- `AccountingFacade` - Centralized boundary for journal posting operations
- `AccountingFacadeCore` - Core implementation with caching and idempotency

### 2. Core Engine Pattern
- `AccountingCoreEngine` - Abstract base with shared posting logic
- `AccountingCoreEngineCore` - Internal implementation with all dependencies
- Services extend `AccountingCoreEngine` for consistent posting behavior

### 3. Ledger Pattern
- `DealerLedgerService` / `SupplierLedgerService` - Partner-specific subledger tracking
- `AbstractPartnerLedgerService` - Shared ledger operations

### 4. Event Sourcing
- `AccountingEventStore` - Append-only event log for audit
- `AccountingEvent` entity with sequence numbers per aggregate

## Anti-Patterns to Avoid

### 1. Direct Journal Creation
❌ **Wrong:** Bypass `AccountingFacade` and create `JournalEntry` directly
```java
journalEntryRepository.save(entry); // Bypasses validation, idempotency, period checks
```
✅ **Correct:** Use facade methods
```java
accountingFacade.postSalesJournal(dealerId, orderNumber, ...);
accountingFacade.createStandardJournal(request);
```

### 2. Posting to Closed Periods
❌ **Wrong:** Creating entries without period validation
✅ **Correct:** Use `AccountingPeriodService.requirePostablePeriod()` or request exceptions via `ClosedPeriodPostingExceptionService`

### 3. Unbalanced Entries
❌ **Wrong:** Creating journal lines where debits ≠ credits
✅ **Correct:** The engine enforces `JOURNAL_BALANCE_TOLERANCE = BigDecimal.ZERO`

### 4. Duplicate References
❌ **Wrong:** Using arbitrary reference numbers
✅ **Correct:** Use `ReferenceNumberService` or reserved prefixes (see `AccountingFacade.RESERVED_REFERENCE_PREFIXES`)

### 5. Missing Partner Context
❌ **Wrong:** Posting to AR/AP accounts without dealer/supplier context
✅ **Correct:** Set `dealerId` or `supplierId` on journal entries for ledger tracking

## Canonicality Status

| Component | Status | Notes |
|-----------|--------|-------|
| `AccountingFacade` | ✅ Canonical | Single entry point for journal operations |
| `AccountingCoreEngine` | ✅ Canonical | Shared posting logic for all services |
| `AccountingPeriodService` | ✅ Canonical | Period management with maker-checker workflow |
| `ReconciliationService` | ✅ Canonical | GL/subledger reconciliation |
| `DealerLedgerService` | ✅ Canonical | AR subledger |
| `SupplierLedgerService` | ✅ Canonical | AP subledger |
| `AccountingEventStore` | ⚠️ Audit Only | For audit trail, not source of truth for closed periods |

## Security Requirements

- **Roles:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_SUPER_ADMIN` (period reopen)
- **Tenant Isolation:** All queries filtered by `Company` from `CompanyContextHolder`
- **Period Protection:** Closed periods reject postings unless exception granted

## Performance Considerations

- **Account Caching:** `AccountingFacadeCore` caches accounts with 5-minute TTL
- **Optimistic Locking:** Journal entries use `@Version` for concurrent update handling
- **Retry on Contention:** `@Retryable` on journal posting methods for lock failures
- **Pessimistic Locking:** Period close uses `lockByCompanyAndId` for atomicity

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `erp.benchmark.skip-date-validation` | `false` | Disable date validation in benchmark mode |
| `erp.accounting.event-trail.strict` | `true` | Fail on event trail persistence errors |
| `erp.inventory.accounting.events.enabled` | `true` | Auto-post inventory events to GL |
