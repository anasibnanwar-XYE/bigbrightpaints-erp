# Recommendations - BigBright ERP

This document provides refactoring suggestions, identifies canonical winners for consolidation, outlines migration paths, and lists deprecated code to phase out.

---

## 1. Critical Refactoring (High Priority)

### 1.1 Resolve Duplicate Class Names

#### InventoryValuationService Duplicate

**Current State:**
- `modules.inventory.service.InventoryValuationService`
- `modules.reports.service.InventoryValuationService`

**Recommendation:**
```
KEEP:     modules.reports.service.InventoryValuationService (for reporting)
RENAME:   modules.inventory.service.InventoryValuationService
    TO:   modules.inventory.service.InventoryBatchValuationService
```

**Migration Path:**
1. Create `InventoryBatchValuationService` extending current functionality
2. Update all references in inventory module
3. Deprecate old class with `@Deprecated(forRemoval = true)`
4. Remove after one release cycle

**Impact:** High - Prevents confusion, improves maintainability

---

#### TenantRuntimeEnforcementService Duplicate

**Current State:**
- `core.security.TenantRuntimeEnforcementService` (primary)
- `modules.company.service.TenantRuntimeEnforcementService` (duplicate?)

**Recommendation:**
```
CANONICAL: core.security.TenantRuntimeEnforcementService
ACTION:    Remove company module version if unused
           OR rename to clarify relationship
```

**Migration Path:**
1. Audit usage of `modules.company.service.TenantRuntimeEnforcementService`
2. If wrapper only, remove and update callers
3. If has unique functionality, merge into core version

---

### 1.2 CatalogService Consolidation

**Current State:**
- `CatalogService` in production module
- `ProductionCatalogService` in production module

**Recommendation:**
```
CANONICAL: ProductionCatalogService
DEPRECATE: CatalogService
```

**Migration Path:**
1. Document which service is canonical
2. Merge functionality into `ProductionCatalogService`
3. Deprecate `CatalogService`
4. Update all callers

---

## 2. Canonical Winners (Consolidation Targets)

### 2.1 Idempotency Pattern

**Current State:** Multiple module-specific idempotency services

**Canonical Pattern:**
```
core.idempotency.IdempotencyReservationService (base)
    ↑
    ├── AccountingIdempotencyService
    ├── SalesIdempotencyService
    ├── PackingIdempotencyService
    └── OrchestratorIdempotencyService
```

**Recommendation:**
- Extract common logic to `IdempotencyReservationService`
- Module services delegate to base for storage
- Add interface `IdempotentOperation` for consistency

**Implementation:**
```java
public interface IdempotentOperation<T> {
    String operationType();
    T execute();
}

// Usage
accountingIdempotencyService.execute(idempotencyKey, operationType, () -> {
    // business logic
});
```

---

### 2.2 Partner Ledger Pattern

**Canonical:** `AbstractPartnerLedgerService<Partner, Entry>`

**Current Implementations:**
- `DealerLedgerService` (AR)
- `SupplierLedgerService` (AP)

**Recommendation:**
- Pattern is correct, no changes needed
- Document clearly in code comments
- Consider adding `PartnerLedgerService` interface for polymorphism

---

### 2.3 Accounting Audit Trail

**Canonical Winner:** `AccountingAuditTrailService`

**To Deprecate:** `AccountingAuditService`

**Migration Path:**
1. Audit all callers of `AccountingAuditService`
2. Update to use `AccountingAuditTrailService`
3. Add `@Deprecated` annotation
4. Remove in next major version

---

## 3. Migration Paths

### 3.1 Accounting Audit Migration

**From:** `AccountingAuditService`
**To:** `AccountingAuditTrailService`

**Steps:**
```java
// Old
accountingAuditService.auditDigest(from, to);

// New
accountingAuditTrailService.listTransactions(from, to, null, null, null, 0, 100);
```

---

### 3.2 Legacy Dispatch-Invoice Link

**From:** `LegacyDispatchInvoiceLinkMatcher`
**To:** Direct `invoice_id` FK usage

**Steps:**
1. Run migration script to populate missing `invoice_id` fields
2. Verify all dispatches have explicit invoice links
3. Remove `LegacyDispatchInvoiceLinkMatcher` usage
4. Keep class for historical reference only

---

### 3.3 Bank Reconciliation

**From:** `ReconciliationService.reconcileLegacy()`
**To:** `BankReconciliationSessionService`

**Steps:**
```java
// Old - single call
reconciliationService.reconcileLegacy(request);

// New - session-based
var session = bankReconciliationSessionService.startSession(createRequest);
session = bankReconciliationSessionService.updateItems(sessionId, itemsUpdate);
session = bankReconciliationSessionService.completeSession(sessionId, completionRequest);
```

---

## 4. Deprecated Code to Phase Out

### 4.1 Immediately Deprecate

| Class/Method | Location | Replacement | Timeline |
|--------------|----------|-------------|----------|
| `AccountingAuditService` | accounting | `AccountingAuditTrailService` | 2 releases |
| `CatalogService` | production | `ProductionCatalogService` | 2 releases |
| `ReconciliationService.reconcileLegacy()` | accounting | `BankReconciliationSessionService` | 3 releases |

### 4.2 Already Deprecated (Confirm Removal)

| Class | Location | Status |
|-------|----------|--------|
| `LegacyDispatchInvoiceLinkMatcher` | core.util | Keep for migration only |

