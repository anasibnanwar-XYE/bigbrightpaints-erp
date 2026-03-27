# Accounting Entities

## Overview

| Category | Count | Description |
|----------|-------|-------------|
| Core Entities | 6 | Account, JournalEntry, JournalLine, AccountingPeriod |
| Ledger Entities | 2 | DealerLedgerEntry, SupplierLedgerEntry |
| Settlement Entities | 1 | PartnerSettlementAllocation |
| Reconciliation Entities | 3 | BankReconciliationSession, BankReconciliationItem, ReconciliationDiscrepancy |
| Audit Entities | 3 | AccountingEvent, ClosedPeriodPostingException, AccountingPeriodSnapshot |
| Import Entities | 2 | TallyImport, OpeningBalanceImport |
| Configuration Entities | 1 | PeriodCloseRequest |

---

## Core Entities

### Account

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/Account.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `accounts`

**Unique Constraints**: `(company_id, code)`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| public_id | UUID | No | Public identifier |
| company_id | Long | No | FK to Company |
| code | String | No | Account code |
| name | String | No | Account name |
| type | AccountType | No | ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE, COGS |
| balance | BigDecimal | No | Current balance |
| active | boolean | No | Active flag |
| parent_id | Long | Yes | FK to parent Account (hierarchy) |
| hierarchy_level | Integer | Yes | Level in hierarchy (1=Category, 3=Detail) |

**Relationships**:
- ManyToOne to Company
- ManyToOne to Account (parent)

**Business Rules**:
- Balance sign validation by account type (warning only)
- Hierarchy level auto-calculated from parent

**Invariants**:
- Balance cannot be null
- Code is unique per company

---

### JournalEntry

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntry.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `journal_entries`

**Unique Constraints**: `(company_id, reference_number)`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| public_id | UUID | No | Public identifier |
| company_id | Long | No | FK to Company |
| reference_number | String | No | Unique reference |
| entry_date | LocalDate | No | Date of entry |
| memo | String | Yes | Description/memo |
| status | String | No | DRAFT, POSTED, REVERSED |
| dealer_id | Long | Yes | FK to Dealer |
| supplier_id | Long | Yes | FK to Supplier |
| accounting_period_id | Long | Yes | FK to AccountingPeriod |
| journal_type | JournalEntryType | No | AUTOMATED, MANUAL |
| source_module | String | Yes | Originating module |
| source_reference | String | Yes | Original reference |
| attachment_references | String | Yes | JSON/text attachment refs |
| reversal_of_id | Long | Yes | FK to original entry (if reversal) |
| correction_type | JournalCorrectionType | Yes | REVERSAL, ADJUSTMENT |
| correction_reason | String | Yes | Reason for correction |
| void_reason | String | Yes | Reason for void |
| voided_at | Instant | Yes | When voided |
| created_at | Instant | No | Creation timestamp |
| updated_at | Instant | No | Last update timestamp |
| posted_at | Instant | Yes | When posted |
| created_by | String | Yes | Creator username |
| posted_by | String | Yes | Poster username |
| last_modified_by | String | Yes | Last modifier |
| currency | String | No | Currency code (default INR) |
| fx_rate | BigDecimal | Yes | Exchange rate |
| foreign_amount_total | BigDecimal | Yes | Foreign currency total |

**Relationships**:
- ManyToOne to Company
- ManyToOne to Dealer
- ManyToOne to Supplier
- ManyToOne to AccountingPeriod
- ManyToOne to JournalEntry (reversalOf)
- OneToMany to JournalLine (lines)
- OneToOne to JournalEntry (reversalEntry)

**Status Values**:
- DRAFT: Not yet posted
- POSTED: Posted to ledger
- REVERSED: Reversed/corrected

---

### JournalLine

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalLine.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `journal_lines`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| journal_entry_id | Long | No | FK to JournalEntry |
| account_id | Long | No | FK to Account |
| description | String | Yes | Line description |
| debit | BigDecimal | No | Debit amount |
| credit | BigDecimal | No | Credit amount |

**Relationships**:
- ManyToOne to JournalEntry
- ManyToOne to Account

**Business Rules**:
- Either debit or credit must be non-zero (not both)
- Amounts must be non-negative

---

### AccountingPeriod

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriod.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `accounting_periods`

