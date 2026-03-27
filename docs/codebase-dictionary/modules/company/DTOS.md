# Company Entities

## Company
**Location:** `domain/Company.java`
**Table:** `companies`

Core multi-tenant entity representing a customer organization.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK, auto-generated | Surrogate key |
| `publicId` | UUID | NOT NULL | External identifier |
| `name` | String | NOT NULL | Company name |
| `code` | String | NOT NULL, UNIQUE | Tenant code |
| `timezone` | String | NOT NULL | Default timezone |
| `stateCode` | String | NULL | 2-letter state code |
| `createdAt` | Instant | NOT NULL | Creation timestamp |
| `lifecycleState` | CompanyLifecycleState | NOT NULL, default ACTIVE | Tenant state |
| `lifecycleReason` | String | NULL | State change reason |
| `enabledModules` | Set<String> | NOT NULL, JSON | Enabled gatable modules |
| `baseCurrency` | String | NOT NULL, default "INR" | Base currency |
| `defaultGstRate` | BigDecimal | NOT NULL, default 18 | Default GST % |

### Quota Fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `quotaMaxActiveUsers` | Long | 0 | Max active users (0 = unlimited) |
| `quotaMaxApiRequests` | Long | 0 | Max API requests |
| `quotaMaxStorageBytes` | Long | 0 | Max storage in bytes |
| `quotaMaxConcurrentRequests` | Long | 0 | Max concurrent requests |
| `quotaSoftLimitEnabled` | Boolean | false | Soft limit flag |
| `quotaHardLimitEnabled` | Boolean | true | Hard limit flag |

### Default Account IDs

| Field | Description |
|-------|-------------|
| `defaultInventoryAccountId` | Default inventory account |
| `defaultCogsAccountId` | Default COGS account |
| `defaultRevenueAccountId` | Default revenue account |
| `defaultDiscountAccountId` | Default discount account |
| `defaultTaxAccountId` | Default tax account |
| `gstInputTaxAccountId` | GST input tax account |
| `gstOutputTaxAccountId` | GST output tax account |
| `gstPayableAccountId` | GST payable account |

### Payroll Accounts

| Field | Type | Description |
|-------|------|-------------|
| `payrollExpenseAccount` | Account (FK) | Payroll expense |
| `payrollCashAccount` | Account (FK) | Payroll cash |

### Admin & Support

| Field | Type | Description |
|-------|------|-------------|
| `mainAdminUserId` | Long | Main admin user ID |
| `supportNotes` | String (TEXT) | Support notes |
| `supportTags` | Set<String> (JSON) | Support tags |

### Onboarding Metadata

| Field | Type | Description |
|-------|------|-------------|
| `onboardingCoaTemplateCode` | String | CoA template used |
| `onboardingAdminEmail` | String | First admin email |
| `onboardingAdminUserId` | Long | First admin user ID |
| `onboardingCompletedAt` | Instant | Onboarding completion |
| `onboardingCredentialsEmailedAt` | Instant | Credentials sent at |

### Pre-Persist/Pre-Update Hooks
- Generate `publicId` if null
- Set `createdAt` if null
- Default `timezone` to "UTC"
- Normalize `enabledModules` (only gatable modules)
- Normalize `supportTags` (uppercase)
- Initialize quota defaults
- Enforce fail-closed quota policy (hard limit if neither enabled)

---

## CompanyModule (Enum)
**Location:** `domain/CompanyModule.java`

Feature module enumeration for gating.

### Values

| Module | Gatable | Default |
|--------|:-------:|:-------:|
| AUTH | ✗ | (always) |
| ACCOUNTING | ✗ | (always) |
| SALES | ✗ | (always) |
| INVENTORY | ✗ | (always) |
| MANUFACTURING | ✓ | ✓ |
| HR_PAYROLL | ✓ | ✗ |
| PURCHASING | ✓ | ✓ |
| PORTAL | ✓ | ✓ |
| REPORTS_ADVANCED | ✓ | ✓ |

### Methods
```java
boolean isGatable()  // true if can be disabled
boolean isCore()     // true if always enabled
static Set<String> defaultEnabledGatableModuleNames()
static Set<String> normalizeEnabledGatableModuleNames(Set<String>)
```

---

## CompanyLifecycleState (Enum)
**Location:** `domain/CompanyLifecycleState.java`

Tenant lifecycle states.

### Values
- `ACTIVE` - Normal operation
- `SUSPENDED` - Read-only mode
- `DEACTIVATED` - Fully blocked

### Methods
```java
String toExternalValue()  // Returns name()
static CompanyLifecycleState fromRequestValue(String)  // Parse with validation
static Optional<CompanyLifecycleState> fromStoredValue(String)  // Safe parse
```

---

## CoATemplate
**Location:** `domain/CoATemplate.java`
**Table:** `coa_templates`

Chart of Accounts templates for tenant onboarding.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `publicId` | UUID | NOT NULL | External ID |
| `code` | String | NOT NULL, UNIQUE, Length 64 | Template code |
| `name` | String | NOT NULL, Length 128 | Display name |
| `description` | String | NOT NULL, Length 2000 | Description |
| `accountCount` | Integer | NOT NULL | Number of accounts |
| `active` | boolean | NOT NULL | Active flag |
| `createdAt` | Instant | NOT NULL | Creation time |

### Template Codes
- `GENERIC` - Standard chart
- `INDIAN_STANDARD` - India-specific with GST
- `MANUFACTURING` - Manufacturing-specific

---

## TenantSupportWarning
**Location:** `domain/TenantSupportWarning.java`
**Table:** `tenant_support_warnings`

Support warnings issued to tenants.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `company` | Company | FK, NOT NULL | Target company |
| `warningCategory` | String | NOT NULL | Warning type |
| `message` | String | NOT NULL, Length 500 | Warning message |
| `requestedLifecycleState` | String | NOT NULL, Length 32 | Target state |
| `gracePeriodHours` | Integer | NOT NULL | Grace period |
| `issuedBy` | String | NOT NULL, Length 255 | Issuing admin |
| `issuedAt` | Instant | NOT NULL | Issue timestamp |

### Pre-Persist
- Default `warningCategory` to "GENERAL"
- Default `requestedLifecycleState` to "SUSPENDED"
- Default `gracePeriodHours` to 24

---

## TenantAdminEmailChangeRequest
**Location:** `domain/TenantAdminEmailChangeRequest.java`
**Table:** `tenant_admin_email_change_requests`

Admin email change verification requests.

### Fields

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | PK | Surrogate key |
| `companyId` | Long | NOT NULL | Target company |
| `adminUserId` | Long | NOT NULL | Target admin |
| `requestedBy` | String | NOT NULL, Length 255 | Requesting admin |
| `currentEmail` | String | NOT NULL, Length 255 | Current email |
| `requestedEmail` | String | NOT NULL, Length 255 | New email |
| `verificationToken` | String | NOT NULL, Length 255 | Verification token |
| `verificationSentAt` | Instant | NOT NULL | Token sent time |
| `verifiedAt` | Instant | NULL | Verification time |
| `confirmedAt` | Instant | NULL | Confirmation time |
| `expiresAt` | Instant | NOT NULL | Token expiration |
| `consumed` | boolean | NOT NULL | Request consumed |

### Pre-Persist
- Default expiration to 24 hours from sent
- Normalize emails to lowercase

---

## TenantUsageMetricsInterceptor
**Location:** `service/TenantUsageMetricsInterceptor.java`

Request interceptor for tracking tenant API usage.

### Behavior
- Increments API call counter per company
- Updates last activity timestamp
- Uses system settings for persistence
