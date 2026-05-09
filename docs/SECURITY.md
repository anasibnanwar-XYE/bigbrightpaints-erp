# Security Controls

Last reviewed: 2026-05-05

## High-Risk Change Classes

- Changes under `erp-domain/src/main/resources/db/migration_v2/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/`
- Changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/`

## R2 Approval Workflow

- High-risk changes must ship with an updated `docs/approvals/R2-CHECKPOINT.md`.
- The checkpoint must declare the approval mode, escalation decision, rollback owner, expiry window, and concrete verification evidence.
- Narrow high-risk remediation changes that keep current canonical behavior intact may use orchestrator approval; widen-scope, compatibility-shim, or destructive-risk changes require human escalation.

## Review Validation Surface

- Docs-only review-policy changes validate with `bash ci/lint-knowledgebase.sh`.
- Runtime/config/schema/test-impacting changes validate through the PR ship-safety lane in `.github/workflows/ci.yml`.
- High-risk changes must also satisfy `High-Risk Change Control` / `bash ci/check-high-risk-changes.sh` and keep rollback notes current.

## Super Admin Control-Plane Invariants

- Super Admin controls tenant accounts and operational metadata only. It must
  not browse tenant invoices, journal/ledger lines, inventory/batch records,
  salary/payroll data, vendor/customer payloads, uploaded files, GST returns,
  request bodies, raw logs, activation/reset tokens, password hashes, JWTs, or
  provider credentials.
- Platform routes live under `/api/v1/superadmin/**`; tenant workflow prefixes
  such as `/api/v1/admin/**`, `/api/v1/accounting/**`, `/api/v1/factory/**`,
  `/api/v1/hr/**`, `/api/v1/portal/**`, and `/api/v1/dealer-portal/**` remain
  outside the Super Admin frontend shell.
- Standard error envelopes must include a safe code, message/reason, path,
  trace ID, and field details when available. They must not include stack
  traces, SQL, Java class names, tokens, credentials, or private tenant data.
- Rate limiting covers platform/public-auth burst controls plus tenant quota
  dimensions. `429` responses should include retry/reset metadata when
  available and must not duplicate side effects.
- Every accepted cross-area Super Admin mutation needs privacy-safe audit
  evidence with actor, action, target, old/new values when relevant, timestamp,
  trace/correlation ID, and bounded metadata. Required-audit mutations fail
  closed or roll back if audit evidence cannot be written.
- Sentry and Datadog integrations use server-side credentials only. Exposed
  metadata must be pseudonymous and bounded: route template, status/outcome,
  environment, release, trace ID, tenant hash, actor role/hash, and component
  names. Raw emails, company names/codes, request bodies, query strings,
  support text, bug text, bearer tokens, and canaries are forbidden.
- Documentation, OpenAPI examples, validation evidence, screenshots, and
  support tickets must use redacted placeholders for bearer tokens, passwords,
  activation/reset links, Sentry/Datadog credentials, SMTP credentials, database
  secrets, and `.env` values.
