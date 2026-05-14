# API Contracts

Last reviewed: 2026-04-30

## Shared transport rules

- The executable contract is `openapi.json`; this document is the frontend route/payload/error checklist for the Super Admin shell.
- All `/api/v1/superadmin/**` routes require `ROLE_SUPER_ADMIN` and return the standard `ApiResponse` envelope.
- Public activation routes and tenant-owner setup routes are listed here because Add Client hands off to them, but they are not Super Admin shell screens.
- Session bootstrap uses `GET /api/v1/auth/me` before entering the Super Admin shell.
- Control-plane routes target tenants by path id. Do not send `X-Company-Id`. Do not rely on `X-Company-Code` to switch tenants from the platform shell.
- All responses must expose or preserve `traceId`/correlation metadata. Frontend should capture it in error banners and support copy actions.
- Examples use placeholders only. Never log bearer tokens, passwords, activation/reset links, Sentry/Datadog credentials, SMTP credentials, database secrets, or `.env` values.

## Standard query and error contract

List routes use zero-based `page`, bounded `size`, and documented filters. Unsupported filters, invalid enum values, invalid sort fields, forbidden body fields, malformed JSON, unsupported media type, and oversize payloads fail explicitly with safe `400/405/406/413/415` envelopes; they are not silently ignored.

Common response statuses documented by OpenAPI for Super Admin operations: `200`/`201`, `400`, `401`, `403`, `404`, `405`, `406`, `409`, `413`, `415`, and `429`.

`429` means either platform/public-auth burst limiting, tenant API quota enforcement, or concurrent-request enforcement. Show retry/reset metadata when present, keep mutation buttons disabled until safe, and do not retry non-idempotent writes with a new intent.

## Redacted curl examples

```bash
curl -sS \
  -H 'Authorization: Bearer <ACCESS_TOKEN_REDACTED>' \
  -H 'X-Correlation-Id: docs-m15-example' \
  'http://localhost:8081/api/v1/superadmin/tenants?page=0&size=20&sort=companyName,asc'
```

```bash
curl -sS -X POST \
  -H 'Authorization: Bearer <ACCESS_TOKEN_REDACTED>' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"operator-approved validation fixture"}' \
  'http://localhost:8081/api/v1/superadmin/audit/suspicious-events/<EVENT_ID>/acknowledge'
```

Activation evidence may show `messageId=<MAILHOG_MESSAGE_ID>` and `activationUrl=<ACTIVATION_URL_REDACTED>` only.

## Route checklist

### Shell, profile, settings, roles, changelog

| Method | Path | Purpose | Request/query |
| --- | --- | --- | --- |
| GET | `/api/v1/superadmin/dashboard` | aggregate platform cards | none |
| GET, PUT | `/api/v1/superadmin/profile` | safe self profile read/update | update allows `displayName`, `phone`, `avatarUrl`, `timezone`, `language` |
| POST | `/api/v1/superadmin/profile/password` | password change | `currentPassword`, `newPassword`, `confirmPassword`; values redacted in logs/evidence |
| GET | `/api/v1/superadmin/profile/sessions` | safe session metadata | none |
| POST | `/api/v1/superadmin/profile/sessions/{sessionId}/revoke` | revoke session | path id only |
| GET, PUT | `/api/v1/superadmin/settings` | grouped platform settings | groups: `access`, `mail`, `workflow`; secrets are redacted |
| GET, POST | `/api/v1/superadmin/roles` | platform role catalog/create | create uses `name`, `description`, `permissions` |
| GET | `/api/v1/superadmin/roles/{roleKey}` | role detail | path key |
| POST | `/api/v1/superadmin/changelog` | create release note | `version`, `title`, `body`, `isHighlighted` |
| PUT, DELETE | `/api/v1/superadmin/changelog/{id}` | update/delete release note | delete returns success envelope, not empty body |
| POST | `/api/v1/superadmin/notify` | platform notification dispatch | `to`, `subject`, `body`; keep examples non-private |

### Add Client, activation, setup, and seed status

