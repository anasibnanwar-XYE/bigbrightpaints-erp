# Canonicality Map - BigBright ERP

This document assigns canonicality status to every major service and component in the BigBright ERP codebase.

## Status Definitions

| Status | Description | Action |
|--------|-------------|--------|
| **Canonical** | Primary implementation, use this | Active development |
| **Scoped** | Valid only in specific workflow/area | Use within scope only |
| **Legacy** | Still used, avoid new usage | Phase out gradually |
| **Duplicate-risk** | Overlaps with other code | Consolidate or deprecate |
| **Deprecated** | Phase out | Remove when safe |
| **Dangerous** | Risky patterns or side effects | Review before use |

---

## Core Infrastructure

### Utilities (`com.bigbrightpaints.erp.core.util`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `MoneyUtils` | Canonical | Null-safe monetary operations, used across all financial services |
| `CompanyClock` | Canonical | Injectable Clock for testing and company timezone awareness |
| `CompanyTime` | Canonical | Static access to CompanyClock for domain contexts |
| `IdempotencyHeaderUtils` | Canonical | Header resolution for idempotent operations |
| `CostingMethodUtils` | Canonical | FIFO/LIFO/WAC normalization |
| `PasswordUtils` | Canonical | Secure temporary password generation |
| `BusinessDocumentTruths` | Canonical | Document lifecycle state derivation |
| `CompanyEntityLookup` | Canonical | Centralized company-scoped entity lookups |
| `DashboardWindow` | Canonical | Dashboard date range calculation |
| `LegacyDispatchInvoiceLinkMatcher` | Legacy | Migration-only, not for new code |

### Security (`com.bigbrightpaints.erp.core.security`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `SecurityConfig` | Canonical | Main Spring Security configuration |
| `CompanyContextHolder` | Canonical | Thread-local company context |
| `CompanyContextFilter` | Canonical | Tenant lifecycle enforcement |
| `JwtAuthenticationFilter` | Canonical | JWT validation and authentication |
| `JwtTokenService` | Canonical | Token generation and parsing |
| `JwtProperties` | Canonical | JWT configuration binding |
| `TokenBlacklistService` | Canonical | Token revocation management |
| `MustChangePasswordCorridorFilter` | Canonical | Password change enforcement |
| `CryptoService` | Canonical | AES-256-GCM encryption |
| `TenantRuntimeEnforcementService` | Canonical | Tenant throttling and admission |
| `SecurityMonitoringService` | Canonical | Brute force detection |
| `PortalRoleActionMatrix` | Canonical | Role/action SpEL expressions |
| `SecurityActorResolver` | Canonical | Audit actor resolution |
| `LicensingGuard` | Scoped | License validation on startup only |

### Exception Handling (`com.bigbrightpaints.erp.core.exception`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `ErrorCode` | Canonical | Centralized error code enum |
| `ApplicationException` | Canonical | Base application exception |
| `AuthSecurityContractException` | Canonical | Auth-specific exception with HTTP status |
| `CreditLimitExceededException` | Canonical | Credit limit violation exception |
| `GlobalExceptionHandler` | Canonical | Primary exception handler |
| `CoreFallbackExceptionHandler` | Canonical | Fallback for unhandled exceptions |
| `SettlementExceptionHandler` | Canonical | Settlement failure routing |
| `AuditExceptionRoutingService` | Canonical | Exception to audit routing |

### Audit (`com.bigbrightpaints.erp.core.audit`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `AuditService` | Canonical | Core audit logging |
| `AuditLog` | Canonical | Audit log entity |
| `AuditEvent` | Canonical | Audit event types enum |
| `AuditStatus` | Canonical | Audit status enum |
| `IntegrationFailureAlertRoutingPolicy` | Canonical | Failure routing |
| `IntegrationFailureMetadataSchema` | Canonical | Failure metadata schema |

### Audit Trail (`com.bigbrightpaints.erp.core.audittrail`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `EnterpriseAuditTrailService` | Canonical | Enterprise audit with ML tracking |
| `AuditActionEvent` | Canonical | Business event entity |
| `MlInteractionEvent` | Canonical | ML interaction tracking |
| `AuditActionEventRetry` | Canonical | Retry queue for failed events |

