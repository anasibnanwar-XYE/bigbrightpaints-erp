# Admin + Super Admin Portal Endpoint Map (V1)

Source of truth:
- `openapi.json`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java`

This doc is the implementation packet for two surfaces:
1. `ADMIN` (tenant admin portal)
2. `SUPER_ADMIN_CONSOLE` (platform control plane, separate login + JSON-rendered UI)

## 1) Shared Foundation Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/auth/me` | session and authority bootstrap |
| POST | `/api/v1/auth/login` | sign in |
| POST | `/api/v1/auth/logout` | sign out |
| GET | `/api/v1/auth/profile` | profile read |
| PUT | `/api/v1/auth/profile` | profile update |
| POST | `/api/v1/auth/password/change` | password rotation |
| GET | `/api/v1/companies` | available company scopes |
| POST | `/api/v1/multi-company/companies/switch` | active company switch |

## 2) Tenant Admin (`ADMIN`) Endpoint Packet

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/admin/users` | list in-tenant users |
| POST | `/api/v1/admin/users` | create in-tenant user |
| PUT | `/api/v1/admin/users/{id}` | update in-tenant user |
| DELETE | `/api/v1/admin/users/{id}` | remove user |
| PATCH | `/api/v1/admin/users/{id}/suspend` | suspend account |
| PATCH | `/api/v1/admin/users/{id}/unsuspend` | unsuspend account |
| PATCH | `/api/v1/admin/users/{id}/mfa/disable` | force MFA reset |
| GET | `/api/v1/admin/roles` | role listing |
| POST | `/api/v1/admin/roles` | role create |
| GET | `/api/v1/admin/roles/{roleKey}` | role detail |
| GET | `/api/v1/admin/approvals` | approval queue |
| GET | `/api/v1/admin/settings` | settings read |
| PUT | `/api/v1/admin/settings` | settings update |
| POST | `/api/v1/admin/notify` | controlled notifications |

## 3) Super Admin (`SUPER_ADMIN_CONSOLE`) Endpoint Packet

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/companies` | tenant list |
| POST | `/api/v1/companies` | tenant bootstrap/create |
| PUT | `/api/v1/companies/{id}` | tenant quota/config update |
| POST | `/api/v1/companies/{id}/lifecycle-state` | tenant hold/block/active state changes with reason |
| GET | `/api/v1/companies/{id}/tenant-metrics` | tenant metrics, usage, quota envelope |

OpenAPI parity note:
- `/api/v1/companies/{id}/lifecycle-state` and `/api/v1/companies/{id}/tenant-metrics` are runtime controller endpoints and must be present in OpenAPI before v1 release closure.

## 4) Required Role Matrix (Fail-Closed)

| Action Domain | Required Authority | Deny Behavior |
|---|---|---|
| tenant user/role/settings ops | `ROLE_ADMIN` | `403` with no side effects |
| tenant bootstrap/lifecycle/quota controls | `ROLE_SUPER_ADMIN` | `403` + audit trail |
| cross-tenant access attempts | super-admin only with explicit tenant context | deny by default |

## 5) UX Workflow Contracts

### 5.1 Admin user lifecycle
1. Load `/auth/me` and authority claims.
2. Load `/admin/users`.
3. Execute create/update/suspend actions with confirmation.
4. Refresh list and show immutable action result.

### 5.2 Super admin tenant lifecycle
1. Load tenants via `/companies`.
2. Select tenant and fetch `/companies/{id}/tenant-metrics`.
3. Apply lifecycle change with mandatory reason.
4. Re-fetch metrics; show updated state and quota fields.

## 6) Error and Recovery UX
- `401`: force relogin.
- `403`: show authority boundary message; no retry loop.
- `409`: show conflict details; allow refresh and retry.
- `422`: inline field errors and request schema hints.

## 7) Frontend Implementation Checklist
- Use explicit portal split: `ADMIN` vs `SUPER_ADMIN_CONSOLE`.
- Hide super-admin controls unless `ROLE_SUPER_ADMIN`.
- Require reason input for lifecycle transitions.
- Always re-fetch tenant metrics after lifecycle/quota writes.
- Treat missing OpenAPI schema for runtime endpoint as release blocker.