**Unique Constraints**: `(company_id, year, month)`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| public_id | UUID | No | Public identifier |
| company_id | Long | No | FK to Company |
| year | int | No | Fiscal year |
| month | int | No | Month (1-12) |
| start_date | LocalDate | No | Period start |
| end_date | LocalDate | No | Period end |
| costing_method | CostingMethod | No | FIFO, WEIGHTED_AVERAGE, etc. |
| status | AccountingPeriodStatus | No | OPEN, LOCKED, CLOSED |
| bank_reconciled | boolean | No | Bank reconciliation flag |
| bank_reconciled_at | Instant | Yes | When reconciled |
| bank_reconciled_by | String | Yes | Who reconciled |
| inventory_counted | boolean | No | Inventory count flag |
| inventory_counted_at | Instant | Yes | When counted |
| inventory_counted_by | String | Yes | Who counted |
| checklist_notes | String | Yes | Month-end checklist notes |
| closed_at | Instant | Yes | When closed |
| closed_by | String | Yes | Who closed |
| locked_at | Instant | Yes | When locked |
| locked_by | String | Yes | Who locked |
| lock_reason | String | Yes | Reason for lock |
| reopened_at | Instant | Yes | When reopened |
| reopened_by | String | Yes | Who reopened |
| reopen_reason | String | Yes | Reason for reopen |
| closing_journal_entry_id | Long | Yes | Closing journal entry |

**Status Values**:
- OPEN: Normal operations
- LOCKED: Temporary lock for processing
- CLOSED: Period closed, no modifications

**Business Rules**:
- Date range auto-calculated from year/month
- Label generated (e.g., "January 2024")
- contains(LocalDate) for date range check

---

## Ledger Entities

### DealerLedgerEntry

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/DealerLedgerEntry.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `dealer_ledger_entries`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| dealer_id | Long | No | FK to Dealer |
| journal_entry_id | Long | Yes | FK to JournalEntry |
| entry_date | LocalDate | No | Entry date |
| reference_number | String | No | Journal reference |
| memo | String | Yes | Description |
| debit | BigDecimal | No | Debit (increase AR) |
| credit | BigDecimal | No | Credit (decrease AR) |
| due_date | LocalDate | Yes | Invoice due date |
| paid_date | LocalDate | Yes | When fully paid |
| invoice_number | String | Yes | Invoice reference |
| payment_status | String | Yes | UNPAID, PARTIAL, PAID, VOID, REVERSED |
| amount_paid | BigDecimal | Yes | Amount paid so far |
| created_at | Instant | No | Creation timestamp |

**Business Methods**:
- `getOutstandingAmount()`: Returns debit - credit - amountPaid
- `isOverdue(LocalDate)`: Check if entry is overdue
- `getDaysOverdue(LocalDate)`: Days past due date

**Indexes**:
- `idx_dealer_ledger_company`
- `idx_dealer_ledger_dealer`

---

### SupplierLedgerEntry

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/SupplierLedgerEntry.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `supplier_ledger_entries`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| supplier_id | Long | No | FK to Supplier |
| journal_entry_id | Long | Yes | FK to JournalEntry |
| entry_date | LocalDate | No | Entry date |
| reference_number | String | No | Journal reference |
| memo | String | Yes | Description |
| debit | BigDecimal | No | Debit (decrease AP) |
| credit | BigDecimal | No | Credit (increase AP) |
| created_at | Instant | No | Creation timestamp |

**Note**: Similar to DealerLedgerEntry but credit increases AP balance

---

## Settlement Entities

### PartnerSettlementAllocation

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/PartnerSettlementAllocation.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `partner_settlement_allocations`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| partner_type | PartnerType | No | DEALER or SUPPLIER |
| dealer_id | Long | Yes | FK to Dealer (if DEALER) |
| supplier_id | Long | Yes | FK to Supplier (if SUPPLIER) |
| invoice_id | Long | Yes | FK to Invoice |
| purchase_id | Long | Yes | FK to RawMaterialPurchase |
| journal_entry_id | Long | No | FK to JournalEntry |
| settlement_date | LocalDate | No | Date of settlement |
| allocation_amount | BigDecimal | No | Amount applied |
| discount_amount | BigDecimal | No | Discount given |
| write_off_amount | BigDecimal | No | Amount written off |
| fx_difference_amount | BigDecimal | No | FX gain/loss |
| currency | String | No | Currency code |
| idempotency_key | String | Yes | For idempotency |
| memo | String | Yes | Notes |
| created_at | Instant | No | Creation timestamp |

**Indexes**:
- `idx_partner_settlement_company`
- `idx_partner_settlement_partner`
- `idx_partner_settlement_invoice`
- `idx_partner_settlement_purchase`

---

## Reconciliation Entities

### BankReconciliationSession

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/BankReconciliationSession.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `bank_reconciliation_sessions`

