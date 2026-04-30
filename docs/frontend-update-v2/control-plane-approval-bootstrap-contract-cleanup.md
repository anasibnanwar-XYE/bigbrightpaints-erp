# Control-plane approval and bootstrap contract cleanup

This handoff covers the bounded ERP-19 contract cleanup that landed with the admin approval inbox and tenant onboarding packet.

## Included now

- `GET /api/v1/admin/approvals` remains the single tenant-scoped approval inbox.
- `AdminApprovalItemDto` now exposes typed `originType` and `ownerType` fields, and export approval rows retain machine-readable export detail.
- `GET /api/v1/admin/exports/pending` is retired.
- Historical note: the old flat Super Admin tenant onboarding route (`POST /api/v1/superadmin/tenants/onboard`) is retired and must not be used as a current bootstrap/create-tenant contract.
- `GET /api/v1/companies/superadmin/dashboard` is retired.
- `GET /api/v1/superadmin/dashboard` remains the live aggregate-count dashboard route.

## Approval inbox contract

Endpoint:
- `GET /api/v1/admin/approvals`

Changed payload fields for each approval item:
- added `originType`
- added `ownerType`
- removed `type`
- removed `sourcePortal`

Export approval rows (`originType=EXPORT_REQUEST`) additionally expose:
- `reportType` for all tenant-scoped inbox viewers
- `parameters`, `requesterUserId`, and `requesterEmail` only for tenant-admin viewers
- accounting viewers receive the export row with those sensitive fields redacted

Current emitted `originType` values:
- `CREDIT_REQUEST`
- `CREDIT_LIMIT_OVERRIDE_REQUEST`
- `PAYROLL_RUN`
- `PERIOD_CLOSE_REQUEST`
- `EXPORT_REQUEST`

Current emitted `ownerType` values:
- `SALES`
- `FACTORY`
- `HR`
- `ACCOUNTING`
- `REPORTS`

Stable approval item fields:
- `reference`
- `status`
- `summary`
- `createdAt`

Conditional action fields:
- tenant admin viewers keep `actionType`, `actionLabel`, `approveEndpoint`, and `rejectEndpoint` on export approval rows
- accounting-only viewers receive export approval rows with those action fields as `null`

Frontend action:
- switch approval queue rendering, filtering, and badge logic to `originType` and `ownerType`
- render export approval detail from `reportType`
- only render `parameters`, `requesterUserId`, and `requesterEmail` when the caller is a tenant admin
- treat `actionType`, `actionLabel`, `approveEndpoint`, and `rejectEndpoint` as nullable on accounting export rows; do not render export decision controls when they are `null`
- stop reading `type` and `sourcePortal`
- keep `GET /api/v1/admin/approvals` limited to tenant-scoped admin/accounting callers; do not point platform super-admin tooling at that prefix

## Historical tenant onboarding note

The legacy flat onboarding endpoint was part of the ERP-19 cleanup context only.
It is not a current frontend contract, is documented in the deprecated registry, and
must not be called by new Super Admin Add Client UI.

Current frontend action:
- use the canonical V1 Add Client + activation workflow from `docs/frontend-portals/superadmin/**`
  and `docs/frontend-api/**`
- remove any UI that displays, copies, or persists onboarding passwords
- do not depend on old bootstrap-status response fields from the retired flat route

## Retired control-plane aliases

- do not call `GET /api/v1/admin/exports/pending`
- do not call `GET /api/v1/companies/superadmin/dashboard`
- do not call `POST /api/v1/superadmin/tenants/onboard`; it is retired/historical, not a
  current tenant creation API
- use `GET /api/v1/admin/approvals` for tenant-scoped admin/accounting approval queue access only
- use `GET /api/v1/superadmin/dashboard` only for aggregate platform dashboard counts; it does not replace the retired detailed tenant payload

## Out of scope follow-up

The broader tenant route-family hard cut onto `/api/v1/superadmin/tenants/**` is still outside this packet and should be handled separately if it remains needed after fresh inspection.
