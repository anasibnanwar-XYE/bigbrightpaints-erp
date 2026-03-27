# Where Should New Code Go?

This guide helps you determine where to place new code in the BigBright ERP codebase. It covers extension points, safe modification areas, and prohibited patterns.

## Extension Point Guide

### Core Principles
1. **Services over repositories**: New business logic goes in services, not repositories
2. **Company scoping**: All database queries must filter by company
3. **Idempotency**: Write operations must support idempotency keys
4. **Accounting integration**: Financial operations must go through `AccountingFacade`

---

## Module-by-Module Extension Points

### Accounting Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add new journal type | `JournalEntryService` | ✅ | Use `AccountingFacade` for posting |
| Add new account type | `AccountType` enum | ✅ | Update business rules in `Account` entity |
| Add reconciliation type | `ReconciliationService` | ✅ | Implement new reconciliation method |
| Add financial report | `reports` module | ✅ | Use `ReportService` pattern |
| Add period close operation | `PeriodCloseHook` | ✅ | Implement interface |
| Direct journal creation | ❌ | **Prohibited** | Use `AccountingFacade` |

### Sales Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add order status | `SalesOrderLifecycleService` | ✅ | Update status constants |
| Add dealer field | `Dealer` entity, `DealerService` | ✅ | Add migration, update DTOs |
| Add credit logic | `CreditLimitRequestService` | ✅ | Use existing workflow pattern |
| Add fulfillment step | `SalesFulfillmentService` | ✅ | Extend orchestration |
| Add pricing rule | `SalesCoreEngine` | ✅ | Inject price calculation |
| Bypass credit check | ❌ | **Prohibited** | Must go through credit policy |

### Inventory Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add inventory type | `InventoryType` enum | ✅ | Update valuation logic |
| Add movement type | `InventoryMovementRecorder` | ✅ | Add movement type constant |
| Add batch source | `InventoryBatchSource` enum | ✅ | Update batch creation |
| Add costing method | `InventoryValuationService` | ✅ | Implement new method |
| Direct stock update | ❌ | **Prohibited** | Use `FinishedGoodsService` |
| Skip reservation | ❌ | **Prohibited** | Use `FinishedGoodsReservationEngine` |

### Factory Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add production status | `ProductionLogStatus` enum | ✅ | Update lifecycle service |
| Add packing type | `PackingService` | ✅ | Extend pack line processing |
| Add material consumption | `PackagingMaterialService` | ✅ | Add size mapping |
| Add cost allocation | `CostAllocationService` | ✅ | Extend allocation logic |
| Direct batch creation | ❌ | **Prohibited** | Use `PackingBatchService` |
| Skip material consumption | ❌ | **Prohibited** | Must consume materials |

### Purchasing Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add PO status | `PurchaseOrderStatus` enum | ✅ | Update lifecycle service |
| Add supplier field | `Supplier` entity | ✅ | Add migration, update DTOs |
| Add receipt processing | `GoodsReceiptService` | ✅ | Extend receipt logic |
| Add tax rule | `PurchaseTaxPolicy` | ✅ | Add tax calculation |
| Direct invoice posting | ❌ | **Prohibited** | Use `PurchaseInvoiceEngine` |
| Skip approval workflow | ❌ | **Prohibited** | Must follow workflow |

### Auth Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add MFA type | `MfaService` | ✅ | Implement new MFA provider |
| Add password rule | `PasswordPolicy` | ✅ | Add validation rule |
| Add session type | `RefreshTokenService` | ✅ | Extend token handling |
| Add auth step | `AuthService` | ✅ | Extend login flow |
| Direct password storage | ❌ | **Prohibited** | Use `PasswordEncoder` |
| Bypass MFA check | ❌ | **Prohibited** | Must go through MfaService |

### HR Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add employee type | `EmployeeType` enum | ✅ | Update payroll calculations |
| Add leave type | `LeaveTypePolicy` entity | ✅ | Add accrual rules |
| Add deduction type | `StatutoryDeductionEngine` | ✅ | Add calculation method |
| Add payroll component | `PayrollCalculationService` | ✅ | Extend calculation |
| Direct salary posting | ❌ | **Prohibited** | Use `PayrollPostingService` |
| Skip attendance check | ❌ | **Prohibited** | Must calculate from attendance |

### Company Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add lifecycle state | `CompanyLifecycleState` enum | ✅ | Update transition service |
| Add module | `CompanyModule` enum | ✅ | Add gating rules |
| Add tenant setting | `SystemSetting` entity | ✅ | Use `SystemSettingsService` |
| Add CoA account | `CoATemplateService` | ✅ | Add to template |
| Direct tenant creation | ❌ | **Prohibited** | Use `TenantOnboardingService` |
| Skip quota check | ❌ | **Prohibited** | Must enforce quotas |

