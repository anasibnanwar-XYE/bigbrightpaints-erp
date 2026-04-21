#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(git rev-parse --show-toplevel)"
ENV_FILE="$PROJECT_ROOT/.env"
ENV_EXAMPLE="$PROJECT_ROOT/.env.example"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
fi

generate_token() {
  python3 - <<'PY'
import secrets
print(secrets.token_urlsafe(48))
PY
}

generate_hex_key() {
  python3 - <<'PY'
import secrets
print(secrets.token_hex(32))
PY
}

upsert_env_value() {
  key="$1"
  value="$2"
  temp_file="$(mktemp "${TMPDIR:-/tmp}/accounting-mission-env.XXXXXX")"

  awk -v key="$key" -v value="$value" '
    BEGIN { replaced = 0 }
    index($0, key "=") == 1 {
      print key "=" value
      replaced = 1
      next
    }
    { print }
    END {
      if (!replaced) {
        print key "=" value
      }
    }
  ' "$ENV_FILE" > "$temp_file"

  mv "$temp_file" "$ENV_FILE"
}

current_env_value() {
  key="$1"
  python3 - "$ENV_FILE" "$key" <<'PY'
import sys
from pathlib import Path

env_path = Path(sys.argv[1])
key = sys.argv[2]
for line in env_path.read_text().splitlines():
    if line.startswith(f"{key}="):
        print(line.split("=", 1)[1])
        break
PY
}

ensure_value() {
  key="$1"
  value="$2"
  current_value="$(current_env_value "$key")"
  if [ "$current_value" != "$value" ]; then
    upsert_env_value "$key" "$value"
  fi
}

ensure_secret_if_missing() {
  key="$1"
  placeholder="$2"
  generator="$3"
  current_value="$(current_env_value "$key")"

  if [ -z "$current_value" ] || [ "$current_value" = "$placeholder" ] || [ "$current_value" = "placeholder" ]; then
    upsert_env_value "$key" "$($generator)"
  fi
}

ensure_value "SPRING_PROFILES_ACTIVE" "prod,flyway-v2,mock,validation-seed"
ensure_value "SPRING_DATASOURCE_URL" "jdbc:postgresql://db:5432/erp_domain"
ensure_value "SPRING_DATASOURCE_USERNAME" "erp"
ensure_value "DB_PORT" "5433"
ensure_value "APP_PORT" "18081"
ensure_value "MANAGEMENT_PORT" "19090"
ensure_value "MAILHOG_SMTP_PORT" "11025"
ensure_value "MAILHOG_UI_PORT" "18025"
ensure_value "RABBIT_PORT" "15673"
ensure_value "RABBIT_MANAGEMENT_PORT" "15674"
ensure_value "ERP_ENVIRONMENT_VALIDATION_ENABLED" "false"
ensure_value "ERP_ENVIRONMENT_VALIDATION_HEALTH_INDICATOR_SKIP_WHEN_VALIDATION_DISABLED" "true"
ensure_value "ERP_VALIDATION_SEED_ENABLED" "true"
ensure_value "ERP_INVENTORY_OPENING_STOCK_ENABLED" "true"
ensure_value "ERP_CORS_ALLOWED_ORIGINS" "https://app.bigbrightpaints.com"
ensure_value "ERP_CORS_ALLOW_TAILSCALE_HTTP_ORIGINS" "true"
ensure_value "ERP_MAIL_BASE_URL" "http://localhost:3000"
ensure_value "SPRING_MAIL_HOST" "mailhog"
ensure_value "SPRING_MAIL_PORT" "1025"
ensure_value "SPRING_MAIL_USERNAME" ""
ensure_value "SPRING_MAIL_PASSWORD" ""
ensure_value "SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH" "false"
ensure_value "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE" "false"
ensure_value "SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED" "false"
ensure_value "ERP_SECURITY_AUDIT_PRIVATE_KEY" "local-dev-audit-private-key-20260420"

ensure_secret_if_missing "SPRING_DATASOURCE_PASSWORD" "" generate_token
ensure_secret_if_missing "JWT_SECRET" "YOUR_JWT_SECRET_HERE" generate_token
ensure_secret_if_missing "ERP_SECURITY_ENCRYPTION_KEY" "YOUR_ENCRYPTION_KEY_HERE" generate_hex_key

# Compose validation requires deterministic mock + validation seed actors by default.
# Keep placeholders safe and deterministic for local validation runtime resets.
if [ -z "$(current_env_value "ERP_VALIDATION_SEED_PASSWORD")" ]; then
  ensure_value "ERP_VALIDATION_SEED_PASSWORD" "Validation1!AccountingMission"
fi
if [ -z "$(current_env_value "ERP_SEED_MOCK_ADMIN_EMAIL")" ]; then
  ensure_value "ERP_SEED_MOCK_ADMIN_EMAIL" "admin@local.test"
fi
if [ -z "$(current_env_value "ERP_SEED_MOCK_ADMIN_PASSWORD")" ]; then
  ensure_value "ERP_SEED_MOCK_ADMIN_PASSWORD" "Validation1!AccountingMission"
fi

echo "[accounting-mission-init] .env defaults are ready for compose validation on DB 5433 / app 18081 / actuator 19090 / MailHog 18025 / RabbitMQ 15673+15674"
