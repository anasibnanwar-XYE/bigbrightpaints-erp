# Module Dependencies

This document maps inter-module dependencies in BigBrightPaints ERP, showing which modules call which other modules and identifying shared dependencies.

## Module Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER                              │
│    Controllers (REST endpoints)                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              APPLICATION LAYER                               │
│    Services (Business Logic)                                                 │
│    ┌───────────────────────────────────────────────────────────────────┐    │
│    │                     Orchestrator Module                            │    │
│    │   CommandDispatcher, EventPublisherService, DashboardAggregation  │    │
│    └───────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               DOMAIN LAYER                                   │
│    ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│    │  Auth   │ │ Company │ │  RBAC   │ │  Admin  │ │ Portal  │ │Reports  │ │
│    └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ │
│         │          │          │          │          │          │           │
│    ┌────┴────┐ ┌────┴────┐ ┌────┴────┐                                 │
│    │  Sales  │ │Purchasing│ │  HR    │                                 │
│    └────┬────┘ └────┬────┘ └────┬────┘                                 │
│         │          │          │          ┌────┴────┐ ┌────┴────┐         │
│    ┌────┴────┐ ┌────┴────┐              │ Invoice │ │Production│         │
│    │Inventory│ │ Factory │              └────┬────┘ └────┬────┘         │
│    └────┬────┘ └────┬────┘                   │          │               │
│         │          │                          └──────────┘               │
│         └──────────┴────────────────────────────────┐                   │
│                                                      │                   │
│    ┌─────────────────────────────────────────────────┴─────────────────┐ │
│    │                      Accounting Module                             │ │
│    │    Core financial engine, journal entries, settlements, ledgers   │ │
│    └───────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            INFRASTRUCTURE LAYER                              │
│    Core (Audit, Security, Config, Notification)                             │
│    Repositories (JPA Data Access)                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Module Dependency Matrix

| From \ To | Auth | Company | Accounting | Sales | Purchasing | Inventory | Factory | HR | Invoice | Production | Reports | Portal | Admin | RBAC | Orchestrator | Core |
|-----------|------|---------|------------|-------|------------|-----------|---------|-----|---------|------------|---------|--------|-------|------|--------------|------|
| **Auth** | - | ✓ | | | | | | | | | | | | ✓ | | ✓ |
| **Company** | ✓ | - | | | | | | | | | | | | | | ✓ |
| **Accounting** | | ✓ | - | | | | | ✓ | ✓ | | | ✓ | | | | ✓ |
| **Sales** | | ✓ | ✓ | - | | ✓ | ✓ | | | ✓ | | ✓ | | | | ✓ |
| **Purchasing** | | | ✓ | | - | ✓ | | | | | | | | | | ✓ |
| **Inventory** | | ✓ | ✓ | | | - | | | | | | | | | | ✓ |
| **Factory** | | | ✓ | ✓ | | ✓ | - | | | ✓ | | | | | | ✓ |
| **HR** | | ✓ | ✓ | | | | | - | | | | | | | | ✓ |
| **Invoice** | | | ✓ | ✓ | | | | | - | | | ✓ | | | | ✓ |
| **Production** | | ✓ | | | | ✓ | | | | - | | | | | | ✓ |
| **Reports** | | | ✓ | | | ✓ | ✓ | ✓ | | | - | | | | | ✓ |
| **Portal** | | ✓ | ✓ | ✓ | | | ✓ | ✓ | ✓ | | | - | | | | ✓ |
| **Admin** | ✓ | ✓ | | | | | | | | | | | - | | | ✓ |
| **RBAC** | ✓ | | | | | | | | | | | | | - | | |
| **Orchestrator** | | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | | | ✓ | | | - | ✓ |

---

## Detailed Module Dependencies

### 1. Accounting Module
**Central dependency - called by most other modules**

