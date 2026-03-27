# Core Infrastructure: Utilities

This document covers all utility classes in `com.bigbrightpaints.erp.core.util`.

## Overview

Utility classes provide cross-cutting functionality used throughout the ERP system. They handle money calculations, time zones, idempotency headers, password generation, costing methods, and document lifecycle management.

---

## MoneyUtils

| Field | Value |
|-------|-------|
| **Name** | MoneyUtils |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/MoneyUtils.java |
| **Responsibility** | Provides null-safe arithmetic operations for monetary calculations with 2-decimal precision |
| **Use when** | Performing any monetary calculation requiring null-safety |
| **Do not use when** | Direct BigDecimal operations are sufficient for non-monetary values |
| **Public methods** | `static BigDecimal zeroIfNull(BigDecimal value)`<br>`static BigDecimal safeMultiply(BigDecimal left, BigDecimal right)`<br>`static BigDecimal safeAdd(BigDecimal left, BigDecimal right)`<br>`static BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor, int scale, RoundingMode roundingMode)`<br>`static BigDecimal positiveCurrencyDelta(BigDecimal left, BigDecimal right, BigDecimal tolerance)`<br>`static BigDecimal roundCurrency(BigDecimal value)`<br>`static boolean withinTolerance(BigDecimal left, BigDecimal right, BigDecimal tolerance)` |
| **Callers** | All financial services: AccountingService, SalesService, PayrollService, CreditService |
| **Dependencies** | None (static utility) |
| **Side effects** | None |
| **Invariants protected** | Never returns null; always returns 2-decimal precision for currency; handles null/zero gracefully |
| **Status** | Canonical |

---

## CompanyClock

| Field | Value |
|-------|-------|
| **Name** | CompanyClock |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CompanyClock.java |
| **Responsibility** | Provides injectable Clock for company timezone-aware date/time operations with benchmark mode support |
| **Use when** | Need injectable Clock for testing or company-aware time; benchmark data generation |
| **Do not use when** | Simple CompanyTime.now() static access is sufficient |
| **Public methods** | `CompanyClock(ObjectProvider<Clock> clockProvider)`<br>`LocalDate today(Company company)`<br>`Instant now(Company company)`<br>`LocalDate dateForInstant(Company company, Instant instant)`<br>`ZoneId zoneId(Company company)` |
| **Callers** | IntegrationCoordinator, EnterpriseAuditTrailService, MockDataInitializer, services requiring testable time |
| **Dependencies** | `ObjectProvider<Clock>` (Spring injectable), Company (domain) |
| **Side effects** | None |
| **Invariants protected** | Timestamps in company timezone; benchmark override date respected |
| **Status** | Canonical |

### Configuration

```properties
# Override date for benchmark mode (optional)
erp.benchmark.override-date=2026-02-28
```

---

## IdempotencyHeaderUtils

| Field | Value |
|-------|-------|
| **Name** | IdempotencyHeaderUtils |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/IdempotencyHeaderUtils.java |
| **Responsibility** | Resolves idempotency keys from HTTP headers with legacy support and mismatch detection |
| **Use when** | Processing requests that require idempotency guarantees |
| **Do not use when** | Building new idempotency signatures (use IdempotencySignatureBuilder) |
| **Public methods** | `static String resolveHeaderKey(String idempotencyKeyHeader, String legacyIdempotencyKeyHeader)`<br>`static String resolveBodyOrHeaderKey(String bodyKey, String idempotencyKeyHeader, String legacyIdempotencyKeyHeader)` |
| **Callers** | Controllers with idempotent operations: DispatchController, JournalEntryController |
| **Dependencies** | `ApplicationException`, `ErrorCode` (core.exception) |
| **Side effects** | Throws ApplicationException on header mismatch |
| **Invariants protected** | Header consistency; prevents conflicting idempotency keys; logs warnings on mismatch |
| **Status** | Canonical |

---

## CostingMethodUtils

