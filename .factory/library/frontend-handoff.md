# Frontend Handoff

Last reviewed: 2026-05-01

This `.factory` handoff is a short current-state pointer for workers. It is not
the canonical API contract. Use the generated `openapi.json` and the docs under
`docs/frontend-portals/**` and `docs/frontend-api/**` as the source of truth.

## Canonical Docs

- Super Admin portal: `docs/frontend-portals/superadmin/README.md`
- Super Admin API contracts: `docs/frontend-portals/superadmin/api-contracts.md`
- Super Admin route map: `docs/frontend-portals/superadmin/routes.md`
- Super Admin workflows: `docs/frontend-portals/superadmin/workflows.md`
- Super Admin states and errors: `docs/frontend-portals/superadmin/states-and-errors.md`
- Super Admin role boundaries: `docs/frontend-portals/superadmin/role-boundaries.md`
- Super Admin Playwright journeys: `docs/frontend-portals/superadmin/playwright-journeys.md`
- Tenant-admin IAM handoff: `docs/frontend-portals/tenant-admin/identity-iam-handoff-2026-04-30.md`
- Shared frontend API rules: `docs/frontend-api/README.md`
- Endpoint inventory: `docs/endpoint-inventory.md`
- Deprecated surfaces registry: `docs/deprecated/INDEX.md`

## PR198 Current Backend State

- PR #198 is based on PR #197 IAM/auth mainline.
- Frontend identity bootstrap is `GET /api/v1/auth/me`.
- Self-profile, contact, security, MFA, and session management use the canonical
  `/api/v1/auth/me/**`, `/api/v1/auth/mfa/**`, and `/api/v1/auth/sessions/**`
  contracts documented in the tenant-admin IAM handoff.
- Super Admin control-plane users are platform-scoped `ROLE_SUPER_ADMIN` actors.
  The platform shell must not call tenant business routes.
- Add Client uses `GET /api/v1/superadmin/tenants/new`,
  `GET /api/v1/superadmin/tenants/coa-templates`, and
  `POST /api/v1/superadmin/tenants`.
- Owner activation uses `GET /api/v1/auth/activation/verify` and
  `POST /api/v1/auth/activation/complete`.
- Owner setup uses the setup corridor routes:
  `GET /api/v1/setup/status`, `PUT /api/v1/setup/company-details`,
  `PUT /api/v1/setup/gst`, `PUT /api/v1/setup/accounting`,
  `POST /api/v1/setup/invite-team`, and `POST /api/v1/setup/finish`.
- Super Admin tenant detail, lifecycle, plan, entitlement, quota, usage,
  billing, support, audit, infra, settings, profile, and changelog screens are
  documented in `docs/frontend-portals/superadmin/**`.

## Retired Surfaces

Do not build new UI against these routes or payload shapes:

- `POST /api/v1/superadmin/tenants/onboard`
- `POST /api/v1/companies/{id}/support/admin-password-reset`
- `/api/v1/auth/profile`
- `/api/v1/auth/password/forgot/superadmin`
- `/api/v1/superadmin/plan-templates/**`
- tenant-switch/session aliases from the pre-IAM auth era
- plaintext temporary password responses
- raw reset or activation token persistence contracts
- `X-Company-Id` based frontend tenant switching

Retired routes should fail closed or be absent from `openapi.json`. If a stale
test fixture, old validation report, or historical `.factory/validation/**`
artifact disagrees with the canonical docs above, treat the historical artifact
as non-authoritative and update the current docs or code rather than adding a
compatibility bridge.

## Validation Handoff

For this PR, durable evidence lives in `docs/approvals/R2-CHECKPOINT.md`.
Use the PR checks on the current head for live CI status.

Before handing frontend work to another agent or team, verify:

- `openapi.json` contains the route being wired.
- `docs/frontend-portals/superadmin/api-contracts.md` describes the request,
  response, auth, and error behavior.
- `docs/frontend-portals/superadmin/routes.md` maps the route to a UI screen.
- `docs/frontend-portals/superadmin/states-and-errors.md` covers the failure
  state the UI needs.
- no current handoff points at the retired surfaces listed above.

No backwards-compatibility shim, fallback path, or alternate historical route is
approved by this handoff.
