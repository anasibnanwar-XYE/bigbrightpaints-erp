# Service Dependency Graph

This document maps the service-to-service relationships across BigBrightPaints ERP, showing who calls whom and the key dependency chains (Controller → Service → Repository pattern).

## Overview

The ERP follows a layered architecture:
- **Controllers** → Handle HTTP requests/responses
- **Services** → Business logic orchestration
- **Repositories** → Data access (JPA)

---

## Module Service Dependency Chains

### Accounting Module

```
AccountingController
├── AccountingService (Facade)
│   ├── JournalEntryService
│   │   └── JournalEntryRepository
│   ├── DealerReceiptService
│   │   ├── DealerLedgerService
│   │   │   └── DealerLedgerRepository
│   │   └── SettlementService
│   ├── SettlementService
│   │   ├── PartnerSettlementAllocationRepository
│   │   ├── InvoiceRepository
│   │   └── RawMaterialPurchaseRepository
│   ├── CreditDebitNoteService
│   │   └── JournalEntryRepository
│   ├── InventoryAccountingService
│   │   ├── FinishedGoodBatchRepository
│   │   └── RawMaterialBatchRepository
│   ├── AccountingPeriodService
│   │   ├── AccountingPeriodRepository
│   │   └── AccountingPeriodSnapshotRepository
│   └── AccountingFacade
│       └── AccountingCoreEngine
├── DealerLedgerService
├── SupplierLedgerService
│   └── SupplierLedgerRepository
├── TaxService
│   └── GstService
├── AccountingAuditService
├── BankReconciliationSessionService
│   └── BankReconciliationSessionRepository
└── TallyImportService
    └── TallyImportRepository
```

**Key Cross-Module Dependencies:**
- `AccountingService` → `InvoiceRepository` (Invoice module)
- `AccountingService` → `RawMaterialPurchaseRepository` (Purchasing module)
- `AccountingService` → `PayrollRunRepository` (HR module)
- `AccountingService` → `DealerRepository`, `SupplierRepository` (Sales/Purchasing)

---

### Sales Module

```
SalesController
├── SalesService (Facade)
│   ├── SalesOrderCrudService
│   │   └── SalesOrderRepository
│   ├── SalesOrderLifecycleService
│   │   ├── SalesOrderRepository
│   │   └── SalesOrderStatusHistoryRepository
│   ├── SalesDealerCrudService
│   │   └── DealerRepository
│   ├── SalesDispatchReconciliationService
│   │   └── PackagingSlipRepository
│   └── SalesDashboardService
├── DealerService
│   ├── DealerRepository
│   ├── DealerLedgerService (Accounting)
│   └── AccountingService (Accounting)
├── CreditLimitOverrideService
│   └── CreditLimitOverrideRequestRepository
├── CreditLimitRequestService
│   └── CreditRequestRepository
└── DealerPortalService
    ├── DealerRepository
    ├── DealerLedgerService (Accounting)
    └── InvoiceRepository (Invoice)
```

**Key Cross-Module Dependencies:**
- `SalesService` → `AccountingService`, `AccountingFacade` (Accounting)
- `SalesService` → `FinishedGoodsService` (Inventory)
- `SalesService` → `InvoiceNumberService` (Invoice)
- `DealerService` → `DealerLedgerService` (Accounting)
- `SalesCoreEngine` → `GstService` (Accounting)

---

### Inventory Module

```
DispatchController
├── FinishedGoodsWorkflowEngineService
│   ├── FinishedGoodsReservationEngine
│   │   └── InventoryReservationRepository
│   ├── FinishedGoodsDispatchEngine
│   │   ├── FinishedGoodBatchRepository
│   │   └── PackagingSlipRepository
│   └── PackagingSlipService
│       └── PackagingSlipRepository
├── InventoryAdjustmentService
│   ├── InventoryAdjustmentRepository
│   └── AccountingFacade (Accounting)
├── InventoryBatchQueryService
│   └── FinishedGoodBatchRepository
└── DeliveryChallanPdfService

FinishedGoodController
├── FinishedGoodsService
│   ├── FinishedGoodRepository
│   └── FinishedGoodBatchRepository
├── InventoryValuationService
│   └── FinishedGoodBatchRepository
└── InventoryBatchTraceabilityService

RawMaterialController
├── RawMaterialService
│   ├── RawMaterialRepository
│   └── RawMaterialBatchRepository
├── InventoryAdjustmentService
└── InventoryMovementRecorder
    └── InventoryMovementRepository

OpeningStockImportController
└── OpeningStockImportService
    ├── OpeningStockImportRepository
    └── AccountingService (Accounting)
```

