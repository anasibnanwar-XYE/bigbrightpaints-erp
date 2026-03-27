# Master Index: All Classes

This index lists all documented classes in the BigBright ERP codebase (~858 classes total).

## Legend
| Status | Meaning |
|--------|---------|
| ✅ | Canonical - Primary implementation |
| ⚠ | Scoped - Valid in specific contexts only |
| 🔄 | Legacy - Avoid new usage |
| ❌ | Deprecated - Phase out |

---

## Core Infrastructure

### Utilities (`com.bigbrightpaints.erp.core.util`)

| Name | Type | Package | Purpose | Status |
|------|------|---------|---------|--------|
| MoneyUtils | Utility | core.util | Null-safe monetary calculations with 2-decimal precision | ✅ |
| CompanyClock | Service | core.util | Injectable Clock for company timezone-aware operations | ✅ |
| IdempotencyHeaderUtils | Utility | core.util | Resolves idempotency keys from HTTP headers | ✅ |
| CostingMethodUtils | Utility | core.util | Normalizes and validates costing methods (FIFO/LIFO/WAC) | ✅ |
| PasswordUtils | Utility | core.util | Generates secure random temporary passwords | ✅ |
| BusinessDocumentTruths | Utility | core.util | Derives document lifecycle states | ✅ |
| CompanyEntityLookup | Service | core.util | Centralized company-scoped entity lookups | ✅ |
| CompanyTime | Service | core.util | Static access to company-aware time | ✅ |
| DashboardWindow | Record | core.util | Date ranges and bucketing for dashboard queries | ✅ |
| LegacyDispatchInvoiceLinkMatcher | Utility | core.util | Matches legacy dispatch-invoice links | 🔄 |

### Configuration (`com.bigbrightpaints.erp.core.config`)

| Name | Type | Package | Purpose | Status |
|------|------|---------|---------|--------|
| AsyncConfig | Config | core.config | Async task executor with context propagation | ✅ |
| CompanyContextTaskDecorator | Utility | core.config | Propagates context to async threads | ✅ |
| DataInitializer | Config | core.config | Seeds default users/companies for dev/test | ⚠ |
| BbpSampleDataInitializer | Config | core.config | Seeds BBP demo data | ⚠ |
| BenchmarkDataInitializer | Config | core.config | Creates benchmark testing data | ⚠ |
| MockDataInitializer | Config | core.config | Seeds training environment | ⚠ |
| CriticalFixtureInitializer | Config | core.config | Seeds critical fixtures | ⚠ |
| ValidationSeedDataInitializer | Config | core.config | Seeds validation actors | ⚠ |
| EmailProperties | Config Props | core.config | Email configuration binding | ✅ |
| GitHubProperties | Config Props | core.config | GitHub API configuration | ✅ |
| LicensingProperties | Config Props | core.config | CryptoLens licensing config | ✅ |
| JacksonConfig | Config | core.config | Jackson ObjectMapper config | ✅ |
| OpenApiConfig | Config | core.config | OpenAPI/Swagger documentation | ✅ |
| OpenApiTaggingConfig | Config | core.config | Auto-tags API endpoints by module | ✅ |
| SmtpPropertiesValidator | Config | core.config | Validates SMTP config on startup | ✅ |
| SystemSetting | Entity | core.config | Key-value store for runtime settings | ✅ |
| SystemSettingsRepository | Repository | core.config | JPA repository for SystemSetting | ✅ |
| SystemSettingsService | Service | core.config | Manages runtime-tunable settings | ✅ |

### Security (`com.bigbrightpaints.erp.core.security`)

| Name | Type | Package | Purpose | Status |
|------|------|---------|---------|--------|
| SecurityConfig | Config | core.security | Main Spring Security configuration | ✅ |
| CompanyContextHolder | Utility | core.security | Thread-local storage for company code | ✅ |
| CompanyContextFilter | Filter | core.security | Validates company access and tenant lifecycle | ✅ |
| JwtAuthenticationFilter | Filter | core.security | Validates JWT tokens and sets authentication | ✅ |
| JwtTokenService | Service | core.security | Generates and parses JWT tokens | ✅ |
| JwtProperties | Config Props | core.security | JWT configuration binding | ✅ |
| TokenBlacklistService | Service | core.security | Manages JWT token blacklist | ✅ |
| MustChangePasswordCorridorFilter | Filter | core.security | Enforces password change requirement | ✅ |
| CryptoService | Service | core.security | AES-256-GCM encryption for sensitive data | ✅ |
| TenantRuntimeEnforcementService | Service | core.security | Tenant concurrency/rate limits | ✅ |
| SecurityMonitoringService | Service | core.security | Monitors security events, brute force detection | ✅ |
| PortalRoleActionMatrix | Utility | core.security | Role/action SpEL expressions | ✅ |
| SecurityActorResolver | Utility | core.security | Resolves authenticated actor for audit | ✅ |
| LicensingGuard | Component | core.security | Validates CryptoLens license on startup | ⚠ |
| TenantRuntimeRequestAttributes | Utility | core.security | Request attribute keys for runtime enforcement | ✅ |