#### Incoming Dependencies (Who calls Accounting):
- **Sales**: `SalesCoreEngine`, `DealerService`, `CreditLimitOverrideService`
- **Purchasing**: `GoodsReceiptService`, `PurchaseInvoiceService`, `PurchaseReturnService`
- **Inventory**: `FinishedGoodsWorkflowEngineService`, `InventoryAdjustmentService`, `OpeningStockImportService`
- **Factory**: `PackingJournalBuilder`, `CostAllocationService`, `ProductionLogService`
- **HR**: `PayrollPostingService`
- **Invoice**: `InvoiceSettlementPolicy`
- **Reports**: All report query services
- **Portal**: `DealerPortalService`, `EnterpriseDashboardService`
- **Orchestrator**: `CommandDispatcher`, `AuditDigestScheduler`

#### Outgoing Dependencies (Who Accounting calls):
- **Company**: `CompanyContextService` (tenant context)
- **HR**: `PayrollRunRepository`, `PayrollRunLineRepository` (payroll posting)
- **Invoice**: `InvoiceRepository` (settlements)
- **Core**: `AuditService`, `SystemSettingsService`, `CompanyClock`

#### Shared Repositories:
- `AccountRepository`
- `JournalEntryRepository`
- `DealerLedgerRepository`
- `SupplierLedgerRepository`
- `PartnerSettlementAllocationRepository`
- `AccountingPeriodRepository`

---

### 2. Sales Module

#### Incoming Dependencies:
- **Portal**: `PortalInsightsService` (dashboard)
- **Factory**: `FactoryService` (order fulfillment)
- **Invoice**: `InvoiceService` (invoice generation)
- **Accounting**: `DealerLedgerService` (ledger queries)
- **Orchestrator**: `CommandDispatcher`, `DashboardAggregationService`

#### Outgoing Dependencies:
- **Accounting**: `AccountingService`, `AccountingFacade`, `DealerLedgerService`, `GstService`
- **Inventory**: `FinishedGoodsService`, `FinishedGoodRepository`, `PackagingSlipRepository`
- **Invoice**: `InvoiceNumberService`, `InvoiceRepository`
- **Company**: `CompanyContextService`
- **Core**: `AuditService`, `CompanyClock`

#### Shared Repositories:
- `DealerRepository`
- `SalesOrderRepository`
- `SalesOrderItemRepository`
- `CreditLimitOverrideRequestRepository`
- `PromotionRepository`

---

### 3. Inventory Module

#### Incoming Dependencies:
- **Sales**: `SalesCoreEngine`, `SalesFulfillmentService`
- **Factory**: `BulkPackingService`, `PackingService`
- **Purchasing**: `GoodsReceiptService` (raw material intake)
- **Production**: `SkuReadinessService`
- **Reports**: `InventoryValuationService`
- **Accounting**: `InventoryAccountingService`

#### Outgoing Dependencies:
- **Accounting**: `AccountingFacade` (adjustments)
- **Company**: `CompanyContextService`
- **Core**: `AuditService`

#### Shared Repositories:
- `FinishedGoodRepository`
- `FinishedGoodBatchRepository`
- `RawMaterialRepository`
- `RawMaterialBatchRepository`
- `PackagingSlipRepository`
- `InventoryMovementRepository`
- `InventoryReservationRepository`

---

### 4. Factory Module

#### Incoming Dependencies:
- **Sales**: `SalesFulfillmentService` (order dispatch)
- **Orchestrator**: `CommandDispatcher`, `DashboardAggregationService`
- **Reports**: `ReportService` (production costs)

#### Outgoing Dependencies:
- **Accounting**: `AccountingFacade`, `PackingJournalBuilder`
- **Inventory**: `FinishedGoodRepository`, `FinishedGoodBatchRepository`
- **Production**: `ProductionProductRepository`
- **Sales**: `SalesOrderRepository` (fulfillment status)
- **Core**: `AuditService`

#### Shared Repositories:
- `ProductionLogRepository`
- `ProductionPlanRepository`
- `FactoryTaskRepository`
- `PackingRecordRepository`
- `PackagingSizeMappingRepository`

---

### 5. Purchasing Module

