# R2 Checkpoint

Last reviewed: 2026-05-01

## Addendum — `m5-digest-token-storage-guards`

- Scope: activation/password-reset token storage hardening and non-black-box guard proof for digest-only persistence.
- Risk trigger: touches high-risk auth token persistence and Flyway v2 schema under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**`, `erp-domain/src/main/resources/db/migration_v2/**`, and activation control-plane tests.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary change, or secret-handling relaxation was introduced. The schema change removes legacy password-reset raw-token storage/indexing, enforces digest metadata, and adds guard tests that prove activation/reset lookups hash input before persistence lookup.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun compile, Spotless, targeted digest-token guard/auth activation/reset tests, OpenAPI drift guard if affected, high-risk guard, and mission-safe baseline validators.
- Expiry: 2026-05-05.
- Verification evidence:
  - password reset persistence entity exposes `tokenDigest` only; the raw `token` field, getter, and legacy migration helper were removed
  - v2 migration `V191__digest_only_activation_reset_token_storage_guards.sql` drops the legacy password-reset raw-token unique constraint and column, makes `token_digest` non-null, and adds digest-shape checks for reset and activation tokens
  - activation persistence remains `token_digest` plus status/expiry metadata only; integration proof verifies copied/emailed activation token material is present only at the explicit delivery/copy boundary and not in storage columns
  - non-black-box truth-suite guard verifies schema/entity/repository/service lookup paths and audit/log metadata stay digest/metadata-only
  - password-reset public contract proof verifies the final schema has no raw reset-token column and canonical reset flows use digest rows
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TS_AuthDigestTokenStorageGuardTest,AuthPasswordResetPublicContractIT,SuperAdminControllerIT test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:apply`
- Result summary:
  - targeted digest-token/auth/activation suite reported 34 tests run, 0 failures/errors/skips after formatting
  - no bearer tokens, passwords, activation links, reset links, token digests, provider credentials, or `.env` values were printed in evidence

## Scope
- Feature: `identity-account-hardcut-20260427` / PR #197
- Branch: codex identity-account-hardcut-20260427
- PR: https://github.com/evilfps/bigbrightpaints-erp/pull/197
- Review candidate:
  - hard-cuts identity/account storage to IAM-backed accounts, sessions, MFA factors, security events, and password reset/change flows
  - retires duplicate identity/profile/admin-user aliases and keeps canonical `/api/v1/auth/**`, `/api/v1/auth/mfa/**`, and `/api/v1/admin/users/**` surfaces documented
  - adds V190 IAM schema migration plus focused auth/admin/security regression coverage
- Why this is R2: this PR changes high-risk auth/session/MFA behavior and a Flyway v2 migration; incorrect behavior could break login/session revocation, expose tenant/account security data, or fail schema rollout.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/**`
  - `erp-domain/src/main/resources/db/migration_v2/V190__iam_core_schema_and_model_hard_cut.sql`
  - auth/admin integration and unit tests under `erp-domain/src/test/java`
- Contract surfaces affected:
  - login, refresh, logout, password reset/change, MFA, My Account profile/contact/security/session APIs, tenant-admin user controls, OpenAPI, endpoint inventories, migration/rollback runbooks
- Failure mode if wrong:
  - sessions or refresh tokens are not revoked correctly
  - IAM migration fails or preserves invalid token/session rows
  - tenant-admin users can infer or mutate out-of-scope identities
  - frontend callers use retired aliases or stale response contracts

## Approval Authority
- Mode: orchestrator
- Approver: Droid
- Canary owner: Droid
- Approval status: approved for PR review after passing CI
- Basis: this is a pre-release hard-cut that narrows identity/account behavior, removes retired aliases, and does not intentionally widen privileges or tenant boundaries.

## Escalation Decision
- Human escalation required: no
- Reason: the packet consolidates and hardens existing auth/admin behavior for a pre-release system; escalate if scope expands into new privileges, tenant-boundary widening, or destructive production-data migration assumptions.

## Rollback Owner
- Owner: Droid / PR owner
- Rollback method:
  - before merge: revert PR #197 or this branch, then rerun compile, focused auth/admin tests, Spotless, knowledgebase lint, high-risk change control, OpenAPI guard, and CI
  - after merge: revert through a follow-up PR and rerun the same validation lane plus migration rollback checks from `docs/runbooks/rollback.md`
- Rollback trigger:
  - V190 migration fails in CI or staging
  - login/refresh/logout/session revocation regressions appear
  - retired auth/admin aliases become routable or mutating
  - tenant-admin controls can access privileged or foreign-tenant targets

## Expiry
- Valid until: 2026-05-07
- Re-evaluate if: the PR adds new public auth routes, role-policy redesign, broader session-store behavior, destructive data migration assumptions, or tenant-boundary changes.

## Verification Evidence
- Commands run:
  - `git diff --check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q clean test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuditServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuditServiceTest,TenantOnboardingServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuditServiceTest,TenantOnboardingServiceTest,CriticalFixtureServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=PasswordServiceTest,AuthServiceAuditAttributionTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=MfaServiceTest,TS_IamCoreSchemaAndModelHardCutMigrationContractTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AdminUserServiceTest,TS_RuntimeCompanyContextFilterExecutableCoverageTest,TS_RuntimeTenantRuntimeEnforcementTest,CompanyContextFilterControlPlaneBindingTest,TS_IamCoreSchemaAndModelHardCutMigrationContractTest,MfaServiceTest,PasswordResetServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Dtest=AdminUserServiceTest,MfaServiceTest,PasswordResetServiceTest,AuditServiceTest,JwtAuthenticationFilterRoleHierarchyTest,TS_RuntimePasswordResetServiceExecutableCoverageTest test`
  - `bash ci/lint-knowledgebase.sh`
  - `bash ci/check-high-risk-changes.sh`
  - GitHub Actions CI for PR #197
- Result summary:
  - local formatting, compile/test-compile, docs lint, high-risk change control, and selected non-Docker auth/admin/security/migration tests passed
  - Docker-backed OpenAPI/integration tests were verified in GitHub Actions
  - latest CI run passed including Compile Check, Access And Tenant Tests, Changed-Code Coverage, and PR Ship Gate
- Artifacts/links:
  - PR: https://github.com/evilfps/bigbrightpaints-erp/pull/197
  - CI run: https://github.com/evilfps/bigbrightpaints-erp/actions/runs/25162444152
  - frontend handoff: `docs/frontend-portals/tenant-admin/identity-iam-handoff-2026-04-30.md`
  - migration and rollback docs: `docs/runbooks/migrations.md`, `docs/runbooks/rollback.md`