**Key Cross-Module Dependencies:**
- `FinishedGoodsWorkflowEngineService` → `AccountingService` (Accounting)
- `InventoryAdjustmentService` → `AccountingFacade` (Accounting)
- `OpeningStockImportService` → `AccountingService` (Accounting)
- `InventoryAccountingService` → `FinishedGoodBatchRepository`, `RawMaterialBatchRepository` (Inventory)

---

### Factory Module

```
FactoryController
├── FactoryService
│   ├── ProductionPlanRepository
│   └── FactoryTaskRepository
├── ProductionLogService
│   ├── ProductionLogRepository
│   └── ProductionLogMaterialRepository
├── PackingService
│   ├── PackingRecordRepository
│   ├── PackingIdempotencyService
│   └── PackingJournalBuilder
│       └── AccountingFacade (Accounting)
├── BulkPackingService
│   ├── BulkPackingOrchestrator
│   │   ├── FinishedGoodBatchRegistrar
│   │   └── PackingJournalBuilder
│   ├── BulkPackingCostService
│   └── BulkPackingInventoryService
└── CostAllocationService
    └── AccountingFacade (Accounting)

PackingController
├── PackingService
├── PackingBatchService
│   └── FinishedGoodBatchRepository
├── PackingReadService
│   └── PackingRecordRepository
└── BulkPackingReadService
    └── PackingRecordRepository

ProductionLogController
└── ProductionLogService
    ├── ProductionLogRepository
    └── AccountingFacade (Accounting)
```

**Key Cross-Module Dependencies:**
- `PackingService` → `AccountingFacade` (Accounting)
- `CostAllocationService` → `AccountingFacade` (Accounting)
- `BulkPackingOrchestrator` → `FinishedGoodRepository` (Inventory)
- `FactorySlipEventListener` → `ProductionLogService` (event-driven)

---

### Purchasing Module

```
PurchasingWorkflowController
├── PurchasingService
│   ├── PurchaseOrderService
│   │   ├── PurchaseOrderRepository
│   │   └── SupplierApprovalPolicy
│   ├── GoodsReceiptService
│   │   ├── GoodsReceiptRepository
│   │   └── AccountingService (Accounting)
│   └── PurchaseReturnService
│       ├── PurchaseReturnAllocationService
│       └── AccountingService (Accounting)
├── SupplierService
│   └── SupplierRepository
└── PurchaseInvoiceService
    ├── RawMaterialPurchaseRepository
    └── AccountingService (Accounting)

SupplierController
├── SupplierService
│   ├── SupplierRepository
│   └── SupplierApprovalPolicy
└── SupplierLedgerService (Accounting)

RawMaterialPurchaseController
├── PurchasingService
├── PurchaseReturnService
│   └── PurchaseReturnAllocationService
└── PurchaseInvoiceEngine
    └── AccountingService (Accounting)
```

**Key Cross-Module Dependencies:**
- `GoodsReceiptService` → `AccountingService` (Accounting)
- `PurchaseInvoiceService` → `AccountingService` (Accounting)
- `SupplierService` → `SupplierLedgerService` (Accounting)
- `PurchaseReturnService` → `AccountingService` (Accounting)

---

### HR Module

```
HrController
├── HrService
│   ├── EmployeeService
│   │   └── EmployeeRepository
│   ├── LeaveService
│   │   ├── LeaveRequestRepository
│   │   └── LeaveBalanceRepository
│   └── AttendanceService
│       └── AttendanceRepository
├── SalaryStructureTemplateService
│   └── SalaryStructureTemplateRepository
└── PayrollService
    ├── PayrollCalculationService
    │   ├── PayrollCalculationSupport
    │   └── StatutoryDeductionEngine
    ├── PayrollRunService
    │   └── PayrollRunRepository
    └── PayrollPostingService
        ├── AccountingService (Accounting)
        └── PayrollRunRepository

HrPayrollController
├── PayrollService
├── PayrollRunService
│   └── PayrollRunRepository
└── PayrollPostingService
    └── AccountingService (Accounting)
```

**Key Cross-Module Dependencies:**
- `PayrollPostingService` → `AccountingService` (Accounting)
- `HrService` → `CompanyContextService` (Company)

---

### Invoice Module

```
InvoiceController
├── InvoiceService
│   ├── InvoiceRepository
│   ├── InvoiceNumberService
│   │   └── InvoiceSequenceRepository
│   ├── InvoicePdfService
│   └── InvoiceSettlementPolicy
└── DealerService (Sales)
    └── DealerRepository
```

**Key Cross-Module Dependencies:**
- `InvoiceService` → `SalesOrderRepository` (Sales)
- `InvoiceService` → `DealerRepository` (Sales)
- `InvoiceSettlementPolicy` → `AccountingService` (Accounting)

