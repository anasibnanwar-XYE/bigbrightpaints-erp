# R2 Checkpoint

Last reviewed: 2026-04-28

## Current Packet Evidence — auth-core-disabled-login-nonenumeration-remediation

## Scope
- Feature: `auth-core-disabled-login-nonenumeration-remediation`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - mask disabled-account login behind the same generic `Invalid credentials` / `VAL_001` envelope and `400` status used for unknown-account and wrong-password login failures
  - preserve disabled-user fail-closed behavior for existing bearer tokens, refresh-token rotation, auth-me, password change, and MFA self-service by keeping non-login enabled-account guards intact
  - update focused login-matrix coverage so disabled, unknown, and wrong-password responses have the same external response shape and never contain access or refresh tokens
  - narrow stale frontend/code-review guidance so clients do not expect `AUTH_006` or account-disabled disclosure during login
- Why this is R2: this packet touches high-risk authn failure semantics in `AuthService` and auth integration tests, where incorrect behavior could reintroduce account-state enumeration or accidentally mint credentials for disabled users.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java`
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthDisabledUserTokenIT.java`
  - `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/service/AuthServiceAuditAttributionTest.java`
  - scoped auth guidance under `.factory/library/**`, `docs/code-review/flows/auth-identity.md`, and `docs/ERP-DOD-BIBLE.md`
- Contract surfaces affected:
  - `VAL-AUTH-002`, `VAL-AUTH-010`, `VAL-ADV-001`
- Failure mode if wrong:
  - disabled-account login could disclose account state through status, code, message, or response shape
  - disabled-account login could mint access or refresh tokens
  - unknown, wrong-password, and disabled-login failures could diverge and become enumerable
  - refresh or existing-token paths could stop failing closed after disablement

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is an accepted auth-core scrutiny remediation that removes externally visible account-state disclosure from login while preserving existing fail-closed disabled-account denial on authenticated and refresh-token paths.

## Escalation Decision
- Human escalation required: no
- Reason: the packet narrows an auth disclosure and does not widen privileges, change tenant boundaries, expose secrets, alter persisted schema, or introduce destructive migration behavior.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert this packet and rerun test-compile, focused auth login/token tests, Spotless, OpenAPI guard, lint/architecture, and High-Risk Change Control
  - after merge: revert through a new remediation packet and rerun the same auth-core non-enumeration proof lane
- Rollback trigger:
  - disabled login no longer matches wrong-password and unknown-account external failure status/body shape
  - disabled login issues an access token or refresh token
  - refresh, auth-me, password change, or MFA self-service remain usable after account disablement
  - focused compile/tests, Spotless, OpenAPI guard, lint/architecture, or High-Risk Change Control fail

## Expiry
- Valid until: 2026-05-05
- Re-evaluate if: scope expands into locked-account public semantics, MFA challenge semantics, refresh-token error-envelope changes, broader RBAC/tenant-boundary policy, or schema/migration behavior.

## Verification Evidence
- Scope-to-evidence mapping:
  - Login non-enumeration proof: `AuthDisabledUserTokenIT.disabledUnknownAndWrongPasswordLoginFailures_shareGenericInvalidCredentialsEnvelope` verifies disabled, unknown, and wrong-password login attempts all return `400`, `success=false`, `message=Invalid credentials`, `VAL_001`, no disabled-account strings, and no token fields.
  - Disabled-token proof: `AuthDisabledUserTokenIT.disabledUserToken_isRejectedEvenWhenJwtStillValid` and `disabledUserRefreshToken_isRejectedAfterDisablement` verify existing bearer and refresh credentials fail after disablement.
  - Regression proof: `AuthControllerIT`, `AuthHardeningIT`, `AuthServiceAuditAttributionTest`, and `TenantRuntimeEnforcementAuthIT` recheck login, auth-me, lockout, tenant-runtime denial, and audit-attribution adjacency.
  - Guidance proof: frontend/code-review guidance now scopes `AUTH_006` to authenticated/refresh/self-service paths after login and states login remains generic invalid-credentials.
