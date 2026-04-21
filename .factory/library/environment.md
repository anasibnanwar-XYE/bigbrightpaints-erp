# Environment

Mission-specific environment guidance for the accounting hard-cut worktree.

Keep this file focused on what workers actually need to boot, test, and reason about the mission safely.

---

## Runtime Policy

- **Default state is runtime off.** Start backend/runtime services only when testing is required.
- Valid reasons to start runtime are limited to:
  - DB-backed characterization capture
  - integration tests that need live dependencies
  - curl runtime proof for the validation contract
- Pure code cleanup or docs-only work should not leave compose/Spring processes running.

## Validated Dry-Run Host Boundary

These are the host ports already validated for this mission's dry-run runtime:

- Postgres: `5433`
- App HTTP: `18081`
- Actuator/management: `19090`
- MailHog UI/API: `18025`
- RabbitMQ AMQP: `15673`
- RabbitMQ management: `15674`

Use these ports for mission runtime proof. Host Postgres `5432` is off-limits.

## Required Runtime Inputs

- `SPRING_DATASOURCE_URL` / `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `ERP_SECURITY_ENCRYPTION_KEY`
- `ERP_SECURITY_AUDIT_PRIVATE_KEY`
- `SPRING_MAIL_HOST` / `SPRING_MAIL_PORT` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD`
- `SPRING_PROFILES_ACTIVE=prod,flyway-v2,mock,validation-seed` for the init-managed compose-backed validation runtime
- `ERP_VALIDATION_SEED_ENABLED=true` so compose-backed runtime proof boots the seeded validation fixtures
- `MIGRATION_SET=v2` for Maven-side validation

## Mission Constraints

- Flyway v2 only.
- Run Maven from `erp-domain/`.
- Accounting-table RLS rollout is part of the environment assumption for this mission; do not validate accounting behavior on a setup that bypasses tenant isolation.
- Sensitive financial disclosure paths must stay approval-gated in validation fixtures too; do not create convenience shortcuts that erase the gate.
- Anomaly/review remains default-off until manually enabled by superadmin because it is a paid feature and currently warn-only.

## Worker Notes

- Older generic repo guidance may still mention different host ports; for this mission, the validated dry-run host boundary above is the worker-facing truth.
- Docs are deliverables here, so environment guidance must stay aligned with the mission proposal and validation contract when runtime assumptions change.
