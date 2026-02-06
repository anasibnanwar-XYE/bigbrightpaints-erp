# Gate Evidence - 2026-02-06

## Commands Executed

1. `DIFF_BASE=$(git rev-parse HEAD~1) bash scripts/gate_fast.sh`
2. `bash scripts/gate_core.sh`
3. `bash scripts/gate_reconciliation.sh`
4. `bash scripts/gate_quality.sh`
5. `PGHOST=127.0.0.1 PGPORT=55432 PGUSER=erp PGPASSWORD=erp bash scripts/gate_release.sh`

## Outcomes

- gate-fast: PASS
  - artifact: `artifacts/gate-fast/changed-coverage.json`
  - note: local long-lived branch run used explicit `DIFF_BASE`; PR mode still defaults to merge-base against `origin/main`.
- gate-core: PASS
  - artifact: `artifacts/gate-core/module-coverage.json`
  - line_ratio: `0.7010` (threshold `0.55`)
  - branch_ratio: `0.5556` (threshold `0.35`)
  - active_classes: `9` (min `7`)
  - active_packages: `5` (min `4`)
- gate-reconciliation: PASS
  - artifact: `artifacts/gate-reconciliation/reconciliation-summary.json`
  - tests: `71`, failures: `0`, errors: `0`
- gate-quality: PASS
  - artifacts:
    - `artifacts/gate-quality/mutation-summary.json`
    - `artifacts/gate-quality/flake-rate.json`
  - mutation_score: `63.889` (threshold `60.0`)
  - scored_total: `180` (min `50`)
  - excluded_ratio: `0.2623` (max `0.80`)
  - flake window: `20/20` runs, flake_rate `0.0`
- gate-release: PASS
  - artifacts:
    - `artifacts/gate-release/migration-matrix.json`
    - `artifacts/gate-release/predeploy-scans-fresh.txt`
    - `artifacts/gate-release/predeploy-scans-upgrade.txt`
  - migration matrix:
    - expected_count: `132`
    - expected_max_version: `132`
    - fresh: `132/132`
    - upgrade: `132/132`

## Key Fixes Applied During This Run

- `scripts/release_migration_matrix.sh`
  - fallback credentials order hardened:
    - `PGUSER/PGPASSWORD` -> `SPRING_DATASOURCE_USERNAME/SPRING_DATASOURCE_PASSWORD` -> `erp/erp`
- `docs/CODE-RED/confidence-suite/GATE_CONTRACTS.md`
  - documented local release DB override command and fallback behavior.
- `docs/CODE-RED/confidence-suite/final-enterprise/TESTING_STRATEGY.md`
  - documented local release lane DB env usage.