- Commands run:
  - `mission init.sh`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/identity-account-hardcut-20260427 && bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthDisabledUserTokenIT,AuthControllerIT,AuthHardeningIT,AuthServiceAuditAttributionTest,TenantRuntimeEnforcementAuthIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:apply`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthDisabledUserTokenIT,AuthControllerIT,AuthHardeningIT,AuthServiceAuditAttributionTest,TenantRuntimeEnforcementAuthIT' test`
  - `bash scripts/guard_openapi_contract_drift.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `bash ci/lint-knowledgebase.sh && bash ci/check-architecture.sh && bash ci/check-high-risk-changes.sh`
- Result summary:
  - baseline IAM mission lane passed with 89 tests plus OpenAPI guard before code changes
  - test-compile passed after implementation and after formatting
  - focused disabled-login/auth hardening lane passed with 39 tests after implementation and after formatting
  - OpenAPI guard, Spotless check, knowledgebase lint, architecture check, and High-Risk Change Control passed
  - no raw JWTs, refresh tokens, reset tokens, MFA secrets, recovery codes, password hashes, or production secrets were recorded in this checkpoint

---

## Previous Packet Evidence — auth-scope-jwt-lockout-and-http-hardening

## Scope
- Feature: `auth-scope-jwt-lockout-and-http-hardening`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - serialize scoped login failure accounting with a pessimistic failed-account update and commit failed-attempt/lockout state on validation failures
  - reject legacy `X-Company-Id` before public auth/password-reset bypass handling
  - require bearer JWTs to include subject, `jti`, `companyCode`, expiry, and issued-at evidence before authentication is installed
  - add focused regression coverage for lockout/session revocation, malformed public auth payloads, bearer-only cookie posture, legacy-header rejection, CORS origin validation, and sensitive actuator denial
- Why this is R2: this packet changes high-risk authn/token/company-context code paths where incorrect behavior could weaken lockout, tenant scope, bearer-token validation, or management-endpoint exposure controls.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtAuthenticationFilter.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java`
  - focused auth/security/actuator tests under `erp-domain/src/test/java`
- Contract surfaces affected:
  - `VAL-AUTH-002`, `VAL-AUTH-003`, `VAL-AUTH-004`, `VAL-AUTH-005`, `VAL-AUTH-010`, `VAL-AUTH-011`, `VAL-AUTH-012`, `VAL-AUTH-013`
  - `VAL-POLICY-001`, `VAL-POLICY-002`, `VAL-POLICY-003`, `VAL-ADV-001`, `VAL-ADV-002`, `VAL-ADV-004`, `VAL-ADV-010`, `VAL-ADV-012`, `VAL-HTTP-001`, `VAL-OPS-001`
- Failure mode if wrong:
  - failed-login counters could undercount or roll back, allowing lockout bypass
  - legacy company-id headers could select or bypass tenant context on public/self-service paths
  - bearer tokens missing mandatory JWT claims could authenticate without blacklist/scope checks
  - malformed auth requests or sensitive actuator probes could leak stack traces, secrets, cookies, or management data

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is an accepted auth-core hardening packet that tightens existing deny paths and validation requirements without widening privileges, changing public route names/envelopes, changing tenant boundaries, or introducing destructive migrations.

## Escalation Decision
- Human escalation required: no
- Reason: the packet strengthens fail-closed authentication, tenant-header, JWT, and actuator controls using existing mission contract assertions; it does not add new authority, expose secrets, alter persisted schema, or require production migration decisions.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert this packet and rerun test-compile, focused auth/JWT/company-context/actuator tests, Spotless, OpenAPI guard, and High-Risk Change Control
  - after merge: revert through a new remediation packet and rerun the same auth-core hardening proof lane
- Rollback trigger:
  - failed login attempts no longer lock after 5 failures for 15 minutes or old sessions remain usable after lockout
  - public/self-service requests with `X-Company-Id` are not rejected fail-closed
  - bearer JWTs without required claims authenticate or leak protected data
  - malformed auth payloads or actuator probes produce broad 5xx, cookies, secrets, or sensitive management responses
  - focused compile/tests, Spotless, OpenAPI guard, or High-Risk Change Control fail

## Expiry
- Valid until: 2026-05-05
- Re-evaluate if: scope expands into public API shape changes, refresh/session-family semantics, broader RBAC/tenant-boundary policy, CORS origin contract changes, management endpoint exposure, or schema/migration behavior.

