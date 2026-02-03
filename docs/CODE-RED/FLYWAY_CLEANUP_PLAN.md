# Flyway “Cleanup” Plan (CODE-RED, Forward-Only)

This is the concrete plan for handling the **100+ Flyway migrations** safely for production.

## Direct Answer: How Many Migrations Should Be Deleted Before Prod?

**0. Delete none.**

Reasons (Flyway basics):
- Flyway validates both the **presence** and **checksum** of every migration that was applied.
- Deleting or editing a migration that has ever been applied will cause **validation failures** and block deploys.
- Deleting migrations also removes the historical audit trail needed for “how did this schema happen?” investigations.

If you want fewer migrations for **brand-new installs**, use a **squashed baseline** profile (see Phase 4). That is not deletion.

---

## Current Inventory (Repo Reality)

As of **2026-02-02**, this repo contains **119** Flyway migrations:
- Location: `erp-domain/src/main/resources/db/migration`
- Versions: `V1__...` through `V116__...` (placeholders/gap fillers are intentional and must remain)

Reference docs:
- Audit summary: `erp-domain/docs/FLYWAY_AUDIT_AND_STRATEGY.md`
- Duplicate findings: `erp-domain/docs/DUPLICATES_REPORT.md`

---

## Phase 0 — Freeze + Record (Before Any “Cleanup”)

Goal: ensure everyone can prove what was shipped.

On the release commit, record:
1) Repo migration count + max version
   - `ls -1 erp-domain/src/main/resources/db/migration | wc -l`
   - `ls -1 erp-domain/src/main/resources/db/migration | sed -n 's/^V\\([0-9]\\+\\)__.*$/\\1/p' | sort -n | tail -n 1`
2) DB migration count + max version (staging/prod-like DB)
   - `SELECT count(*) FROM flyway_schema_history WHERE success = true;`
   - `SELECT max(version) FROM flyway_schema_history WHERE success = true;`

NO‑GO:
- checksum drift (unless proven safe and repaired)
- DB history count/max mismatch vs repo expectations

---

## Phase 1 — Converge Drift (Forward-Only, No Edits)

Goal: make “fresh DB” and “upgraded DB” end up with the **same final schema**, especially for drift-heavy areas.

Rule: **Never edit applied migrations.** Add new migrations only.

### The four “convergence migrations”

These are already documented as the CODE-RED convergence plan:
- `V117__payroll_convergence.sql`
- `V118__journal_uniqueness_convergence.sql`
- `V119__accounting_events_uniqueness_convergence.sql`
- `V120__index_consolidation.sql`

What each one should do:

1) `V117__payroll_convergence.sql`
   - Ensure payroll tables (`payroll_runs`, `payroll_run_lines`) have one final canonical shape.
   - Remove redundant indexes/constraints created by duplicate guarded DDL (drop extra indexes safely if present).

2) `V118__journal_uniqueness_convergence.sql`
   - Ensure exactly one uniqueness mechanism exists for `(company_id, reference_number)` on `journal_entries`.
   - If both constraints exist (one auto-generated from table DDL + the explicit `uk_journal_company_reference`),
     drop the redundant one **conditionally**.

3) `V119__accounting_events_uniqueness_convergence.sql`
   - Ensure only one uniqueness enforcement exists for `(aggregate_id, sequence_number)` on `accounting_events`
     (constraint OR index; not both).
   - Keep the chosen one and drop the redundant one conditionally.

4) `V120__index_consolidation.sql`
   - Remove duplicate “performance indexes” that were created in multiple migrations (guarded duplicates).
   - Ensure the intended final index set exists exactly once.

### Writing these migrations safely (Postgres)

Use `DO $$ BEGIN ... END $$;` blocks with existence checks:
- Constraints: `pg_constraint` / `information_schema.table_constraints`
- Indexes: `pg_indexes`

Always pre-check data before adding a uniqueness constraint/index:
- Query for duplicates and `RAISE EXCEPTION` if any exist (NO-GO until cleaned via audited repair).

---

## Phase 2 — Validate On Clean DB + Upgraded DB

Goal: prove convergence migrations actually converge.

Run two validations:
1) Clean DB install (all migrations apply from scratch)
   - App boots cleanly, no Flyway errors.
2) Upgrade simulation (start from a DB that represents the real staging/prod history)
   - Apply new release; Flyway applies `V117..V120`; final schema matches clean DB.

Required gates:
- `bash scripts/verify_local.sh`
- On staging/prod-like DB: `scripts/db_predeploy_scans.sql` returns zero rows.

---

## Phase 3 — Production Deploy Discipline

Goal: ship without “mystery schema” and without manual SQL.

On staging (then prod):
1) Backup/restore dataset
2) Deploy app (Flyway migrates forward)
3) Run drift gate:
   - repo count/max vs `flyway_schema_history` count/max
4) Run predeploy scans:
   - `scripts/db_predeploy_scans.sql` (NO‑GO if any rows)
5) Run smoke checks:
   - `erp-domain/scripts/ops_smoke.sh`

---

## Phase 4 (Optional) — Squashed Baseline For Greenfield Installs

Goal: reduce migration count for **brand-new environments only** (not for existing DB upgrades).

Approach (safe):
- Keep legacy migrations in `db/migration` unchanged.
- Add a separate location, e.g. `db/migration_squashed`, containing a single baseline:
  - `src/main/resources/db/migration_squashed/V1__baseline.sql`
- Add a dedicated Spring profile (example: `prod-squashed`) that points Flyway to `db/migration_squashed` for
  greenfield installs.

Important:
- This does **not** delete anything.
- Existing DB upgrades still use the legacy `db/migration` path and keep validating.

---

## When Can We Ever “Remove” Old Migrations?

For CODE-RED: treat this as **out of scope** until after production is stable.

Even then, “remove” should mean “archive but keep” (retain history for audit/repro), not delete:
- only after you’re 100% sure no environment needs to migrate from scratch using the legacy chain
- only after you have a tested squashed baseline profile
- only with a rollback plan + CI enforcement