---

## Accounting Module

### Services (`com.bigbrightpaints.erp.modules.accounting.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| AccountingService | Service | Main orchestrating service for accounting operations | ✅ |
| AccountingFacade | Service | Entry point for manual journal operations | ✅ |
| JournalEntryService | Service | Journal entry CRUD, listing, reversal | ✅ |
| SettlementService | Service | Partner (dealer/supplier) settlements | ✅ |
| DealerReceiptService | Service | Dealer receipt recording | ✅ |
| DealerLedgerService | Service | AR sub-ledger management | ✅ |
| SupplierLedgerService | Service | AP sub-ledger management | ✅ |
| AccountingPeriodService | Service | Accounting period lifecycle management | ✅ |
| ReconciliationService | Service | Bank and sub-ledger reconciliation | ✅ |
| BankReconciliationSessionService | Service | Interactive bank reconciliation sessions | ✅ |
| StatementService | Service | Partner statements and aging reports | ✅ |
| AgingReportService | Service | Aged receivables reports and DSO | ✅ |
| TaxService | Service | GST return generation and reconciliation | ✅ |
| GstService | Service | GST calculations (CGST, SGST, IGST) | ✅ |
| TemporalBalanceService | Service | Point-in-time balance queries | ✅ |
| ReferenceNumberService | Service | Generate unique journal reference numbers | ✅ |
| AccountHierarchyService | Service | Chart of accounts hierarchy operations | ✅ |
| CompanyDefaultAccountsService | Service | Manage company default accounts | ✅ |
| AccountingIdempotencyService | Service | Idempotent wrapper for accounting operations | ✅ |
| CreditDebitNoteService | Service | Credit notes, debit notes, accruals | ✅ |
| InventoryAccountingService | Service | Inventory-related accounting | ✅ |
| TallyImportService | Service | Import chart of accounts from Tally XML | ✅ |
| OpeningBalanceImportService | Service | Import opening balances from CSV | ✅ |
| AccountingAuditTrailService | Service | Transaction audit queries | ✅ |
| AuditTrailQueryService | Service | Generic audit trail query service | ✅ |
| AccountingAuditService | Service | Audit digest generation | 🔄 |
| AccountingComplianceAuditService | Service | Compliance audit checks | ✅ |
| CostingMethodService | Service | Inventory costing method operations | ✅ |
| NoopPeriodCloseHook | Service | Default no-op period close hook | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.accounting.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| AccountingController | Controller | Main accounting operations (60+ endpoints) | ✅ |
| AccountingConfigurationController | Controller | Configuration health check | ✅ |
| AccountingCatalogController | Controller | Placeholder (empty) | ⚠ |
| AccountingAuditTrailController | Controller | Audit trail queries | ✅ |
| PayrollController | Controller | Payroll batch payments | ✅ |
| TallyImportController | Controller | Tally XML import | ✅ |
| OpeningBalanceImportController | Controller | Opening balance CSV import | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.accounting.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| Account | Entity | Chart of accounts | ✅ |
| JournalEntry | Entity | Journal entry header | ✅ |
| JournalLine | Entity | Journal entry line | ✅ |
| AccountingPeriod | Entity | Accounting period | ✅ |
| DealerLedgerEntry | Entity | Dealer AR sub-ledger | ✅ |
| SupplierLedgerEntry | Entity | Supplier AP sub-ledger | ✅ |
| PartnerSettlementAllocation | Entity | Settlement allocation | ✅ |
| BankReconciliationSession | Entity | Bank reconciliation session | ✅ |
| BankReconciliationItem | Entity | Bank reconciliation item | ✅ |
| ReconciliationDiscrepancy | Entity | Reconciliation discrepancy | ✅ |
| AccountingEvent | Entity | Event-sourced audit log | ✅ |
| AccountingPeriodSnapshot | Entity | Period snapshot for reporting | ✅ |
| ClosedPeriodPostingException | Entity | Closed period exception | ✅ |
| PeriodCloseRequest | Entity | Period close request | ✅ |

