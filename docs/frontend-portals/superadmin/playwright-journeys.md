# Playwright Journeys

Last reviewed: 2026-04-30

The repository has no native frontend app today. These journeys describe future frontend smoke coverage that must drive the documented backend contract and keep evidence secret-free.

## Journey 1: Add Client as draft

1. Log in as a platform Super Admin.
2. Visit `/platform/clients/new`.
3. Wait for `GET /api/v1/superadmin/tenants/new` and `GET /api/v1/superadmin/tenants/coa-templates`.
4. Fill company, owner, commercial, quota, module, and support fields only.
5. Submit with `createMode=DRAFT` to `POST /api/v1/superadmin/tenants`.
6. Assert response `data.status === "DRAFT"`, activation status is not sent, and `auditEventId` is present.
7. Assert no activation link, token, or password value is visible.

## Journey 2: Send activation and owner setup

1. Open `/platform/clients/:tenantId/onboarding`.
2. Trigger `POST /api/v1/superadmin/tenants/{id}/activation/send`.
3. Assert pending activation metadata and a redacted email/message marker.
4. In the tenant owner setup shell, verify and complete activation with redacted token/password inputs.
5. Complete company details, GST, accounting, optional invite-team skip, and finish setup.
6. Assert setup status is trial/active and no location setup fields were requested.

## Journey 3: Rate-limit and validation errors

1. Open a list route such as `/platform/clients`.
2. Submit unsupported filter, invalid enum, invalid sort, and oversize page probes.
3. Assert each failure uses the standard safe envelope with `data.code`, `data.path`, and `data.traceId`.
4. Burst a low-risk read route in a validation fixture until `429` and assert retry/reset metadata is shown without duplicate side effects.

## Journey 4: Support, SLA, and Sentry

1. Open `/platform/support` and filter by status/SLA state.
2. Open a ticket, post a customer-visible reply, post an internal note, and change status.
3. Assert internal note is platform-only and timeline order is stable.
4. Link a Sentry issue by issue id and sync it.
5. Assert UI shows only safe issue id/URL/status/timestamps/error summary and no credentials.

## Journey 5: Billing, lifecycle, and audit

1. Open `/platform/clients/:tenantId/billing`.
2. Create subscription, invoice, payment, adjustment, grace, suspension, resume, cancel, and archive actions in isolated validation data.
3. Assert each accepted mutation returns a trace ID and appears in the platform audit tab.
4. Assert tenant-private invoices, ledgers, GST returns, files, and payroll data never render in the Super Admin shell.
