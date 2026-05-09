# Frontend Portal Build Guide

Last reviewed: 2026-05-09

This is the first-stop build guide for frontend portal work. Use it to choose
the correct shell, role guard, backend prefix, and state model before opening
the deeper portal files.

## Shared Rules

- Bootstrap identity with `GET /api/v1/auth/me` only.
- Tenant shells persist `companyCode` and send `X-Company-Code` on tenant-scoped
  calls.
- Do not send `X-Company-Id`; the backend rejects the retired header.
- Route `mustChangePassword=true` users into password change before rendering a
  normal shell.
- Treat validation errors, forbidden body fields, unsupported filters, and
  removed routes as hard failures. Do not retry historical aliases.
- Use `Idempotency-Key` for write flows that require replay safety.

## Portal Personas

| Portal | Primary roles | What the user can do | Main backend prefixes | Must not build here |
| --- | --- | --- | --- | --- |
| Superadmin | `ROLE_SUPER_ADMIN` | tenant onboarding, activation, owner setup visibility, lifecycle, plans, entitlements, usage, billing, support, SLA, infra health, platform settings, role catalog, changelog | `/api/v1/superadmin/**` | tenant business records, accounting, sales, factory, dealer self-service, tenant user CRUD |
| Tenant admin | `ROLE_ADMIN` | tenant dashboard, users and access, approval inbox, audit feed, tenant support, self settings, changelog | `/api/v1/admin/**`, `/api/v1/changelog`, `GET /api/v1/auth/me` | superadmin control plane, accounting journals, factory execution, dealer self-service |
| Accounting | `ROLE_ACCOUNTING`, admin-read where documented | COA, default accounts, tax readiness, journals, reversals, reconciliation, period-close request, opening stock review, AR/AP settlement, reports, export requests | `/api/v1/accounting/**`, `/api/v1/reports/**`, `/api/v1/exports/**`, accounting-owned `/api/v1/portal/**` reads | tenant onboarding, tenant user lifecycle, dispatch confirm, export approval decisions |
| Sales | `ROLE_SALES` | dealer master, sales orders, credit-limit requests, credit overrides, commercial dashboards, dispatch-read handoff | `/api/v1/sales/**`, `/api/v1/dealers/**`, `/api/v1/credit/**` | dispatch confirm, production packing, accounting correction, dealer self-service |
| Factory | `ROLE_FACTORY` | production planning/logs, packing, packaging mappings, raw-material usage, batch lineage, dispatch execution | `/api/v1/factory/**`, dispatch operational endpoints | COA/default accounts/tax setup, settlements, tenant administration |
| Dealer client | `ROLE_DEALER` | own dashboard, orders, invoices, invoice PDF, ledger, aging, support tickets, credit-limit requests | `/api/v1/dealer-portal/**` | internal dealer editing, factory execution, accounting correction, admin approvals |

## Key State Transitions

- Add Client: superadmin creates draft or sends activation. Activation never
  returns raw passwords; frontend shows activation state and redacted mail
  evidence only.
- Owner setup: activation leads to ordered owner setup steps. Tenant workflow
  shells stay blocked while setup is required.
- Period close: accounting submits `request-close`; tenant-admin decides
  `PERIOD_CLOSE_REQUEST` through the generic approval endpoint; accounting then
  reflects `OPEN`, `LOCKED`, or `CLOSED` period state.
- Exports: accounting creates an export request; tenant-admin approves or
  rejects `EXPORT_REQUEST`; accounting attempts download only after approval.
- Dealer finance: internal finance reads require a `dealerId` query under
  `/api/v1/portal/finance/**`; dealer-client reads derive the dealer from the
  authenticated dealer session and must not send a dealer id.
- Dispatch: sales can prepare and observe commercial state; factory owns the
  operational confirmation action.

## Request Examples

Identity bootstrap:

```http
GET /api/v1/auth/me
Authorization: Bearer <ACCESS_TOKEN_REDACTED>
```

Tenant-scoped read:

```http
GET /api/v1/accounting/periods
Authorization: Bearer <ACCESS_TOKEN_REDACTED>
X-Company-Code: ACME
```

Tenant-admin approval decision:

```http
POST /api/v1/admin/approvals/PERIOD_CLOSE_REQUEST/77/decisions
Authorization: Bearer <ACCESS_TOKEN_REDACTED>
X-Company-Code: ACME
Content-Type: application/json

{"decision":"APPROVE","reason":"month close reviewed"}
```

Dealer-client credit request:

```http
POST /api/v1/dealer-portal/credit-limit-requests
Authorization: Bearer <ACCESS_TOKEN_REDACTED>
Content-Type: application/json

{"requestedAmount":25000.00,"reason":"seasonal order spike"}
```

## Intentionally Unsupported After Cleanup

- Deleted internal research/indexing docs are not product inputs for frontend
  work.
- No standalone physical-count or cycle-count portal workflow. Use inventory
  adjustments and reporting reconciliation screens.
- No direct `POST /api/v1/accounting/periods/{periodId}/close` frontend action.
- No export-specific approve/reject/status routes. Use tenant-admin generic
  approvals and the export download endpoint.
- No `POST /api/v1/invoices`; invoices are created by backend workflow side
  effects after fulfillment/dispatch.
- No dealer finance mutation or settlement actions in dealer-client.
- No tenant business workflows in superadmin.
- No role creation in tenant-admin.

## Where To Continue

1. Use [portal-matrix.md](./portal-matrix.md) to place the screen.
2. Open the target portal folder `README.md`.
3. Implement routes from `routes.md`.
4. Wire API calls from `api-contracts.md`.
5. Model workflows from `workflows.md`.
6. Implement failure and empty states from `states-and-errors.md`.
7. Turn `playwright-journeys.md` into browser coverage.
