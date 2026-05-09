# Company Module

Last reviewed: 2026-03-30

This document describes the **company module** (`modules/company`) and the tenant-runtime infrastructure it owns. It covers tenant lifecycle, runtime admission, module gating, super-admin control-plane operations, company-context resolution, usage-enforcement surfaces, and the V1 Add Client activation boundary.

## Ownership Summary

The company module owns the **tenant lifecycle and runtime enforcement** surface: company CRUD, tenant lifecycle transitions, runtime request admission, per-tenant quota enforcement, module gating, super-admin control-plane operations, company-context resolution, and V1 Add Client activation.

| Area | Package |
| --- | --- |
| Company controllers | `modules/company/controller/` |
| Company services | `modules/company/service/` |
| Company DTOs | `modules/company/dto/` |
| Company domain entities | `modules/company/domain/` |

## Primary Controllers and Routes

### CompanyController — `/api/v1/companies`

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/api/v1/companies` | `ROLE_SUPER_ADMIN`, `ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_SALES` | List companies (super-admin: all; tenant users: own company only) |
| DELETE | `/api/v1/companies/{id}` | `ROLE_ADMIN` | Currently always denies deletion — companies cannot be deleted |

### SuperAdmin control plane — `/api/v1/superadmin`

All endpoints require `ROLE_SUPER_ADMIN`.

The final V1 surface is documented for frontend consumers in
[`docs/frontend-portals/superadmin/api-contracts.md`](../frontend-portals/superadmin/api-contracts.md)
and is generated in `openapi.json`. Company-owned Super Admin routes include:

| Route family | Purpose |
| --- | --- |
| `GET /api/v1/superadmin/dashboard` | Platform aggregate dashboard; no private tenant business rows |
| `GET, POST /api/v1/superadmin/tenants` | Paginated/filtered tenant list and strict Add Client create payload |
| `GET /api/v1/superadmin/tenants/new` | Add Client option schema for company, owner, commercial, quotas, modules, support, create modes, and seed policy |
| `GET /api/v1/superadmin/tenants/{id}` | State-aware tenant profile tabs as summaries only |
| `/api/v1/superadmin/tenants/{id}/activation/{send,resend,copy,expire}` | Digest-token activation actions; link values are only at explicit copy/delivery boundaries and must be redacted in evidence |
| `/api/v1/superadmin/tenants/{id}/seed-status/**` and `/accounting-mappings/{mappingKey}` | Seed readiness, repair, and locked mapping controls |
| `/api/v1/superadmin/tenants/{id}/plan`, `/entitlements/**`, `/limits`, `/modules`, `/quota-check`, `/quota-policy` | Plan assignment, effective entitlement source metadata, quotas, rate limits, and module gates |
| `/api/v1/superadmin/tenants/{id}/billing/**` plus `/billing/metrics` | Subscription, immutable manual billing ledger, MRR/ARR summaries, and balance actions |
| `/api/v1/superadmin/tenants/{id}/suspension/**`, `/resume`, `/cancel`, `/archive`, `/commercial-state`, `/lifecycle` | Commercial/lifecycle access matrix and current lifecycle update endpoint |
| `/api/v1/superadmin/support/tickets/**` | Support queue, customer-visible messages, internal notes, SLA refresh, feature/incident conversion, and Sentry link/sync |
| `/api/v1/superadmin/audit/**`, `/infra/**`, `/observability/datadog/status` | Privacy-safe audit/security events, infra health/cost, and safe observability status |
| `/api/v1/superadmin/profile/**`, `/settings`, `/roles/**`, `/changelog/**`, `/notify` | Platform operator profile, settings, role catalog, release notes, and notification utility |

All list routes use documented query parameters and explicit validation for
unknown filters, invalid enums, invalid sort fields, and oversized pages. All
accepted mutations write privacy-safe audit evidence with trace/correlation IDs.

### SuperAdminTenantOnboardingController — `/api/v1/superadmin/tenants`

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/api/v1/superadmin/tenants/coa-templates` | `ROLE_SUPER_ADMIN` | List available chart-of-accounts templates |

## Key Services

### TenantLifecycleService

Manages company lifecycle state transitions with a constrained state machine:

```
ACTIVE  ←→  SUSPENDED  →  DEACTIVATED
  │               │             ↑
  │               └─────────────┘
  └─────────────────────────────┘
```

- `ACTIVE → SUSPENDED`, `ACTIVE → DEACTIVATED`
- `SUSPENDED → ACTIVE`, `SUSPENDED → DEACTIVATED`
- `DEACTIVATED → ACTIVE` (recovery)

Each transition requires a reason string. Invalid transitions are rejected. All transitions are audited.

Lifecycle states are enforced by `CompanyContextFilter` on every tenant-scoped request:

| State | GET/HEAD/OPTIONS | POST/PUT/DELETE/PATCH |
| --- | --- | --- |
| `ACTIVE` | Allowed | Allowed |
| `SUSPENDED` | Allowed | Denied |
| `DEACTIVATED` | Denied | Denied |

### TenantRuntimeEnforcementService

The canonical tenant runtime policy-mutation and snapshot owner. Manages per-tenant runtime policies and in-flight request tracking.

**Runtime states** (separate from lifecycle states):

| Runtime State | Effect |
| --- | --- |
| `ACTIVE` | Normal operation |
| `HOLD` | Read-only — mutating requests rejected with `HTTP 423 LOCKED` |
| `BLOCKED` | All requests rejected with `HTTP 403 FORBIDDEN` |

**Quota enforcement:**

| Limit | Default | Description |
| --- | --- | --- |
| `maxConcurrentRequests` | 200 | Maximum in-flight requests per tenant |
| `maxRequestsPerMinute` | 5000 | Maximum requests per minute per tenant |
| `maxActiveUsers` | 500 | Maximum enabled user accounts per tenant |

**Request admission flow:**

1. Normalize company code.
2. Resolve runtime policy (cached in-memory with configurable TTL, default 15 seconds; backed by `system_settings` table).
3. Check runtime state (`HOLD` rejects mutating requests; `BLOCKED` rejects all).
4. Check per-minute rate limit.
5. Check concurrent request limit.
6. Admit request, increment in-flight counter.
7. On completion: decrement in-flight counter, track error responses.

**Auth-operation enforcement:**
Called during login and refresh to check tenant state and active-user quota. Rejects login if the tenant is on hold/blocked or if the active-user count exceeds the quota.

**Policy persistence:**
Runtime policies are persisted to the `system_settings` table with keys like `tenant.runtime.hold-state.{companyId}`, `tenant.runtime.max-concurrent-requests.{companyId}`, etc. On startup or cache expiry, policies are loaded from persistence. If persistence is unavailable during a cache refresh, the last-known in-memory policy is kept (request admission remains available during transient outages).

### TenantRuntimeEnforcementService

Canonical runtime policy, request-admission, auth-operation, counter, and snapshot owner. `CompanyContextFilter`, the portal/runtime interceptor, auth login/refresh, and super-admin runtime controls call this service directly.

### Add Client and Owner Setup Services

`SuperAdminTenantControlPlaneService` backs Add Client creation, activation actions, tenant profile summaries, quotas, billing, support, and audit-safe control-plane operations. `TenantDefaultSeedingService` owns default seed execution/status, and `OwnerSetupService` owns the owner setup corridor under `/api/v1/setup/**`. Tenant creation is stateful: draft or pending activation first, owner credential setup through activation, then setup completion through `/api/v1/setup/**`.

### SuperAdminTenantControlPlaneService

The super-admin control plane for tenant management. Provides tenant listing/detail, lifecycle transitions, usage-limit updates, module management, support operations (warnings and notes/tags), session management (force-logout, main-admin replacement), and admin email changes with verification tokens. Platform-issued support password reset is not a current route; use activation or scoped auth password recovery.

### ModuleGatingService

Controls which optional modules are available per tenant:

| Module | Gatable | Default |
| --- | --- | --- |
| `AUTH` | No (core) | Always enabled |
| `ACCOUNTING` | No (core) | Always enabled |
| `SALES` | No (core) | Always enabled |
| `INVENTORY` | No (core) | Always enabled |
| `MANUFACTURING` | Yes | Enabled |
| `HR_PAYROLL` | Yes | **Disabled** |
| `PURCHASING` | Yes | Enabled |
| `PORTAL` | Yes | Enabled |
| `REPORTS_ADVANCED` | Yes | Enabled |

Core modules cannot be disabled. `HR_PAYROLL` is the only gatable module that defaults to disabled.

### CompanyContextService

Resolves the current company from `CompanyContextHolder` (ThreadLocal set by `CompanyContextFilter`). Provides `requireCurrentCompany()` and `resolveCurrentCompanyCode()`.

## Domain Entities

| Entity | Purpose |
| --- | --- |
| `Company` | Tenant entity with code, name, lifecycle state, enabled modules, quotas |
| `CompanyLifecycleState` | Enum: `ACTIVE`, `SUSPENDED`, `DEACTIVATED` |
| `CompanyModule` | Enum: core and gatable module definitions with default enabled sets |
| `CoATemplate` | Chart-of-accounts template for tenant onboarding |
| `TenantAdminEmailChangeRequest` | Tracks pending admin email change requests with verification tokens |
| `TenantSupportWarning` | Support warning records issued against tenants |

## Company Context in the Request Pipeline

Company context is set by `CompanyContextFilter` (in `core/security/`, documented in [auth.md](auth.md)):

1. JWT `companyCode` claim is extracted by `JwtAuthenticationFilter`.
2. `CompanyContextFilter` validates the claim against any `X-Company-Code` header, rejects retired `X-Company-Id` headers, resolves the company lifecycle state, enforces lifecycle restrictions, runs tenant runtime admission, and sets `CompanyContextHolder`.
3. Downstream services use `CompanyContextHolder.getCompanyCode()` or `CompanyContextService` to access tenant context.

### Super-Admin Platform Scope

Super-admin users authenticate with the platform scope code (default: `PLATFORM`, configurable via `auth.platform.code` system setting). They are restricted to:

- `/api/v1/superadmin/**` — control-plane operations.
- `/api/v1/auth/**` — auth endpoints.
- `/api/v1/companies` — company listing.
- `/api/v1/superadmin/settings` — global settings.

Super-admin users are **explicitly blocked** from tenant business endpoints (sales, inventory, factory, purchasing, HR, portal, dealer-portal, etc.) by `CompanyContextFilter`.

### V1 Add Client / Activation Boundary

Tenant creation uses the Add Client + activation workflow: Super Admin prepares a draft or pending tenant, activation handles owner credential setup, and setup finishes through the V1 owner setup corridor. The historical flat onboarding and platform-issued support password-reset URLs are not mapped in the current API contract; stale clients receive `404` or `405` depending on whether the removed URL collides with a surviving route template.

The current V1 status vocabulary separates tenant lifecycle, activation,
onboarding, billing, and suspension state. Super Admin summaries may show
`DRAFT`, `PENDING_ACTIVATION`, `SETUP_PENDING`, `TRIAL_ACTIVE`, `ACTIVE`,
`GRACE`, `SUSPENDED_READ_ONLY`, `SUSPENDED_BLOCKED`, `CANCELED`, `ARCHIVED`,
and `SEED_FAILED` as documented list/profile states. Owner setup uses only
company details, GST, accounting defaults, optional team invite, and finish;
location setup is outside this V1 corridor.

Super Admin summaries must stay privacy-safe. They may expose tenant identity
markers, owner contact markers, plan/billing/usage summaries, support/SLA/bug
metadata, seed readiness, trace IDs, and audit IDs. They must not expose tenant
invoices, ledger lines, inventory rows, salaries, vendors/customers, files, GST
returns, request/response bodies, raw logs, token values, password hashes,
provider credentials, or `.env` values.

## Cross-Module Boundaries

| Boundary | Direction | Description |
| --- | --- | --- |
| company → auth | dependency | Super-admin control plane calls `TokenBlacklistService` and `RefreshTokenService` for force-logout |
| company → auth | dependency | `TenantRuntimeEnforcementService` is called by `AuthService` for login/refresh admission |
| company → auth | dependency | Add Client and owner setup use canonical auth/account services for activation, scoped account setup, session revocation, and password recovery |
| company → accounting | dependency | `TenantDefaultSeedingService` and setup mappings seed and repair accounting defaults through the current seed-status corridor |
| company → core/security | dependency | `CompanyContextFilter` enforces lifecycle and runtime admission |
| company → core/config | dependency | `TenantRuntimeEnforcementService` persists policies via `SystemSettingsRepository` |

## Known Caveats

1. **Runtime counters are in-memory**: in-flight and rate-limit counters (`ConcurrentHashMap` + `AtomicInteger`/`AtomicLong`) do not survive restarts or share across instances. The policy itself (state, quotas) is persisted to `system_settings`.
2. **Policy cache TTL**: runtime policies are cached for 15 seconds (configurable via `erp.tenant.runtime.policy-cache-seconds`). Changes to quotas take up to 15 seconds to propagate, unless the cache is explicitly invalidated (which happens automatically when a super-admin updates limits through the control-plane API).
3. **Tenant deletion is blocked**: the `DELETE /api/v1/companies/{id}` endpoint exists but always throws `AccessDeniedException`. Companies cannot be deleted through the API.
4. **HR_PAYROLL defaults to disabled**: unlike other gatable modules, HR/Payroll must be explicitly enabled per tenant during or after onboarding.
5. **Tenant runtime hold/blocked states are orthogonal to lifecycle states**: a tenant can be `ACTIVE` in lifecycle but `HOLD` or `BLOCKED` in runtime. Both layers are checked independently by `CompanyContextFilter`.
6. **Graceful degradation during policy persistence outage**: if the `system_settings` table is unavailable during a cache refresh, the last-known in-memory policy is kept rather than blocking all tenant requests. This is intentional to maintain availability.

## Cross-References

- [docs/modules/auth.md](auth.md) — auth module (login, refresh, logout, MFA, token revocation, security filters)
- [docs/BACKEND-FEATURE-CATALOG.md](../BACKEND-FEATURE-CATALOG.md) — backend feature catalog
- [docs/adrs/ADR-002-multi-tenant-auth-scoping.md](../adrs/ADR-002-multi-tenant-auth-scoping.md) — ADR for multi-tenant auth scoping
- [docs/ARCHITECTURE.md](../ARCHITECTURE.md) — overall architecture reference
- [docs/SECURITY.md](../SECURITY.md) — security controls
- [docs/RELIABILITY.md](../RELIABILITY.md) — reliability posture
- [docs/adrs/ADR-006-portal-and-host-boundary-separation.md](../adrs/ADR-006-portal-and-host-boundary-separation.md) — portal/host boundary ADR
- [docs/BACKEND-FEATURE-CATALOG.md](../BACKEND-FEATURE-CATALOG.md) — backend feature catalog
- [docs/flows/auth-identity.md](../flows/auth-identity.md) — canonical auth/identity flow (behavioral entrypoint)
- [docs/flows/tenant-admin-management.md](../flows/tenant-admin-management.md) — canonical tenant/admin management flow (behavioral entrypoint)
