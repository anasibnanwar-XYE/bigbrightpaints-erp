# Company Services

## CompanyService
**Location:** `service/CompanyService.java`

Core company CRUD and lifecycle management service.

### Dependencies
- `CompanyRepository` - Persistence
- `AuditService` - Audit logging
- `UserAccountRepository` - User metrics
- `AuditLogRepository` - Activity metrics
- `TenantRuntimeEnforcementService` - Runtime policy
- `TenantAdminProvisioningService` - Admin creation
- `TenantLifecycleService` - State transitions

### Key Methods

| Method | Description |
|--------|-------------|
| `findAll()` | List all companies |
| `findAll(Set<Company>)` | List companies by membership |
| `create(CompanyRequest)` | Create company (super-admin only) |
| `update(Long, CompanyRequest, Set<Company>)` | Update company |
| `findByCode(String)` | Find company by code |
| `switchCompany(String, Set<Company>)` | Switch active company |
| `updateLifecycleState(Long, CompanyLifecycleStateRequest)` | Transition lifecycle |
| `updateEnabledModules(Long, Set<String>)` | Update modules |
| `getTenantMetrics(Long)` | Get tenant usage metrics |
| `getSuperAdminDashboard()` | Get dashboard overview |
| `resetTenantAdminPassword(Long, String, String)` | Reset admin password |
| `isRuntimeAccessAllowed(Long)` | Check if tenant can accept requests |

### Quota Enforcement
- `isRuntimeAccessAllowed()` checks:
  - Tenant is ACTIVE
  - Hard limit enabled with configured quota envelope
  - Active users ≤ quota
  - API activity ≤ quota
  - Storage ≤ quota
  - Concurrent requests ≤ quota

---

## CompanyContextService
**Location:** `service/CompanyContextService.java`

Helper service for current company resolution.

### Key Methods

| Method | Description |
|--------|-------------|
| `requireCurrentCompany()` | Get current company from context, throw if missing |

Uses `CompanyContextHolder.getCompanyCode()` to resolve company.

---

## TenantOnboardingService
**Location:** `service/TenantOnboardingService.java`

End-to-end tenant onboarding with CoA seeding.

### Dependencies
- `CompanyRepository` - Company persistence
- `UserAccountRepository` - Admin lookup
- `AccountRepository` - Chart of accounts
- `AccountingPeriodService` - Default period
- `CoATemplateService` - Template resolution
- `TenantAdminProvisioningService` - Admin creation
- `SystemSettingsRepository` - Settings init

### Key Method

| Method | Description |
|--------|-------------|
| `onboardTenant(TenantOnboardingRequest)` | Complete tenant setup |

### Onboarding Flow
1. Validate company code and admin email available
2. Load CoA template blueprints
3. Create and save company
4. Create template accounts (50-100 accounts)
5. Apply company default accounts
6. Provision first admin user
7. Create default accounting period
8. Initialize system settings
9. Record onboarding metadata

### Template Blueprints

**GENERIC**: Standard accounts including:
- Assets (Cash, Bank, AR, Inventory, Fixed Assets)
- Liabilities (AP, Tax Payable, GST)
- Equity (Share Capital, Retained Earnings)
- Revenue (Sales, Other Income)
- COGS (Cost of Goods Sold)
- Expenses (Salary, Rent, Utilities)

**INDIAN_STANDARD**: Generic + CGST/SGST/IGST, TDS accounts

**MANUFACTURING**: Indian Standard + Raw Materials, WIP, Finished Goods, Production Variance

---

## TenantLifecycleService
**Location:** `service/TenantLifecycleService.java`

Tenant lifecycle state transitions with validation.

### Key Methods

| Method | Description |
|--------|-------------|
| `transition(Company, CompanyLifecycleState, String reason, Authentication)` | Transition state |
| `validateTransition(CompanyLifecycleState, CompanyLifecycleState)` | Validate allowed transition |

### Valid Transitions
```
ACTIVE ──────────> SUSPENDED
ACTIVE ──────────> DEACTIVATED
SUSPENDED ───────> ACTIVE
SUSPENDED ───────> DEACTIVATED
DEACTIVATED ─────> ACTIVE
```

### Audit
All transitions logged with:
- Actor
- Previous state
- New state
- Reason
- Audit chain ID

---

## SuperAdminTenantControlPlaneService
**Location:** `service/SuperAdminTenantControlPlaneService.java`

Super-admin operations for tenant management.

### Dependencies
- `CompanyRepository` - Company lookup
- `UserAccountRepository` - User management
- `AuditLogRepository` - Activity history
- `AuditService` - Audit logging
- `EmailService` - Email notifications
- `TokenBlacklistService` - Session revocation
- `RefreshTokenService` - Token cleanup
- `TenantSupportWarningRepository` - Warning persistence
- `TenantAdminEmailChangeRequestRepository` - Email change requests
- `TenantRuntimeEnforcementService` - Runtime policy
- `CompanyService` - Core operations

### Key Methods

