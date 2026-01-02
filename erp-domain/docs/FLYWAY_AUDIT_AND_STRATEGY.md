# Flyway Audit and Strategy

## Inventory
- Total migrations: 91 (`src/main/resources/db/migration`)
- Naming pattern: `V{version}__{description}.sql` with incremental versions 1-91.

## Duplicate/Conflict Findings
- Table creation duplicated but guarded with `IF NOT EXISTS` / DO blocks:
  - `payroll_runs`: `V7__hr_tables.sql`, `V78__payroll_enhancement.sql`
  - `payroll_run_lines`: `V57__payroll_run_lines.sql`, `V78__payroll_enhancement.sql`
- Index creation duplicated but guarded:
  - `idx_invoices_dealer`: `V12__invoices.sql`, `V62__performance_indexes.sql`
  - `idx_journal_lines_account_id`: `V62__performance_indexes.sql`, `V65__performance_accounting_inventory_indexes.sql`

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

## Changes in This Pass
- No Flyway file edits; only audit and documentation updates.