### 4.3 Candidate for Removal

| Class | Location | Reason |
|-------|----------|--------|
| `DemoController` | demo | Should not be in production builds |
| Demo module | modules.demo | Remove or gate with profile |

---

## 5. Code Quality Improvements

### 5.1 Service Naming Standardization

**Current Patterns:**
- `*Service` - Standard
- `*CoreEngine` - Core orchestrator
- `*Facade` - Entry point
- `*Helper` - Utility class

**Recommendation:**
Create naming guidelines document:

| Suffix | When to Use |
|--------|-------------|
| Service | Standard business logic |
| Facade | External-facing entry point |
| CoreEngine | Internal orchestrator |
| Helper | Stateless utility within module |
| Utils | Cross-cutting utility |

**Action Items:**
1. Document naming conventions
2. Audit existing services
3. Rename where appropriate

---

### 5.2 Reduce Service Proliferation

**Issue:** Too many thin CRUD services

**Recommendation:**
- Keep facade pattern with internal methods
- Only extract if complexity warrants
- Document when to extract

**Example:**
```java
// Instead of SalesOrderCrudService + SalesDealerCrudService
// Keep in SalesService with clear method groups
public class SalesService {
    // Order operations
    public SalesOrderDto createOrder(...) { ... }
    public SalesOrderDto updateOrder(...) { ... }
    
    // Dealer operations
    public DealerDto createDealer(...) { ... }
    public DealerDto updateDealer(...) { ... }
}
```

---

### 5.3 DTO Mapping Strategy

**Current State:** Mixed centralized/distributed

**Recommendation:**
- Use MapStruct with `CentralMapperConfig`
- Module-specific mappers extend central config
- No manual mapping

**Implementation:**
```java
@Mapper(config = CentralMapperConfig.class)
public interface SalesMapper {
    SalesOrderDto toDto(SalesOrder entity);
    SalesOrder toEntity(SalesOrderRequest request);
}
```

---

## 6. Architecture Improvements

### 6.1 Extract Domain Events

**Current State:** Events published but not formalized

**Recommendation:**
Create domain event interfaces:

```java
public interface DomainEvent {
    UUID eventId();
    Instant occurredAt();
    String aggregateType();
    Long aggregateId();
}

public interface SalesOrderEvent extends DomainEvent {
    Long orderId();
}
```

**Benefits:**
- Type-safe event handling
- Better audit trail
- Easier testing

---

### 6.2 Repository Pattern Standardization

**Current State:** Mix of Spring Data repositories and custom implementations

**Recommendation:**
- Standardize on Spring Data JPA
- Custom queries via `@Query` or QueryDSL
- Document patterns

---

### 6.3 Transaction Boundary Documentation

**Current State:** `@Transactional` scattered without clear patterns

**Recommendation:**
- Document transaction boundaries in service docs
- Use `@Transactional(readOnly = true)` for queries
- Consider explicit transaction service for complex operations

---

## 7. Security Improvements

### 7.1 Encryption at Rest

**Current State:** Bank details encrypted, some sensitive data may not be

**Recommendation:**
Audit all sensitive data fields:
- [ ] Bank account details ✓ (encrypted)
- [ ] GST credentials
- [ ] API keys
- [ ] Personal identification numbers

---

### 7.2 Audit Trail Completeness

**Current State:** Most operations audited

**Recommendation:**
Ensure all write operations have audit trail:
- Add `@Audited` annotation for automatic logging
- Verify ML interaction tracking covers all UI actions

---

## 8. Performance Optimizations

### 8.1 Query Optimization

**Recommendations:**
1. Add indexes on frequently queried columns
2. Use batch queries for dashboard aggregations
3. Consider read replicas for reports

### 8.2 Caching Strategy

**Recommendations:**
1. Cache frequently accessed reference data
2. Use `@Cacheable` for expensive calculations
3. Implement cache invalidation on updates

---

## 9. Documentation Improvements

### 9.1 API Documentation

**Recommendations:**
1. Ensure all endpoints have OpenAPI annotations
2. Document error codes per endpoint
3. Add request/response examples

### 9.2 Code Documentation

**Recommendations:**
1. Add Javadoc to all public methods
2. Document invariants in service classes
3. Link related classes in documentation

---

## 10. Priority Matrix

| Action | Priority | Effort | Impact |
|--------|----------|--------|--------|
| Resolve InventoryValuationService duplicate | High | Low | High |
| Resolve TenantRuntimeEnforcementService duplicate | High | Low | High |
| Deprecate AccountingAuditService | High | Medium | Medium |
| Consolidate CatalogService | Medium | Low | Medium |
| Standardize idempotency pattern | Medium | Medium | High |
| Document service naming conventions | Medium | Low | Medium |
| Implement DTO mapping strategy | Low | High | Medium |
| Extract domain events | Low | High | Medium |

---

## Summary

### Immediate Actions (This Sprint)
1. Resolve `InventoryValuationService` duplicate naming
2. Resolve `TenantRuntimeEnforcementService` duplicate naming
3. Add `@Deprecated` to `AccountingAuditService`

### Short-term Actions (Next 2 Sprints)
1. Consolidate `CatalogService` into `ProductionCatalogService`
2. Document service naming conventions
3. Complete audit of all deprecated code usage

### Long-term Actions (Next Quarter)
1. Implement standardized DTO mapping
2. Extract domain event interfaces
3. Create comprehensive API documentation
4. Implement caching strategy
