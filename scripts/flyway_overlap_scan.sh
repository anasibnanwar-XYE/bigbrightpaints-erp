#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATIONS_DIR="$ROOT_DIR/erp-domain/src/main/resources/db/migration"

FAIL_ON_FINDINGS="${FAIL_ON_FINDINGS:-false}"

if [[ ! -d "$MIGRATIONS_DIR" ]]; then
  echo "[flyway_overlap_scan] missing migrations dir: $MIGRATIONS_DIR"
  exit 2
fi

export MIGRATIONS_DIR FAIL_ON_FINDINGS

python3 - <<'PY'
import collections
import glob
import os
import re
import sys

migrations_dir = os.environ["MIGRATIONS_DIR"]
fail_on_findings = os.environ.get("FAIL_ON_FINDINGS", "false").lower() == "true"

files = sorted(glob.glob(os.path.join(migrations_dir, "V*__*.sql")))
if not files:
    print(f"[flyway_overlap_scan] no migrations found under: {migrations_dir}")
    sys.exit(2)

patterns = {
    "table": re.compile(
        r'\bCREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+("?)([A-Za-z0-9_.]+)\1',
        re.IGNORECASE,
    ),
    "index": re.compile(
        r'\bCREATE\s+(?:UNIQUE\s+)?INDEX(?:\s+IF\s+NOT\s+EXISTS)?\s+("?)([A-Za-z0-9_.]+)\1',
        re.IGNORECASE,
    ),
    "constraint": re.compile(r'\bCONSTRAINT\s+("?)([A-Za-z0-9_.]+)\1', re.IGNORECASE),
}

found: dict[str, dict[str, set[str]]] = {
    kind: collections.defaultdict(set) for kind in patterns.keys()
}

for path in files:
    base = os.path.basename(path)
    try:
        with open(path, "r", encoding="utf-8", errors="ignore") as fh:
            for line in fh:
                stripped = line.strip()
                if stripped.startswith("--"):
                    continue
                for kind, pat in patterns.items():
                    m = pat.search(line)
                    if not m:
                        continue
                    name = m.group(2).lower()
                    found[kind][name].add(base)
    except OSError as e:
        print(f"[flyway_overlap_scan] failed to read {path}: {e}")
        sys.exit(2)

findings = 0
print(f"[flyway_overlap_scan] scanning: {migrations_dir}")

for kind in ("table", "constraint", "index"):
    print()
    print(f"[flyway_overlap_scan] duplicate {kind} definitions (heuristic)")
    dups = {name: sorted(list(fs)) for name, fs in found[kind].items() if len(fs) > 1}
    if not dups:
        print("  (none)")
        continue
    findings = 1
    for name, fs in sorted(dups.items()):
        print(f"  {name}: {', '.join(fs)}")

print()
print(f"[flyway_overlap_scan] scanned {len(files)} migrations (findings={findings})")

if fail_on_findings and findings:
    sys.exit(1)
sys.exit(0)
PY
