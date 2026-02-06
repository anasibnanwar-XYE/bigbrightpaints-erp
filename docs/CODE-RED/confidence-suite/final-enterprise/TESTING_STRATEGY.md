# Final Enterprise Truth-Suite Strategy

This folder is the final strategy snapshot for authoritative deploy-readiness tests.

## Scope

- Authoritative test package:
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/**`
- Authoritative policy docs:
  - `docs/CODE-RED/**`
- Primary runtime truth:
  - production code + Flyway schema.

## Gate ownership

1. `gate-fast`
- critical invariants + changed-file coverage.
- CI/PR diff-base uses `origin/main` merge-base.
- local long-lived branch reruns should set `DIFF_BASE` explicitly.

2. `gate-core`
- critical + concurrency + reconciliation truth tests.

3. `gate-release`
- strict verify + fresh/upgrade migration matrix + scan hard-fail.
- local run can target isolated postgres with explicit env:
  - `PGHOST=127.0.0.1 PGPORT=55432 PGUSER=erp PGPASSWORD=erp bash scripts/gate_release.sh`

4. `gate-reconciliation`
- operational-to-financial truth checks with mismatch artifact.

5. `gate-quality`
- mutation threshold (`60.0`) + actionable-signal checks (`min_scored_total=50`, `max_excluded_ratio=0.80`) + rolling flake-rate threshold + catalog governance.

## Failure semantics

- Any invariant mismatch, drift-scan hit, or required-threshold miss is `NO-GO`.
- No legacy test file can be promoted as truth evidence for these gates.