## Verification Evidence
- Scope-to-evidence mapping:
  - Lockout proof: `AuthHardeningIT` verifies 5 failed passwords lock the account, preserve lock window, and revoke prior bearer/refresh credentials; `AuthService` now uses pessimistic account locking when recording failed attempts.
  - Legacy-header proof: `AuthControllerIT` and `CompanyContextFilterPasswordResetBypassTest` verify `X-Company-Id` is rejected on authenticated auth-me and public password-reset paths while canonical `X-Company-Code` reset bypass remains intact.
  - JWT proof: `AuthHardeningIT.bearerTokenMissingJwtId_isRejectedFailClosed` verifies a locally signed token missing `jti` cannot authenticate to the canonical auth-me endpoint.
  - HTTP/abuse/ops proof: `AuthHardeningIT` verifies auth responses do not set cookies and malformed public auth JSON returns controlled `400`; `SystemSettingsServiceCorsTest` verifies no wildcard credentialed origins and prod HTTP-origin policy; `CR_ActuatorProdHardeningIT` verifies sensitive actuator endpoints are not exposed and do not contain configured test secrets.
- Commands run:
  - `mission init.sh`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/identity-account-hardcut-20260427 && bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthHardeningIT,CompanyContextFilterPasswordResetBypassTest,CR_ActuatorProdHardeningIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,AuthPlatformScopeCodeIT,CompanyContextFilterControlPlaneBindingTest,AuthHardeningIT,CompanyContextFilterPasswordResetBypassTest,CR_ActuatorProdHardeningIT,SystemSettingsServiceCorsTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthHardeningIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthHardeningIT,CompanyContextFilterPasswordResetBypassTest,CR_ActuatorProdHardeningIT,SystemSettingsServiceCorsTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `bash scripts/guard_openapi_contract_drift.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/check-architecture.sh`
- Result summary:
  - baseline IAM mission lane passed with 89 tests plus OpenAPI guard before code changes
  - focused auth/MFA regression tests passed with 12 tests after the transaction-boundary fix
  - final mission IAM lane passed with 89 tests plus OpenAPI guard after implementation
  - focused auth/company-context/CORS/actuator hardening tests passed with 28 tests after implementation
  - test-compile, architecture check, knowledgebase lint, High-Risk Change Control, Spotless, and OpenAPI guard passed
  - no raw JWTs, refresh tokens, reset tokens, MFA secrets, recovery codes, password hashes, or production secrets were recorded in this checkpoint

---

## Previous Packet Evidence — schema-core-active-storage-hard-cut-remediation-round2

## Scope
- Feature: `schema-core-active-storage-hard-cut-remediation-round2`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - sync canonical IAM rows from `SuperAdminTenantControlPlaneService.confirmAdminEmailChange` immediately after confirmed tenant-admin email mutation
  - keep `iam_accounts.email` and `iam_account_contacts.primary_email` current in the same transaction as the supported super-admin email-change confirmation
  - sync canonical IAM rows for `ValidationSeedDataInitializer` and `MockDataInitializer` user saves after Flyway-created IAM tables exist
  - add focused regression coverage for super-admin email-change canonical storage and seed/mock initializer canonical sync hooks