---

## Sales Module

### Services (`com.bigbrightpaints.erp.modules.sales.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| SalesCoreEngine | Service | Central orchestrator for all sales operations | ✅ |
| SalesService | Service | Facade aggregating all sales subservices | ✅ |
| SalesOrderCrudService | Service | Thin wrapper for order CRUD | ✅ |
| SalesDealerCrudService | Service | Thin wrapper for dealer CRUD | ✅ |
| SalesOrderLifecycleService | Service | Order status transitions | ✅ |
| DealerService | Service | Complete dealer management | ✅ |
| DealerPortalService | Service | Dealer-portal scoped data retrieval | ✅ |
| CreditLimitRequestService | Service | Credit limit increase requests | ✅ |
| CreditLimitOverrideService | Service | One-time credit limit overrides | ✅ |
| SalesFulfillmentService | Service | Complete fulfillment flow orchestration | ✅ |
| SalesReturnService | Service | Sales return processing | ✅ |
| DunningService | Service | Automated dunning | ✅ |
| SalesDashboardService | Service | Dashboard metrics aggregation | ✅ |
| SalesDispatchReconciliationService | Service | Dispatch confirmation | ✅ |
| OrderNumberService | Service | Sequential order numbers | ✅ |
| SalesIdempotencyService | Service | Idempotency wrapper for order creation | ✅ |
| SalesProformaBoundaryService | Service | Proforma order processing | ✅ |
| SalesOrderCreditExposurePolicy | Service | Credit exposure calculation | ✅ |
| DispatchMetadataValidator | Service | Validates dispatch confirmation metadata | ✅ |
| SalesAccountConfigurationValidator | Config | Validates accounting configuration | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.sales.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| SalesController | Controller | Sales orders, promotions, targets, dispatch | ✅ |
| DealerController | Controller | Dealer CRUD, ledger, invoices, credit | ✅ |
| DealerPortalController | Controller | Dealer-scoped portal endpoints | ✅ |
| CreditLimitRequestController | Controller | Credit limit increase requests | ✅ |
| CreditLimitOverrideController | Controller | Credit limit override requests | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.sales.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| Dealer | Entity | Customer/dealer master data | ✅ |
| SalesOrder | Entity | Sales order header | ✅ |
| SalesOrderItem | Entity | Sales order line items | ✅ |
| SalesOrderStatusHistory | Entity | Order status transitions | ✅ |
| CreditRequest | Entity | Credit limit increase requests | ✅ |
| CreditLimitOverrideRequest | Entity | One-time dispatch overrides | ✅ |
| Promotion | Entity | Promotional campaigns | ✅ |
| SalesTarget | Entity | Sales performance targets | ✅ |
| OrderSequence | Entity | Order number sequences | ✅ |

---

## Inventory Module

