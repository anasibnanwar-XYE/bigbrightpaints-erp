# Super Admin Portal

Last reviewed: 2026-04-30

## Purpose

This portal is the platform-owner control plane. It lets `ROLE_SUPER_ADMIN` users operate tenant accounts and operational metadata: dashboard, Add Client, activation, owner setup status, plans, entitlements, usage, billing, support, bugs, SLA, infra health, settings, audit, and observability status.

The portal must not expose tenant-private business records. It may show safe tenant identity markers, aggregate counts, operational state, trace IDs, audit IDs, MailHog message IDs, and redacted observability status only.

## Users

- Allowed: `ROLE_SUPER_ADMIN` in the platform scope.
- Disallowed: tenant admins, accounting users, sales users, factory users, HR users, dealer users, and anonymous users.

## Navigation

Recommended top-level navigation:

- `Dashboard`
- `Clients`
- `Plans`
- `Usage`
- `Billing`
- `Support`
- `Bugs`
- `Audit`
- `Infra`
- `Settings`
- `Profile`
- `Changelog`

Recommended client-detail tabs:

- `Overview`
- `Onboarding`
- `Plan & Limits`
- `Usage`
- `Billing`
- `Support`
- `Bugs`
- `Audit`
- `Settings`
- `Seed Status`

## What belongs here

- Platform dashboard and aggregate health cards.
- Add Client wizard and activation actions.
- Client list/detail summaries, lifecycle actions, quota/module/plan/entitlement changes.
- Billing subscriptions, manual invoices, payments, adjustments, grace/suspension/cancel/archive actions.
- Support queue, ticket chat, internal notes, SLA refresh, feature-request and bug/incident workflow.
- Sentry issue link/sync status and Datadog status through server-side wrappers.
- Infra health and cost snapshots.
- Platform audit, security events, suspicious-event remediation.
- Profile, sessions, password change, platform settings, roles, and changelog authoring.

## What does not belong here

- Tenant approval inbox or tenant user CRUD.
- Accounting, sales, inventory, factory, purchasing, HR/payroll, dealer, or portal workflows.
- Any route under `/api/v1/admin/**`, `/api/v1/accounting/**`, `/api/v1/factory/**`, `/api/v1/hr/**`, `/api/v1/portal/**`, or `/api/v1/dealer-portal/**`.
- Private tenant invoices, ledgers, inventory rows, salaries, vendor/customer names, uploaded files, GST returns, request bodies, raw logs, activation/reset token values, password hashes, JWTs, Sentry/Datadog credentials, SMTP credentials, or `.env` values.

## Critical frontend rules

- Use only the current routes in [api-contracts.md](api-contracts.md) and the generated `openapi.json` snapshot.
- Do not build UI against retired flat onboarding or platform-issued credential-reset routes; they are registry-only retired surfaces.
- Do not ask operators to manage `X-Company-Code` or `X-Company-Id` headers for control-plane routes. The target tenant is either the platform scope or the tenant id in the path.
- Capture and show `traceId`/correlation IDs on every success and failure so support can correlate API, audit, Sentry, and Datadog evidence.
- Render safe summaries only. Hide tenant business tabs entirely instead of disabling them.
