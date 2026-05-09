# Tenant / Admin Management Flow

Last reviewed: 2026-04-15

This document describes the canonical behavior for tenant and admin management after the tenant-admin hard-cut refactor.

## Product Boundary

### Tenant-admin product (in scope)

- Dashboard
- User management
- Approval inbox + decisions
- Tenant audit feed
- Internal support tickets (admin host)
- Self-settings read model
- Tenant changelog reads

### Control-plane (out of tenant-admin scope)

- V1 Add Client / activation tenant setup
- Tenant lifecycle transitions
- Module enable/disable
- Tenant limit and quota mutation
- Support warnings and context operations
- Platform changelog publishing
- Platform support/SLA/bug/Sentry, infra health/cost, usage/billing, audit,
  suspicious-event remediation, profile/session, settings, and observability
  status operations

All control-plane behavior stays under `/api/v1/superadmin/**`.
Control-plane responses are safe summaries only; they must not include tenant
business records, token values, provider credentials, or `.env` values.

## Actors

| Actor | Role | Scope |
| --- | --- | --- |
| Superadmin | `ROLE_SUPER_ADMIN` | Platform control plane only |
| Tenant admin | `ROLE_ADMIN` | Tenant-admin product workflows |
| Tenant user | non-admin role | Self-service module workflows |
| Accounting | `ROLE_ACCOUNTING` | Accounting/portal workflows, not tenant-admin shell |

## Entrypoints

### Superadmin control-plane entrypoints

- Shell/profile/settings: `/api/v1/superadmin/dashboard`,
  `/api/v1/superadmin/profile/**`, `/api/v1/superadmin/settings`,
  `/api/v1/superadmin/roles/**`, `/api/v1/superadmin/changelog/**`, and
  `/api/v1/superadmin/notify`.
- Add Client and activation:
  `GET /api/v1/superadmin/tenants/new`,
  `GET /api/v1/superadmin/tenants/coa-templates`,
  `POST /api/v1/superadmin/tenants`,
  `/api/v1/superadmin/tenants/{id}/activation/{send,resend,copy,expire}`,
  plus owner handoff routes `/api/v1/auth/activation/**` and `/api/v1/setup/**`.
- Tenant profile, plans, quotas, usage, billing, and lifecycle:
  `/api/v1/superadmin/tenants`,
  `/api/v1/superadmin/tenants/{id}`,
  `/api/v1/superadmin/tenants/{id}/plan`,
  `/api/v1/superadmin/tenants/{id}/entitlements/**`,
  `/api/v1/superadmin/tenants/{id}/usage/**`,
  `/api/v1/superadmin/tenants/{id}/billing/**`,
  `PUT /api/v1/superadmin/tenants/{id}/lifecycle`,
  `/api/v1/superadmin/tenants/{id}/suspension/**`,
  `/api/v1/superadmin/tenants/{id}/resume`,
  `/api/v1/superadmin/tenants/{id}/cancel`,
  `/api/v1/superadmin/tenants/{id}/archive`,
  `/api/v1/superadmin/tenants/{id}/commercial-state`,
  `PUT /api/v1/superadmin/tenants/{id}/limits`,
  `/api/v1/superadmin/tenants/{id}/modules`,
  `/api/v1/superadmin/tenants/{id}/quota-check`, and
  `/api/v1/superadmin/tenants/{id}/quota-policy`.
- Support, SLA, bugs, observability, infra, and audit:
  `/api/v1/superadmin/support/tickets/**`,
  `/api/v1/superadmin/observability/datadog/status`,
  `/api/v1/superadmin/infra/**`, and `/api/v1/superadmin/audit/**`.

The executable route/payload/error checklist is
[`docs/frontend-portals/superadmin/api-contracts.md`](../frontend-portals/superadmin/api-contracts.md).
All list routes validate unknown filters, enum values, sort fields, page/size,
and forbidden body fields explicitly instead of ignoring stale frontend state.
Every accepted mutation must produce privacy-safe audit evidence with a
trace/correlation ID.

### Removed Superadmin routes

Historical flat onboarding and platform-issued support password-reset URLs are not active workflows and are not mapped in the current OpenAPI/frontend contract. Stale clients receive `404` or `405` depending on whether the removed URL collides with a surviving route template. Use V1 Add Client + activation for tenant creation and activation/password recovery for credential setup.

### Tenant-admin product entrypoints

