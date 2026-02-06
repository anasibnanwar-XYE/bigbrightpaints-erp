#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ARTIFACT_DIR="$ROOT_DIR/artifacts/gate-fast"
TRUTH_TEST_ROOT="$ROOT_DIR/erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite"
rm -rf "$ARTIFACT_DIR"
mkdir -p "$ARTIFACT_DIR"

resolve_diff_base() {
  if [[ -n "${DIFF_BASE:-}" ]]; then
    echo "$DIFF_BASE"
    return 0
  fi

  if [[ -n "${GITHUB_BASE_SHA:-}" ]]; then
    echo "$GITHUB_BASE_SHA"
    return 0
  fi

  if git rev-parse --verify --quiet origin/main >/dev/null; then
    git merge-base origin/main HEAD
    return 0
  fi

  if git rev-parse --verify --quiet main >/dev/null; then
    git merge-base main HEAD
    return 0
  fi

  if git rev-parse --verify --quiet origin/master >/dev/null; then
    git merge-base origin/master HEAD
    return 0
  fi

  echo "HEAD~1"
}

echo "[gate-fast] validate catalog"
python3 "$ROOT_DIR/scripts/validate_test_catalog.py" \
  --catalog "$ROOT_DIR/docs/CODE-RED/confidence-suite/TEST_CATALOG.json" \
  --quarantine "$ROOT_DIR/scripts/test_quarantine.txt" \
  --tests-root "$TRUTH_TEST_ROOT" \
  --gate gate-fast \
  --output "$ARTIFACT_DIR/catalog-validation.json"

echo "[gate-fast] flaky guard"
python3 "$ROOT_DIR/scripts/check_flaky_tags.py" \
  --tests-root "$TRUTH_TEST_ROOT" \
  --quarantine "$ROOT_DIR/scripts/test_quarantine.txt" \
  --gate gate-fast \
  --output "$ARTIFACT_DIR/flake-guard.json"

echo "[gate-fast] run critical truth tests"
(
  cd "$ROOT_DIR/erp-domain"
  rm -rf target/surefire-reports target/site/jacoco target/jacoco.exec
  mvn -B -ntp -Pgate-fast test
)

DIFF_BASE="$(resolve_diff_base)"
echo "[gate-fast] changed-files coverage against base=$DIFF_BASE"
python3 "$ROOT_DIR/scripts/changed_files_coverage.py" \
  --jacoco "$ROOT_DIR/erp-domain/target/site/jacoco/jacoco.xml" \
  --diff-base "$DIFF_BASE" \
  --src-root erp-domain/src/main/java \
  --threshold-line 0.95 \
  --threshold-branch 0.90 \
  --output "$ARTIFACT_DIR/changed-coverage.json"

echo "[gate-fast] OK"