### Services (`com.bigbrightpaints.erp.modules.inventory.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| FinishedGoodsService | Service | Facade for finished goods operations | ✅ |
| RawMaterialService | Service | Raw material inventory management | ✅ |
| InventoryValuationService | Service | Inventory valuation with costing methods | ✅ |
| InventoryBatchQueryService | Service | Query service for batch data | ✅ |
| InventoryBatchTraceabilityService | Service | Batch traceability | ✅ |
| BatchNumberService | Service | Generate unique batch codes | ✅ |
| InventoryAdjustmentService | Service | Finished goods adjustments | ✅ |
| OpeningStockImportService | Service | CSV-based opening stock import | ✅ |
| PackagingSlipService | Service | Packaging slip management | ✅ |
| DeliveryChallanPdfService | Service | Generate delivery challan PDFs | ✅ |
| InventoryMovementRecorder | Service | Record inventory movements | ✅ |
| FinishedGoodsReservationEngine | Service | Inventory reservations for orders | ✅ |
| FinishedGoodsDispatchEngine | Service | Dispatch workflow for orders | ✅ |
| FinishedGoodsWorkflowEngineService | Service | Orchestrate finished goods workflows | ✅ |
| DispatchArtifactPaths | Utility | Delivery challan path generation | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.inventory.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| FinishedGoodController | Controller | Finished goods inventory, batches, stock | ✅ |
| RawMaterialController | Controller | Raw material inventory, adjustments | ✅ |
| InventoryBatchController | Controller | Batch traceability | ✅ |
| DispatchController | Controller | Read-only dispatch operations | ✅ |
| InventoryAdjustmentController | Controller | Finished goods adjustments | ✅ |
| OpeningStockImportController | Controller | Opening stock CSV imports | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.inventory.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| FinishedGood | Entity | Finished product in inventory | ✅ |
| RawMaterial | Entity | Raw material used in production | ✅ |
| FinishedGoodBatch | Entity | Batch tracking for finished goods | ✅ |
| RawMaterialBatch | Entity | Batch tracking for raw materials | ✅ |
| InventoryMovement | Entity | Finished goods movements | ✅ |
| RawMaterialMovement | Entity | Raw material movements | ✅ |
| PackagingSlip | Entity | Packaging slip for dispatch | ✅ |
| PackagingSlipLine | Entity | Packaging slip line item | ✅ |
| InventoryReservation | Entity | Inventory reservations | ✅ |
| InventoryAdjustment | Entity | Finished goods adjustment | ✅ |
| InventoryAdjustmentLine | Entity | Adjustment line item | ✅ |
| RawMaterialAdjustment | Entity | Raw material adjustment | ✅ |
| RawMaterialAdjustmentLine | Entity | Raw material adjustment line | ✅ |
| OpeningStockImport | Entity | Opening stock import record | ✅ |
| RawMaterialIntakeRecord | Entity | Manual raw material intake | ✅ |

---

## Factory Module

### Services (`com.bigbrightpaints.erp.modules.factory.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| FactoryService | Service | Production planning and task management | ✅ |
| ProductionLogService | Service | M2S production logging | ✅ |
| PackingService | Service | Standard packing operations | ✅ |
| BulkPackingService | Service | Bulk-to-size packing | ✅ |
| PackagingMaterialService | Service | Packaging mappings and consumption | ✅ |
| CostAllocationService | Service | Period cost allocation to batches | ✅ |
| PackingIdempotencyService | Service | Packing idempotency | ✅ |
| PackingAllowedSizeService | Service | Size target resolution | ✅ |
| PackingReadService | Service | Packing queries | ✅ |
| PackingInventoryService | Service | Semi-finished inventory | ✅ |
| PackingBatchService | Service | FG batch registration | ✅ |
| PackingLineResolver | Component | Line resolution | ✅ |
| PackingProductSupport | Component | Product utilities | ✅ |
| PackingJournalBuilder | Component | Journal line building | ✅ |
| PackingJournalLinkHelper | Service | Movement-journal linking | ✅ |
| BulkPackingOrchestrator | Service | Bulk packing orchestration | ✅ |
| BulkPackingCostService | Service | Bulk packaging costs | ✅ |
| BulkPackingInventoryService | Service | Bulk inventory | ✅ |
| BulkPackingReadService | Service | Bulk packing queries | ✅ |
| FinishedGoodBatchRegistrar | Service | FG batch registration | ✅ |
| PackagingSizeParser | Utility | Parse packaging size strings | ✅ |
| FactorySlipEventListener | Component | Event handling | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.factory.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| FactoryController | Controller | Plans, tasks, dashboard, cost allocation | ✅ |
| PackingController | Controller | Packing operations | ✅ |
| ProductionLogController | Controller | Production log entries | ✅ |
| PackagingMappingController | Controller | Packaging rule configuration | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.factory.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| ProductionLog | Entity | Manufacturing batch | ✅ |
| ProductionPlan | Entity | Planned production run | ✅ |
| PackingRecord | Entity | Packing operation record | ✅ |
| ProductionLogMaterial | Entity | Material consumption record | ✅ |
| FactoryTask | Entity | Factory task | ✅ |
| ProductionBatch | Entity | Production batch execution | ✅ |
| PackagingSizeMapping | Entity | Packaging size to material mapping | ✅ |
| SizeVariant | Entity | Size variant definition | ✅ |
| PackingRequestRecord | Entity | Idempotency for packing | ✅ |

---

## Purchasing Module

