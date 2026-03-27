# Duplicate Watchlist - BigBright ERP

This document identifies overlapping helpers, services doing similar things, competing mappers, and old/new flows both active.

---

## Critical Duplicates (Immediate Action Required)

### 1. InventoryValuationService

| Location | Purpose |
|----------|---------|
| `modules.inventory.service.InventoryValuationService` | Inventory batch valuation |
| `modules.reports.service.InventoryValuationService` | Report-level inventory valuation |

**Issue:** Two services with identical class names but in different packages. This causes confusion about which one to use and increases maintenance burden.

**Recommendation:**
- Keep `reports.InventoryValuationService` for reporting purposes
- Rename `inventory.InventoryValuationService` to `InventoryBatchValuationService` or consolidate

**Impact:** High - Both services are used, potential for inconsistent behavior

---

### 2. TenantRuntimeEnforcementService

| Location | Purpose |
|----------|---------|
| `core.security.TenantRuntimeEnforcementService` | Core tenant enforcement |
| `modules.company.service.TenantRuntimeEnforcementService` | Company module enforcement |

**Issue:** Two services with identical names. The core.security version is the primary implementation; the company module version may be a wrapper or outdated.

**Recommendation:**
- Verify if both are needed
- If one wraps the other, rename to clarify relationship
- Consolidate if functionality is duplicated

**Impact:** High - Tenant enforcement is security-critical

---

## Moderate Overlaps (Review Required)

### 3. CatalogService vs ProductionCatalogService

| Service | Location | Responsibility |
|---------|----------|----------------|
| `CatalogService` | modules.production | Unclear scope |
| `ProductionCatalogService` | modules.production | Product catalog operations |

**Issue:** Both in the same module with potentially overlapping catalog responsibilities.

**Recommendation:**
- Determine canonical service
- Deprecate or merge the other
- If serving different purposes, rename for clarity

---

### 4. Multiple Idempotency Services

| Service | Module | Scope |
|---------|--------|-------|
| `IdempotencyReservationService` | core.idempotency | Cross-module |
| `SalesIdempotencyService` | sales | Sales operations |
| `AccountingIdempotencyService` | accounting | Accounting operations |
| `PackingIdempotencyService` | factory | Packing operations |
| `OrchestratorIdempotencyService` | orchestrator | Workflow operations |

**Issue:** While module-specific idempotency is a valid pattern, there may be code duplication in the idempotency logic.

**Recommendation:**
- Extract common idempotency logic to `IdempotencyReservationService`
- Module-specific services should delegate to core
- Document when to use which service

---

### 5. Ledger Services

| Service | Module | Partner Type |
|---------|--------|--------------|
| `DealerLedgerService` | accounting | Dealers (AR) |
| `SupplierLedgerService` | accounting | Suppliers (AP) |
| `AbstractPartnerLedgerService` | accounting | Shared base |

**Issue:** Good abstraction exists but naming is inconsistent with domain terminology.

**Recommendation:**
- Maintain current structure - abstraction is appropriate
- Add documentation clarifying AR vs AP scope

---

## Legacy Flows (Both Old and New Active)

### 6. Accounting Audit Trail

| Service | Status | Notes |
|---------|--------|-------|
| `AccountingAuditService` | Legacy | Simple audit |
| `AccountingAuditTrailService` | Canonical | Enterprise audit with ML tracking |

**Issue:** Both services may be in use.

**Recommendation:**
- Audit usage of `AccountingAuditService`
- Migrate callers to `AccountingAuditTrailService`
- Deprecate `AccountingAuditService`

---

### 7. Dispatch-Invoice Link Matching

| Component | Status | Purpose |
|-----------|--------|---------|
| `LegacyDispatchInvoiceLinkMatcher` | Legacy | Implicit link matching |
| Direct invoice_id FK | Canonical | Explicit foreign key |

**Issue:** Legacy matching still exists for migration/reconciliation.

**Recommendation:**
- Keep for migration purposes only
- Mark as `@Deprecated` with removal timeline
- Ensure all new code uses explicit FK

---

### 8. Bank Reconciliation

| Flow | Status | Description |
|------|--------|-------------|
| `ReconciliationService.reconcileLegacy()` | Legacy | Single-call reconciliation |
| `BankReconciliationSessionService` | Canonical | Interactive sessions |