| Method | Path | Purpose | Request/query |
| --- | --- | --- | --- |
| GET | `/api/v1/superadmin/tenants/new` | Add Client option schema | V1 company/owner/commercial/quota/module/support/create-mode fields only |
| GET | `/api/v1/superadmin/tenants/coa-templates` | COA templates | none |
| POST | `/api/v1/superadmin/tenants` | create draft or pending activation client | `company`, `owner`, `commercial`, `quotas`, `modules`, `support`, `createMode` |
| POST | `/api/v1/superadmin/tenants/{id}/activation/send` | send activation for draft/recoverable tenant | no token in response |
| POST | `/api/v1/superadmin/tenants/{id}/activation/resend` | supersede and email new activation | no old token exposed |
| POST | `/api/v1/superadmin/tenants/{id}/activation/copy` | explicit copy-link boundary | UI/evidence redacts returned URL |
| POST | `/api/v1/superadmin/tenants/{id}/activation/expire` | expire current activation | none |
| GET | `/api/v1/auth/activation/verify?token=<REDACTED>` | owner verifies activation | response is minimal setup metadata |
| POST | `/api/v1/auth/activation/complete` | owner sets password | `token`, `newPassword`, `confirmPassword`; all redacted |
| GET | `/api/v1/setup/status` | owner setup checklist | authenticated tenant owner |
| PUT | `/api/v1/setup/company-details` | update allowed company details | `name`, `timezone`, optional `stateCode` |
| PUT | `/api/v1/setup/gst` | GST setup | `enabled`, optional `stateCode`, `defaultGstRate` |
| PUT | `/api/v1/setup/accounting` | confirm seeded defaults | optional `confirmDefaults` |
| POST | `/api/v1/setup/invite-team` | optional invite/skip | `invitations[]` or `skip=true` |
| POST | `/api/v1/setup/finish` | finish setup idempotently | empty body allowed |
| GET | `/api/v1/superadmin/tenants/{id}/seed-status` | seed run status | path id |
| POST | `/api/v1/superadmin/tenants/{id}/seed-status/repair` | repair failed seed categories | none |
| PUT, DELETE | `/api/v1/superadmin/tenants/{id}/accounting-mappings/{mappingKey}` | remap/delete locked setup mappings | update uses `accountId`; delete may return locked conflict |

`POST /api/v1/superadmin/tenants` request shape:

```json
{
  "company": {
    "name": "Example Paints",
    "code": "EXAMPLE",
    "timezone": "Asia/Kolkata",
    "stateCode": "KA",
    "baseCurrency": "INR",
    "defaultGstRate": 18,
    "coaTemplateCode": "SME"
  },
  "owner": {
    "email": "owner@example.invalid",
    "displayName": "Owner Example",
    "phone": "+910000000000"
  },
  "commercial": {
    "planId": "TRIAL",
    "billingStatus": "MANUAL",
    "trialDays": 14,
    "supportTier": "STANDARD"
  },
  "quotas": {
    "maxActiveUsers": 10,
    "maxApiRequests": 10000,
    "maxStorageBytes": 1073741824,
    "maxConcurrentRequests": 8,
    "softLimitEnabled": false,
    "hardLimitEnabled": true
  },
  "modules": {"enabled": ["ACCOUNTING", "SALES"]},
  "support": {"notes": "Safe platform-only note", "tags": ["TRIAL"]},
  "createMode": "SEND_ACTIVATION"
}
```

### Client list/detail and tenant operations

Runtime limits are mutated through `PUT /api/v1/superadmin/tenants/{id}/limits`.

| Method | Path | Purpose | Request/query |
| --- | --- | --- | --- |
| GET | `/api/v1/superadmin/tenants` | client list | `status`, `q`, `page`, `size`, `sort`, `includeArchived` |
| GET | `/api/v1/superadmin/tenants/{id}` | state-aware profile tabs | path id |
| PUT | `/api/v1/superadmin/tenants/{id}/lifecycle` | current lifecycle state update | `state`, `reason` |
| PUT | `/api/v1/superadmin/tenants/{id}/limits` | direct runtime limits | quota and burst/concurrency fields |
| PUT | `/api/v1/superadmin/tenants/{id}/modules` | full module replacement | `enabledModules[]` |
| POST | `/api/v1/superadmin/tenants/{id}/force-logout` | revoke tenant sessions | optional `reason` |
| PUT | `/api/v1/superadmin/tenants/{id}/admins/main` | replace main admin | `adminUserId` |
| POST | `/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/request` | start email change | `newEmail` |
| POST | `/api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/confirm` | confirm email change | `requestId`, `verificationToken` placeholder |
| PUT | `/api/v1/superadmin/tenants/{id}/support/context` | platform support context | `supportNotes`, `supportTags[]`; sanitized summary only |
| POST | `/api/v1/superadmin/tenants/{id}/support/warnings` | issue support warning | `message`, optional category/state/grace |
| GET, PUT | `/api/v1/superadmin/tenants/{id}/review-intelligence` | review/AI assist toggle | toggle request |

