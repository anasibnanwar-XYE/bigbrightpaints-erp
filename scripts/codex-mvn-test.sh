#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$REPO_ROOT"

# In Codex/cloud containers, internet access is often restricted during runtime.
# Prefetch dependencies first so tests don't fail due to network restrictions.
mvn -B -ntp -f erp-domain/pom.xml -s .mvn/settings.xml -DskipTests dependency:go-offline

cd erp-domain
mvn -B -ntp -s ../.mvn/settings.xml test
