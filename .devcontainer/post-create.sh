#!/bin/bash
set -euo pipefail

echo "=== BigBright ERP Dev Container Post-Create Setup ==="

# Verify toolchain
echo ""
echo "Verifying toolchain..."
java -version
echo ""

# Ensure Maven is available via SDKMAN
if ! command -v mvn &> /dev/null; then
    echo "Initializing SDKMAN and installing Maven..."
    source /usr/local/sdkman/bin/sdkman-init.sh
fi

mvn -version
echo ""

# Verify Docker access (works with Colima on macOS, Docker Desktop on Windows/Linux)
echo "Verifying Docker access..."
if docker info &> /dev/null; then
    echo "Docker is accessible"
else
    echo "Warning: Docker socket may not be accessible. If using Colima, ensure it is running."
fi
echo ""

# Verify psql
echo "Verifying PostgreSQL client..."
psql --version
echo ""

# Wait for database to be ready (with timeout)
echo "Waiting for database to be ready..."
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if PGPASSWORD=erp psql -h db -U erp -d erp_domain -c "SELECT 1" &> /dev/null; then
        echo "Database is ready!"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts - waiting for database..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo "Warning: Database not ready after timeout. Tests requiring DB may fail."
fi
echo ""

# Warm Maven dependencies by compiling once
echo "Warming Maven dependencies..."
cd /workspace/erp-domain
MIGRATION_SET=v2 mvn -q -ntp -DskipTests dependency:go-offline || echo "Note: Some dependencies may need to be downloaded on first build"
MIGRATION_SET=v2 mvn -q -ntp -DskipTests compile || echo "Note: First compile completed"
cd /workspace
echo ""

echo "=== Dev Container Setup Complete ==="
echo ""
echo "Useful commands:"
echo "  cd erp-domain && MIGRATION_SET=v2 mvn test -Pgate-fast -Djacoco.skip=true  # Run quick gate tests"
echo "  cd erp-domain && MIGRATION_SET=v2 mvn test -Djacoco.skip=true              # Run all tests"
echo "  bash scripts/gate_fast.sh                                                    # CI-aligned gate script"
echo ""