Tenant profile data is summary-only: overview, onboarding, plan/limits, usage, billing, support, bugs, audit, settings, seed status, actions, and safe tenant identifiers. Do not render tenant business records.

### Plans, entitlements, usage, quotas, billing, and lifecycle

| Method | Path | Purpose | Request/query |
| --- | --- | --- | --- |
| GET, POST | `/api/v1/superadmin/plans` | list/create plans | `includeArchived`; create fields include stable id, display name, cadence, price, currency, trial, support tier, default limits, feature flags |
| GET, PUT | `/api/v1/superadmin/plans/{stableId}` | read/update plan version | `version`, `includeArchived`; update includes reason/effective date |
| POST | `/api/v1/superadmin/plans/{stableId}/archive` | archive plan | archive reason |
| PUT | `/api/v1/superadmin/tenants/{id}/plan` | assign plan/custom plan | `planId` or `customPlan`, `reason`, `repriceSubscription` |
| GET | `/api/v1/superadmin/tenants/{id}/entitlements` | effective limits/features | path id |
| PUT | `/api/v1/superadmin/tenants/{id}/entitlements/overrides` | add/update overrides | `features`, `limits`, `reason` |
| DELETE | `/api/v1/superadmin/tenants/{id}/entitlements/overrides/{key}` | remove override | optional reason body |
| GET | `/api/v1/superadmin/usage` | platform aggregate usage | none |
| GET | `/api/v1/superadmin/tenants/{id}/usage` | tenant usage dashboard | path id |
| GET | `/api/v1/superadmin/tenants/{id}/usage/history` | historical usage windows | `periodType` |
| POST | `/api/v1/superadmin/tenants/{id}/quota-check` | quota policy probe | `dimension`, `units`, `bytes`, `emailCategory`, `dryRun` |
| GET | `/api/v1/superadmin/tenants/{id}/quota-policy` | quota policy matrix | path id |
| GET | `/api/v1/superadmin/billing/metrics` | MRR/ARR and billing aggregates | none |
| GET, POST | `/api/v1/superadmin/tenants/{id}/billing/subscription` | subscription read/write | amount, cadence, currency, collection mode, dates, status, reason |
| GET | `/api/v1/superadmin/tenants/{id}/billing/ledger` | immutable billing ledger | path id |
| POST | `/api/v1/superadmin/tenants/{id}/billing/invoices` | manual invoice | amount, currency, idempotency key, reason |
| POST | `/api/v1/superadmin/tenants/{id}/billing/payments` | manual payment | amount, currency, idempotency key, reason |
| POST | `/api/v1/superadmin/tenants/{id}/billing/adjustments` | adjustment | direction, amount, currency, idempotency key, reason |
| GET | `/api/v1/superadmin/tenants/{id}/commercial-state` | state matrix view | path id |
| POST | `/api/v1/superadmin/tenants/{id}/suspension/grace` | enter grace | reason, optional times |
| POST | `/api/v1/superadmin/tenants/{id}/suspension/read-only` | read-only suspension | reason |
| POST | `/api/v1/superadmin/tenants/{id}/suspension/blocked` | blocked suspension | reason |
| POST | `/api/v1/superadmin/tenants/{id}/resume` | resume | reason |
| POST | `/api/v1/superadmin/tenants/{id}/cancel` | cancel | reason/effective date |
| POST | `/api/v1/superadmin/tenants/{id}/archive` | archive | reason/effective date |

