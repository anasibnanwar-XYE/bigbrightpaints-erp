# AI Context Guide

This document provides token-efficient context for AI agents working with the BigBright ERP codebase.

## Quick Reference

If you need to... | Start here
----------------| | ----------
Add new business logic | [Extension Points](#extension-points-by-module)
Understand the architecture | [Module Overview](#module-overview)
Fix a bug | Check [Common Mistakes](#common-mistakes-to-avoid)
Learn coding patterns | [Preferred Patterns](#preferred-patterns)
Find existing code | [MASTER_INDEX.md](MASTER_INDEX.md)

---

## Module Overview

| Module | Primary Purpose | Key Services |
|--------|----------------|--------------|
| **Accounting** | Financial ledger, AR/AP, journal entries | AccountingService, JournalEntryService |
| **Sales** | Order-to-cash workflow | SalesCoreEngine  DealerService |
| **Inventory** | Stock management, dispatch | FinishedGoodsService  RawMaterialService |
| **Factory** | Manufacturing-to-stock | ProductionLogService  PackingService |
| **Purchasing** | Procure-to-pay workflow | PurchasingService  SupplierService |
| **Auth** | Authentication, sessions | AuthService  MfaService |
| **HR** | Employees, payroll, attendance | EmployeeService  PayrollService |
| **Admin** | User management, support | AdminUserService  SupportTicketService |
| **Company** | Multi-tenancy, onboarding | CompanyService  TenantOnboardingService |
| **Production** | Product catalog, SKUs | CatalogService |
| **Invoice** | Invoice generation, PDFs | InvoiceService  InvoicePdfService |
| **Portal** | Dashboard insights | PortalInsightsService |
| **RBAC** | Roles, permissions | RoleService |
| **Reports** | Financial reports | ReportService |

---

## Extension Points by Module

### Accounting Module
**Safe to extend:**
- Add new journal reference types (extend `JournalReference` enum)
- Add new account types (extend `AccountType` enum)
- Add new reconciliation types (implement custom `ReconciliationService`)

**Extension patterns:**
- `AccountingCoreEngine` can be extended for custom journal types
- `PeriodCloseHook` interface for custom period close operations

### Sales Module
**Safe to extend:**
- Add new order statuses (update `SalesOrder` status values)
- Add new credit limit policies (implement `CreditLimitPolicy` interface)
- Add new dispatch confirmation types

**Extension patterns:**
- `SalesCoreEngine` orchestrates the O2C flow - inject custom services
- `SalesFulfillmentService` for custom fulfillment logic

### Inventory Module
**Safe to extend:**
- Add new inventory types (STANDARD, EXEMPT)
- Add new movement types (extend `InventoryMovement` movement types)
- Add new batch sources

**Extension patterns:**
- `InventoryValuationService` supports FIFO, LIFO, WAC
- `FinishedGoodsReservationEngine` for custom reservation logic

### Factory Module
**Safe to extend:**
- Add new production log statuses
- Add new packing types (custom sizes)
- Add new cost allocation rules

**Extension patterns:**
- `ProductionLogService` handles M2S flow
- `PackingService` for standard packing, `BulkPackingService` for bulk-to-size

### Purchasing Module
**Safe to extend:**
- Add new purchase order statuses
- Add new goods receipt types
- Add new supplier approval workflows

**Extension patterns:**
- `PurchasingService` is the main facade
- `PurchaseInvoiceEngine` handles invoice posting

### Auth Module
**Safe to extend:**
- Add new MFA providers (implement `MfaService` patterns)
- Add new password policies (extend `PasswordPolicy`)
- Add new token types

**Extension patterns:**
- `AuthService` handles login/logout/refresh
- `MfaService` for TOTP-based MFA

### HR Module
**Safe to extend:**
- Add new employee types (STAFF, LABOUR)
- Add new leave types (extend `LeaveTypePolicy`)
- Add new payroll components

**Extension patterns:**
- `PayrollService` orchestrates payroll runs
- `PayrollCalculationService` for custom calculation logic

### Company Module
**Safe to extend:**
- Add new company lifecycle states
- Add new module gating rules
- Add new tenant quota types

**Extension patterns:**
- `TenantOnboardingService` for tenant setup
- `CoATemplateService` for custom chart of accounts

### Production Module (Catalog)
**Safe to extend:**
- Add new product categories
- Add new size variant configurations
- Add new SKU readiness conditions

**Extension patterns:**
- `CatalogService` for product CRUD
- `SkuReadinessService` for readiness checks

---

## Common Mistakes to Avoid

### 1. Direct Repository Access in Business Logic
❌ **Wrong:**
```java
// Bypassing service layer
Dealer dealer = dealerRepository.findById(dealerId);
```

✅ **Correct:**
```java
// Use service layer
DealerDto dealer = dealerService.getDealer(dealerId);
```

### 2. Bypassing Company Context
❌ **Wrong:**
```java
// Missing company scoping
List<Account> accounts = accountRepository.findAll();
```

✅ **Correct:**
```java
// Use company context
Company company = companyContextService.requireCurrentCompany();
List<Account> accounts = accountRepository.findByCompany(company);
```

### 3. Not Using Idempotency for Write Operations
❌ **Wrong:**
```java
// No idempotency protection
JournalEntry entry = journalEntryService.createJournalEntry(request);
```

✅ **Correct:**
```java
// Use idempotency key
String idempotencyKey = IdempotencyHeaderUtils.resolveHeaderKey(header, legacyHeader);
JournalEntry entry = journalEntryService.createJournalEntry(request, idempotencyKey);
```

### 4. Direct Journal Entry Creation
❌ **Wrong:**
```java
// Creating journals directly bypasses accounting rules
JournalEntry entry = new JournalEntry();
journalEntryRepository.save(entry);
```

✅ **Correct:**
```java
// Use AccountingFacade for proper journal posting
JournalEntryDto entry = accountingFacade.createJournalEntry(request);
```

### 5. Ignoring Accounting Period Status
❌ **Wrong:**
```java
// Posting to closed period
journalEntryService.createEntry(request);
```

✅ **Correct:**
```java
// Check period is open first
accountingPeriodService.requireOpenPeriod(entryDate);
journalEntryService.createEntry(request);
```

### 6. Not Using CompanyEntityLookup
❌ **Wrong:**
```java
// Manual lookup without validation
Dealer dealer = dealerRepository.findById(dealerId).orElseThrow();
```

✅ **Correct:**
```java
// Use CompanyEntityLookup for company-scoped validation
Dealer dealer = companyEntityLookup.requireDealer(company, dealerId);
```

### 7. Hardcoding Currency Precision
❌ **Wrong:**
```java
// Manual rounding can cause precision issues
BigDecimal total = amount.setScale(2, RoundingMode.HALF_UP);
```

✅ **Correct:**
```java
// Use MoneyUtils for consistent currency handling
BigDecimal total = MoneyUtils.roundCurrency(amount);
```

### 8. Not Using BusinessDocumentTruths
❌ **Wrong:**
```java
// Manually deriving lifecycle status
String status = journalEntry != null ? "POSTED" : "DRAFT";
```

✅ **Correct:**
```java
// Use BusinessDocumentTruths for consistent lifecycle derivation
DocumentLifecycleDto lifecycle = BusinessDocumentTruths.journalLifecycle(journalEntry);
```

---

## Preferred Patterns

### 1. Service Layer Pattern
Always use service layer for business logic. Never access repositories directly from controllers.

```java
@Service
public class MyService {
    private final MyRepository repository;
    private final CompanyContextService companyContext;
    
    public MyDto doSomething(MyRequest request) {
        Company company = companyContext.requireCurrentCompany();
        // Business logic here
        return result;
    }
}
```

### 2. Company-Scoped Queries
Always filter by company for multi-tenant isolation.

```java
public List<Account> findByCompany(Company company) {
    return accountRepository.findByCompany(company);
}
```

### 3. Idempotency Pattern
Use idempotency keys for all write operations.

```java
@PostMapping("/api/v1/resource")
public ResponseEntity<ApiResponse<MyDto>> create(
        @Valid @RequestBody MyRequest request,
        @RequestHeader("Idempotency-Key") String idempotencyKey) {
    
    String key = IdempotencyHeaderUtils.resolveHeaderKey(idempotencyKey, null);
    MyDto result = service.create(request, key);
    return ResponseEntity.ok(ApiResponse.success("Created", result));
}
```

### 4. Accounting Integration Pattern
Use `AccountingFacade` for journal entries, never create them directly.

```java
// Correct pattern for creating journal entries
JournalEntryDto entry = accountingFacade.createManualJournal(request);
```

### 5. Entity Lookup Pattern
Use `CompanyEntityLookup` for entity lookups with validation.

```java
// Correct pattern for entity lookup
Dealer dealer = companyEntityLookup.requireDealer(company, dealerId);
```

### 6. Money Handling Pattern
Use `MoneyUtils` for all monetary calculations.

```java
// Correct pattern for money calculations
BigDecimal total = MoneyUtils.safeAdd(subtotal, tax);
BigDecimal rounded = MoneyUtils.roundCurrency(total);
```

### 7. Time Handling Pattern
Use `CompanyClock` or `CompanyTime` for company-aware timestamps.

```java
// Correct pattern for time handling
LocalDate today = companyClock.today(company);
Instant now = companyClock.now(company);
```

### 8. Document Lifecycle Pattern
Use `BusinessDocumentTruths` for deriving lifecycle status.

```java
// Correct pattern for lifecycle derivation
DocumentLifecycleDto lifecycle = BusinessDocumentTruths.invoiceLifecycle(
    invoice.getStatus(), 
    invoice.getJournalEntry()
);
```

### 9. Exception Handling Pattern
Use `ApplicationException` with appropriate `ErrorCode`.

```java
// Correct pattern for business exceptions
throw new ApplicationException("Dealer not found", ErrorCode.DEALER_NOT_FOUND);
```

### 10. Module Gating Pattern
Check module enablement before using gated features.

```java
// Correct pattern for module gating
moduleGatingService.requireEnabledForCurrentCompany(
    CompanyModule.MANUFACTURING, 
    "/api/v1/factory"
);
```

---

## NO FALLBACKS POLICY (CRITICAL)

**FALLBACKS ARE PROHIBITED** in this codebase.

### Why No Fallbacks?
1. Fallbacks hide bugs silently
2. Fallbacks create data corruption
3. Fallbacks make debugging impossible
4. Fallbacks violate invariants silently

### Examples of PROHIBITED Fallbacks

❌ **WRONG - Silent Failure:**
```java
public Account getAccount(Long id) {
    Account account = repository.findById(id);
    if (account == null) {
        log.warn("Account not found, returning default");
        return new Account(); // NEVER DO THIS
    }
    return account;
}
```

✅ **RIGHT - Explicit Failure:**
```java
public Account getAccount(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException(
            "Account not found with id: " + id, 
            ErrorCode.ACCOUNT_NOT_FOUND
        ));
}
```

### Only Allowed Fallbacks
1. Optional display data (UI can show "N/A")
2. Cache misses (fall back to DB, but log)
3. Feature flags (default to safe behavior)

---

## MIGRATION TRACKING (CRITICAL)

**Every migration MUST be logged in MIGRATION_LOG.md**

Before creating a migration:
1. Check MIGRATION_LOG.md for conflicts
2. Get next available V2_XXX number
3. Log your migration with: created by, date, purpose, used by, conflicts

---

## DOCUMENTATION UPDATE RULES

**After code changes, you MUST update docs:**

| What You Did | What to Update | Max Time |
|--------------|----------------|----------|
| New class/service | MASTER_INDEX.md, module docs | 2 min |
| Changed method | Module SERVICES.md/CONTROLLERS.md | 1 min |
| New endpoint | ENTRY_POINT_MAP.md, CONTROLLERS.md | 1 min |
| New migration | MIGRATION_LOG.md | 30 sec |
| New test | TEST_INVENTORY.json | 30 sec |
| Deprecated class | CANONICALITY_MAP.md | 30 sec |

**See [LIVING_DOCUMENTATION_PROTOCOL.md](LIVING_DOCUMENTATION_PROTOCOL.md) for full rules.**

---

## Token Efficiency Tips
- **Use this document first** before exploring codebase
- **Check MASTER_INDEX.md** for specific class locations
- **Reference extension points** for safe modification areas
- **Follow preferred patterns** to avoid common mistakes
- **Use CompanyEntityLookup** instead of direct repository access
- **NO FALLBACKS** - always fail explicitly
- **LOG MIGRATIONS** - check MIGRATION_LOG.md before creating
- **UPDATE DOCS** - spend max 2 min after code changes
