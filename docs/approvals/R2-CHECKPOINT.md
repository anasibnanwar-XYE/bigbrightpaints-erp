# R2 Checkpoint

## Scope
- Feature: `ERP-38 remove-legacy-production-batches-surface`
- Branch: `refactor/erp-38-canonical-factory-flow`
- Review candidate: remove `GET/POST /api/v1/factory/production-batches`, delete `FactoryService.logBatch(...)`, remove the retired batch DTOs and the orphaned orchestrator-side `IntegrationCoordinator.releaseInventory(...)` caller, then refresh OpenAPI/endpoint inventory/frontend handoff surfaces so `POST /api/v1/factory/production/logs` remains the only public batch-create contract.
- Why this is R2: the packet touches `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java`, which is an R2-governed high-risk path, while hard-cutting a public factory execution surface and an internal orchestrator seam in the same change set.

## Risk Trigger
- Triggered by changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java`, plus the paired factory runtime/test/contract surfaces that remove the legacy batch path.
- Contract surfaces affected: controller mappings under `/api/v1/factory`, `FactoryService` API shape, the generated `openapi.json` snapshot, `docs/endpoint-inventory.md`, `erp-domain/docs/endpoint_inventory.tsv`, `.factory/library/frontend-handoff.md`, and the orchestrator background-flow review note.
- Failure mode if wrong: callers could still depend on a removed factory route or stale orchestrator batch helper, producing broken runtime dispatch/factory expectations or contradictory published API guidance.

## Approval Authority
- Mode: orchestrator
- Approver: `ERP-38 mission orchestrator`
- Canary owner: `ERP-38 mission orchestrator`
- Approval status: `pending green validators and remote review`
- Basis: this is a compatibility-preserving hard-cut that removes a retired route/caller without widening privileges, tenant boundaries, or migration/destructive data risk.

## Escalation Decision
- Human escalation required: no
- Reason: the packet only removes legacy factory/orchestrator ownership seams and stale contract artifacts; it does not add new authority, change persistence/migration semantics, or widen dispatch/factory behavior.

## Rollback Owner
- Owner: `ERP-38 mission orchestrator`
- Rollback method: revert the packet commit if any downstream ERP-38 slice still depends on the retired route/caller; do not restore the legacy seam piecemeal or via hidden compatibility bridges.
- Rollback trigger:
  - runtime probes or targeted regressions show `/api/v1/factory/production/logs` no longer remains the surviving batch-create contract
  - an internal caller outside this packet still requires `FactoryService.logBatch(...)` or the removed orchestrator release-batch helper
  - published contract artifacts drift from the runtime surface after the hard-cut

## Expiry
- Valid until: `2026-04-01`
- Re-evaluate if: the packet scope expands beyond removing the legacy batch surface/caller set, or if follow-up ERP-38 work reintroduces orchestrator-owned factory writes.

## Verification Evidence
- Commands run:
  - `ROOT=$(git rev-parse --show-toplevel) && cd "$ROOT/erp-domain" && MIGRATION_SET=v2 mvn -T8 compile -q`
  - `ROOT=$(git rev-parse --show-toplevel) && cd "$ROOT/erp-domain" && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest=CR_FactoryLegacyBatchProdGatingIT,FactoryServiceTest,OpenApiSnapshotIT,IntegrationCoordinatorTest,CommandDispatcherTest test`
  - `ROOT=$(git rev-parse --show-toplevel) && cd "$ROOT" && bash ci/check-codex-review-guidelines.sh && bash scripts/guard_openapi_contract_drift.sh && bash scripts/guard_workflow_canonical_paths.sh && bash scripts/guard_accounting_portal_scope_contract.sh`
- Result summary:
  - focused regression coverage proves the legacy factory controller route is absent, the service no longer exposes `logBatch`, the retired DTO source files are gone, and the stale orchestrator release-batch helper is removed.
  - OpenAPI and endpoint inventory contract guards stay aligned after the hard-cut and refresh.
- Artifacts/links:
  - Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/ERP-38-canonical-factory-flow`
  - Validation scope: `VAL-BATCH-004`
