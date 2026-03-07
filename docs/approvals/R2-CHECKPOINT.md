# R2 Checkpoint

## Scope
- Feature: `auth-reset-recovery-contract-hardening`
- Branch: `security-auth-hardening`
- High-risk paths touched: auth controller/service/repository code, security matcher/context filter code, and auth/admin contract tests.
- Why this is R2: the change set modifies password recovery behavior across public, tenant-admin, and root-support flows in security-sensitive auth paths.

## Risk Trigger
- Triggered by edits under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/` and `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/`.
- Contract surfaces affected: public forgot/reset, tenant-admin force reset, and root support tenant-admin password reset.
- Main risks being controlled: stale alias drift, silent undispatched reset tokens, and temporary-credential exposure in API responses.

## Approval Authority
- Mode: orchestrator
- Approver: security/auth hardening mission orchestration
- Basis: compatibility-preserving remediation within the active security auth mission scope.

## Escalation Decision
- Human escalation required: no
- Reason: the only intentional contract break is a controlled retirement of a stale compatibility alias, and the remaining behavior hardens existing supported flows without widening privileges.

## Rollback Owner
- Owner: security-auth-hardening mission worker
- Rollback method: revert the feature commit, then rerun the targeted auth/reset suites and `mvn test -Pgate-fast -Djacoco.skip=true` before merge.

## Expiry
- Valid until: 2026-03-13
- Re-evaluate if: additional auth/company/orchestrator endpoints, persistence migrations, or response-shape changes are added.

## Verification Evidence
- Commands run: `mvn test -Djacoco.skip=true -pl . -Dtest=AuthPasswordResetPublicContractIT,AuthControllerIT,CompanyContextFilterPasswordResetBypassTest,AdminUserServiceTest`; `mvn test -Djacoco.skip=true -pl . -Dtest=PasswordResetServiceTest,AuthTenantAuthorityIT#root_only_super_admin_can_reset_tenant_admin_password_for_support`; `mvn test -Djacoco.skip=true -pl . -Dtest=OpenApiSnapshotIT -Derp.openapi.snapshot.verify=true -Derp.openapi.snapshot.refresh=true`; `mvn compile -q`; `mvn test -Djacoco.skip=true -pl . -Dtest=OpenApiSnapshotIT -Derp.openapi.snapshot.verify=true`; `bash ci/lint-knowledgebase.sh && bash ci/check-architecture.sh && bash ci/check-enterprise-policy.sh && bash ci/check-orchestrator-layer.sh && python3 scripts/check_flaky_tags.py --tests-root erp-domain/src/test/java --gate gate-fast && bash scripts/guard_openapi_contract_drift.sh`; `mvn test -Pgate-fast -Djacoco.skip=true`
- Result summary: feature-specific contract tests passed, password reset service hardening tests passed, the isolated support reset integration check still excluded temporary credential fields, the OpenAPI snapshot was refreshed and then verified, repository lint/policy guards passed, and the full `gate-fast` suite finished green with 394 tests and 0 failures.
- Artifacts/links: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/AuthController.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetService.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthPasswordResetPublicContractIT.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/PasswordResetServiceTest.java`, `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthTenantAuthorityIT.java`
