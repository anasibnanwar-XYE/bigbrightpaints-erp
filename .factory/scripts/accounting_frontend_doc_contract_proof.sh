#!/usr/bin/env bash
set -euo pipefail
ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT/erp-domain"
MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Dtest='SettlementControllerIdempotencyHeaderParityTest,AccountingEndpointContractTest,AccountingApplicationExceptionAdviceTest,AccountingApplicationExceptionResponsesTest,ReportControllerRouteContractIT,ReportControllerSecurityIT,PortalFinanceControllerIT,StatementAgingIT,DealerLedgerIT,AdminApprovalRbacIT,OpenApiSnapshotIT' test
