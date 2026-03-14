# R2 Checkpoint

## Scope
- Feature: `o2c-dispatch-canonicalization` (post-merge regression closure)
- Branch: `codex/pr111-followups`
- High-risk paths touched: `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java`
- Why this is R2: this packet hardens the orchestrator canonical-path redirect following post-merge dispatch canonicalization; enterprise policy requires explicit same-diff approval evidence whenever orchestrator sources are touched.

## Risk Trigger
- Triggered by post-merge regression: `IntegrationCoordinator.updateFulfillment` error detail and message still referenced the old `/api/v1/dispatch/confirm` path that was removed in the dispatch canonicalization work; callers received a stale redirect target.
- Contract change: orchestrator now directs all dispatch-like status rejections to `/api/v1/sales/dispatch/confirm` (the order-capable sales dispatch route).
- Guard alignment: `scripts/guard_orchestrator_correlation_contract.sh` and `scripts/guard_workflow_canonical_paths.sh` updated to match the removed `dispatchBatch` methods and the new canonical path.

## Approval Authority
- Mode: orchestrator
- Approver: ERP truth-stabilization mission orchestration
- Basis: compatibility-preserving regression fix that corrects a stale error-detail path; no new privileges, tenant scope changes, or fallback behavior added.

## Escalation Decision
- Human escalation required: no
- Reason: the packet corrects a stale canonical-path redirect in error details and aligns guard contracts; it does not introduce new privileges, tenant boundary changes, or destructive migration risk.

## Rollback Owner
- Owner: Factory-droid integration worker
- Rollback method: revert the `codex/pr111-followups` packet commits, rerun `bash scripts/guard_workflow_canonical_paths.sh`, and rerun `bash ci/check-enterprise-policy.sh` before re-review.

## Expiry
- Valid until: 2026-03-28
- Re-evaluate if: additional high-risk orchestrator files are added to the packet, canonical dispatch posting changes again, or any validator disproves the fail-closed/canonical-only dispatch contract.

## Verification Evidence
- Commands run: `bash scripts/guard_workflow_canonical_paths.sh` (`OK`), `bash scripts/guard_orchestrator_correlation_contract.sh` (`OK`), `bash ci/check-enterprise-policy.sh` (`OK`), `cd erp-domain && mvn -B -ntp -Dtest=IntegrationCoordinatorTest,OrchestratorControllerIT test`.
- Result summary: canonical-path guard passes, correlation guard passes, enterprise-policy gate passes; orchestrator now directs all dispatch-like rejections to `/api/v1/sales/dispatch/confirm`; tests updated to pin the new canonical path.
- Artifacts/links: `docs/approvals/R2-CHECKPOINT.md`, `scripts/guard_workflow_canonical_paths.sh`, `scripts/guard_orchestrator_correlation_contract.sh`.
