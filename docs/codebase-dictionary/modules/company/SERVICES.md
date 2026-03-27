# Company Controllers

## CompanyController
**Location:** `controller/CompanyController.java`
**Base Path:** `/api/v1/companies`

Company listing and management for authenticated users.

### Endpoints

| Method | Path | Description | Auth Required | Roles |
|--------|------|-------------|---------------|-------|
| GET | `/` | List companies | Yes | ADMIN/ACCOUNTING/SALES/SUPER_ADMIN |
| DELETE | `/{id}` | Delete company (disabled) | Yes | ADMIN |

### Endpoint Details

#### GET /
- Super-admin: returns all companies
- Others: returns companies user belongs to
- Response: `List<CompanyDto>`

#### DELETE /{id}
- Always throws `AccessDeniedException` (deletion not permitted)
- Validates user membership before denying

---

## MultiCompanyController
**Location:** `controller/MultiCompanyController.java`
**Base Path:** `/api/v1/multi-company`

Company switching for multi-tenant users.

### Endpoints

| Method | Path | Description | Auth Required |
|--------|------|-------------|---------------|
| POST | `/companies/switch` | Switch active company | Yes |

### Request/Response

```java
// Request
record SwitchCompanyRequest(@NotBlank String companyCode)

// Response: CompanyDto
record CompanyDto(Long id, UUID publicId, String name, String code, String timezone, String stateCode, BigDecimal defaultGstRate)
```

### Authorization
- User must be member of target company
- Returns `AccessDeniedException` if not a member

---

## SuperAdminController
**Location:** `controller/SuperAdminController.java`
**Base Path:** `/api/v1/superadmin`
**Required Role:** `ROLE_SUPER_ADMIN`

Super-admin control plane for tenant management.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/dashboard` | Super-admin dashboard with tenant overview |
| GET | `/tenants` | List all tenants (optional status filter) |
| GET | `/tenants/{id}` | Get tenant detail |
| PUT | `/tenants/{id}/lifecycle` | Update tenant lifecycle state |
| PUT | `/tenants/{id}/limits` | Update tenant quota limits |
| PUT | `/tenants/{id}/modules` | Update enabled modules |
| POST | `/tenants/{id}/support/warnings` | Issue support warning |
| POST | `/tenants/{id}/support/admin-password-reset` | Reset tenant admin password |
| PUT | `/tenants/{id}/support/context` | Update support notes/tags |
| POST | `/tenants/{id}/force-logout` | Force logout all tenant users |
| PUT | `/tenants/{id}/admins/main` | Replace main admin |
| POST | `/tenants/{id}/admins/{adminId}/email-change/request` | Request admin email change |
| POST | `/tenants/{id}/admins/{adminId}/email-change/confirm` | Confirm admin email change |

### Request DTOs

```java
// Lifecycle update
record CompanyLifecycleStateRequest(@NotBlank String state, @NotBlank String reason)

// Limits update
record TenantLimitsUpdateRequest(
    @Min(0) Long quotaMaxActiveUsers,
    @Min(0) Long quotaMaxApiRequests,
    @Min(0) Long quotaMaxStorageBytes,
    @Min(0) Long quotaMaxConcurrentRequests,
    Boolean quotaSoftLimitEnabled,
    Boolean quotaHardLimitEnabled
)

// Modules update
record TenantModulesUpdateRequest(@NotNull Set<@NotBlank @Size(max=64) String> enabledModules)

// Support warning
record TenantSupportWarningRequest(
    @Size(max=100) String warningCategory,
    @NotBlank @Size(max=500) String message,
    @Size(max=32) String requestedLifecycleState,
    @Min(1) Integer gracePeriodHours
)

// Admin password reset
record TenantAdminPasswordResetRequest(@Email @NotBlank String adminEmail, @Size(max=300) String reason)

// Support context update
record TenantSupportContextUpdateRequest(
    @Size(max=4000) String supportNotes,
    Set<@NotBlank @Size(max=64) String> supportTags
)

// Force logout
record TenantForceLogoutRequest(@Size(max=300) String reason)

// Main admin replacement
record TenantMainAdminUpdateRequest(@NotNull Long adminUserId)

// Email change
record TenantAdminEmailChangeRequest(@Email @NotBlank String newEmail)
record TenantAdminEmailChangeConfirmRequest(@NotNull Long requestId, @NotBlank @Size(max=255) String verificationToken)
```

---

## SuperAdminTenantOnboardingController
**Location:** `controller/SuperAdminTenantOnboardingController.java`
**Base Path:** `/api/v1/superadmin/tenants`
**Required Role:** `ROLE_SUPER_ADMIN`

Tenant onboarding with chart of accounts seeding.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/coa-templates` | List available CoA templates |
| POST | `/onboard` | Onboard new tenant |

### Request/Response

```java
// Request: TenantOnboardingRequest
record TenantOnboardingRequest(
    @NotBlank @Size(max=255) String name,
    @NotBlank @Size(max=64) String code,
    @NotBlank @Size(max=64) String timezone,
    @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal defaultGstRate,
    @Min(0) Long maxActiveUsers,
    @Min(0) Long maxApiRequests,
    @Min(0) Long maxStorageBytes,
    @Min(0) Long maxConcurrentRequests,
    Boolean softLimitEnabled,
    Boolean hardLimitEnabled,
    @Email @NotBlank String firstAdminEmail,
    @Size(max=255) String firstAdminDisplayName,
    @NotBlank @Size(max=64) String coaTemplateCode
)

// Response: TenantOnboardingResponse
record TenantOnboardingResponse(
    Long companyId,
    String companyCode,
    String templateCode,
    String bootstrapMode,           // "SEEDED"
    boolean seededChartOfAccounts,
    Integer accountsCreated,
    Long accountingPeriodId,
    boolean defaultAccountingPeriodCreated,
    String adminEmail,
    Long mainAdminUserId,
    boolean tenantAdminProvisioned,
    boolean credentialsEmailSent,
    Instant credentialsEmailedAt,
    Instant onboardingCompletedAt,
    boolean systemSettingsInitialized
)

// CoA Template DTO
record CoATemplateDto(String code, String name, String description, Integer accountCount)
```

### Available Templates
- `GENERIC` - Standard chart of accounts (50+ accounts)
- `INDIAN_STANDARD` - Generic + GST/TDS specific accounts
- `MANUFACTURING` - Indian Standard + production-specific accounts

## Authorization Matrix

| Endpoint | SUPER_ADMIN | ADMIN | USER |
|----------|:-----------:|:-----:|:----:|
| GET /companies | ✓ (all) | ✓ (member) | ✓ (member) |
| POST /multi-company/switch | ✓ | ✓ | ✓ |
| /superadmin/** | ✓ | ✗ | ✗ |
