# Role Boundaries

Last reviewed: 2026-04-30

## Access model

- Required backend authority: `ROLE_SUPER_ADMIN`.
- Required frontend shell: platform-only shell.
- Platform scope comes from auth identity, not from a tenant header.

## Hard boundaries

- A Super Admin must not see tenant-admin navigation, approval inboxes, tenant user CRUD, accounting, sales, factory, HR, inventory, purchasing, portal, or dealer workflows.
- A Super Admin must not be routed into `/tenant/*`, `/accounting/*`, `/sales/*`, `/factory/*`, `/hr/*`, `/portal/*`, or `/dealer/*`.
- A Super Admin token must not call tenant-admin workflow prefixes such as `/api/v1/admin/**` except documented platform-safe reads if they exist.
- `X-Company-Code` and `X-Company-Id` must not grant tenant workflow access or alter platform scope.

## Allowed data

Allowed Super Admin data is bounded operational metadata: tenant/client identity summary, owner contact marker, status, plan, billing summary, support tier, setup/activation state, aggregate usage, quota state, support/bug metadata, audit/security event IDs, infra health, cost estimates, trace IDs, and redacted provider status.

## Forbidden data

Do not render tenant invoices, journal/ledger line items, inventory/batch rows, employee salary/payroll records, vendor/customer names, file contents, GST returns, request/response bodies, raw logs, activation/reset token values, password hashes, JWTs, Sentry/Datadog credentials, SMTP credentials, database secrets, or `.env` values.

## UI implications

- Hide tenant business tabs entirely, not just disable them.
- Render tenant mutations only when both the role guard and response `availableActions` allow them.
- Treat support, bug, audit, and observability text as sanitized summaries; never display raw private canaries or provider credentials.
- Copy buttons may copy trace IDs, audit IDs, message IDs, or redacted activation URL shapes only.