#### Incoming Dependencies:
- **Accounting**: `SupplierLedgerService` (supplier queries)
- **Orchestrator**: `CommandDispatcher`

#### Outgoing Dependencies:
- **Accounting**: `AccountingService` (invoice posting)
- **Inventory**: `RawMaterialRepository`, `RawMaterialBatchRepository`
- **Company**: `CompanyContextService`
- **Core**: `AuditService`

#### Shared Repositories:
- `SupplierRepository`
- `PurchaseOrderRepository`
- `GoodsReceiptRepository`
- `RawMaterialPurchaseRepository`

---

### 6. HR Module

#### Incoming Dependencies:
- **Accounting**: `PayrollPostingService` (payroll journal entries)
- **Reports**: `ReportService` (workforce insights)
- **Portal**: `PortalInsightsService`
- **Orchestrator**: `CommandDispatcher`

#### Outgoing Dependencies:
- **Accounting**: `AccountingService` (payroll posting)
- **Company**: `CompanyContextService`
- **Core**: `AuditService`

#### Shared Repositories:
- `EmployeeRepository`
- `PayrollRunRepository`
- `PayrollRunLineRepository`
- `LeaveRequestRepository`
- `AttendanceRepository`

---

### 7. Invoice Module

#### Incoming Dependencies:
- **Sales**: `SalesCoreEngine` (invoice creation)
- **Accounting**: `InvoiceRepository` (settlements)
- **Portal**: `DealerPortalService`

#### Outgoing Dependencies:
- **Accounting**: `AccountingService` (settlement policy)
- **Sales**: `DealerRepository`, `SalesOrderRepository`
- **Core**: `CompanyClock`

#### Shared Repositories:
- `InvoiceRepository`
- `InvoiceLineRepository`
- `InvoiceSequenceRepository`

---

### 8. Production/Catalog Module

#### Incoming Dependencies:
- **Factory**: `FactoryService` (BOM, product info)
- **Inventory**: `FinishedGoodService` (SKU management)

#### Outgoing Dependencies:
- **Inventory**: `FinishedGoodRepository`
- **Company**: `CompanyContextService`

#### Shared Repositories:
- `ProductionProductRepository`
- `ProductionBrandRepository`
- `CatalogImportRepository`

---

### 9. Reports Module

#### Incoming Dependencies:
- **Portal**: Dashboard displays
- **Admin**: Export approvals

#### Outgoing Dependencies:
- **Accounting**: All accounting repositories for financial reports
- **Inventory**: `FinishedGoodBatchRepository` (valuation)
- **Factory**: `ProductionLogRepository` (cost reports)
- **HR**: `EmployeeRepository` (workforce reports)

---

### 10. Portal Module

#### Incoming Dependencies:
- Frontend applications (dealer portal, admin portal)

#### Outgoing Dependencies:
- **Accounting**: `DealerLedgerService`, `AccountingService`
- **Sales**: `DealerRepository`, `SalesDashboardService`
- **Factory**: `FactoryService`
- **HR**: `HrService`
- **Invoice**: `InvoiceRepository`, `InvoicePdfService`
- **Company**: `TenantRuntimeEnforcementService`

---

### 11. Auth Module

#### Incoming Dependencies:
- **All modules**: Via `JwtAuthenticationFilter`, security context
- **Company**: `TenantAdminProvisioningService`
- **Admin**: `AdminUserService`

#### Outgoing Dependencies:
- **RBAC**: `RoleRepository`, `PermissionRepository`
- **Core**: `EmailService`, `AuditService`

#### Shared Repositories:
- `UserAccountRepository`
- `RefreshTokenRepository`
- `PasswordResetTokenRepository`
- `MfaRecoveryCodeRepository`

---

### 12. Company Module

#### Incoming Dependencies:
- **All modules**: Via `CompanyContextService` (tenant context)
- **Auth**: `TenantAdminProvisioningService`
- **Admin**: `SuperAdminTenantControlPlaneService`

