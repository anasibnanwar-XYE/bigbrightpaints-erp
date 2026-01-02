# Deploy Checklist

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
- `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`
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

## Startup Commands
- Build: `mvn package`
- Run (dev): `mvn spring-boot:run`
- Run (prod): `mvn -Dspring-boot.run.profiles=prod spring-boot:run`

## Health Checks
- `/actuator/health`
- `/api/integration/health`
- `/api/v1/orchestrator/health/integrations`
- `/api/v1/orchestrator/health/events`

## Logs/Metrics
- File log: `logs/erp-backend.log`
- Actuator endpoints enabled for `health`, `info`, `metrics`.