### Idempotency (`com.bigbrightpaints.erp.core.idempotency`)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `IdempotencyReservationService` | Canonical | Cross-module idempotency |
| `IdempotencyUtils` | Canonical | Idempotency utilities |

---

## Orchestrator Layer

| Component | Status | Rationale |
|-----------|--------|-----------|
| `CommandDispatcher` | Canonical | Command dispatch orchestration |
| `EventPublisherService` | Canonical | Event publishing |
| `OrchestratorIdempotencyService` | Canonical | Orchestrator-level idempotency |
| `DashboardAggregationService` | Canonical | Dashboard metrics aggregation |
| `TraceService` | Canonical | Distributed tracing |
| `WorkflowService` | Canonical | Workflow management |
| `SchedulerService` | Canonical | Scheduled jobs |
| `ExternalSyncService` | Canonical | External system sync |
| `IntegrationCoordinator` | Canonical | Integration orchestration |
| `PolicyEnforcer` | Canonical | Policy enforcement |

---

## Sales Module (O2C)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `SalesCoreEngine` | Canonical | Central O2C orchestrator |
| `SalesService` | Canonical | Main facade for sales |
| `SalesOrderCrudService` | Canonical | Order CRUD |
| `SalesOrderLifecycleService` | Canonical | Order status transitions |
| `SalesDealerCrudService` | Canonical | Dealer CRUD |
| `DealerService` | Canonical | Complete dealer management |
| `DealerPortalService` | Canonical | Dealer-portal data access |
| `CreditLimitRequestService` | Canonical | Permanent credit increase |
| `CreditLimitOverrideService` | Canonical | One-time credit override |
| `SalesFulfillmentService` | Canonical | Fulfillment orchestration |
| `SalesReturnService` | Canonical | Return processing |
| `DunningService` | Canonical | Overdue management |
| `SalesDashboardService` | Canonical | Dashboard metrics |
| `SalesDispatchReconciliationService` | Canonical | Dispatch confirmation |
| `OrderNumberService` | Canonical | Order number generation |
| `SalesIdempotencyService` | Canonical | Sales idempotency |
| `SalesProformaBoundaryService` | Canonical | Proforma processing |
| `SalesOrderCreditExposurePolicy` | Canonical | Credit exposure calculation |
| `DispatchMetadataValidator` | Canonical | Dispatch validation |
| `SalesAccountConfigurationValidator` | Canonical | Account config validation |

---

## Accounting Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `AccountingService` | Canonical | Main accounting facade |
| `AccountingFacade` | Canonical | Manual journal operations |
| `JournalEntryService` | Canonical | Journal entry CRUD |
| `SettlementService` | Canonical | Partner settlements |
| `DealerReceiptService` | Canonical | Dealer receipts |
| `DealerLedgerService` | Canonical | AR sub-ledger |
| `SupplierLedgerService` | Canonical | AP sub-ledger |
| `AccountingPeriodService` | Canonical | Period management |
| `ReconciliationService` | Canonical | Bank reconciliation |
| `BankReconciliationSessionService` | Canonical | Interactive reconciliation |
| `StatementService` | Canonical | Partner statements |
| `AgingReportService` | Canonical | Aging reports |
| `TaxService` | Canonical | GST return generation |
| `GstService` | Canonical | GST calculations |
| `TemporalBalanceService` | Canonical | Point-in-time balances |
| `ReferenceNumberService` | Canonical | Journal references |
| `AccountHierarchyService` | Canonical | Chart of accounts tree |
| `CompanyDefaultAccountsService` | Canonical | Default accounts |
| `AccountingIdempotencyService` | Canonical | Accounting idempotency |
| `CreditDebitNoteService` | Canonical | Credit/debit notes |
| `InventoryAccountingService` | Canonical | Inventory accounting |
| `TallyImportService` | Canonical | Tally migration |
| `OpeningBalanceImportService` | Canonical | Opening balance import |
| `AccountingAuditTrailService` | Canonical | Transaction audit |
| `AccountingAuditService` | Legacy | Deprecated - use AccountingAuditTrailService |
| `AccountingComplianceAuditService` | Canonical | Compliance audits |
| `AccountingEventStore` | Canonical | Event-sourced audit |
| `CostingMethodService` | Canonical | Costing method operations |
| `ClosedPeriodPostingExceptionService` | Canonical | Period exceptions |