| Workflow | Method + path |
| --- | --- |
| Dashboard | `GET /api/v1/admin/dashboard` |
| User list/create | `GET, POST /api/v1/admin/users` |
| User detail/update | `GET, PUT /api/v1/admin/users/{id}` |
| User status | `PUT /api/v1/admin/users/{userId}/status` |
| User lock/unlock | `POST /api/v1/admin/users/{userId}/lock`, `POST /api/v1/admin/users/{userId}/unlock` |
| User sessions revoke | `DELETE /api/v1/admin/users/{userId}/sessions` |
| User MFA disable | `PATCH /api/v1/admin/users/{id}/mfa/disable` |
| User reset link | `POST /api/v1/admin/users/{userId}/force-reset-password` |
| Approval inbox | `GET /api/v1/admin/approvals` |
| Approval decision | `POST /api/v1/admin/approvals/{originType}/{id}/decisions` |
| Audit feed | `GET /api/v1/admin/audit/events` |
| Internal support | `POST, GET /api/v1/admin/support/tickets`, `GET /api/v1/admin/support/tickets/{ticketId}` |
| Self settings | `GET /api/v1/admin/self/settings` |
| Tenant changelog | `GET /api/v1/changelog`, `GET /api/v1/changelog/latest-highlighted` |

## Preconditions

### Tenant-admin session preconditions

1. Caller must authenticate and pass `GET /api/v1/auth/me`.
2. Tenant-scoped requests must use `X-Company-Code`.
3. If `mustChangePassword=true`, user must complete `POST /api/v1/auth/password/change` before normal shell routes.

### Tenant-admin user-management preconditions

1. Caller must be `ROLE_ADMIN` in the active tenant.
2. Target user must be in the same tenant scope.
3. Assignable roles are fixed to:
   - `ROLE_ACCOUNTING`
   - `ROLE_FACTORY`
   - `ROLE_SALES`
   - `ROLE_DEALER`
4. Unknown/blank/custom roles are rejected.

### Tenant-admin approval preconditions

1. Caller must be tenant admin (`ROLE_ADMIN`).
2. `originType` must be one of:
   - `EXPORT_REQUEST`
   - `CREDIT_REQUEST`
   - `CREDIT_LIMIT_OVERRIDE_REQUEST`
   - `PAYROLL_RUN`
   - `PERIOD_CLOSE_REQUEST`
3. Decision request must include `decision=APPROVE|REJECT`.
4. Decision semantics are origin-specific:
   - `CREDIT_REQUEST`: reason is required for approve/reject.
   - `CREDIT_LIMIT_OVERRIDE_REQUEST`: reason is required; `expiresAt` may be required by workflow policy.
   - `PAYROLL_RUN`: only approve is supported; reject fails validation.
   - `PERIOD_CLOSE_REQUEST`: reason is required for approve/reject; workflow force posture remains request-owned.

## Lifecycle Flows

### 1) Session bootstrap and corridor

```text
GET /api/v1/auth/me
  -> mustChangePassword=true ? force password-change corridor : proceed to tenant-admin shell
```

### 2) Dashboard

```text
GET /api/v1/admin/dashboard
  -> returns activity + approval + user + support + runtime + security summary
```

### 3) User lifecycle

```text
Create -> validate fixed role set -> provision scoped account -> optional dealer provisioning for ROLE_DEALER
Update -> validate fixed role set -> apply display/role changes -> revoke tokens when role set changes
Status disable, lock/unlock, session revoke, and MFA disable -> scoped target checks -> revoke sessions/tokens where required -> audit
Force reset link -> scoped target checks -> password reset token + mail dispatch -> audit
```

### 4) Approval lifecycle

```text
GET /api/v1/admin/approvals
  -> normalized item list + pendingCount
POST /api/v1/admin/approvals/{originType}/{id}/decisions
  -> domain-delegated decision with origin-specific validation rules
  -> returns normalized updated item
```

### 5) Internal support lifecycle

```text
POST /api/v1/admin/support/tickets
  -> ticket created in tenant scope
GET /api/v1/admin/support/tickets
GET /api/v1/admin/support/tickets/{ticketId}
  -> list/detail and sync state visibility
```

### 6) Self settings lifecycle

```text
GET /api/v1/admin/self/settings
  -> identity + MFA + mustChangePassword + role list + runtime metrics + active session estimate
```

## Current Boundaries

