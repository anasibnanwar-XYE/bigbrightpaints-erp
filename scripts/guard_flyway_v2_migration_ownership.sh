#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATIONS_DIR="$ROOT_DIR/erp-domain/src/main/resources/db/migration_v2"

fail() {
  echo "[guard_flyway_v2_migration_ownership] FAIL: $1" >&2
  exit 1
}

[[ -d "$MIGRATIONS_DIR" ]] || fail "missing migrations dir: $MIGRATIONS_DIR"
git -C "$ROOT_DIR" rev-parse --git-dir >/dev/null 2>&1 || fail "repository metadata unavailable"

mapfile -t migration_files < <(find "$MIGRATIONS_DIR" -maxdepth 1 -type f -name 'V*__*.sql' | sort -V)
[[ ${#migration_files[@]} -gt 0 ]] || fail "no migration_v2 files found"

untracked=()
for abs_path in "${migration_files[@]}"; do
  rel_path="${abs_path#"$ROOT_DIR"/}"
  if ! git -C "$ROOT_DIR" ls-files --error-unmatch "$rel_path" >/dev/null 2>&1; then
    untracked+=("$rel_path")
  fi
done

if [[ ${#untracked[@]} -gt 0 ]]; then
  printf '[guard_flyway_v2_migration_ownership] Untracked migration_v2 files:\n' >&2
  printf '  - %s\n' "${untracked[@]}" >&2
  fail "all migration_v2 files must be tracked"
fi

# Detect CREATE TABLE blocks that define `id bigint NOT NULL` but never establish
# PRIMARY KEY/UNIQUE on that table (inline or via ALTER TABLE).
mapfile -t missing_pk_tables < <(
  awk '
    BEGIN { IGNORECASE=1 }

    function normalize_table(raw, cleaned) {
      cleaned = raw
      gsub(/"/, "", cleaned)
      gsub(/\(/, "", cleaned)
      gsub(/;/, "", cleaned)
      sub(/^ONLY[[:space:]]+/, "", cleaned)
      sub(/^public\./, "", cleaned)
      return cleaned
    }

    function maybe_mark_primary_key(table_name, line) {
      if (table_name == "") return
      if (line ~ /PRIMARY KEY/ || line ~ /UNIQUE[[:space:]]*\([[:space:]]*id[[:space:]]*\)/) {
        has_pk[table_name] = 1
      }
    }

    /^[[:space:]]*CREATE[[:space:]]+TABLE/ {
      in_create = 1
      in_alter = 0
      create_has_id = 0
      create_has_pk = 0

      table_name = ""
      for (i = 1; i <= NF; i++) {
        if (toupper($i) == "TABLE") {
          if (toupper($(i+1)) == "IF" && toupper($(i+2)) == "NOT" && toupper($(i+3)) == "EXISTS") {
            table_name = $(i+4)
          } else {
            table_name = $(i+1)
          }
          break
        }
      }

      current_table = normalize_table(table_name)
      next
    }

    in_create {
      if ($0 ~ /^[[:space:]]*id[[:space:]]+bigint[[:space:]]+NOT[[:space:]]+NULL/) {
        create_has_id = 1
      }
      maybe_mark_primary_key(current_table, $0)
      if ($0 ~ /\)[[:space:]]*;/) {
        if (create_has_id && !has_pk[current_table]) {
          needs_pk[current_table] = 1
        }
        in_create = 0
      }
      next
    }

    /^[[:space:]]*ALTER[[:space:]]+TABLE/ {
      in_alter = 1
      alter_table = ""
      for (i = 1; i <= NF; i++) {
        if (toupper($i) == "TABLE") {
          if (toupper($(i+1)) == "ONLY") {
            alter_table = $(i+2)
          } else {
            alter_table = $(i+1)
          }
          break
        }
      }
      alter_table = normalize_table(alter_table)
      maybe_mark_primary_key(alter_table, $0)
      if ($0 ~ /;/) in_alter = 0
      next
    }

    in_alter {
      maybe_mark_primary_key(alter_table, $0)
      if ($0 ~ /;/) in_alter = 0
      next
    }

    END {
      for (table in needs_pk) {
        if (!has_pk[table]) {
          print table
        }
      }
    }
  ' "${migration_files[@]}" | sort -u
)

if [[ ${#missing_pk_tables[@]} -gt 0 ]]; then
  printf '[guard_flyway_v2_migration_ownership] Tables missing PK/UNIQUE(id) contract:\n' >&2
  printf '  - %s\n' "${missing_pk_tables[@]}" >&2
  fail "migration_v2 id-column ownership contract violated"
fi

echo "[guard_flyway_v2_migration_ownership] OK"