---

### Production/Catalog Module

```
CatalogController
├── CatalogService
│   ├── ProductionProductRepository
│   └── ProductionBrandRepository
├── ProductionCatalogService
│   ├── ProductionProductRepository
│   ├── CatalogImportRepository
│   └── SkuReadinessService
└── SkuReadinessService
    ├── ProductionProductRepository
    └── FinishedGoodRepository (Inventory)
```

**Key Cross-Module Dependencies:**
- `SkuReadinessService` → `FinishedGoodRepository` (Inventory)
- `CatalogService` → `CompanyContextService` (Company)

---

### Company Module

```
CompanyController
├── CompanyService
│   ├── CompanyRepository
│   └── CompanyContextService
├── ModuleGatingService
│   └── ModuleGatingInterceptor
├── TenantRuntimeEnforcementService
│   └── CompanyRepository
└── CoATemplateService
    └── CoATemplateRepository

SuperAdminController
├── SuperAdminTenantControlPlaneService
│   ├── CompanyRepository
│   └── TenantAdminProvisioningService (Auth)
├── TenantOnboardingService
│   ├── CompanyRepository
│   └── CoATemplateRepository
└── TenantLifecycleService
    └── CompanyRepository

MultiCompanyController
└── CompanyContextService
```

**Key Cross-Module Dependencies:**
- `TenantOnboardingService` → `TenantAdminProvisioningService` (Auth)
- `ModuleGatingInterceptor` → All modules (gating)

---

### Auth Module

```
AuthController
├── AuthService
│   ├── UserAccountDetailsService
│   │   └── UserAccountRepository
│   ├── MfaService
│   │   └── MfaRecoveryCodeRepository
│   ├── PasswordService
│   │   └── UserPasswordHistoryRepository
│   └── RefreshTokenService
│       └── RefreshTokenRepository
├── PasswordResetService
│   ├── PasswordResetTokenRepository
│   └── EmailService (Core)
└── TokenBlacklistService
    └── BlacklistedTokenRepository

MfaController
├── MfaService
│   └── MfaRecoveryCodeRepository
└── AuthService

UserProfileController
├── UserProfileService
│   └── UserAccountRepository
└── AuthService
```

**Key Cross-Module Dependencies:**
- `AuthService` → `RoleRepository` (RBAC)
- `TenantAdminProvisioningService` → `CompanyRepository` (Company)
- `PasswordResetService` → `EmailService` (Core)

---

### Admin Module

```
AdminSettingsController
├── SystemSettingsService
│   └── SystemSetting
├── ExportApprovalService
│   └── ExportRequestRepository
└── SupportTicketService
    └── SupportTicketRepository

AdminUserController
├── AdminUserService
│   └── UserAccountRepository (Auth)
└── AuthService (Auth)

SupportTicketController
├── SupportTicketService
│   └── SupportTicketRepository
└── SupportTicketGitHubSyncService
    └── GitHubIssueClient

ChangelogController
└── ChangelogService
    └── ChangelogEntryRepository
```

**Key Cross-Module Dependencies:**
- `AdminUserService` → `UserAccountRepository` (Auth)
- `TenantRuntimePolicyService` → `TenantRuntimeEnforcementService` (Company)

---

### Portal Module

```
PortalInsightsController
├── PortalInsightsService
│   ├── SalesDashboardService (Sales)
│   ├── EnterpriseDashboardService
│   │   ├── AccountingService (Accounting)
│   │   └── HrService (HR)
│   └── OperationsInsights
│       └── FactoryService (Factory)
└── TenantRuntimeEnforcementInterceptor
    └── TenantRuntimeEnforcementService (Company)

DealerPortalController
├── DealerPortalService
│   ├── DealerRepository (Sales)
│   ├── InvoiceRepository (Invoice)
│   └── DealerLedgerService (Accounting)
└── InvoicePdfService (Invoice)
```

**Key Cross-Module Dependencies:**
- `PortalInsightsService` → All major modules for dashboard aggregation
- `DealerPortalService` → `DealerRepository`, `InvoiceRepository`, `DealerLedgerService`

---

### Reports Module

```
ReportController
├── ReportService
│   ├── TrialBalanceReportQueryService
│   │   └── JournalEntryRepository (Accounting)
│   ├── BalanceSheetReportQueryService
│   │   └── AccountRepository (Accounting)
│   ├── ProfitLossReportQueryService
│   │   └── JournalEntryRepository (Accounting)
│   ├── AgedDebtorsReportQueryService
│   │   └── DealerLedgerRepository (Accounting)
│   └── InventoryValuationService
│       └── FinishedGoodBatchRepository (Inventory)
└── ExportApprovalService (Admin)
```