Default limit fields include `maxActiveUsers`, `maxStorageBytes`, `maxApiRequests`, `maxPdfExports`, `maxEmails`, `maxJobs`, `burstRequestsPerMinute`, and `maxConcurrentRequests`. Document explicit zero/null/unlimited semantics in UI copy from the returned plan/entitlement metadata.

### Support, bugs, SLA, Sentry, Datadog, infra, and audit

| Method | Path | Purpose | Request/query |
| --- | --- | --- | --- |
| GET | `/api/v1/superadmin/support/tickets` | support/feature/bug queue | `status`, `category`, `slaStatus`, `q`, `page`, `size`, `sort` |
| GET | `/api/v1/superadmin/support/tickets/{ticketId}` | ticket detail | path id |
| GET, POST | `/api/v1/superadmin/support/tickets/{ticketId}/messages` | customer-visible chat | list supports `page`, `size`, `includeInternal`; create uses `content` |
| POST | `/api/v1/superadmin/support/tickets/{ticketId}/internal-notes` | platform-only note | `content` |
| POST | `/api/v1/superadmin/support/tickets/{ticketId}/status` | status transition | `status`, optional `reason` |
| GET | `/api/v1/superadmin/support/tickets/{ticketId}/timeline` | immutable timeline | path id |
| POST | `/api/v1/superadmin/support/tickets/sla/refresh` | SLA recalculation | optional `asOf` validation fixture/time |
| POST | `/api/v1/superadmin/support/tickets/{ticketId}/convert-to-incident` | feature/bug conversion | optional `reason` |
| POST | `/api/v1/superadmin/support/tickets/{ticketId}/sentry/link` | link configured Sentry issue | `issueId` only; no URLs/credentials |
| POST | `/api/v1/superadmin/support/tickets/{ticketId}/sentry/sync` | sync Sentry status | path id |
| GET | `/api/v1/superadmin/observability/datadog/status` | safe Datadog status | none |
| GET | `/api/v1/superadmin/infra/health` | component health dashboard | none |
| GET | `/api/v1/superadmin/infra/costs` | cost dashboard | `currency` |
| GET, POST | `/api/v1/superadmin/infra/costs/snapshots` | list/create cost snapshots | list: `currency`, `includeArchived`; create: component, source, amount, currency, period, reason |
| PUT | `/api/v1/superadmin/infra/costs/snapshots/{snapshotId}` | correction/update | snapshot request fields |
| POST | `/api/v1/superadmin/infra/costs/snapshots/{snapshotId}/archive` | archive snapshot | `reason` |
| GET | `/api/v1/superadmin/infra/costs/snapshots/{snapshotId}/corrections` | correction history | path id |
| GET | `/api/v1/superadmin/audit/platform-events` | platform audit feed | `from`, `to`, `module`, `action`, `status`, `actor`, `entityType`, `reference`, `tenantId`, `category`, `page`, `size` |
| GET | `/api/v1/superadmin/audit/security-events` | login/security feed | `from`, `to`, `action`, `status`, `actor`, `entityType`, `reference`, `tenantId`, `category`, `page`, `size` |
| GET | `/api/v1/superadmin/audit/suspicious-events` | suspicious event queue | `from`, `to`, `status`, `actor`, `reference`, `tenantId`, `page`, `size` |
| POST | `/api/v1/superadmin/audit/suspicious-events/{eventId}/acknowledge` | acknowledge event | `reason` |
| POST | `/api/v1/superadmin/audit/suspicious-events/{eventId}/resolve` | resolve event | `reason` |
| POST | `/api/v1/superadmin/audit/suspicious-events/{eventId}/reopen` | reopen event | `reason` |

Support/bug free text is sanitized and bounded. Sentry/Datadog metadata is pseudonymous and bounded: route template, status/outcome, environment, release, trace ID, tenant hash, actor role/hash, and component names. Do not send raw emails, company legal names/codes, request bodies, query strings, support text, bug descriptions, bearer tokens, or canaries.

## Retired route handling

Current frontend docs and `openapi.json` must not present retired flat onboarding, platform-issued admin credential reset, old external support-sync aliases, or stale tenant setup payloads as active V1 APIs.
