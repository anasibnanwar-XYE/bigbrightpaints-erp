# Workflows

Last reviewed: 2026-04-30

## Add Client

1. Load `GET /api/v1/superadmin/tenants/new` before enabling submit.
2. Collect only `company`, `owner`, `commercial`, `quotas`, `modules`, `support`, and `createMode`.
3. Submit `POST /api/v1/superadmin/tenants`.
4. Treat success as complete only when response `data.tenantId`, `data.status`, `data.activation`, and `data.auditEventId` are present.
5. `createMode=DRAFT` must not show an email-sent state. `createMode=SEND_ACTIVATION` must show pending activation metadata and a redacted MailHog/message marker in validation environments.
6. Redirect to `/platform/clients/:tenantId` using the numeric tenant id from the Super Admin response.

## Activation actions

1. Use tenant detail and seed status to decide whether activation actions are available.
2. `POST /api/v1/superadmin/tenants/{id}/activation/send`, `resend`, `copy`, and `expire` are explicit operator actions.
3. Copy-link is the only API action that may return a link, and UI/evidence must render it as `<ACTIVATION_URL_REDACTED>`.
4. Send/resend proof uses email message id/subject only; never display token values or plaintext credentials.
5. After every action, refresh tenant detail and audit tab.

## Owner setup corridor

1. Owner verifies activation through `GET /api/v1/auth/activation/verify?token=<REDACTED>`.
2. Owner completes activation with `POST /api/v1/auth/activation/complete` using token and password fields; frontend never logs either value.
3. Owner completes setup in order: status, company details, GST, accounting, optional invite team, finish.
4. Setup payloads do not include location setup fields; V1 setup is company/accounting/GST/team only.
5. Finish is idempotent and moves the tenant to trial/active according to commercial state.

## Plan, entitlements, usage, and quotas

1. Load plan templates and tenant entitlements before showing plan/limit forms.
2. Plan assignment uses `PUT /api/v1/superadmin/tenants/{id}/plan` and may include `planId`, `customPlan`, `reason`, and `repriceSubscription`.
3. Entitlement overrides use `PUT /api/v1/superadmin/tenants/{id}/entitlements/overrides`; removal uses `DELETE /api/v1/superadmin/tenants/{id}/entitlements/overrides/{key}` with a reason body when available.
4. Usage pages distinguish monthly quota, burst rate limit, concurrent requests, rejected requests, jobs, PDFs, emails, users, and storage.
5. `429` means rate or quota throttling; use retry/reset metadata when present and avoid duplicate mutation retries.

## Billing and lifecycle

1. Create/read subscription through `/billing/subscription` before posting ledger entries.
2. Manual invoices/payments use `LedgerEntryRequest` with amount, currency, idempotency key, and reason.
3. Adjustments use direction, amount, currency, idempotency key, and reason.
4. Grace, read-only suspension, blocked suspension, resume, cancel, and archive use explicit reason payloads and write audit evidence.
5. After mutation, refresh tenant detail, billing ledger, billing metrics, and audit tab.

## Support, SLA, bugs, and Sentry

1. Queue filters use `status`, `category`, `slaStatus`, `q`, `page`, `size`, and `sort`.
2. Ticket detail loads safe tenant summary, status, SLA, messages, timeline, bug/feature classification, and Sentry status.
3. Customer-visible replies use `/messages`; internal notes use `/internal-notes` and must remain platform-only.
4. Status changes and feature-to-incident conversion require explicit reason when the backend asks for it.
5. Sentry link accepts an issue id, not an arbitrary URL or client-supplied credentials. Sync returns safe issue status/timestamps/errors only.

## Audit, security, infra, and observability

1. Platform/security/suspicious event lists preserve filters in the URL.
2. Suspicious-event acknowledge/resolve/reopen actions require a remediation reason and refresh the dashboard/security counters.
3. Infra health shows redacted component status only.
4. Cost snapshots validate component, source, amount, currency, period, and reason; corrections are read-only history.
5. Datadog status and Sentry links are server-side wrappers. UI must never ask for or display provider credentials.