| Field | Value |
|-------|-------|
| **Name** | CostingMethodUtils |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CostingMethodUtils.java |
| **Responsibility** | Normalizes and validates inventory costing methods (FIFO/LIFO/WAC) |
| **Use when** | Validating or normalizing inventory costing methods |
| **Do not use when** | Method is already validated and normalized |
| **Public methods** | `static boolean isWeightedAverage(String method)`<br>`static <T> T selectWeightedAverageValue(String method, Supplier<T> weightedAverageSupplier, Supplier<T> nonWeightedSupplier)`<br>`static String normalizeRawMaterialMethodOrDefault(String method)`<br>`static String normalizeFinishedGoodMethodOrDefault(String method)`<br>`static String canonicalizeFinishedGoodMethodForSync(String method)`<br>`static String canonicalizeRawMaterialMethodForSync(String method)`<br>`static FinishedGoodBatchSelectionMethod resolveFinishedGoodBatchSelectionMethod(String method)` |
| **Callers** | InventoryService, FinishedGoodsService, RawMaterialService, ProductionService |
| **Dependencies** | None (static utility) |
| **Side effects** | None |
| **Invariants protected** | Costing methods normalized to FIFO/LIFO/WAC; defaults to FIFO |
| **Status** | Canonical |

### Valid Methods

| Normalized | Accepted Aliases |
|------------|------------------|
| FIFO | FIFO |
| LIFO | LIFO (finished goods only) |
| WAC | WAC, WEIGHTED_AVERAGE, WEIGHTED-AVERAGE |

---

## PasswordUtils

| Field | Value |
|-------|-------|
| **Name** | PasswordUtils |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/PasswordUtils.java |
| **Responsibility** | Generates secure random temporary passwords |
| **Use when** | Creating temporary passwords, password resets, demo account setup |
| **Do not use when** | Hashing passwords (use PasswordEncoder) |
| **Public methods** | `static String generateTemporaryPassword(int length)` |
| **Callers** | AuthService, UserManagementService, DataInitializers |
| **Dependencies** | `SecureRandom` (java.security) |
| **Side effects** | None (stateless) |
| **Invariants protected** | Passwords contain mixed case, digits, and special characters |
| **Status** | Canonical |

---

## BusinessDocumentTruths

| Field | Value |
|-------|-------|
| **Name** | BusinessDocumentTruths |
| **Type** | Utility (final class with static methods) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/BusinessDocumentTruths.java |
| **Responsibility** | Derives document lifecycle states (workflow + accounting) from domain objects |
| **Use when** | Determining document status for UI display or business rules |
| **Do not use when** | Simple status field check is sufficient |
| **Public methods** | `static DocumentLifecycleDto salesOrderLifecycle(SalesOrder order)`<br>`static DocumentLifecycleDto packagingSlipLifecycle(PackagingSlip slip)`<br>`static DocumentLifecycleDto invoiceLifecycle(String workflowStatus, JournalEntry journalEntry)`<br>`static DocumentLifecycleDto goodsReceiptLifecycle(GoodsReceipt goodsReceipt, RawMaterialPurchase linkedPurchase)`<br>`static DocumentLifecycleDto purchaseLifecycle(RawMaterialPurchase purchase)`<br>`static DocumentLifecycleDto settlementLifecycle(JournalEntry journalEntry)`<br>`static DocumentLifecycleDto journalLifecycle(JournalEntry journalEntry)`<br>`static LinkedBusinessReferenceDto reference(...)` |
| **Callers** | OrderService, DispatchService, InvoiceService, SettlementService, ReportService |
| **Dependencies** | SalesOrder, PackagingSlip, Invoice, JournalEntry, GoodsReceipt, RawMaterialPurchase (domain objects) |
| **Side effects** | None |
| **Invariants protected** | Consistent lifecycle derivation logic across all document types |
| **Status** | Canonical |

### Lifecycle States

| Workflow Status | Accounting Status | Condition |
|-----------------|-------------------|-----------|
| DRAFT | NOT_ELIGIBLE | No journal entry linked |
| PENDING | PENDING | Ready but not posted |
| POSTED | POSTED | Journal entry linked and posted |
| VOID/REVERSED/CANCELLED | REVERSED | Document reversed |
| BLOCKED | BLOCKED | Document blocked |

---

## CompanyEntityLookup

