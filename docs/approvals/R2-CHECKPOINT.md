# R2 Checkpoint

## Scope
- Feature: `o2c-dispatch-canonicalization`
- Branch: `feature/o2c-dispatch-canonicalization`
- High-risk paths touched: `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/**` and `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/**`
- Why this is R2: this packet removes and hardens high-risk orchestrator/accounting dispatch behavior in the same branch diff, so enterprise policy requires explicit same-diff approval evidence for the canonical O2C dispatch posting path.

## Risk Trigger
- Triggered by removal of `IntegrationCoordinator.postDispatchJournal` and adjacent dead legacy dispatch-posting paths from the orchestrator/accounting control surface.
- Contract change: orchestrator dispatch entrypoints now fail closed instead of minting duplicate accounting truth, and no legacy `DISPATCH-*` posting path remains.
- Canonical posting rule: `SalesCoreEngine.confirmDispatch -> AccountingFacade` is the sole commercial-to-accounting trigger for dispatch truth.

## Approval Authority
- Mode: orchestrator
- Approver: ERP truth-stabilization mission orchestration
- Basis: compatibility-preserving remediation that removes the duplicate-truth dispatch posting path without adding new privileges, tenant scope, or fallback behavior.

## Escalation Decision
- Human escalation required: no
- Reason: the packet removes duplicate-truth code and hardens fail-closed behavior only; it does not introduce new privileges, tenant boundary changes, or destructive migration risk.

## Rollback Owner
- Owner: Factory-droid integration worker
- Rollback method: revert the `feature/o2c-dispatch-canonicalization` packet commits, rerun `cd erp-domain && MIGRATION_SET=v2 mvn compile -q`, rerun `cd erp-domain && MIGRATION_SET=v2 mvn test -Pgate-fast -Djacoco.skip=true`, rerun `bash scripts/guard_workflow_canonical_paths.sh`, and rerun `bash ci/check-enterprise-policy.sh` before re-review.

## Expiry
- Valid until: 2026-03-28
- Re-evaluate if: additional high-risk orchestrator/accounting files are added to the packet, canonical dispatch posting changes again, or any validator disproves the fail-closed/canonical-only dispatch contract.

## Verification Evidence
- Commands run: `cd erp-domain && MIGRATION_SET=v2 mvn test -Pgate-fast -Djacoco.skip=true` (`666` tests, `0` failures/errors), `cd erp-domain && MIGRATION_SET=v2 mvn test -Pgate-core -Djacoco.skip=true` (`432` tests, `0` failures/errors), `bash scripts/guard_workflow_canonical_paths.sh` (`OK`), `python3 scripts/changed_files_coverage.py` (`100%` line / `100%` branch).
- Result summary: all packet gates are green, the orchestrator dispatch journal path has been removed, fail-closed orchestrator behavior is enforced, and canonical dispatch posting remains exclusively `SalesCoreEngine.confirmDispatch -> AccountingFacade`.
- Artifacts/links: `docs/approvals/R2-CHECKPOINT.md`, `/home/realnigga/.factory/missions/d7e6cd26-f391-4380-bab8-8e9c76f1a3b6/validation-contract.md`, `.factory/validation/freeze-dispatch-seams/scrutiny/synthesis.json`, `.factory/validation/freeze-dispatch-seams/user-testing/synthesis.json`.