### Services (`com.bigbrightpaints.erp.modules.purchasing.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| PurchasingService | Service | Facade for all purchasing operations | ✅ |
| PurchaseOrderService | Service | Purchase order lifecycle | ✅ |
| GoodsReceiptService | Service | Goods receipt processing | ✅ |
| PurchaseInvoiceService | Service | Purchase invoice facade | ✅ |
| PurchaseInvoiceEngine | Service | Core invoice processing | ✅ |
| PurchaseReturnService | Service | Purchase returns | ✅ |
| PurchaseReturnAllocationService | Service | Return quantity allocation | ✅ |
| SupplierService | Service | Supplier master data | ✅ |
| PurchaseResponseMapper | Service | Entity to DTO mapping | ✅ |
| PurchaseTaxPolicy | Service | GST calculation rules | ✅ |
| SupplierApprovalPolicy | Service | Supplier approval validation | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.purchasing.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| PurchasingWorkflowController | Controller | PO and goods receipts | ✅ |
| RawMaterialPurchaseController | Controller | Purchase invoices and returns | ✅ |
| SupplierController | Controller | Supplier master data | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.purchasing.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| Supplier | Entity | Supplier master data | ✅ |
| PurchaseOrder | Entity | Purchase order header | ✅ |
| PurchaseOrderLine | Entity | Purchase order line | ✅ |
| PurchaseOrderStatusHistory | Entity | PO status audit | ✅ |
| GoodsReceipt | Entity | Goods receipt header | ✅ |
| GoodsReceiptLine | Entity | Goods receipt line | ✅ |
| RawMaterialPurchase | Entity | Purchase invoice header | ✅ |
| RawMaterialPurchaseLine | Entity | Purchase invoice line | ✅ |

---

## Auth Module

### Services (`com.bigbrightpaints.erp.modules.auth.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| AuthService | Service | Core authentication (login, logout, refresh) | ✅ |
| MfaService | Service | TOTP-based MFA | ✅ |
| PasswordResetService | Service | Self-service password reset | ✅ |
| PasswordService | Service | Password change and reset | ✅ |
| PasswordPolicy | Component | Password validation rules | ✅ |
| RefreshTokenService | Service | Long-lived token management | ✅ |
| UserProfileService | Service | Profile viewing and updates | ✅ |
| UserAccountDetailsService | Service | Spring Security UserDetailsService | ✅ |
| TenantAdminProvisioningService | Service | Initial tenant admin provisioning | ✅ |
| AuthTokenDigests | Utility | Token digest generation | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.auth.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| AuthController | Controller | Login, logout, token refresh, password | ✅ |
| MfaController | Controller | MFA enrollment and management | ✅ |
| UserProfileController | Controller | User profile management | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.auth.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| UserAccount | Entity | Core user identity | ✅ |
| UserPrincipal | Class | Spring Security UserDetails wrapper | ✅ |
| RefreshToken | Entity | Long-lived session token | ✅ |
| PasswordResetToken | Entity | Self-service reset tokens | ✅ |
| BlacklistedToken | Entity | Revoked JWT tokens | ✅ |
| MfaRecoveryCode | Entity | One-time MFA recovery codes | ✅ |
| UserPasswordHistory | Entity | Password reuse prevention | ✅ |
| UserTokenRevocation | Entity | Global token revocation | ✅ |

---

## HR Module

### Services (`com.bigbrightpaints.erp.modules.hr.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| HrService | Service | HR facade delegating to subservices | ✅ |
| EmployeeService | Service | Employee CRUD | ✅ |
| AttendanceService | Service | Attendance tracking | ✅ |
| LeaveService | Service | Leave request workflow | ✅ |
| SalaryStructureTemplateService | Service | Salary structure CRUD | ✅ |
| PayrollService | Service | Payroll facade | ✅ |
| PayrollCalculationService | Service | Pay calculations | ✅ |
| PayrollPostingService | Service | Journal posting | ✅ |
| StatutoryDeductionEngine | Service | PF/ESI/TDS/PT calculations | ✅ |
| PayrollCalculationSupport | Service | Calculation utilities | ✅ |
| PayrollRunService | Service | Payroll run lifecycle | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.hr.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| Employee | Entity | Employee master data | ✅ |
| Attendance | Entity | Daily attendance record | ✅ |
| LeaveRequest | Entity | Leave application | ✅ |
| LeaveBalance | Entity | Employee leave balances | ✅ |
| LeaveTypePolicy | Entity | Leave type definitions | ✅ |
| PayrollRun | Entity | Payroll batch | ✅ |
| PayrollRunLine | Entity | Employee payroll breakdown | ✅ |
| SalaryStructureTemplate | Entity | Salary structure definition | ✅ |

