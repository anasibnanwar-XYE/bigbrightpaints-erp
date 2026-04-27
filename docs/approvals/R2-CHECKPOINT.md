# R2 Checkpoint

Last reviewed: 2026-04-28

## Scope
- Feature: `contract-routing-scrutiny-remediation-round2`
- Branch: codex/identity-account-hardcut-20260427 (base: origin/main)
- PR: pending
- Review candidate:
  - enforce `VAL-ROUTE-001` canonical identity route disposition for auth, My Account, MFA, sessions, and Users & Access admin route inventory
  - preserve current canonical login/refresh/logout/me/password/MFA/admin-user envelopes where kept
  - add narrow canonical route gaps for self profile/contact/security/session and admin lock/unlock/session/security-event/assignable-role surfaces
  - retire duplicate admin suspend/unsuspend and hard-delete user aliases from OpenAPI/runtime mutation paths
  - update route-disposition tests to normalize method + concrete URI behavior rather than treating OpenAPI parameter names such as `{id}` and `{userId}` as distinct runtime routes
  - require fresh TOTP or unused recovery-code proof before `POST /api/v1/auth/mfa/recovery-codes/regenerate` rotates recovery codes
  - revoke affected access-session markers and refresh tokens after successful recovery-code regeneration
  - remove stale retired admin suspend/unsuspend/hard-delete lifecycle guidance from the scoped docs/library surfaces
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
  - Stale admin lifecycle cleanup: `docs/ERP-DOD-BIBLE.md`, `docs/code-review/flows/admin-governance.md`, frontend update docs, and repo/mission library guidance now point to canonical status, lock/unlock, session-revoke, force-reset, and MFA-disable routes rather than retired suspend/unsuspend/hard-delete aliases.
- Commands run:
  - /Users/anas/.factory/missions/7ef22e70-61c7-4cdf-b7a7-1c48f4127853/init.sh
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