**Unique Constraints**: `(company_id, reference_number)`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| bank_account_id | Long | No | FK to Account (bank) |
| accounting_period_id | Long | Yes | FK to AccountingPeriod |
| reference_number | String | No | Session reference |
| statement_date | LocalDate | No | Bank statement date |
| statement_ending_balance | BigDecimal | No | Statement balance |
| status | BankReconciliationSessionStatus | No | DRAFT, COMPLETED |
| created_by | String | No | Creator |
| completed_by | String | Yes | Completer |
| note | String | Yes | Notes |
| created_at | Instant | No | Creation timestamp |
| updated_at | Instant | No | Update timestamp |
| completed_at | Instant | Yes | Completion timestamp |

---

### BankReconciliationItem

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/BankReconciliationItem.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `bank_reconciliation_items`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| session_id | Long | No | FK to BankReconciliationSession |
| journal_line_id | Long | No | FK to JournalLine |
| reference_number | String | Yes | Transaction reference |
| amount | BigDecimal | No | Net amount |
| cleared_at | Instant | Yes | When cleared |
| cleared_by | String | Yes | Who cleared |

---

### ReconciliationDiscrepancy

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/ReconciliationDiscrepancy.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `reconciliation_discrepancies`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| type | ReconciliationDiscrepancyType | No | SUBLEDGER, INTER_COMPANY, etc. |
| status | ReconciliationDiscrepancyStatus | No | OPEN, RESOLVED |
| description | String | No | Description |
| amount | BigDecimal | No | Discrepancy amount |
| resolution | ReconciliationDiscrepancyResolution | Yes | Resolution details |

---

## Event/Audit Entities

### AccountingEvent

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEvent.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.event`

**Table**: `accounting_events`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| event_id | UUID | No | Unique event ID |
| company_id | Long | No | FK to Company |
| event_type | AccountingEventType | No | Type of event |
| aggregate_id | UUID | No | Aggregate root ID |
| aggregate_type | String | No | Aggregate type name |
| sequence_number | Long | No | Event sequence |
| event_timestamp | Instant | No | When event occurred |
| effective_date | LocalDate | No | Business date |
| account_id | Long | Yes | Related account |
| account_code | String | Yes | Account code |
| journal_entry_id | Long | Yes | Related journal entry |
| journal_reference | String | Yes | Journal reference |
| debit_amount | BigDecimal | Yes | Debit if applicable |
| credit_amount | BigDecimal | Yes | Credit if applicable |
| balance_before | BigDecimal | Yes | Balance before |
| balance_after | BigDecimal | Yes | Balance after |
| description | String | Yes | Event description |
| user_id | String | Yes | Actor |
| correlation_id | UUID | Yes | Correlation for batch |
| payload | String | Yes | JSON additional data |
| created_at | Instant | No | Creation timestamp |

**Indexes**:
- `idx_acct_events_company_ts`
- `idx_acct_events_account`
- `idx_acct_events_journal`
- `idx_acct_events_aggregate`

---

### AccountingPeriodSnapshot

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/AccountingPeriodSnapshot.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `accounting_period_snapshots`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| period_id | Long | No | FK to AccountingPeriod |
| as_of_date | LocalDate | No | Snapshot date |
| trial_balance_total_debit | BigDecimal | Yes | Total debits |
| trial_balance_total_credit | BigDecimal | Yes | Total credits |

---

### ClosedPeriodPostingException

**File**: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/ClosedPeriodPostingException.java`

**Package**: `com.bigbrightpaints.erp.modules.accounting.domain`

**Table**: `closed_period_posting_exceptions`

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| id | Long | No | Primary key |
| company_id | Long | No | FK to Company |
| period_id | Long | No | FK to AccountingPeriod |
| journal_entry_id | Long | No | FK to JournalEntry |
| reason | String | No | Exception reason |
| authorized_by | String | No | Who authorized |

---

## Enumerations

### AccountType
- ASSET (debit normal)
- LIABILITY (credit normal)
- EQUITY (credit normal)
- REVENUE (credit normal)
- EXPENSE (debit normal)
- COGS (debit normal)

### AccountingPeriodStatus
- OPEN
- LOCKED
- CLOSED

### BankReconciliationSessionStatus
- DRAFT
- COMPLETED

### CostingMethod
- FIFO
- WEIGHTED_AVERAGE
- STANDARD
- SPECIFIC_IDENTIFICATION

### JournalEntryType
- AUTOMATED
- MANUAL

### JournalCorrectionType
- REVERSAL
- ADJUSTMENT

### PartnerType
- DEALER
- SUPPLIER

### AccountingEventType
- JOURNAL_ENTRY_POSTED
- JOURNAL_ENTRY_REVERSED
- ACCOUNT_DEBIT_POSTED
- ACCOUNT_CREDIT_POSTED
- BALANCE_CORRECTION
