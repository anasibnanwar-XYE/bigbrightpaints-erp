# Schema Drift Scan Waiver (Legacy Migrations)

Reviewed: 2026-02-05

Purpose:
- Allow legacy migrations that predate CODE-RED drift rules to pass the
  `FAIL_ON_FINDINGS` schema drift scan without editing historical files.
- Keep the scan strict for **new** migrations: any new use of
  `CREATE TABLE/INDEX IF NOT EXISTS` or `UPDATE ... FROM` backfills
  must be explicitly reviewed and added to the allowlist.

Scope:
- Applies only to historical migration files listed in
  `scripts/schema_drift_scan_allowlist.txt`.
- No historical migrations may be edited; forward-only fixes only.

Rules:
1) If a new migration introduces a drift pattern, it **must** be reviewed and
   either fixed (preferred) or added to the allowlist with a rationale.
2) Any allowlist change must be recorded in CODE-RED decision log.
3) `FAIL_ON_FINDINGS=true bash scripts/verify_local.sh` must remain green for
   release commits unless a waiver is explicitly recorded.

Rationale:
- Historical migrations contain known `IF NOT EXISTS` and deterministic
  backfill patterns. These are accepted as legacy behavior but should not
  expand under CODE-RED.
