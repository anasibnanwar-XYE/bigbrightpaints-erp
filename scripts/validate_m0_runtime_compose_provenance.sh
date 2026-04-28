#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT="$(cd -- "${SCRIPT_DIR}/.." >/dev/null 2>&1 && pwd)"
APPROVED_WORKTREE="${APPROVED_WORKTREE:-/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/super-admin-redesign}"
MISSION_DIR="${MISSION_DIR:-/Users/anas/.factory/missions/c22fb3a9-6009-4bbf-902d-b7af4d2864ea}"
VALIDATION_CONTRACT_PATH="${VALIDATION_CONTRACT_PATH:-${MISSION_DIR}/validation-contract.md}"
RUN_MARKER="${RUN_MARKER:-m0-runtime-compose-provenance-$(date -u +%Y%m%dT%H%M%SZ)}"
MODE="${1:---static}"

APP_PORT="${APP_PORT:-8081}"
MANAGEMENT_PORT="${MANAGEMENT_PORT:-9090}"
MAILHOG_UI_PORT="${MAILHOG_UI_PORT:-8025}"

fail() {
  echo "[m0-runtime-provenance] FAIL: $*" >&2
  exit 1
}

note() {
  echo "[m0-runtime-provenance] $*"
}

require_file() {
  [[ -f "$1" ]] || fail "required file missing: $1"
}

require_contains() {
  local file="$1"
  local needle="$2"
  grep -Fq -- "$needle" "$file" || fail "$file does not contain required text: $needle"
}

require_not_contains() {
  local file="$1"
  local needle="$2"
  if grep -Fq -- "$needle" "$file"; then
    fail "$file contains forbidden text: $needle"
  fi
}

require_reset_pins_after_env_source() {
  local reset_script="$1"
  python3 - "$reset_script" <<'PY'
import sys

path = sys.argv[1]
with open(path, "r", encoding="utf-8") as handle:
    lines = handle.readlines()

source_lines = [
    index
    for index, line in enumerate(lines)
    if 'source "$ROOT/.env"' in line or '. "$ROOT/.env"' in line
]

if not source_lines:
    print(f"{path} does not source $ROOT/.env", file=sys.stderr)
    sys.exit(1)

last_source_line = max(source_lines)
required_calls = [
    'pin_approved_runtime_port "DB_PORT" "$PINNED_DB_PORT"',
    'pin_approved_runtime_port "RABBIT_PORT" "$PINNED_RABBIT_PORT"',
    'pin_approved_runtime_port "RABBIT_MANAGEMENT_PORT" "$PINNED_RABBIT_MANAGEMENT_PORT"',
    'pin_approved_runtime_port "APP_PORT" "$PINNED_APP_PORT"',
    'pin_approved_runtime_port "MANAGEMENT_PORT" "$PINNED_MANAGEMENT_PORT"',
    'pin_approved_runtime_port "MAILHOG_SMTP_PORT" "$PINNED_MAILHOG_SMTP_PORT"',
    'pin_approved_runtime_port "MAILHOG_UI_PORT" "$PINNED_MAILHOG_UI_PORT"',
]

missing = []
for call in required_calls:
    if not any(call in line for line in lines[last_source_line + 1 :]):
        missing.append(call)

if missing:
    print(
        f"{path} does not pin approved runtime ports after .env sourcing: {', '.join(missing)}",
        file=sys.stderr,
    )
    sys.exit(1)
PY
}

require_base_compose_render_without_datadog_key() {
  local compose_stdout compose_stderr
  compose_stdout="$(mktemp)"
  compose_stderr="$(mktemp)"

  if ! (
    cd "$ROOT" &&
      env -u DD_API_KEY COMPOSE_DISABLE_ENV_FILE=1 docker compose config >"$compose_stdout" 2>"$compose_stderr"
  ); then
    fail "base docker compose config must render with DD_API_KEY unset and Datadog profile disabled"
  fi

  if ! (
    cd "$ROOT" &&
      COMPOSE_DISABLE_ENV_FILE=1 DD_API_KEY="" docker compose config >"$compose_stdout" 2>"$compose_stderr"
  ); then
    fail "base docker compose config must render with DD_API_KEY empty and Datadog profile disabled"
  fi

  if ! (
    cd "$ROOT" &&
      COMPOSE_DISABLE_ENV_FILE=1 DD_API_KEY="synthetic-validation-key" docker compose --profile datadog config >"$compose_stdout" 2>"$compose_stderr"
  ); then
    fail "Datadog-profile docker compose config must render when DD_API_KEY is supplied server-side"
  fi
}

