# Flyway Audit and Strategy

## Inventory
- Total migrations: 131 (`src/main/resources/db/migration`)
- Naming pattern: `V{version}__{description}.sql` with incremental versions 1-131.
- Placeholder/gap fillers exist (example: `V25__fill_migration_gap.sql`, `V42__placeholder.sql`). These are intentional and
  should remain explicit in audit/review so the version history stays stable.

## Duplicate/Conflict Findings
High-risk duplicates (schema drift generators)
- Payroll tables defined multiple times (guarded with `IF NOT EXISTS` / conditional blocks):
  - `payroll_runs`: `V7__hr_tables.sql`, `V78__payroll_enhancement.sql`
  - `payroll_run_lines`: `V57__payroll_run_lines.sql`, `V78__payroll_enhancement.sql`
- Journal uniqueness defined multiple times (duplicate uniqueness enforcement / indexes):
  - `journal_entries` unique reference in table DDL + later explicit constraint:
    `V5__accounting_tables.sql`, `V66__accounting_journal_unique_constraint.sql`
- Event store uniqueness enforced multiple times:
  - `accounting_events` unique constraint + later unique index:
    `V70__accounting_event_store.sql`, `V115__accounting_event_sequence_unique.sql`

Lower-risk duplicates (mostly performance / survivable)
- Index creation duplicated but guarded:
  - `idx_invoices_dealer`: `V12__invoices.sql`, `V62__performance_indexes.sql`
  - `idx_journal_lines_account_id`: `V62__performance_indexes.sql`, `V65__performance_accounting_inventory_indexes.sql`
  - `idx_partner_settlement_idempotency`: `V48__settlement_idempotency_keys.sql`, `V102__partner_settlement_idempotency_scope.sql`
  - `uk_payroll_runs_company_idempotency`: `V68__payroll_idempotency_key.sql`, `V100__payroll_idempotency_scope.sql`

## Fresh DB Procedure (Safe)
1) Ensure Postgres is available and credentials are set.
2) Start the app with Flyway enabled (default).
3) On first boot, Flyway applies all migrations in order.
4) Verify schema via app health + basic endpoint smoke (see `docs/DEPLOY_CHECKLIST.md`).

## Existing DB Strategy (No Rewrites)
- Assume migrations have been applied in at least one environment.
- Do NOT edit or delete applied migration files.
- If checksum drift is detected:
  - Use `flyway repair` to align checksums only when files are unchanged from deployed versions.
  - Otherwise, add a new forward migration to correct schema.
- For new schema fixes, always add a new migration (forward-fix).

## Checksum Drift Handling
- Detect drift from startup logs or by querying: `SELECT version, checksum, installed_on FROM flyway_schema_history ORDER BY installed_rank DESC;`
- If drift is caused by a safe, identical file (e.g., line endings), use `flyway repair` after confirming the deployed file matches.
- If drift indicates a real change, create a new forward migration that resolves the issue; do not edit the applied file.

## Forward-Fix Guidance by Migration Type
- Schema additions/constraint changes: add a forward migration that reverts or adjusts the change safely.
- Data backfills: keep migrations idempotent with guards (`WHERE`/`EXISTS`) to allow re-runs.
- Destructive operations: require a backup/restore plan; use forward migrations only after recovery safety is confirmed.

## CODE-RED Convergence Plan (Forward-Only)
CODE-RED requires the schema to converge so "fresh install" and "prod upgrade" behave the same.

Principles
- Do not edit applied migrations.
- Add new "convergence" migrations that declare the final intended table shape and constraints.
- Prefer deterministic backfills over "IF NOT EXISTS" drift patterns.

Recommended convergence migrations (forward-only, implemented)
- `V128__converge_payroll_schema.sql`
  - converge `payroll_runs` + `payroll_run_lines` to the entity-driven shape
  - add deterministic backfills + fail-closed guards before NOT NULL enforcement
- `V129__converge_journal_uniqueness.sql`
  - ensure exactly one uniqueness mechanism exists for `(company_id, reference_number)` on `journal_entries`
- `V130__converge_accounting_events.sql`
  - normalize `accounting_events` uniqueness so only one mechanism remains (constraint or index, not both)
- `V131__index_consolidation.sql`
  - remove duplicate “performance” indexes that exist in multiple versions (fresh DB vs upgraded DB drift)
- Optional: `V132__auth_token_mfa_convergence.sql`
  - only if/when auth token + MFA storage is converged to a single canonical source-of-truth

## Environment Validation Steps
- Dev/Test: boot against a clean database and confirm Flyway applies all migrations without errors.
- Prod: take backups, deploy during a maintenance window, and verify `/actuator/health` plus recent `flyway_schema_history` entries.

## Optional: Squashed Baseline For Greenfield Installs (Future)
Goal: reduce "100+ migrations" cost/complexity **for brand-new environments only**, without rewriting history for existing DBs.

Rules
- Do not edit or delete `db/migration` files (existing DB upgrades must keep validating).
- Squashing is only for a **new Flyway location** and a **new Spring profile** (greenfield installs).

Proposed approach
1) Create a fully-migrated reference DB using the current `db/migration` set.
2) Dump the schema (and required seed data, if any) into a single baseline file, for example:
   - `src/main/resources/db/migration_squashed/V1__baseline.sql`
3) Add a new profile (example: `prod-squashed`) that points Flyway to `db/migration_squashed` for **new** installs only.
4) Keep `db/migration` as the default for all existing environments.

Notes
- This should be done only after CODE-RED stabilization has converged the drift-heavy tables (so the baseline is stable).
- This does not remove the need for forward migrations; it just reduces the starting cost for greenfield environments.

## Changes in This Pass
- Added forward-only convergence migrations for payroll, journal uniqueness, accounting events, and index consolidation.