| Method | Description |
|--------|-------------|
| `listTenants(String status)` | List tenants with optional status filter |
| `getTenantDetail(Long)` | Get detailed tenant info |
| `updateLifecycleState(Long, CompanyLifecycleStateRequest)` | Update state |
| `updateModules(Long, Set<String>)` | Update modules |
| `resetTenantAdminPassword(Long, String, String)` | Reset admin password |
| `updateLimits(Long, ...)` | Update quota limits |
| `issueSupportWarning(Long, ...)` | Issue support warning |
| `updateSupportContext(Long, String, Set<String>)` | Update notes/tags |
| `forceLogoutAllUsers(Long, String)` | Revoke all tenant sessions |
| `replaceMainAdmin(Long, Long)` | Change main admin |
| `requestAdminEmailChange(Long, Long, String)` | Start email change |
| `confirmAdminEmailChange(Long, Long, Long, String)` | Complete email change |

### Tenant-Exclusive Validation
For operations like force logout and email change:
- Validates users are not shared across multiple companies
- Prevents impact on users with cross-tenant access

---

## ModuleGatingService
**Location:** `service/ModuleGatingService.java`

Feature module enablement checking.

### Key Methods

| Method | Description |
|--------|-------------|
| `isEnabledForCurrentCompany(CompanyModule)` | Check if module enabled |
| `requireEnabledForCurrentCompany(CompanyModule, String path)` | Throw if disabled |
| `isEnabled(Company, CompanyModule)` | Check for specific company |
| `resolveEnabledGatableModules(Company)` | Get enabled modules |

### Module Categories
- **Core** (always enabled): AUTH, ACCOUNTING, SALES, INVENTORY
- **Gatable**: MANUFACTURING, HR_PAYROLL, PURCHASING, PORTAL, REPORTS_ADVANCED

---

## ModuleGatingInterceptor
**Location:** `service/ModuleGatingInterceptor.java`

Spring MVC interceptor for API path-based module gating.

### Path Mapping

| Path Prefix | Module |
|-------------|--------|
| `/api/v1/factory`, `/api/v1/production` | MANUFACTURING |
| `/api/v1/hr`, `/api/v1/payroll` | HR_PAYROLL |
| `/api/v1/purchasing`, `/api/v1/suppliers` | PURCHASING |
| `/api/v1/portal`, `/api/v1/dealer-portal` | PORTAL |
| `/api/v1/reports` | REPORTS_ADVANCED |

### Behavior
- Skips paths not starting with `/api/v1/`
- Skips if no company context
- Core modules (AUTH, ACCOUNTING, SALES, INVENTORY) always pass

---

## TenantRuntimeEnforcementService
**Location:** `service/TenantRuntimeEnforcementService.java`

Runtime request admission and quota enforcement.

### Configuration
- `erp.tenant.runtime.default-max-concurrent-requests` (default: 200)
- `erp.tenant.runtime.default-max-requests-per-minute` (default: 5000)
- `erp.tenant.runtime.default-max-active-users` (default: 500)
- `erp.tenant.runtime.policy-cache-seconds` (default: 15)

### Key Methods

| Method | Description |
|--------|-------------|
| `beginRequest(companyCode, path, method, actor)` | Request admission |
| `completeRequest(admission, status)` | Complete tracking |
| `enforceAuthOperationAllowed(companyCode, actor, operation)` | Auth-time check |
| `holdTenant(companyCode, reason, actor)` | Set state to HOLD |
| `blockTenant(companyCode, reason, actor)` | Set state to BLOCKED |
| `resumeTenant(companyCode, actor)` | Set state to ACTIVE |
| `updateQuotas(...)` | Update quota limits |
| `updatePolicy(...)` | Full policy update |
| `snapshot(companyCode)` | Get current state |

### States
- `ACTIVE` - Normal operation
- `HOLD` - Read-only (no mutations)
- `BLOCKED` - All requests rejected

### Admission Result
- Admitted: Request proceeds
- Rejected: HTTP 423 (Locked), 429 (Too Many Requests), or 403 (Forbidden)

### Fail-Closed Policy
- Unknown company code → rejected
- Missing telemetry → rejected
- Quota exceeded → rejected

---

## TenantUsageMetricsService
**Location:** `service/TenantUsageMetricsService.java`

Simple API call tracking via system settings.

### Key Methods

| Method | Description |
|--------|-------------|
| `recordApiCall(String companyCode)` | Increment API call counter |
| `getApiCallCount(Long companyId)` | Get total API calls |
| `getLastActivityAt(Long companyId)` | Get last activity timestamp |

### Storage
- `tenant.usage.api-call-count.{companyId}` - Counter
- `tenant.usage.last-activity-at.{companyId}` - Timestamp

---

## CoATemplateService
**Location:** `service/CoATemplateService.java`

Chart of Accounts template management.

### Key Methods

| Method | Description |
|--------|-------------|
| `listActiveTemplates()` | List available templates |
| `requireActiveTemplate(String code)` | Get template or throw |

### Response
```java
record CoATemplateDto(String code, String name, String description, Integer accountCount)
```