status_code() {
  local url="$1"
  local output="$2"
  curl -sS -o "$output" -w '%{http_code}' "$url" || true
}

json_status_field() {
  local file="$1"
  python3 - "$file" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    print(payload.get("status", "<missing>"))
except Exception:
    print("<non-json>")
PY
}

json_component_names() {
  local file="$1"
  python3 - "$file" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    components = payload.get("components")
    if isinstance(components, dict) and components:
        print(",".join(sorted(components.keys())))
    else:
        print("<not-exposed>")
except Exception:
    print("<non-json>")
PY
}

print_provenance() {
  cd "$ROOT"
  [[ "$ROOT" == "$APPROVED_WORKTREE" ]] || fail "worktree path mismatch: $ROOT"
  require_file "$VALIDATION_CONTRACT_PATH"

  local branch upstream head origin_main merge_base contract_hash openapi_hash migration_set status_summary
  branch="$(git rev-parse --abbrev-ref HEAD)"
  upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
  head="$(git rev-parse HEAD)"
  origin_main="$(git rev-parse origin/main)"
  merge_base="$(git merge-base HEAD origin/main)"
  contract_hash="$(shasum -a 256 "$VALIDATION_CONTRACT_PATH" | awk '{print $1}')"
  openapi_hash="$(shasum -a 256 "$ROOT/openapi.json" | awk '{print $1}')"
  migration_set="${MIGRATION_SET:-v2}"
  status_summary="$(
    git status --porcelain |
      python3 -c 'import sys, collections; c=collections.Counter(line[:2].strip() or "??" for line in sys.stdin if line.strip()); print("clean" if not c else ",".join(f"{k}:{v}" for k,v in sorted(c.items())))'
  )"

  [[ "$branch" == "factory/super-admin-redesign" ]] || fail "branch mismatch: $branch"
  [[ "$upstream" == "origin/main" ]] || fail "upstream mismatch: ${upstream:-<none>}"
  [[ "$migration_set" == "v2" ]] || fail "MIGRATION_SET must be v2, found $migration_set"

  note "run_marker_start=$RUN_MARKER"
  note "pwd=$ROOT"
  note "branch=$branch"
  note "upstream=$upstream"
  note "head_sha=$head"
  note "origin_main_sha=$origin_main"
  note "merge_base_sha=$merge_base"
  note "worktree_status_summary=$status_summary"
  note "validation_contract_sha256=$contract_hash"
  note "openapi_json_sha256=$openapi_hash"
  note "runtime_profile=${SPRING_PROFILES_ACTIVE:-prod,flyway-v2}"
  note "MIGRATION_SET=$migration_set"
  if [[ -f "$ROOT/.env" ]]; then
    note ".env=present(redacted; values not read by this guard)"
  else
    note ".env=absent"
  fi
}