### Internal (Core Abstracts)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `AccountingCoreService` | Scoped | Abstract base for accounting |
| `AccountingCoreEngine` | Scoped | Core accounting engine |
| `AccountingFacadeCore` | Scoped | Abstract facade |
| `AccountingPeriodServiceCore` | Scoped | Abstract period service |
| `ReconciliationServiceCore` | Scoped | Abstract reconciliation |
| `AccountingAuditTrailServiceCore` | Scoped | Abstract audit trail |

---

## Inventory Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `RawMaterialService` | Canonical | Raw material management |
| `FinishedGoodsService` | Canonical | Finished goods management |
| `InventoryBatchQueryService` | Canonical | Batch queries |
| `BatchNumberService` | Canonical | Batch number generation |
| `InventoryBatchTraceabilityService` | Canonical | Batch traceability |
| `InventoryAdjustmentService` | Canonical | Inventory adjustments |
| `OpeningStockImportService` | Canonical | Opening stock import |
| `PackagingSlipService` | Canonical | Packaging slip management |
| `DeliveryChallanPdfService` | Canonical | Challan PDF generation |
| `FinishedGoodsWorkflowEngineService` | Canonical | FG workflow |
| `InventoryValuationService` | Duplicate-risk | Overlaps with reports.InventoryValuationService |

**⚠️ Duplicate Risk:** Two `InventoryValuationService` classes exist:
- `modules.inventory.service.InventoryValuationService`
- `modules.reports.service.InventoryValuationService`

---

## Purchasing Module (P2P)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `PurchasingService` | Canonical | Main purchasing facade |
| `PurchaseOrderService` | Canonical | PO management |
| `GoodsReceiptService` | Canonical | Goods receipt processing |
| `PurchaseInvoiceService` | Canonical | Purchase invoice |
| `SupplierService` | Canonical | Supplier management |
| `PurchaseReturnService` | Canonical | Purchase returns |
| `PurchaseReturnAllocationService` | Canonical | Return allocations |
| `PurchaseResponseMapper` | Canonical | Purchase response mapping |

---

## Factory Module (Manufacturing)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `FactoryService` | Canonical | Main factory facade |
| `PackingService` | Canonical | Packing operations |
| `PackingBatchService` | Canonical | Packing batches |
| `BulkPackingService` | Canonical | Bulk packing |
| `PackingInventoryService` | Canonical | Packing inventory |
| `BulkPackingInventoryService` | Canonical | Bulk packing inventory |
| `PackingReadService` | Canonical | Packing reads |
| `BulkPackingReadService` | Canonical | Bulk packing reads |
| `PackagingMaterialService` | Canonical | Packaging materials |
| `PackingIdempotencyService` | Canonical | Packing idempotency |
| `BulkPackingCostService` | Canonical | Bulk packing costing |
| `PackingAllowedSizeService` | Canonical | Size configuration |
| `ProductionLogService` | Canonical | Production logs |
| `CostAllocationService` | Canonical | Cost allocation |
| `PackingJournalLinkHelper` | Canonical | Journal linking |

---

## Production Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `ProductionCatalogService` | Canonical | Product catalog |
| `CatalogService` | Duplicate-risk | Overlaps with ProductionCatalogService |
| `SkuReadinessService` | Canonical | SKU readiness checks |

**⚠️ Duplicate Risk:** `CatalogService` and `ProductionCatalogService` appear to have overlapping responsibilities.

---

## HR Module (Payroll)

| Component | Status | Rationale |
|-----------|--------|-----------|
| `HrService` | Canonical | Main HR facade |
| `EmployeeService` | Canonical | Employee management |
| `AttendanceService` | Canonical | Attendance tracking |
| `LeaveService` | Canonical | Leave management |
| `PayrollService` | Canonical | Main payroll facade |
| `PayrollCalculationService` | Canonical | Payroll calculation |
| `PayrollRunService` | Canonical | Payroll run management |
| `PayrollPostingService` | Canonical | Payroll posting |
| `SalaryStructureTemplateService` | Canonical | Salary templates |

