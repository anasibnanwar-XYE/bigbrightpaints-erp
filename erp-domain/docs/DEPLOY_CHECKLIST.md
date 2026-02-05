# Deploy Checklist

Authoritative release procedure:
- `docs/CODE-RED/RELEASE_RUNBOOK.md`

## Required Env Vars (Prod)
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (32+ bytes)
- `ERP_LICENSE_KEY`
- `ERP_DISPATCH_DEBIT_ACCOUNT_ID`
- `ERP_DISPATCH_CREDIT_ACCOUNT_ID`

## Common Env Vars (Non-Prod Defaults Exist)
- `SPRING_PROFILES_ACTIVE` (dev|test|prod)
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`
- `KAFKA_BOOTSTRAP_SERVERS`
- Mail (prod config uses `SMTP_*`; `SPRING_MAIL_*` still works as a Spring override):
  - `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD` (must be overridden; must not be `changeme`)
  - Optional override: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
- `ERP_CORS_ALLOWED_ORIGINS`
- `ERP_MAIL_BASE_URL`, `ERP_MAIL_FROM`
- `ERP_SECURITY_AUDIT_PRIVATE_KEY`
- `ERP_RATE_LIMIT_RPM`, `ERP_AUTO_APPROVAL_ENABLED`
- `ERP_LICENSE_PRODUCT_ID`, `ERP_LICENSE_ALGORITHM`, `ERP_LICENSE_CREATED`, `ERP_LICENSE_DESCRIPTION`, `ERP_LICENSE_ENFORCE`

## Profiles
- dev: `application.yml` (defaults use local Postgres, RabbitMQ, Kafka)
- test: Testcontainers via `AbstractIntegrationTest` (Docker required)
- prod: `application-prod.yml` (requires explicit secrets)

## Flyway/Migrations
- Flyway runs on startup; ensure DB user has DDL privileges.
- For existing DBs: do NOT rewrite migrations; use forward-fix if needed.
- Optional: `flyway repair` only if checksum drift is known-safe.

## CODE-RED Predeploy Scans (Required For Safe Deploy)

CODE-RED policy: deploy is **NO-SHIP** if any predeploy scan returns rows.

- Scan file: `scripts/db_predeploy_scans.sql` (repo root; read-only)
- Run against:
  - staging with a prod-like dataset (preferred: a recent prod snapshot restore)
  - production before/after release if you have access

If any rows are returned:
- stop the deployment
- create a controlled repair plan (admin-only repair endpoint or forward migration), not ad-hoc SQL

## Health Checks
- `/actuator/health`
- `/api/integration/health`
- `/api/v1/orchestrator/health/integrations`
- `/api/v1/orchestrator/health/events`
  - Note: orchestrator health endpoints must be authenticated/authorized in production (ops/admin).

## Outbox Operations
- Retry policy: exponential backoff (30s * 2^retry), max 5 attempts; then status=FAILED and dead_letter=true.
- Metrics: `outbox.events.pending`, `outbox.events.retrying`, `outbox.events.deadletters`.
- Health snapshot: `/api/v1/orchestrator/health/events` (pending/retrying/dead-letter counts).
- Manual replay:
  - Inspect: `SELECT id, status, retry_count, last_error, next_attempt_at FROM orchestrator_outbox WHERE dead_letter = true ORDER BY created_at DESC;`
  - Requeue one: `UPDATE orchestrator_outbox SET status='PENDING', dead_letter=false, retry_count=0, next_attempt_at=now() WHERE id='...';`
  - Requeue all: `UPDATE orchestrator_outbox SET status='PENDING', dead_letter=false, retry_count=0, next_attempt_at=now() WHERE dead_letter = true;`
  - Note: replays are at-least-once; confirm consumer idempotency before bulk requeue.

## Operator Smoke Checks
- Script: `erp-domain/scripts/ops_smoke.sh`
- Required env: `ERP_SMOKE_EMAIL`, `ERP_SMOKE_PASSWORD`, `ERP_SMOKE_COMPANY`.
- Optional overrides: `BASE_URL`, `MGMT_URL`.

## Logs/Metrics
- File log: `logs/erp-backend.log`
- Actuator endpoints enabled for `health`, `info`, `metrics`.