run_static_checks() {
  local compose="$ROOT/docker-compose.yml"
  local prod_yml="$ROOT/erp-domain/src/main/resources/application-prod.yml"
  local reset_script="$ROOT/scripts/reset_final_validation_runtime.sh"

  require_file "$compose"
  require_file "$prod_yml"
  require_file "$reset_script"

  require_contains "$compose" '"127.0.0.1:${DB_PORT:-5433}:5432"'
  require_contains "$compose" '"127.0.0.1:${RABBIT_PORT:-5672}:5672"'
  require_contains "$compose" '"127.0.0.1:${RABBIT_MANAGEMENT_PORT:-15672}:15672"'
  require_contains "$compose" '"127.0.0.1:${MAILHOG_SMTP_PORT:-1025}:1025"'
  require_contains "$compose" '"127.0.0.1:${MAILHOG_UI_PORT:-8025}:8025"'
  require_contains "$compose" '"127.0.0.1:${APP_PORT:-8081}:8081"'
  require_contains "$compose" '"127.0.0.1:${MANAGEMENT_PORT:-9090}:9090"'
  require_not_contains "$compose" '${DB_PORT:-5432}'
  require_not_contains "$compose" '${APP_PORT:-18081}'
  require_not_contains "$compose" '${MANAGEMENT_PORT:-19090}'
  require_contains "$compose" 'DD_JAVA_OPTS: ${DD_API_KEY:+-javaagent:/app/dd-java-agent.jar}'
  require_contains "$compose" 'DD_AGENT_HOST: ${DD_API_KEY:+datadog-agent}'
  require_contains "$compose" 'DD_API_KEY: ${DD_API_KEY:-}'
  require_contains "$compose" 'profiles: ["datadog"]'
  require_base_compose_render_without_datadog_key

  require_contains "$prod_yml" 'enabled: false'
  require_contains "$prod_yml" 'include: health,info'
  require_contains "$prod_yml" 'access: none'
  require_contains "$prod_yml" 'port: ${MANAGEMENT_SERVER_PORT:9090}'

  require_contains "$reset_script" 'PINNED_DB_PORT="5433"'
  require_contains "$reset_script" 'PINNED_RABBIT_PORT="5672"'
  require_contains "$reset_script" 'PINNED_RABBIT_MANAGEMENT_PORT="15672"'
  require_contains "$reset_script" 'PINNED_APP_PORT="8081"'
  require_contains "$reset_script" 'PINNED_MANAGEMENT_PORT="9090"'
  require_contains "$reset_script" 'PINNED_MAILHOG_SMTP_PORT="1025"'
  require_contains "$reset_script" 'PINNED_MAILHOG_UI_PORT="8025"'
  require_contains "$reset_script" '[[ "${!variable_name+x}" == "x" && "$current_value" != "$approved_value" ]]'
  require_contains "$reset_script" 'pin_approved_runtime_port "DB_PORT" "$PINNED_DB_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "RABBIT_PORT" "$PINNED_RABBIT_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "RABBIT_MANAGEMENT_PORT" "$PINNED_RABBIT_MANAGEMENT_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "APP_PORT" "$PINNED_APP_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "MANAGEMENT_PORT" "$PINNED_MANAGEMENT_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "MAILHOG_SMTP_PORT" "$PINNED_MAILHOG_SMTP_PORT"'
  require_contains "$reset_script" 'pin_approved_runtime_port "MAILHOG_UI_PORT" "$PINNED_MAILHOG_UI_PORT"'
  require_contains "$reset_script" 'approved_runtime_ports=pinned-after-env-source'
  require_reset_pins_after_env_source "$reset_script"

  note "static_compose_ports=approved-localhost-only"
  note "static_datadog_disabled_boot=no-DD_API_KEY-required-for-app-service"
  note "static_actuator_boundaries=management-port-9090-health-info-only"
}