**Issue:** Both approaches supported.

**Recommendation:**
- Determine if legacy flow is still used
- If not, deprecate and remove
- If yes, document when to use which

---

## Helper/Utility Overlaps

### 9. Costing Utilities

| Component | Location | Purpose |
|-----------|----------|---------|
| `CostingMethodUtils` | core.util | Normalization, validation |
| `CostingMethodService` | accounting.service | Costing operations |

**Issue:** Both deal with costing methods but at different levels.

**Recommendation:**
- Clear separation: Utils for validation/normalization, Service for business operations
- Document usage guidelines

---

### 10. Multiple Dashboard Services

| Service | Module | Scope |
|---------|--------|-------|
| `SalesDashboardService` | sales | Sales metrics |
| `DealerPortalService.getMyDashboard()` | sales | Dealer self-service |
| `EnterpriseDashboardService` | portal | Enterprise-wide |
| `DashboardAggregationService` | orchestrator | Cross-module aggregation |

**Issue:** Multiple dashboard entry points with unclear boundaries.

**Recommendation:**
- `DashboardAggregationService` for cross-module data
- Module dashboards for domain-specific metrics
- Portal for dealer self-service
- Document clearly

---

## Mapper/DTO Concerns

### 11. Centralized vs Distributed Mapping

| Approach | Location | Components |
|----------|----------|------------|
| Centralized | core.mapper | `CentralMapperConfig` |
| Distributed | modules | `PurchaseResponseMapper` (and likely others) |

**Issue:** Inconsistent mapping strategy.

**Recommendation:**
- Define clear strategy for DTO mapping
- If centralized, consolidate all mappers
- If distributed, document pattern

---

## Services with Similar Names/Responsibilities

### 12. CRUD Service Proliferation

| Module | CRUD Services | Pattern |
|--------|---------------|---------|
| Sales | `SalesOrderCrudService`, `SalesDealerCrudService` | Thin wrappers |
| Other modules | CRUD often in main service | Mixed |

**Issue:** Inconsistent patterns across modules.

**Recommendation:**
- Standardize on pattern (thin CRUD wrappers vs facade methods)
- Document preferred approach

---

### 13. Tenant Lifecycle Management

| Service | Responsibility |
|---------|---------------|
| `TenantLifecycleService` | Lifecycle state management |
| `TenantOnboardingService` | Onboarding |
| `TenantRuntimeEnforcementService` | Runtime admission |
| `TenantRuntimePolicyService` (admin) | Policy management |

**Issue:** Multiple services managing tenant state with unclear boundaries.

**Recommendation:**
- Document clear responsibilities
- Ensure no circular dependencies
- Consider consolidating related functions

---

## Potential Dead Code

### 14. Services to Review

| Service | Location | Concern |
|---------|----------|---------|
| `AccountingAuditService` | accounting | Marked Legacy, verify usage |
| `DemoController` | demo module | Should not be in production |

---

## Patterns to Watch

### 15. Engine vs Service Naming

| Pattern | Examples |
|---------|----------|
| *Service | Most services |
| *CoreEngine | `SalesCoreEngine`, `AccountingCoreEngine` |
| *Facade | `AccountingFacade` |
| *Helper | `PackingJournalLinkHelper` |

**Issue:** Inconsistent naming patterns may confuse new developers.

**Recommendation:**
- Define clear naming conventions
- Document when to use each pattern
- Consider refactoring for consistency

---

## Summary

| Category | Count | Priority |
|----------|-------|----------|
| Critical Duplicates | 2 | High |
| Moderate Overlaps | 7 | Medium |
| Legacy Flows | 3 | Medium |
| Naming Concerns | 3 | Low |

### Immediate Actions

1. Resolve `InventoryValuationService` duplicate naming
2. Resolve `TenantRuntimeEnforcementService` duplicate naming
3. Document idempotency service usage patterns

### Short-term Actions

1. Consolidate or clarify `CatalogService` vs `ProductionCatalogService`
2. Deprecate `AccountingAuditService` with migration plan
3. Standardize DTO mapping approach

### Long-term Actions

1. Standardize service naming conventions
2. Create clear documentation for service patterns
3. Implement code review guidelines to prevent duplicates
