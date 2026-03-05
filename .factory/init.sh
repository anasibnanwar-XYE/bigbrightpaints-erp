#!/bin/bash
set -euo pipefail

PROJECT_ROOT="/home/realnigga/Desktop/Mission-control"
ERP_DIR="$PROJECT_ROOT/erp-domain"
REVIEW_DIR="$PROJECT_ROOT/docs/code-review"

# Ensure .env exists for docker-compose
if [ ! -f "$PROJECT_ROOT/.env" ]; then
  cp "$PROJECT_ROOT/.env.example" "$PROJECT_ROOT/.env"
  # Append missing vars that .env.example does not include
  grep -q 'ERP_SECURITY_ENCRYPTION_KEY' "$PROJECT_ROOT/.env" || \
    echo "ERP_SECURITY_ENCRYPTION_KEY=YOUR_ENCRYPTION_KEY_HERE" >> "$PROJECT_ROOT/.env"
fi

# Ensure review documentation folders exist for review-only missions.
mkdir -p "$REVIEW_DIR/flows"

# Keep worker startup lightweight for review-only missions.
# A quiet compile is best-effort only and should never block the session.
cd "$ERP_DIR"
mvn compile -q >/dev/null 2>&1 || true
