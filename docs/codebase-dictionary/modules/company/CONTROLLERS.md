# Company Module Overview

## Purpose
The Company module handles multi-tenant management, including company lifecycle, onboarding, module gating, quota management, and super-admin control plane operations.

## Location
`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`

## Architecture

```
modules/company/
├── controller/           # REST endpoints
│   ├── CompanyController.java
│   ├── MultiCompanyController.java
│   ├── SuperAdminController.java
│   └── SuperAdminTenantOnboardingController.java
├── service/              # Business logic
│   ├── CompanyService.java
│   ├── CompanyContextService.java
│   ├── TenantOnboardingService.java
│   ├── TenantLifecycleService.java
│   ├── SuperAdminTenantControlPlaneService.java
│   ├── ModuleGatingService.java
│   ├── ModuleGatingInterceptor.java
│   ├── TenantRuntimeEnforcementService.java
│   ├── TenantUsageMetricsService.java
│   └── CoATemplateService.java
├── domain/               # JPA entities
│   ├── Company.java
│   ├── CompanyModule.java (enum)
│   ├── CompanyLifecycleState.java (enum)
│   ├── CompanyRepository.java
│   ├── CoATemplate.java
│   ├── CoATemplateRepository.java
│   ├── TenantSupportWarning.java
│   ├── TenantAdminEmailChangeRequest.java
│   └── repositories
└── dto/                  # Request/Response DTOs
    ├── CompanyDto.java
    ├── CompanyRequest.java
    ├── CompanyEnabledModulesDto.java
    ├── CompanyLifecycleStateDto.java
    ├── CompanyLifecycleStateRequest.java
    ├── CompanyTenantMetricsDto.java
    ├── CompanySuperAdminDashboardDto.java
    ├── CompanySupportWarningDto.java
    ├── CompanyAdminCredentialResetDto.java
    ├── CoATemplateDto.java
    ├── TenantOnboardingRequest.java
    ├── TenantOnboardingResponse.java
    ├── SuperAdminTenantSummaryDto.java
    ├── SuperAdminTenantDetailDto.java
    ├── SuperAdminTenantLimitsDto.java
    ├── SuperAdminTenantSupportContextDto.java
    ├── SuperAdminTenantForceLogoutDto.java
    ├── SuperAdminTenantAdminEmailChangeRequestDto.java
    ├── SuperAdminTenantAdminEmailChangeConfirmationDto.java
    └── MainAdminSummaryDto.java
```

## Key Features

### 1. Multi-Tenancy
- Company isolation via `X-Company-Code` header
- User-to-company many-to-many relationships
- Company-specific timezone and GST settings

### 2. Tenant Onboarding
- Super-admin initiated tenant creation
- Chart of Accounts template seeding (GENERIC, INDIAN_STANDARD, MANUFACTURING)
- Automatic first admin provisioning with email credentials
- Default accounting period creation

### 3. Lifecycle Management
- Three states: ACTIVE, SUSPENDED, DEACTIVATED
- State transition validation (no invalid jumps)
- Automatic runtime policy synchronization

### 4. Module Gating
- Core modules: AUTH, ACCOUNTING, SALES, INVENTORY (always enabled)
- Gatable modules: MANUFACTURING, HR_PAYROLL, PURCHASING, PORTAL, REPORTS_ADVANCED
- Request-level interceptor for API path gating

### 5. Quota Management
- Configurable limits: active users, API requests, storage, concurrent requests
- Soft and hard limit flags
- Runtime enforcement with fail-closed policy

### 6. Super-Admin Control Plane
- Tenant dashboard with metrics
- Lifecycle state transitions
- Module and quota updates
- Support warning issuance
- Admin password reset
- Force logout all users
- Admin email change with verification

## Dependencies
- `modules/auth` - User authentication and admin provisioning
- `modules/accounting` - Chart of Accounts seeding
- `modules/rbac` - Role management for admin assignment
- `core/security/CompanyContextHolder` - Current company context
- `core/audit` - Configuration change audit logging
- `core/notification/EmailService` - Credential emails

## Security Model
- Super-admin (`ROLE_SUPER_ADMIN`) required for:
  - Tenant onboarding
  - Lifecycle state changes
  - Quota and module updates
  - Support operations (warnings, admin reset, force logout)
- Regular admin (`ROLE_ADMIN`) can view companies they belong to
- Company context bound to authenticated user's company memberships
