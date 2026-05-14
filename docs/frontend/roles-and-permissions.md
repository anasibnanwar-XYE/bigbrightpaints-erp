# Frontend Roles And Permissions

Last reviewed: 2026-05-09

This is the role map frontend teams should use when building portals. The
deeper portal folders remain the screen-level source of truth:
`docs/frontend-portals/**`.

## Shared Session Rules

- Bootstrap every shell with `GET /api/v1/auth/me`.
- Use `companyCode` from the bootstrap response for tenant-scoped shells.
- Send tenant scope as `X-Company-Code`; do not send `X-Company-Id`.
- If `mustChangePassword=true`, route to password change before any normal
  shell screen.
- Do not derive portal access from route names alone. Use authorities returned
  by auth bootstrap.

## Role Matrix

| Role | Portal | Can see and do | Must not do |
| --- | --- | --- | --- |
| `ROLE_SUPER_ADMIN` | `superadmin` | Create and operate tenants, choose COA templates, send/copy/expire activation, manage plans, limits, modules, platform support context, platform changelog, platform audit and security review surfaces | Enter tenant-admin product screens, mutate tenant business journals, execute production/dispatch, act as a dealer |
| `ROLE_ADMIN` | `tenant-admin` | Tenant dashboard, user lifecycle for non-admin roles, export approvals, support tickets, tenant audit feed, tenant settings, tenant changelog | Create superadmin tenants, assign `ROLE_ADMIN` or `ROLE_SUPER_ADMIN`, post accounting journals, execute factory dispatch |
| `ROLE_ACCOUNTING` | `accounting` | COA, default accounts, tax setup, journal entries, reversals, reconciliation, period close request/reopen, opening stock, AR/AP settlement, reports | Tenant onboarding, user lifecycle, dispatch confirm, dealer self-service |
| `ROLE_SALES` | `sales` | Dealer master, sales orders, credit checks, credit override requests, order timeline, dispatch/invoice read-only follow-up | Dispatch confirm, manual journals/reversals, period close, admin approvals |
| `ROLE_FACTORY` | `factory` | Production logs, packing records, packaging mappings, batch lineage, dispatch queue, dispatch preview, dispatch confirm | COA/default accounts/tax setup, tenant administration, accounting correction |
| `ROLE_DEALER` | `dealer-client` | Dealer dashboard, own orders, own invoices, own ledger/aging, own support tickets, own credit-limit requests | Internal order edits, accounting writes, factory execution, admin approvals |

## Portal Entry Points

| Portal | Route base | Backend ownership |
| --- | --- | --- |
| Superadmin | `/platform/*` | `/api/v1/superadmin/**` plus activation/setup handoff routes |
| Tenant Admin | `/tenant/*` | `/api/v1/admin/**`, `/api/v1/changelog`, auth self-service |
| Accounting | `/accounting/*` | `/api/v1/accounting/**`, finance report reads, accounting support workflows |
| Sales | `/sales/*` | `/api/v1/sales/**`, `/api/v1/dealers/**`, `/api/v1/credit/**` |
| Factory | `/factory/*` | `/api/v1/factory/**`, dispatch operational endpoints |
| Dealer Client | `/dealer/*` | `/api/v1/dealer-portal/**` |

## Permission UX Rules

- Hide actions the role cannot perform; do not show a disabled action as a
  promise that escalation will work unless the backend exposes an approval flow.
- Treat `401` as session failure and `403` as permission or tenant-scope
  failure.
- For tenant-admin user management, never expose `ROLE_ADMIN` or
  `ROLE_SUPER_ADMIN` as assignable tenant roles.
- For dealer-client, never ask the browser to supply `dealerId` for
  `/api/v1/dealer-portal/**`; backend resolves it from the authenticated dealer.

## Deeper Docs

- Portal ownership: `docs/frontend-portals/README.md`
- Shared API contract: `docs/frontend-api/README.md`
- Portal matrix: `docs/frontend-portals/portal-matrix.md`