### Production Module

| Task | Where to Add Code | Safe? | Notes |
|------|-------------------|-------|-------|
| Add product category | `ProductionProduct` entity | ✅ | Update catalog sync |
| Add size variant | `SizeVariant` entity | ✅ | Update packing |
| Add readiness condition | `SkuReadinessService` | ✅ | Add blocking condition |
| Add catalog field | `CatalogService` | ✅ | Update sync logic |
| Direct SKU creation | ❌ | **Prohibited** | Use `CatalogService` |

---

## Safe Modification Areas

### Adding New Fields to Existing Entities
1. **Add field to entity class** in `domain/` package
2. **Add migration script** for database schema change
3. **Update DTOs** in `dto/` package (Request and Response)
4. **Update service methods** in `service/` package
5. **Update controller** if field is exposed in API

### Adding New API Endpoints
1. **Add method to existing controller** in `controller/` package
2. **Add service method** in `service/` package
3. **Add request/response DTOs** in `dto/` package
4. **Add security annotation** (`@PreAuthorize`)
5. **Add OpenAPI documentation** annotations

### Adding New Business Rules
1. **Add to existing service class** in `service/` package
2. **Use existing patterns** (idempotency, company scoping)
3. **Add validation** in service layer
4. **Add audit logging** using `AuditService`

### Adding New Reports
1. **Add to `reports` module**
2. **Create query service** extending existing pattern
3. **Add controller endpoint** in `ReportController`
4. **Add DTOs** for request/response

---

## Prohibited Patterns

### ❌ Never Do These

1. **Direct Repository Access from Controllers**
   ```java
   // PROHIBITED
   @GetMapping("/dealers")
   public List<Dealer> listDealers() {
       return dealerRepository.findAll();
   }
   ```

2. **Bypassing Company Scoping**
   ```java
   // PROHIBITED
   List<Account> accounts = accountRepository.findAll();
   ```

3. **Direct Journal Entry Creation**
   ```java
   // PROHIBITED
   JournalEntry entry = new JournalEntry();
   journalEntryRepository.save(entry);
   ```

4. **Hardcoded Currency Values**
   ```java
   // PROHIBITED
   BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
   ```

5. **Missing Idempotency on Write Operations**
   ```java
   // PROHIBITED
   @PostMapping("/orders")
   public OrderDto createOrder(@RequestBody OrderRequest request) {
       return orderService.create(request);
   }
   ```

6. **Bypassing Period Lock Check**
   ```java
   // PROHIBITED
   journalEntryService.createEntry(request);
   ```

7. **Direct Stock Modification**
   ```java
   // PROHIBITED
   finishedGood.setCurrentStock(newQuantity);
   finishedGoodRepository.save(finishedGood);
   ```

8. **Manual Transaction Management**
   ```java
   // PROHIBITED
   transaction.begin();
   // ... operations
   transaction.commit();
   ```

---

## New Module Creation Checklist

If you need to create an entirely new module:

1. **Create module directory structure:**
   ```
   modules/{module}/
   ├── controller/
   ├── service/
   ├── domain/
   ├── dto/
   └── repository/
   ```

2. **Add module to `CompanyModule` enum** in company module

3. **Add path mapping to `ModuleGatingInterceptor`**

4. **Create module documentation** in `docs/codebase-dictionary/modules/{module}/`

5. **Update MASTER_INDEX.md** with new module entries

6. **Add to module overview table** in README.md

---

## Common Extension Scenarios

### Adding a New Document Type
1. Create entity in `domain/` package
2. Create repository in `repository/` package
3. Create service in `service/` package
4. Create controller in `controller/` package
5. Add accounting integration if financial
6. Add document lifecycle using `BusinessDocumentTruths`

### Adding a New Workflow
1. Identify existing workflow services (e.g., `SalesOrderLifecycleService`)
2. Create status enum or constants
3. Add status transition methods to service
4. Add status history tracking
5. Add audit logging
6. Add validation for transitions

### Adding a New Integration
1. Create integration service in appropriate module
2. Add configuration properties class
3. Add to application.yml configuration
4. Add error handling for integration failures
5. Add retry logic if needed

---

## Decision Tree: Where Does This Code Go?

```
Is it business logic?
├── Yes → service/ package
│   ├── Financial? → accounting module
│   ├── Sales-related? → sales module
│   ├── Inventory-related? → inventory module
│   └── (other domain) → respective module
└── No
    ├── REST endpoint? → controller/ package
    ├── Data persistence? → repository/ package
    ├── Data structure? → dto/ or domain/ package
    └── Cross-cutting? → core/ package
```

---

*Last updated: 2026-03-27*