run_runtime_strict_checks() {
  local health_out readiness_out auth_out app_actuator_out mailhog_out
  health_out="$(mktemp)"
  readiness_out="$(mktemp)"
  auth_out="$(mktemp)"
  app_actuator_out="$(mktemp)"
  mailhog_out="$(mktemp)"

  local health readiness auth app_actuator mailhog
  health="$(status_code "http://localhost:${MANAGEMENT_PORT}/actuator/health" "$health_out")"
  readiness="$(status_code "http://localhost:${MANAGEMENT_PORT}/actuator/health/readiness" "$readiness_out")"
  auth="$(status_code "http://localhost:${APP_PORT}/api/v1/auth/me" "$auth_out")"
  app_actuator="$(status_code "http://localhost:${APP_PORT}/actuator/health" "$app_actuator_out")"
  mailhog="$(status_code "http://localhost:${MAILHOG_UI_PORT}/api/v2/messages" "$mailhog_out")"

  [[ "$health" == "200" ]] || fail "management health expected 200, got $health"
  [[ "$readiness" == "200" ]] || fail "management readiness expected 200, got $readiness"
  [[ "$auth" == "401" || "$auth" == "403" ]] || fail "anonymous /auth/me expected 401/403, got $auth"
  [[ "$app_actuator" != "200" ]] || fail "app port must not expose actuator health"
  [[ "$mailhog" == "200" ]] || fail "MailHog API expected 200, got $mailhog"

  note "strict_health_http=$health status=$(json_status_field "$health_out")"
  note "strict_readiness_http=$readiness status=$(json_status_field "$readiness_out")"
  note "strict_auth_me_anonymous_http=$auth"
  note "strict_app_port_actuator_http=$app_actuator"
  local mailhog_summary
  mailhog_summary="$(python3 - "$mailhog_out" <<'PY'
import json
import sys

try:
    with open(sys.argv[1], "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    total = payload.get("total")
    items = payload.get("items") or []
    safe = []
    for item in items[:5]:
        content = item.get("Content") or {}
        headers = content.get("Headers") or {}
        subject = (headers.get("Subject") or ["<none>"])[0]
        safe.append(f"{item.get('ID', '<no-id>')}:{subject}")
    print(f"total={total};ids_subjects={'|'.join(safe) if safe else '<none>'};tokens=redacted")
except Exception:
    print("unparseable")
PY
)"
  note "mailhog_http=$mailhog message_summary=$mailhog_summary"

  local endpoint code out
  for endpoint in env configprops beans metrics prometheus loggers heapdump threaddump shutdown; do
    out="$(mktemp)"
    code="$(status_code "http://localhost:${MANAGEMENT_PORT}/actuator/${endpoint}" "$out")"
    case "$code" in
      401|403|404|405) ;;
      *) fail "management non-health endpoint /actuator/${endpoint} exposed unexpected HTTP $code" ;;
    esac
    note "management_endpoint_${endpoint}_http=$code"
  done
}

run_runtime_seeded_checks() {
  local health_out readiness_out health readiness
  health_out="$(mktemp)"
  readiness_out="$(mktemp)"
  health="$(status_code "http://localhost:${MANAGEMENT_PORT}/actuator/health" "$health_out")"
  readiness="$(status_code "http://localhost:${MANAGEMENT_PORT}/actuator/health/readiness" "$readiness_out")"

  case "$health" in
    200)
      note "seeded_health_classification=full-health http=$health status=$(json_status_field "$health_out") components=$(json_component_names "$health_out")"
      ;;
    503)
      note "seeded_health_classification=deterministic-degraded http=$health status=$(json_status_field "$health_out") components=$(json_component_names "$health_out")"
      ;;
    *)
      fail "seeded management health expected deterministic 200 or 503, got $health"
      ;;
  esac
  case "$readiness" in
    200)
      note "seeded_readiness_classification=full-ready http=$readiness status=$(json_status_field "$readiness_out") components=$(json_component_names "$readiness_out")"
      ;;
    503)
      note "seeded_readiness_classification=deterministic-degraded http=$readiness status=$(json_status_field "$readiness_out") components=$(json_component_names "$readiness_out")"
      ;;
    *)
      fail "seeded readiness expected deterministic 200 or 503, got $readiness"
      ;;
  esac
}

run_flyway_bootstrap_check() {
  if ! command -v docker >/dev/null 2>&1; then
    fail "docker is required for Flyway bootstrap evidence"
  fi

  local count
  count="$(
    docker exec erp_db psql -U erp -d erp_domain -Atc \
      "select count(*) from flyway_schema_history_v2 where success = true;" 2>/dev/null || true
  )"
  [[ "$count" =~ ^[0-9]+$ ]] || fail "could not read flyway_schema_history_v2 count"
  [[ "$count" -gt 0 ]] || fail "flyway_schema_history_v2 has no successful migrations"
  note "flyway_v2_successful_migration_count=$count"
}

print_provenance
run_static_checks

case "$MODE" in
  --static)
    ;;
  --runtime-strict)
    run_runtime_strict_checks
    run_flyway_bootstrap_check
    ;;
  --runtime-seeded)
    run_runtime_seeded_checks
    ;;
  *)
    fail "unknown mode: $MODE"
    ;;
esac

note "run_marker_end=$RUN_MARKER"
