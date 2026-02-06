# Gate Evidence - 2026-02-06 (Strict Branch-as-Trunk Validation)

## Candidate

- `RELEASE_ANCHOR_SHA=281c884d8424e5fef8f148328ed10baf4b34293a`

## Commands Executed

1. `DB_PORT=55432 docker compose up -d db`
2. `DIFF_BASE=281c884d8424e5fef8f148328ed10baf4b34293a GATE_FAST_RELEASE_VALIDATION_MODE=true bash scripts/gate_fast.sh`
3. `bash scripts/gate_core.sh`
4. `bash scripts/gate_reconciliation.sh`
5. `PGHOST=127.0.0.1 PGPORT=55432 PGUSER=erp PGPASSWORD=erp bash scripts/gate_release.sh`
6. `bash scripts/gate_quality.sh`

## Gate Outcomes

- `gate-fast`: `PASS` (`exit=0`)
  - artifact: `artifacts/gate-fast/changed-coverage.json`
  - `diff_base=281c884d8424e5fef8f148328ed10baf4b34293a`
  - `files_considered=0`, `line_ratio=1.0` (threshold `0.95`), `branch_ratio=1.0` (threshold `0.90`)
  - release validation mode enforced explicit `DIFF_BASE` (no `HEAD~N` accepted)
- `gate-core`: `PASS` (`exit=0`)
  - artifact: `artifacts/gate-core/module-coverage.json`
  - strict thresholds: `line_threshold=0.92`, `branch_threshold=0.85`
  - results: `line_ratio=0.9887323943661972`, `branch_ratio=0.93125`
  - `active_classes=10` (min `7`), `active_packages=5` (min `4`)
- `gate-reconciliation`: `PASS` (`exit=0`)
  - artifact: `artifacts/gate-reconciliation/reconciliation-summary.json`
  - tests: `93`, failures: `0`, errors: `0`, skipped: `0`
- `gate-release`: `PASS` (`exit=0`)
  - artifacts:
    - `artifacts/gate-release/migration-matrix.json`
    - `artifacts/gate-release/predeploy-scans-fresh.txt`
    - `artifacts/gate-release/predeploy-scans-upgrade.txt`
  - migration matrix:
    - expected_count: `132`
    - expected_max_version: `132`
    - fresh: `132/132`
    - upgrade: `132/132`
  - strict verify path passed (`VERIFY_LOCAL_SKIP_TESTS=true`, `FAIL_ON_FINDINGS=true`)
- `gate-quality`: `PASS` (`exit=0`)
  - artifacts:
    - `artifacts/gate-quality/mutation-summary.json`
    - `artifacts/gate-quality/flake-rate.json`
  - strict mutation rules:
    - `mutation_score=84.298` (threshold `80.0`)
    - `scored_total=121` (min `120`)
    - `excluded_ratio=0.04724` (max `0.60`)
  - flake window:
    - `runs_evaluated=20/20`
    - `flake_rate=0.0` (threshold `< 0.01`)

## Gate Selection Proof (No Legacy Test Dependency)

- `scripts/gate_fast.sh`: `TRUTH_TEST_ROOT=.../erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite`
- `scripts/gate_core.sh`: same `TRUTH_TEST_ROOT` and `mvn -Pgate-core test`
- `scripts/gate_reconciliation.sh`: same `TRUTH_TEST_ROOT` and `mvn -Pgate-reconciliation test`
- `scripts/gate_release.sh`: same `TRUTH_TEST_ROOT` and `mvn -Pgate-release test`
- `scripts/gate_quality.sh`: same `TRUTH_TEST_ROOT`; mutation and flake lanes executed from truthsuite gate profiles
- `erp-domain/pom.xml`: gate profiles include `**/truthsuite/**/*Test.java`, `**/truthsuite/**/*IT.java`, `**/truthsuite/**/*Suite.java`

## Notes

- This validation run is recorded against `RELEASE_ANCHOR_SHA` above.
- Working tree contains additional local modifications; immutable promotion should use a committed SHA containing the validated tree.