| Boundary | Current contract |
| --- | --- |
| Approval decisions | `POST /api/v1/admin/approvals/{originType}/{id}/decisions` |
| Tenant-admin support | `/api/v1/admin/support/tickets/**` |
| Role assignment | fixed role validation in `/api/v1/admin/users/**` |

## Completion Boundary

Flow is complete when:

1. Superadmin control-plane actions remain isolated to `/api/v1/superadmin/**`.
2. Tenant-admin primary screens run on canonical admin product surfaces (`/api/v1/admin/**`).
3. Tenant-admin approval actions happen only through generic admin decisions endpoint with origin-specific decision constraints.
4. Tenant-admin support runs on admin host, not portal host.
5. User role assignment remains fixed-list and escalation-proof.
6. Tenant-admin self-settings use `/api/v1/admin/self/settings` + auth-owned self-service flows.

## Implementation Plan (Executed Slices)

This is the canonical implementation order and status for the tenant-admin hard-cut refactor.

### Slice 1: Contract inventory + hard-cut removals (complete)

- Tenant-admin product ownership moved to canonical `/api/v1/admin/**` surfaces.
- Tenant-admin role creation/custom-role dependency removed from product contract.
- Portal-hosted tenant-admin support ownership retired in favor of admin host.
- Retired route boundaries aligned in openapi endpoint contract + portal docs.

### Slice 2: User management rewrite (complete)

- `AdminUserController` + `AdminUserService` now enforce fixed assignable roles only.
- Tenant-admin user list/detail/mutations mask privileged identities (`ROLE_ADMIN`, `ROLE_SUPER_ADMIN`) as not found.
- User lifecycle actions remain tenant-scoped and audit-emitting.

### Slice 3: Approval system rewrite (complete)

- Canonical approval inbox contract: `GET /api/v1/admin/approvals`.
- Canonical generic decision contract: `POST /api/v1/admin/approvals/{originType}/{id}/decisions`.
- Origin-specific decision constraints enforced in admin orchestration:
  - credit and credit-override require nonblank reason
  - payroll reject blocked (approve-only)
  - period-close force posture preserved

### Slice 4: Dashboard read model (complete)

- Canonical dashboard endpoint: `GET /api/v1/admin/dashboard`.
- Aggregates approval, user, support, runtime, security, and recent activity summaries.
- User-summary and activity rows now follow tenant-admin visibility masking for privileged identities (`ROLE_ADMIN`, `ROLE_SUPER_ADMIN`).
- No quota/runtime mutation behavior is exposed from dashboard.

### Slice 5: Support rewrite (complete)

- Tenant-admin internal support is owned by `/api/v1/admin/support/tickets/**`.
- Portal support host remains accounting-owned and out of tenant-admin ownership.
- Sync state visibility remains scoped to support DTOs only.

### Slice 6: Settings/self-service rewrite (complete)

- Canonical tenant-admin settings payload: `GET /api/v1/admin/self/settings`.
- Self security and password/MFA flows remain auth-owned (`/api/v1/auth/**`).
- Utility notify action moved to superadmin control-plane host (`POST /api/v1/superadmin/notify`).

### Slice 7: Documentation realignment (complete)

- Updated docs:
  - `docs/modules/admin-portal-rbac.md`
  - `docs/flows/tenant-admin-management.md`
  - `docs/frontend-portals/tenant-admin/api-contracts.md`
  - `docs/frontend-portals/tenant-admin/routes.md`
  - `docs/frontend-portals/tenant-admin/role-boundaries.md`
  - `docs/frontend-api/admin-role.md`
  - `docs/frontend-api/auth-and-company-scope.md`
  - `docs/openapi-endpoint-contract.md`

## Verification Contract

Run these from the repository root:

- `bash scripts/guard_openapi_contract_drift.sh`
- `bash scripts/guard_accounting_portal_scope_contract.sh`
- `bash ci/lint-knowledgebase.sh`
- `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AdminUserServiceTest,AdminApprovalServiceTest,AdminApprovalControllerContractTest test`
- Colima + curl contract checks:
  - tenant-admin user list excludes privileged targets
  - privileged-target and missing-target user mutations return the same masked not-found contract
  - credit-override generic decision rejects blank reason
  - payroll generic decision rejects `REJECT`

## Related Docs

- [docs/modules/admin-portal-rbac.md](../modules/admin-portal-rbac.md)
- [docs/frontend-portals/tenant-admin/api-contracts.md](../frontend-portals/tenant-admin/api-contracts.md)
- [docs/frontend-api/admin-role.md](../frontend-api/admin-role.md)