| Field | Value |
|-------|-------|
| **Name** | CompanyEntityLookup |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CompanyEntityLookup.java |
| **Responsibility** | Centralized company-scoped entity lookups with active-product validation |
| **Use when** | Looking up entities by company context with validation |
| **Do not use when** | Direct repository access is simpler and no validation needed |
| **Public methods** | `Dealer requireDealer(Company company, Long dealerId)`<br>`Supplier requireSupplier(Company company, Long supplierId)`<br>`RawMaterial requireRawMaterial(Company company, Long rawMaterialId)`<br>`RawMaterial requireActiveRawMaterial(Company company, Long rawMaterialId)`<br>`RawMaterial lockActiveRawMaterial(Company company, Long rawMaterialId)`<br>`SalesOrder requireSalesOrder(Company company, Long orderId)`<br>`Invoice requireInvoice(Company company, Long invoiceId)`<br>`FinishedGood requireActiveFinishedGood(Company company, Long finishedGoodId)`<br>`FinishedGood lockActiveFinishedGood(Company company, Long finishedGoodId)`<br>`Account requireAccount(Company company, Long accountId)`<br>`JournalEntry requireJournalEntry(Company company, Long journalEntryId)`<br>`Optional<JournalEntry> findJournalEntryByReference(Company company, String referenceNumber)`<br>`...` (20+ entity types) |
| **Callers** | All services requiring cross-module entity lookups |
| **Dependencies** | 20+ repositories injected via constructor |
| **Side effects** | None (read-only) |
| **Invariants protected** | Entity belongs to company; active product validation; pessimistic locking support |
| **Status** | Canonical |

---

## CompanyTime

| Field | Value |
|-------|-------|
| **Name** | CompanyTime |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CompanyTime.java |
| **Responsibility** | Static access to CompanyClock for domain/entity lifecycle hooks; defaults to UTC in non-Spring contexts |
| **Use when** | Need company-aware time from static context (entities, domain events) |
| **Do not use when** | Can inject CompanyClock directly |
| **Public methods** | `static Instant now(Company company)`<br>`static Instant now()`<br>`static LocalDate today(Company company)`<br>`static LocalDate today()` |
| **Callers** | Domain entities, AuditActionEvent, EnterpriseAuditTrailService, OutboxEvent |
| **Dependencies** | CompanyClock (via Spring injection) |
| **Side effects** | None |
| **Invariants protected** | Thread-safe singleton pattern; graceful fallback to UTC |
| **Status** | Canonical |

---

## DashboardWindow

| Field | Value |
|-------|-------|
| **Name** | DashboardWindow |
| **Type** | Record (immutable data class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/DashboardWindow.java |
| **Responsibility** | Calculates date ranges and bucketing for dashboard aggregation queries |
| **Use when** | Building dashboard queries with date range filters and comparison periods |
| **Do not use when** | Simple date range without windowing or comparison |
| **Public methods** | `static DashboardWindow resolve(String window, String compare, String timezone, String fallbackTimezone)`<br>`Instant startInstant()`<br>`Instant endExclusiveInstant()`<br>`List<LocalDate> bucketStarts()` |
| **Record fields** | `LocalDate start`, `LocalDate end`, `LocalDate compareStart`, `LocalDate compareEnd`, `ZoneId zone`, `String bucket`, `int bucketDays` |
| **Callers** | DashboardAggregationService, ReportService |
| **Dependencies** | CompanyTime |
| **Side effects** | None |
| **Invariants protected** | Timezone-aware windows; valid bucket granularity (DAILY for ≤31 days, WEEKLY otherwise) |
| **Status** | Canonical |

### Window Formats

| Format | Example | Description |
|--------|---------|-------------|
| Nd | 30d | Last N days |
| mtd | mtd | Month to date |
| qtd | qtd | Quarter to date |
| ytd | ytd | Year to date |

### Comparison Modes

| Mode | Description |
|------|-------------|
| prev | Previous equivalent period |
| yoy | Year-over-year comparison |

---

## LegacyDispatchInvoiceLinkMatcher

| Field | Value |
|-------|-------|
| **Name** | LegacyDispatchInvoiceLinkMatcher |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.util |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/LegacyDispatchInvoiceLinkMatcher.java |
| **Responsibility** | Matches legacy dispatch-invoice links during data migration and reconciliation |
| **Use when** | Migrating data where dispatch-invoice links were stored implicitly |
| **Do not use when** | New code should use explicit invoice_id foreign keys |
| **Public methods** | `static boolean isSlipLinkedToInvoice(PackagingSlip slip, Invoice invoice, List<PackagingSlip> candidateSlips, int salesOrderInvoiceCount)`<br>`static boolean hasExplicitInvoiceLinks(List<PackagingSlip> candidateSlips)`<br>`static int countCurrentInvoices(List<Invoice> invoices)`<br>`static boolean isCurrentInvoiceStatus(String status)` |
| **Callers** | DataMigrationService, ReconciliationService |
| **Dependencies** | PackagingSlip, Invoice (domain objects) |
| **Side effects** | None |
| **Invariants protected** | Legacy link detection logic preserved for migration accuracy |
| **Status** | Legacy (use only for migration/reconciliation) |