**Key Cross-Module Dependencies:**
- All report services → `AccountingService`/Accounting repositories
- `InventoryValuationService` → `FinishedGoodBatchRepository` (Inventory)

---

### RBAC Module

```
RoleController
├── RoleService
│   ├── RoleRepository
│   └── PermissionRepository
└── AuthService (Auth)
    └── UserAccountRepository
```

**Key Cross-Module Dependencies:**
- `RoleService` → `UserAccountRepository` (Auth) for role assignments

---

### Orchestrator Module

```
OrchestratorController
├── CommandDispatcher
│   ├── SalesService (Sales)
│   ├── AccountingService (Accounting)
│   └── PayrollService (HR)
├── EventPublisherService
│   ├── OutboxEventRepository
│   └── RabbitTemplate
├── IntegrationCoordinator
│   └── ExternalSyncService
├── TraceService
│   └── AuditRepository
├── DashboardAggregationService
│   ├── SalesDashboardService (Sales)
│   ├── EnterpriseDashboardService (Portal)
│   └── FactoryService (Factory)
└── OrchestratorIdempotencyService
    └── OrchestratorCommandRepository

DashboardController
└── DashboardAggregationService

SchedulerService
├── OutboxPublisherJob
│   └── EventPublisherService
└── AuditDigestScheduler
    └── AccountingAuditService (Accounting)
```

**Key Cross-Module Dependencies:**
- `CommandDispatcher` → All major domain services
- `EventPublisherService` → `CompanyContextService` (Company)
- `DashboardAggregationService` → All dashboard services

---

## Core Infrastructure Services

### Core Module

```
AuditService
├── AuditLogRepository
└── ApplicationEventPublisher

EmailService
├── SystemSettingsService
└── JavaMailSender

NumberSequenceService
└── NumberSequenceRepository

SecurityMonitoringService
├── TokenBlacklistService
└── MeterRegistry

TenantRuntimeEnforcementService
├── CompanyRepository (Company)
└── TenantRuntimePolicyService (Admin)
```

---

## Cross-Cutting Concerns

### Filters
- `JwtAuthenticationFilter` → `UserAccountDetailsService`, `TokenBlacklistService`
- `CompanyContextFilter` → `CompanyContextService`
- `MustChangePasswordCorridorFilter` → `UserAccountDetailsService`

### Interceptors
- `ModuleGatingInterceptor` → `ModuleGatingService`
- `TenantUsageMetricsInterceptor` → `TenantUsageMetricsService`
- `TenantRuntimeEnforcementInterceptor` → `TenantRuntimeEnforcementService`

### Exception Handlers
- `GlobalExceptionHandler` → All services (catches exceptions)
- `CoreFallbackExceptionHandler` → All services (last resort)

### Event Listeners
- `FactorySlipEventListener` → `ProductionLogService`
- `InventoryAccountingEventListener` → `InventoryAccountingService`
- `AccountingFacadeCore` (EventListener) → `AccountingService`

### Scheduled Jobs
- `DunningService` → Daily dunning check (cron: 0 15 3 * * *)
- `RefreshTokenService` → Token cleanup (hourly)
- `SupportTicketGitHubSyncService` → GitHub sync (every 5 minutes)
- `AuditDigestScheduler` → Audit digest (daily at 2:30 AM)
- `EnterpriseAuditTrailService` → Audit trail retry (30 seconds)
- `TokenBlacklistService` → Token cleanup (hourly)
- `SecurityMonitoringService` → Security monitoring (1 min / 1 hour)

---

## Summary: Major Service Clusters

1. **Accounting Core** - Central to all financial operations
   - `AccountingService`, `AccountingFacade`, `JournalEntryService`
   - Called by: Sales, Purchasing, HR, Inventory, Factory, Reports

2. **Sales Core** - Order management and dealer relationships
   - `SalesService`, `SalesCoreEngine`, `DealerService`
   - Calls: Accounting, Inventory

3. **Inventory Core** - Stock and warehouse management
   - `FinishedGoodsService`, `RawMaterialService`, `InventoryAdjustmentService`
   - Calls: Accounting

4. **Factory Core** - Production and packing operations
   - `FactoryService`, `PackingService`, `BulkPackingService`, `ProductionLogService`
   - Calls: Accounting, Inventory

5. **Auth Core** - Authentication and authorization
   - `AuthService`, `MfaService`, `PasswordService`
   - Called by: All modules (via filters)

6. **Orchestrator Core** - Cross-module coordination
   - `CommandDispatcher`, `EventPublisherService`, `DashboardAggregationService`
   - Calls: All major domain services
