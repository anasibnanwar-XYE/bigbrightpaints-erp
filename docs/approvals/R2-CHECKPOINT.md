# R2 Checkpoint

## Scope
- Feature: `auth-merge-gate-hardening`
- Branch: `packet/lane02-auth-merge-gate-hardening`
- High-risk paths touched: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetService.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetServiceTest.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthPasswordResetPublicContractIT.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/admin/AdminUserSecurityIT.java`, `erp-domain/pom.xml`, and this approval record.
- Why this is R2: this packet changes the public password-reset response contract on a live auth surface, hardens after-commit cleanup durability, and now closes the post-dispatch stale-token resurrection bug on the fallback restore path.

## Risk Trigger
- Triggered by auth-sensitive public forgot-password behavior changes and the new guard that only restores a prior reset token after the fallback path proves the issued token was deleted.
- Contract surfaces affected: public forgot-password uniform masked-success behavior during token persistence and cleanup failures, after-commit token cleanup durability, guarded prior-token restoration after dispatch-cleanup failure, and the auth regression coverage that proves stale tokens are not resurrected.
- Failure mode if wrong: known enabled emails could still receive a distinguishable `503`/`SYS_003` response while unknown or disabled users stay on the generic `200` path, cleanup after dispatch failure could be non-durable, or a failed dispatch cleanup could resurrect a stale reset token beside a newer one.

## Approval Authority
- Mode: orchestrator
- Approver: ERP auth merge-gate hardening mission orchestration
- Basis: the packet hardens existing auth behavior and merge-gate coverage without widening tenant boundaries, expanding privileges, or introducing migration risk.

## Escalation Decision
- Human escalation required: no
- Reason: the change preserves current-state auth contracts and tightens durability/coverage; it does not widen access or add destructive data-path risk.

## Rollback Owner
- Owner: ERP-5 PR 116 remediation worker
- Rollback method: revert the remediation commit set on `packet/lane02-auth-merge-gate-hardening`, then rerun `cd erp-domain && mvn -Dtest=PasswordResetServiceTest test`, `cd erp-domain && mvn test -Pgate-fast -Djacoco.skip=true`, `bash ci/check-architecture.sh`, `bash ci/check-enterprise-policy.sh`, and `bash ci/check-orchestrator-layer.sh`.

## Expiry
- Valid until: 2026-03-24
- Re-evaluate if: public forgot-password masking behavior changes again, the fallback restore guard changes again, or the validation evidence below is superseded before merge.

## Verification Evidence
- Commands run:
  - `cd erp-domain && mvn compile -q`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Dtest=PasswordResetServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Dtest=AuthPasswordResetPublicContractIT,TS_RuntimePasswordResetServiceExecutableCoverageTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp test -Pgate-fast -Djacoco.skip=true`
  - `bash ci/check-architecture.sh`
  - `bash ci/check-enterprise-policy.sh`
  - `bash ci/check-orchestrator-layer.sh`
  - `bash ci/check-codex-review-guidelines.sh`
- Result summary:
  - `mvn compile -q`: passed on 2026-03-17 with exit 0.
  - `MIGRATION_SET=v2 mvn -B -ntp -Dtest=PasswordResetServiceTest test`: passed on 2026-03-17 with `60` tests, `0` failures, `0` errors, `0` skipped.
  - `MIGRATION_SET=v2 mvn -B -ntp -Dtest=AuthPasswordResetPublicContractIT,TS_RuntimePasswordResetServiceExecutableCoverageTest test`: passed on 2026-03-17 with `21` tests, `0` failures, `0` errors, `0` skipped, proving the public forgot-password endpoint still stays on the generic masked-success response while the fallback restore path no longer resurrects a stale prior token when issued-token cleanup state is unknown.
  - `MIGRATION_SET=v2 mvn -B -ntp test -Pgate-fast -Djacoco.skip=true`: passed on 2026-03-17 with `735` tests, `0` failures, `0` errors, `0` skipped.
  - `bash ci/check-architecture.sh`: passed on 2026-03-17 with `OK` plus compatibility-mode warnings for unresolved cross-module imports / missing legacy orchestrator catalog.
  - `bash ci/check-enterprise-policy.sh`: passed on 2026-03-17 with `[enterprise-policy] OK`.
  - `bash ci/check-orchestrator-layer.sh`: passed on 2026-03-17 with `[orchestrator-layer] OK` plus compatibility-mode warning for missing legacy orchestrator-layer contract files.
  - `bash ci/check-codex-review-guidelines.sh`: passed on 2026-03-17 with `[codex-review-guidelines] OK`.
- Artifacts/links:
  - Current validation was executed from the `erp-domain` module on `packet/lane02-auth-merge-gate-hardening` while fixing GitHub review thread `discussion_r2945806922`, which flagged stale-token resurrection risk in `restorePriorResetTokenAfterCleanupFailure(...)`.