---

## Auth Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `AuthService` | Canonical | Main auth service |
| `PasswordService` | Canonical | Password management |
| `PasswordResetService` | Canonical | Password reset |
| `MfaService` | Canonical | MFA management |
| `RefreshTokenService` | Canonical | Refresh tokens |
| `UserProfileService` | Canonical | User profiles |
| `UserAccountDetailsService` | Canonical | User details |
| `TenantAdminProvisioningService` | Canonical | Tenant admin setup |

---

## Admin Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `AdminUserService` | Canonical | Admin user management |
| `SupportTicketService` | Canonical | Ticket management |
| `ChangelogService` | Canonical | Changelog |
| `ExportApprovalService` | Canonical | Export approvals |
| `TenantRuntimePolicyService` | Canonical | Tenant policies |
| `SupportTicketGitHubSyncService` | Canonical | GitHub sync |

---

## Company Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `CompanyService` | Canonical | Company CRUD |
| `CompanyContextService` | Canonical | Company context |
| `TenantLifecycleService` | Canonical | Tenant lifecycle |
| `TenantOnboardingService` | Canonical | Tenant onboarding |
| `TenantUsageMetricsService` | Canonical | Usage metrics |
| `TenantRuntimeEnforcementService` | Canonical | Runtime enforcement |
| `SuperAdminTenantControlPlaneService` | Canonical | Super-admin operations |
| `ModuleGatingService` | Canonical | Module gating |
| `CoATemplateService` | Canonical | Chart of accounts templates |

**⚠️ Duplicate Risk:** Two `TenantRuntimeEnforcementService` classes exist:
- `core.security.TenantRuntimeEnforcementService`
- `modules.company.service.TenantRuntimeEnforcementService`

---

## Reports Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `ReportService` | Canonical | Main reporting |
| `TrialBalanceReportQueryService` | Canonical | Trial balance |
| `BalanceSheetReportQueryService` | Canonical | Balance sheet |
| `ProfitLossReportQueryService` | Canonical | P&L |
| `AgedDebtorsReportQueryService` | Canonical | Aged debtors |
| `InventoryValuationService` | Scoped | Inventory valuation reports |

---

## RBAC Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `RoleService` | Canonical | Role management |

---

## Portal Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `EnterpriseDashboardService` | Canonical | Enterprise dashboard |
| `PortalInsightsService` | Canonical | Portal insights |

---

## Invoice Module

| Component | Status | Rationale |
|-----------|--------|-----------|
| `InvoiceService` | Canonical | Invoice management |
| `InvoiceNumberService` | Canonical | Invoice numbering |
| `InvoicePdfService` | Canonical | PDF generation |

---

## Core Services

| Component | Status | Rationale |
|-----------|--------|-----------|
| `NumberSequenceService` | Canonical | Number sequences |
| `CriticalFixtureService` | Canonical | Test fixtures |
| `ConfigurationHealthService` | Canonical | Health checks |
| `EmailService` | Canonical | Email sending |

---

## Suspicious Patterns Detected

### 1. Duplicate Class Names

| Class | Locations | Risk |
|-------|-----------|------|
| `InventoryValuationService` | inventory, reports | High |
| `TenantRuntimeEnforcementService` | core.security, company | High |
| `CatalogService` vs `ProductionCatalogService` | production | Medium |

### 2. Potential Overlap

| Area | Services | Issue |
|------|----------|-------|
| Inventory Costing | `InventoryValuationService` (2), `CostingMethodService`, `CostingMethodUtils` | Multiple costing entry points |
| Ledger Management | `DealerLedgerService`, `SupplierLedgerService`, `AbstractPartnerLedgerService` | Inconsistent naming |
| Idempotency | Multiple module-specific idempotency services | Should consolidate pattern |

### 3. Legacy Code Markers

| Component | Location | Notes |
|-----------|----------|-------|
| `AccountingAuditService` | accounting | Use AccountingAuditTrailService |
| `LegacyDispatchInvoiceLinkMatcher` | core.util | Migration only |