#### Outgoing Dependencies:
- **Auth**: `TenantAdminProvisioningService`
- **Core**: `AuditService`

#### Shared Repositories:
- `CompanyRepository`
- `CoATemplateRepository`

---

### 13. Admin Module

#### Incoming Dependencies:
- Admin users (settings, approvals)

#### Outgoing Dependencies:
- **Auth**: `UserAccountRepository`, `AuthService`
- **Company**: `TenantRuntimeEnforcementService`
- **Core**: `EmailService`, `SystemSettingsService`

---

### 14. RBAC Module

#### Incoming Dependencies:
- **Auth**: `UserAccountDetailsService` (role assignment)
- **All modules**: Via `@PreAuthorize` annotations

#### Outgoing Dependencies:
- **Auth**: `UserAccountRepository`

---

### 15. Orchestrator Module

#### Incoming Dependencies:
- External systems (via `ExternalSyncService`)
- All controllers (via `TraceService`)

#### Outgoing Dependencies:
- **All domain modules**: Via `CommandDispatcher`
- **Company**: `CompanyContextService`
- **Core**: `AuditService`

---

### 16. Core Module
**Infrastructure layer - provides shared services**

#### Provided Services:
- `AuditService` → Used by all modules
- `EmailService` → Used by Auth, Admin
- `SystemSettingsService` → Used by Admin, Accounting
- `NumberSequenceService` → Used by Invoice, Sales
- `TokenBlacklistService` → Used by Auth
- `SecurityMonitoringService` → Internal
- `TenantRuntimeEnforcementService` → Used by Company, Portal

#### Shared Entities:
- `AuditLog`
- `NumberSequence`
- `SystemSetting`

---

## Circular Dependencies

### Identified Circular Dependencies:

1. **Sales ↔ Accounting**
   - Sales calls Accounting for: Invoice posting, ledger updates, GST calculations
   - Accounting calls Sales repositories for: Dealer lookups, invoice references

2. **Invoice ↔ Sales**
   - Invoice calls Sales for: Dealer info, order references
   - Sales calls Invoice for: Invoice generation, settlement

3. **Invoice ↔ Accounting**
   - Invoice calls Accounting for: Settlement policy
   - Accounting calls Invoice for: Settlement allocations

4. **Auth ↔ RBAC**
   - Auth calls RBAC for: Role/permission loading
   - RBAC calls Auth for: User account queries

**Resolution**: These are handled via:
- Facade pattern (`AccountingFacade`, `SalesCoreEngine`)
- Repository-only access (not service calls in reverse direction)
- Event-driven communication (`DomainEvent`, `ApplicationEventPublisher`)

---

## Shared Dependencies

### Cross-Module Shared Services:

| Service | Used By |
|---------|---------|
| `CompanyContextService` | All modules |
| `AuditService` | All modules |
| `CompanyClock` | Accounting, Sales, Invoice, Factory |
| `CompanyEntityLookup` | Accounting, Sales, Factory |
| `EmailService` | Auth, Admin |

### Cross-Module Shared Repositories:

| Repository | Used By |
|------------|---------|
| `DealerRepository` | Sales, Accounting, Invoice, Portal |
| `SupplierRepository` | Purchasing, Accounting |
| `FinishedGoodBatchRepository` | Inventory, Factory, Accounting |
| `InvoiceRepository` | Invoice, Accounting, Sales, Portal |
| `JournalEntryRepository` | Accounting, Reports |

---

## Module Boundaries

### Clear Boundaries:
- **Auth**: Authentication, MFA, password management
- **RBAC**: Roles and permissions only
- **Company**: Tenant management, company configuration
- **Admin**: System-wide admin operations

### Overlapping Concerns:
- **Sales + Invoice**: Order-to-invoice flow
- **Inventory + Factory**: Stock management for production
- **Accounting + All**: Financial postings and ledgers

### Integration Points:
- **Orchestrator**: Cross-module workflow coordination
- **Portal**: Unified dashboard aggregating all modules
- **Reports**: Cross-module reporting queries