- Why this is R2: this packet touches high-risk identity/account storage and the super-admin tenant control-plane email-change path, where stale canonical IAM email/contact rows would break the schema-core hard cut.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/SuperAdminTenantControlPlaneService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/ValidationSeedDataInitializer.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/MockDataInitializer.java`
  - focused schema/control-plane/initializer tests under `erp-domain/src/test/java`
- Contract surfaces affected:
  - `VAL-CROSS-005`
  - schema-core active-storage expectation that supported runtime identity/contact mutations keep canonical `iam_*` tables current
- Failure mode if wrong:
  - confirmed tenant-admin email changes could update `app_users.email` while leaving `iam_accounts.email` or `iam_account_contacts.primary_email` stale
  - validation/mock seeded `UserAccount` rows could exist without current canonical account/profile/contact/credential/MFA rows
  - token/session revocation behavior after confirmed admin email change could regress

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is a narrow remediation required by schema-core scrutiny round 2; it synchronizes canonical storage after existing supported mutations without widening privileges, changing tenant boundaries, changing public routes, or introducing destructive migration behavior.

## Escalation Decision
- Human escalation required: no
- Reason: the packet tightens canonical storage consistency for already-authorized mutation paths and seed initializers; it does not add new authority, expose secrets, or change external API shapes.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert this packet and rerun test-compile, focused schema/control-plane/initializer tests, High-Risk Change Control, Spotless, and OpenAPI guard
  - after merge: revert through a new remediation packet and rerun the same schema-core active-storage proof
- Rollback trigger:
  - `iam_accounts.email` or `iam_account_contacts.primary_email` remain stale after confirmed tenant-admin email change
  - validation/mock seed user saves no longer sync canonical IAM account/profile/contact/credential/MFA rows
  - existing token blacklist or refresh-token revocation behavior after email confirmation regresses
  - focused compile/tests, High-Risk Change Control, Spotless, or OpenAPI guard fail

## Expiry
- Valid until: 2026-05-05
- Re-evaluate if: scope expands into public API route/envelope changes, broader tenant-control-plane policy, credential verifier semantics, destructive migrations, or RBAC/tenant-boundary behavior.

## Verification Evidence
- Scope-to-evidence mapping:
  - Super-admin email-change proof: `SuperAdminTenantControlPlaneServiceTest` verifies canonical sync is invoked on successful confirmation and not invoked on conflicting requested-email failure.
  - Runtime canonical-row proof: `IamCoreSchemaAndModelHardCutMigrationIT.superAdminTenantAdminEmailChangeKeepsCanonicalIamEmailAndContactCurrent` confirms `iam_accounts.email` and `iam_account_contacts.primary_email` update after the supported confirmation path, while old refresh/session state is revoked.
  - Seed proof: `ValidationSeedDataInitializerTest`, `MockDataInitializerTest`, and `DataInitializerSecurityTest` verify saved validation/mock admin users call canonical sync while existing seed password/security behavior remains intact.
- Commands run:
  - `mission init.sh`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/identity-account-hardcut-20260427 && bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='IamCoreSchemaAndModelHardCutMigrationIT,SuperAdminTenantControlPlaneServiceTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='ValidationSeedDataInitializerTest,MockDataInitializerTest,DataInitializerSecurityTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `bash ci/check-high-risk-changes.sh`
  - `bash scripts/guard_openapi_contract_drift.sh`
- Result summary:
  - baseline IAM mission test lane passed with 89 tests plus OpenAPI guard before code changes
  - test-compile passed after implementation
  - focused schema/control-plane regression tests passed with 32 tests
  - focused seed/mock initializer regression tests passed with 30 tests
  - Spotless, High-Risk Change Control, and OpenAPI guard passed
  - no raw JWTs, refresh tokens, reset tokens, MFA secrets, recovery codes, or password hashes were recorded in this checkpoint

---

## Previous Packet Evidence — iam-core-schema-and-model-hard-cut

## Scope
- Feature: `iam-core-schema-and-model-hard-cut`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - add forward-only Flyway v2 migration `V190__iam_core_schema_and_model_hard_cut.sql` for canonical IAM account, profile/contact, credential, MFA-factor, session/device, and security-event tables
  - add JPA entities/repositories for the IAM core model
  - remove raw refresh/reset token columns from active v2 schema and keep digest/verifier columns mandatory
  - move MFA recovery-code runtime storage off `app_users.mfa_recovery_codes` into the canonical verifier table
  - keep old v2 migration files unchanged; only a new v2 migration is added
- Why this is R2: this packet changes high-risk identity schema, credential verifier storage, MFA verifier storage, and auth/session model foundations.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/resources/db/migration_v2/V190__iam_core_schema_and_model_hard_cut.sql`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/domain/**`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/MfaService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`
  - IAM schema/model and migration tests under `erp-domain/src/test/java`
- Contract surfaces affected:
  - `VAL-MIG-001`, `VAL-CROSS-005`, `VAL-OWN-001`, `VAL-SESS-016`
  - Flyway v2 migration lane and verifier-only refresh/reset/MFA storage
- Failure mode if wrong:
  - clean v2 migration could fail or rewrite applied migrations
  - raw refresh/reset tokens or delimited MFA recovery-code storage could remain active
  - identity ownership boundaries could remain overloaded in one table
  - downstream auth/session/MFA features could bind to the wrong canonical storage model

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: the packet implements the accepted schema-core hard cut with a new migration only, digest/verifier-only storage constraints, and focused schema/runtime regression tests without widening tenant or privilege boundaries.

## Escalation Decision
- Human escalation required: no
- Reason: this is the explicitly planned forward-only schema/model packet, old migrations are not edited, and the runtime changes remove secret-bearing storage rather than adding privileges or destructive tenant-boundary behavior.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert this packet and rerun compile, migration/schema tests, focused IAM tests, High-Risk Change Control, and OpenAPI guard
  - after merge: revert through a new remediation packet and rerun the same v2 migration/contract gates
- Rollback trigger:
  - Flyway v2 migration fails on a clean database
  - any older v2 migration file is checksum-changed
  - raw refresh/reset token columns remain active or are reintroduced
  - `app_users.mfa_recovery_codes` remains active or recovery-code login/regeneration stops consuming verifier rows
  - IAM ownership/schema contract tests fail

## Expiry
- Valid until: 2026-05-05
- Re-evaluate if: scope expands into public route shape changes, broader credential reset semantics, first-class session API behavior, MFA factor API changes, RBAC policy, or tenant boundary changes.

## Verification Evidence
- Scope-to-evidence mapping:
  - Forward-only migration: `V190__iam_core_schema_and_model_hard_cut.sql` is the only v2 migration added; old migration files are unchanged.
  - Migration/schema proof: `TS_IamCoreSchemaAndModelHardCutMigrationContractTest` and `IamCoreSchemaAndModelHardCutMigrationIT` assert canonical IAM tables, ownership split, and verifier-only storage.
  - Runtime verifier proof: `RefreshTokenServiceTest`, `AuthPasswordResetPublicContractIT`, and `MfaControllerIT` verify digest-only refresh/reset behavior and canonical MFA recovery-code verifier consumption/regeneration.
  - Model proof: new `Iam*` entities/repositories map the account/profile/contact/credentials/MFA/session/security-event schema.
- Commands run:
  - `mission init.sh`
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/identity-account-hardcut-20260427 && bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='TS_IamCoreSchemaAndModelHardCutMigrationContractTest,IamCoreSchemaAndModelHardCutMigrationIT,MfaServiceTest,RefreshTokenServiceTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='MfaControllerIT,AuthPasswordResetPublicContractIT,AdminUserSecurityIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:apply`
- Result summary:
  - baseline IAM characterization passed before changes
  - clean Flyway v2 startup reached version `v190` during integration tests
  - focused schema/model tests passed with 18 tests
  - focused MFA/password reset/admin security regression tests passed with 42 tests
  - no raw JWTs, refresh tokens, reset tokens, MFA secrets, recovery codes, or hashes were recorded in this checkpoint

---


## Scope
- Feature: `contract-routing-scrutiny-remediation-round2`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - enforce `VAL-ROUTE-001` canonical identity route disposition for auth, My Account, MFA, sessions, and Users & Access admin route inventory
  - preserve current canonical login/refresh/logout/me/password/MFA/admin-user envelopes where kept
  - add narrow canonical route gaps for self profile/contact/security/session and admin lock/unlock/session/security-event/assignable-role surfaces
  - retire duplicate admin `suspend`, `unsuspend`, and `DELETE /api/v1/admin/users/{userId}` aliases from OpenAPI/runtime mutation paths
  - update route-disposition tests to normalize method + concrete URI behavior rather than treating OpenAPI parameter names such as `{id}` and `{userId}` as distinct runtime routes
  - require fresh TOTP or unused recovery-code proof before `POST /api/v1/auth/mfa/recovery-codes/regenerate` rotates recovery codes
  - revoke affected access-session markers and refresh tokens after successful recovery-code regeneration
  - remove stale retired admin `suspend`, `unsuspend`, and `DELETE /api/v1/admin/users/{userId}` lifecycle guidance from the scoped docs/library surfaces
- Why this is R2: this packet changes high-risk auth/admin route exposure, MFA verifier behavior, session/token revocation behavior, and tenant-admin user-control documentation in modules/auth and route-contract docs.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/AuthController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/controller/MfaController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/MfaService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/controller/AdminUserController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`
  - `openapi.json`
  - route/OpenAPI contract tests under `erp-domain/src/test/java`
- Contract surfaces affected:
  - `/api/v1/auth/**` canonical auth, self, MFA, and session route inventory
  - `POST /api/v1/auth/mfa/recovery-codes/regenerate` request proof and post-success session invalidation
  - `/api/v1/admin/users/**` canonical Users & Access route inventory
  - retired /api/v1/auth/profile, /api/v1/auth/password/forgot/superadmin, /api/v1/admin/users/{userId}/suspend, /api/v1/admin/users/{userId}/unsuspend, and DELETE /api/v1/admin/users/{userId} aliases
- Failure mode if wrong:
  - duplicate retired aliases could continue mutating identity state
  - frontend/OpenAPI route inventory could drift from `VAL-ROUTE-001`
  - tenant-admin route changes could widen protected-target, cross-tenant, or superadmin tenant-workflow access
  - route tests could produce false positives by counting path parameter variable names as separate HTTP routes
  - missing/invalid MFA proof could rotate or expose recovery codes
  - successful recovery-code regeneration could leave pre-change bearer or refresh tokens usable

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is a hard-cut route-disposition and MFA recovery-code remediation packet required by the accepted mission contract; it removes duplicate mutating aliases, adds verifier proof before code rotation, revokes affected sessions on success, and does not intentionally widen tenant, platform, protected-target, credential, or secret exposure boundaries.

## Escalation Decision
- Human escalation required: no
- Reason: route decisions are explicitly covered by `VAL-ROUTE-001`, and MFA proof/session semantics are covered by `VAL-MFA-009`, `VAL-MFA-014`, and `VAL-MFA-015`; the packet tightens verifier and revocation behavior without destructive migrations or new external dependencies.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert this packet if route inventory, OpenAPI guard, or focused auth/admin tests regress
  - after merge: revert the packet and rerun OpenAPI guard/snapshot, focused route/auth/admin tests, compile, spotless, and High-Risk Change Control
- Rollback trigger:
  - any retired route still mutates identity/admin state
  - recovery-code regeneration succeeds without fresh TOTP or unused recovery-code proof
  - old recovery codes, bearer sessions, or refresh tokens remain usable after successful regeneration
  - any canonical kept route is removed, renamed, or changes envelope unexpectedly
  - OpenAPI snapshot drifts from runtime route inventory
  - tenant-admin or superadmin boundaries regress for Users & Access routes
  - route-disposition tests stop normalizing concrete method + URI behavior

## Expiry
- Valid until: 2026-05-05
- Re-evaluate if: scope expands beyond route disposition and recovery-code regeneration proof/revocation into schema migrations, credential storage semantics, MFA verifier storage, first-class session storage, or broader RBAC/tenant policy changes.

## Verification Evidence
- Scope-to-evidence mapping:
  - Route disposition: `IdentityRouteInventoryContractTest` asserts canonical and retired OpenAPI operations with normalized path-template variables.
  - Runtime route behavior: `AuthControllerIT` and `AdminUserSecurityIT` exercise canonical route availability, logout query-token rejection, and retired alias non-mutation/absence behavior.
  - OpenAPI parity: `OpenApiSnapshotIT` refreshed `openapi.json` and asserts request/response shapes for changed auth/admin routes.
  - Tenant/authz proof: focused admin security tests rechecked tenant-admin-only access, cross-company masking, protected targets, and superadmin tenant-workflow denial.
  - MFA regeneration proof: `MfaControllerIT` now verifies missing and invalid proof do not rotate or expose recovery codes, valid TOTP proof returns a replacement set, old codes fail, and new codes work once.
  - Session invalidation proof: `MfaControllerIT` now verifies two pre-change sessions can call `GET /api/v1/auth/me` before regeneration, then both old bearer tokens and both old refresh tokens fail after successful regeneration.
  - Stale admin lifecycle cleanup: `docs/ERP-DOD-BIBLE.md`, `docs/code-review/flows/admin-governance.md`, frontend update docs, and repo/mission library guidance now point to canonical status, lock/unlock, session-revoke, force-reset, and MFA-disable routes rather than retired `suspend`, `unsuspend`, and `DELETE /api/v1/admin/users/{userId}` aliases.
- Commands run:
  - mission init.sh
  - `cd /Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/identity-account-hardcut-20260427 && bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:apply`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest=OpenApiSnapshotIT -Derp.openapi.snapshot.verify=true -Derp.openapi.snapshot.refresh=true test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='IdentityRouteInventoryContractTest,AuthControllerIT,AdminUserSecurityIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile`
  - `bash ci/check-high-risk-changes.sh`
  - `bash scripts/guard_openapi_contract_drift.sh && cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='AuthPasswordResetPublicContractIT,AdminUserSecurityIT,AuthControllerIT,AuthTenantAuthorityIT,TenantRuntimeEnforcementAuthIT,AuthDisabledUserTokenIT,MfaControllerIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -Dtest='MfaControllerIT,AuthControllerIT,AdminUserSecurityIT,IdentityRouteInventoryContractTest,OpenApiSnapshotIT' test`
  - `OPENAPI_CONTRACT_DRIFT_MODE=report bash scripts/guard_openapi_contract_drift.sh`
  - `bash ci/lint-knowledgebase.sh && bash ci/check-architecture.sh && bash ci/check-high-risk-changes.sh`
- Result summary:
  - mission init completed and baseline route/auth/admin characterization passed with 86 tests plus OpenAPI guard
  - compile passed after route changes
  - OpenAPI snapshot refresh passed with 13 tests and updated auth/admin route inventory
  - focused route/auth/admin tests passed with 38 tests after updating runtime route-disposition assertions
  - final OpenAPI guard, compile, spotless check, High-Risk Change Control, OpenAPI snapshot verification, focused route tests, and 86-test IAM mission baseline all passed
  - no raw JWTs, refresh tokens, reset tokens, MFA secrets, or recovery codes were recorded in this checkpoint
  - round 2 MFA recovery-code regeneration proof and session invalidation tests passed, OpenAPI guard was refreshed for the request body, and docs/library stale alias guidance was cleaned
- Artifacts/links:
  - `openapi.json` updated in-repo
  - focused test reports under `erp-domain/target/surefire-reports/`
  - this checkpoint section is the branch-local R2 evidence artifact for the scoped route-disposition packet

---

## Prior Packet Evidence (Historical)

## Scope
- Feature: `default-account-clear-semantics-followup-hardcut`
- Branch: refactor/accounting-centralization-20260420 (base: origin/main)
- PR: pending
- Review candidate:
  - add explicit `clearAccountFields` semantics to the existing accounting default-account update route so validators can intentionally clear a configured default without treating omitted/null account IDs as accidental mutation intent
  - keep default-account updates company-scoped through the existing `CompanyContextService` + company-scoped account lookup owner
  - audit default-account update/clear outcomes through accounting business audit events
  - seed and verify deterministic MOCK/RIVAL inventory, COGS, revenue, discount, and tax default-account baselines for validation runtime dispatch/invoice proofs
  - preserve downstream fail-closed configuration readiness when a required default is intentionally cleared
- Why this is R2: this packet changes high-risk accounting configuration behavior in `CompanyDefaultAccountsService`, validation seeding, and the validation runtime reset script.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingComplianceAuditService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/ValidationSeedDataInitializer.java`
  - `erp-domain/src/main/resources/db/migration_v2/V184__accounting_truth_rls_hard_cut.sql`
  - `erp-domain/src/main/resources/db/migration_v2/V185__accounting_rls_fail_closed_session_binding.sql`
  - `erp-domain/src/main/resources/db/migration_v2/V186__account_code_case_insensitive_uniqueness.sql`
  - `erp-domain/src/main/resources/db/migration_v2/V187__dealer_receipt_payment_event_hard_cut.sql`
  - `erp-domain/src/main/resources/db/migration_v2/V188__supplier_auto_settlement_due_date_support.sql`
  - `erp-domain/src/main/resources/db/migration_v2/V189__reconciliation_discrepancy_resolution_alignment.sql`
  - `scripts/reset_final_validation_runtime.sh`
- Contract surfaces affected:
  - PUT /api/v1/accounting/default-accounts
  - GET /api/v1/accounting/default-accounts
  - GET /api/v1/accounting/configuration/health
  - validation runtime reset fixture verification for seeded default-account readiness
- Failure mode if wrong:
  - omitted/null fields could still be mistaken for explicit clears, or explicit clears could mutate unrelated default-account slots
  - runtime validation could start without required dispatch/invoice defaults, forcing manual setup drift
  - downstream dispatch/invoice accounting readiness could stay healthy after a required default is cleared instead of failing closed
  - default-account changes could lose audit visibility or cross company boundaries

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is a hard-cut accounting configuration hardening that adds an explicit clear path and deterministic validation defaults without widening tenant, auth, accounting posting, report, export, or payment semantics.

## Escalation Decision
- Human escalation required: no
- Reason: this packet only makes existing accounting default-account mutation intent explicit, adds runtime fixture verification, and does not widen privileges, change tenant boundaries, or introduce destructive migration behavior.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert the packet if default-account update/clear semantics or validation runtime readiness regress
  - after merge: revert packet and rerun focused default-account, accounting proof, runtime reset, OpenAPI, compile, High-Risk Change Control, and PR parity checks
- Rollback trigger:
  - `clearAccountFields` cannot intentionally clear a requested default-account slot, or it clears unrelated slots
  - omitted/null account ID fields start clearing defaults without explicit clear intent
  - reset validation runtime no longer starts with MOCK/RIVAL ready default-account baselines
  - configuration health stays healthy after a required default is cleared
  - default-account public contract or audit behavior drifts from the scoped packet intent
  - policy gate fails after integrating this packet

## Expiry
- Valid until: 2026-05-03
- Re-evaluate if: scope expands beyond default-account clear semantics and validation runtime seeding into broader auth, tenant isolation, payment semantics, destructive migrations, or accounting posting redesign.

## Verification Evidence
- Scope-to-evidence mapping:
  - Explicit clear semantics: `CompanyDefaultAccountsRequest.clearAccountFields` and `CompanyDefaultAccountsService` clear only requested slots, reject set+clear conflicts, and keep null/omitted IDs as no-op partial update semantics.
  - Runtime baseline: `ValidationSeedDataInitializer` creates company-scoped MOCK/RIVAL inventory, COGS, revenue, discount, and tax defaults; `scripts/reset_final_validation_runtime.sh` now verifies those slots and account types.
  - Auditability: `AccountingComplianceAuditService` records `DEFAULT_ACCOUNTS_CLEARED` / `DEFAULT_ACCOUNTS_UPDATED` business audit events with before/after default-account state.
  - Fail-closed proof: compose-backed curl cleared MOCK `taxAccountId`, observed configuration health fail closed, restored the same tax account, and observed health recover.
  - Migration governance: Flyway v2 schema changes `V184` through `V189` are covered by the migration and rollback runbooks, and the new PR lane must be validated with high-risk and PR parity checks against the remote default branch.
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Dtest='CompanyDefaultAccountsServiceTest,AccountControllerTest,ValidationSeedDataInitializerTest' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Derp.openapi.snapshot.verify=true -Derp.openapi.snapshot.refresh=true -Dtest=OpenApiSnapshotIT test`
  - `bash scripts/reset_final_validation_runtime.sh`
  - `commands.strict-runtime-smoke-check`
  - `curl` runtime probes for GET defaults, PUT clear `taxAccountId`, configuration health fail-closed, PUT restore, and audit row inspection
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Dtest='JournalEntryE2ETest,AccountingEndpointContractTest,SettlementControllerIdempotencyHeaderParityTest,CriticalAccountingAxesIT,TS_RuntimeAccountingReplayConflictExecutableCoverageTest,CR_ManualJournalSafetyTest,CR_DealerReceiptSettlementAuditTrailTest,CR_PurchasingToApAccountingTest,CR_SalesReturnCreditNoteIdempotencyTest,NumberSequenceServiceIntegrationTest,ReferenceNumberServiceTest,TS_RuntimeReferenceNumberServiceExecutableCoverageTest,InvoiceServiceTest,AccountingServiceTest#dealerReceiptService_routesLiveReceiptFlowThroughJournalEntryService+creditDebitNoteService_routesLiveCreditNoteFlowThroughJournalEntryService+inventoryAccountingService_routesLiveLandedCostFlowThroughJournalEntryService' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -Dtest='CriticalAccountingAxesIT,AccountingEndpointContractTest,SettlementControllerIdempotencyHeaderParityTest,ReconciliationControlsIT' test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q spotless:check -DspotlessFiles='src/main/java/com/bigbrightpaints/erp/modules/accounting/dto/CompanyDefaultAccountsRequest.java,src/main/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountController.java,src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountResolutionOwnerService.java,src/main/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsService.java,src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingComplianceAuditService.java,src/main/java/com/bigbrightpaints/erp/core/config/ValidationSeedDataInitializer.java,src/test/java/com/bigbrightpaints/erp/modules/accounting/controller/AccountControllerTest.java,src/test/java/com/bigbrightpaints/erp/modules/accounting/service/CompanyDefaultAccountsServiceTest.java'`
  - `bash ci/check-high-risk-changes.sh`
  - `python3 scripts/pr_ci_parity.py --base origin/main --head HEAD`
  - `git diff --check`
- Result summary:
  - focused default-account/controller/validation-seed tests passed
  - OpenAPI snapshot refreshed and exposes optional `clearAccountFields`
  - runtime reset verified actors, tenant fixtures, dealers, finance/UAT fixtures, and the new default-account baseline checks
  - runtime clear proof showed baseline health `healthy=true`, clear response `taxAccountId=null`, health after clear `healthy=false`, restore response `taxAccountId=7`, restored health `healthy=true`
  - audit DB inspection showed one `DEFAULT_ACCOUNTS_CLEARED` and one `DEFAULT_ACCOUNTS_UPDATED` event for `COMPANY_DEFAULT_ACCOUNTS`
  - targeted accounting proof, baseline test pack, compile, scoped spotless check, and diff whitespace check passed
- Artifact note:
  - inline evidence in this checkpoint records the Maven proof, runtime reset, curl clear/restore, audit inspection, and OpenAPI observations for the scoped default-account clear semantics packet.