---

## Admin Module

### Services (`com.bigbrightpaints.erp.modules.admin.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| AdminUserService | Service | User management operations | ✅ |
| ChangelogService | Service | Changelog entry management | ✅ |
| ExportApprovalService | Service | Export approval workflow | ✅ |
| GitHubIssueClient | Service | GitHub API integration | ✅ |
| SupportTicketGitHubSyncService | Service | Support ticket GitHub sync | ✅ |
| SupportTicketService | Service | Support ticket management | ✅ |
| TenantRuntimePolicyService | Service | Tenant quota enforcement | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.admin.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| ChangelogEntry | Entity | System version announcements | ✅ |
| ExportRequest | Entity | Export approval tracking | ✅ |
| SupportTicket | Entity | User support tickets | ✅ |

---

## Company Module

### Services (`com.bigbrightpaints.erp.modules.company.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| CompanyService | Service | Company CRUD and lifecycle | ✅ |
| CompanyContextService | Service | Current company resolution | ✅ |
| TenantOnboardingService | Service | Tenant onboarding with CoA seeding | ✅ |
| TenantLifecycleService | Service | Tenant state transitions | ✅ |
| SuperAdminTenantControlPlaneService | Service | Super-admin tenant operations | ✅ |
| ModuleGatingService | Service | Feature module enablement | ✅ |
| ModuleGatingInterceptor | Interceptor | API path-based module gating | ✅ |
| TenantRuntimeEnforcementService | Service | Request admission and quota | ✅ |
| TenantUsageMetricsService | Service | API call tracking | ✅ |
| CoATemplateService | Service | Chart of Accounts templates | ✅ |

### Controllers (`com.bigbrightpaints.erp.modules.company.controller`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| CompanyController | Controller | Company listing and management | ✅ |
| MultiCompanyController | Controller | Company switching | ✅ |
| SuperAdminController | Controller | Super-admin tenant management | ✅ |
| SuperAdminTenantOnboardingController | Controller | Tenant onboarding | ✅ |

---

## Production Module

### Services (`com.bigbrightpaints.erp.modules.production.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| CatalogService | Service | Catalog CRUD with inventory sync | ✅ |
| ProductionCatalogService | Service | Bulk operations and imports | ✅ |
| SkuReadinessService | Service | SKU readiness evaluation | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.production.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| ProductionBrand | Entity | Product brand | ✅ |
| ProductionProduct | Entity | Product catalog entry | ✅ |
| CatalogImport | Entity | CSV import history | ✅ |

---

## Invoice Module

### Services (`com.bigbrightpaints.erp.modules.invoice.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| InvoiceService | Service | Invoice retrieval and lifecycle | ✅ |
| InvoicePdfService | Service | PDF generation | ✅ |
| InvoiceNumberService | Service | Invoice number sequencing | ✅ |
| InvoiceSettlementPolicy | Service | Payment application | ✅ |

---

## Portal Module

### Services (`com.bigbrightpaints.erp.modules.portal.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| PortalInsightsService | Service | Dashboard, operations, workforce insights | ✅ |
| EnterpriseDashboardService | Service | Enterprise snapshot with trends | ✅ |
| TenantRuntimeEnforcementInterceptor | Interceptor | Request rate limiting | ✅ |

---

## RBAC Module

### Services (`com.bigbrightpaints.erp.modules.rbac.service`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| RoleService | Service | Role and permission management | ✅ |

### Entities (`com.bigbrightpaints.erp.modules.rbac.domain`)

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| Role | Entity | Role definition | ✅ |
| Permission | Entity | Permission definition | ✅ |
| SystemRole | Enum | System role definitions | ✅ |

---

## Reports Module

| Name | Type | Purpose | Status |
|------|------|---------|--------|
| ReportService | Service | Financial report generation | ✅ |
| TrialBalanceReportQueryService | Service | Trial balance queries | ✅ |
| ProfitLossReportQueryService | Service | P&L queries | ✅ |
| BalanceSheetReportQueryService | Service | Balance sheet queries | ✅ |
| ReportController | Controller | Report endpoints | ✅ |

---

*This index is auto-generated from module documentation. For detailed documentation, see individual module files.*
