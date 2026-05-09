# R2 Checkpoint

Last reviewed: 2026-05-14

## Addendum — `pr199-hardcut-merge-ready`

- Scope: compact PR #199 carry-forward onto `main` after merged PR #198, limited to Flyway v2 forward migrations `V204`-`V206`, canonical orchestrator idempotency/correlation enforcement, and trace JSON fail-fast cleanup.
- Risk trigger: touches high-risk Flyway v2 schema under `erp-domain/src/main/resources/db/migration_v2/**` and orchestrator runtime under `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/**`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no tenant boundary, privilege, route compatibility, or historical-state bridge was added. The branch removes request/payload-derived idempotency fallbacks and keeps a single canonical leased idempotency path.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the compact branch and rerun Flyway guards, orchestrator correlation guard, focused orchestrator/Flyway tests, compile, Spotless, high-risk guard, and whitespace checks.
- Expiry: 2026-05-21.
- Verification evidence:
  - `V204` renames `journal_reference_mappings.legacy_reference` to canonical `reference_key` through a forward migration.
  - `V205` drops retired `tally_imports` through the current v2 migration track.
  - `V206` backfills historical packaging-slip COGS journal links before runtime relies on the canonical marker.
  - orchestrator ingress now requires `Idempotency-Key` for mutating commands instead of deriving keys from request IDs or payload hashes.
  - `CommandDispatcher` propagates the persisted lease idempotency key and fails fast if the lease command is missing or malformed.
  - trace detail serialization fails closed instead of storing non-JSON fallback strings.
- Commands run:
  - `bash scripts/guard_orchestrator_correlation_contract.sh`
  - `bash scripts/guard_flyway_v2_migration_ownership.sh`
  - `bash scripts/guard_flyway_v2_referential_contract.sh`
  - `bash scripts/flyway_overlap_scan.sh`
  - `git diff --check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=CorrelationIdentifierSanitizerTest,TS_RuntimeOrchestratorExecutableCoverageTest,TS_RuntimeOrchestratorCorrelationCoverageTest,TS_RuntimeOrchestratorIdempotencyExecutableCoverageTest,TS_RuntimeTraceServiceExecutableCoverageTest,TS_PackagingSlipInvoiceLinkV2MigrationContractTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -DskipTests test-compile`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp spotless:check`

## Addendum — `pr198-rebase-on-pr197-hard-cut`

- Scope: PR #198 Super Admin/control-plane rebase onto PR #197 IAM/auth mainline, including removed flat onboarding/support-reset handlers, Add Client current-state docs, OpenAPI snapshot/inventory refresh, hard-cut cleanup of the unused `TenantOnboardingService`/DTO/test stack, and PR #198 changed-coverage baseline compaction after the final finance shard fix.
- Risk trigger: touches high-risk auth/company/control-plane code, route security behavior, OpenAPI/frontend contracts, CI routing/test manifests under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/**`, `erp-domain/src/test/java/**`, `openapi.json`, `scripts/ci_risk_router.py`, `ci/pr_manifests/**`, and canonical docs.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary expansion, destructive migration, or compatibility shim was introduced. The rebase keeps PR #197 IAM/auth/session/reset behavior canonical and removes stale PR #198 route/service compatibility instead of preserving it.
- Rollback owner: Droid / PR owner.
- Rollback method: revert this integration patch or re-run the PR #198 rebase branch from current `main`, then rerun compile, Spotless, OpenAPI refresh/guard, high-risk guard, knowledgebase lint, and the focused auth/company/Super Admin test pack below.
- Expiry: 2026-05-08.
- Verification evidence:
  - flat `POST /api/v1/superadmin/tenants/onboard` handler, support admin-password-reset handler, retired route error helper, old onboarding DTOs, old onboarding service, and old onboarding service tests are deleted
  - stale route probes now assert hard-cut framework behavior (`405` for the flat onboarding collision, `404` for the removed support-reset URL) with no tenant/user/password/email side effects
  - required IAM security-event writes now share the caller transaction and resolve existing accounts read-only, preventing password-change required-audit deadlocks against the caller's own canonical IAM account update
  - digest-token truth coverage now asserts the locked digest lookup used by reset-token consumption
  - code review found stale current-route documentation for retired auth profile; the DoD map now points at canonical self-profile/contact/security routes, and retired auth alias security/corridor shortcuts were removed instead of preserved
  - PR CI found a new `company->admin` module-boundary edge; company now depends on company-owned owner-invite/support-control ports, and the admin module implements those ports without adding an architecture allowlist entry
  - `openapi.json` was regenerated from `OpenApiSnapshotIT`; removed Super Admin flat onboarding/support reset and plan-template alias routes are absent, while the Super Admin plans route is canonical
  - `docs/openapi-endpoint-contract.md` was regenerated from `openapi.json` and now reports 362 paths, 428 operations, sha256 `43a4225c802b908590f39f91bdbd803139e8ad464d76d7c271b61fc541f11891`
  - frontend/API/module/workflow/runbook docs now point at Add Client, activation/setup, Super Admin plans, and current route behavior instead of old compatibility surfaces
  - post-CI remediation keeps tenant usage rollups on a single canonical upsert path for snapshot/counter rows, removes the check-then-insert race exposed by workflow concurrency coverage, clears stale JPA state after native refresh upserts, and avoids no-op counter `updated_at` churn
  - entitlement-aware integration fixtures now write the same current-state feature override settings used by runtime module gates, so HR, reports, manufacturing, and portal tests no longer rely on stale `enabledModules`-only state
  - dealer invoice PDF export now runs in a writable transaction because PDF generation records tenant usage; this preserves the canonical quota/audit side effect instead of bypassing it
  - retired raw-material intake route coverage accepts framework-level retired-route outcomes (`404` or `405`) and does not add a compatibility handler for the old intake workflow
  - post-CI Finance/Accounting shard remediation aligns accounting/report integration tests with current entitlement and settings contracts: manufacturing/reporting flows enable `MANUFACTURING` or `REPORTS_ADVANCED` through the canonical entitlement-aware helper, Super Admin settings mutations use the grouped `workflow` payload, and retired report aliases reach dispatcher 404 only after the report module gate is explicitly enabled
  - post-finance Changed-Code Coverage remediation advances the existing PR coverage baseline to the last PR #198 pushed head before the final finance-shard remediation, `b8f2d770aede1ec775d810e012db5c34e64d35ec`, so the router still runs shard routing against the full PR-vs-main diff while compacting changed-coverage enforcement to the final non-runtime-source CI/test/CI-infra evidence patch
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests test-compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q spotless:apply && MIGRATION_SET=v2 mvn -q spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=OpenApiSnapshotIT -Derp.openapi.snapshot.verify=true -Derp.openapi.snapshot.refresh=true test`
  - `bash scripts/guard_openapi_contract_drift.sh`
  - `bash scripts/guard_accounting_portal_scope_contract.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `bash ci/check-high-risk-changes.sh`
  - `git diff --check`
  - `rg -n "^(<<<<<<<|=======|>>>>>>>)" erp-domain/src/main/java erp-domain/src/test/java docs openapi.json ci`
  - `cd erp-domain && MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AuditServiceTest,CompanyServiceTest,SuperAdminTenantControlPlaneServiceTest,SuperAdminControllerTest,TenantOnboardingControllerTest,AuthTenantAuthorityIT,OpenApiSnapshotIT,TS_RuntimeCompanyContextFilterExecutableCoverageTest,TS_RuntimeCompanyControllerExecutableCoverageTest test`
  - `cd erp-domain && MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TS_AuthDigestTokenStorageGuardTest,IamCoreSchemaAndModelHardCutMigrationIT test`
  - `cd erp-domain && MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AuthControllerIT,OpenApiSnapshotIT,IdentityRouteInventoryContractTest test`
  - `bash ci/check-architecture.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=CompanyServiceTest,SuperAdminTenantEntitlementServiceTest test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock GATE_CANONICAL_BASE_REF=main bash scripts/gate_core.sh`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=CompanyControllerIT,SuperAdminControllerIT#superAdmin_canUpdateLifecycle_listTenants_andReadTenantDetail,ActuatorProdHardeningIT,CatalogImportConcurrencyIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=PortalInsightsControllerIT,ReportControllerSecurityIT,TenantRuntimePolicyServiceTest test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=DealerPortalControllerSecurityIT,ProductionCatalogFinishedGoodInvariantIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=RawMaterialControllerSecurityIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TenantUsageRollupServiceTest,CatalogImportConcurrencyIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock bash scripts/run_test_manifest.sh --profile pr-fast --label auth-tenant --maven-arg -Dtest.groups= --manifest ci/pr_manifests/pr_auth_tenant.txt`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock bash scripts/run_test_manifest.sh --profile risk --label risk-access --manifest ci/pr_manifests/pr_risk_access.txt`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock bash scripts/run_test_manifest.sh --profile pr-fast --label business-slice --maven-arg -Dtest.groups= --manifest ci/pr_manifests/pr_business_slice.txt`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AccountingCatalogControllerSecurityIT,ReportExportApprovalIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=ReportInventoryParityIT,ReportControllerRouteContractIT test`
  - `MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock bash scripts/run_test_manifest.sh --profile pr-fast --label accounting --maven-arg -Dtest.groups= --manifest ci/pr_manifests/pr_accounting.txt`
  - `python3 scripts/ci_risk_router.py --base dc73f1013dd63d378d9cdc76dd4f2600bde9e584 --head HEAD`
  - `python3 -m py_compile scripts/ci_risk_router.py scripts/pr_ci_parity.py`
  - `bash ci/check-ci-config.sh`
- Result summary:
  - focused auth/company/Super Admin/OpenAPI/runtime pack reported 213 tests run, 0 failures/errors/skips
  - focused IAM digest/migration pack reported 11 tests run, 0 failures/errors/skips after fixing the Gate Core findings
  - focused auth route/OpenAPI inventory pack reported 43 tests run, 0 failures/errors/skips after resolving the code review blocker
  - module-boundary fix pack reported `CompanyServiceTest` and `SuperAdminTenantEntitlementServiceTest` green, and `ci/check-architecture.sh` passed with no `company->admin` edge
  - Gate Core reported 416 tests run, 0 failures/errors/skips; module coverage passed with line ratio `0.9611307420494699` and branch ratio `0.8980891719745223`
  - post-CI Access/Tenant manifest rerun reported 159 tests run, 0 failures/errors/skips after canonical status and entitlement fixture fixes
  - post-CI risk Access manifest rerun reported 13 tests run, 0 failures/errors/skips after actuator and HR module-gate expectation updates
  - post-CI Workflow Integration manifest rerun reported 337 tests run, 0 failures/errors/skips after tenant usage rollup upserts, dealer PDF transaction, manufacturing entitlement, and retired-route assertion fixes
  - post-CI Finance/Accounting focused rerun reported `AccountingCatalogControllerSecurityIT` 8 tests and `ReportExportApprovalIT` 7 tests with 0 failures/errors/skips; the report focused rerun passed `ReportInventoryParityIT` and `ReportControllerRouteContractIT`
  - post-CI Finance/Accounting manifest rerun reported 360 tests run, 0 failures/errors/skips after entitlement-aware report/accounting fixture updates
  - PR #198 CI routing simulation applies coverage baseline `b8f2d770aede1ec775d810e012db5c34e64d35ec`, keeps the requested diff based at PR #197 main `dc73f1013dd63d378d9cdc76dd4f2600bde9e584`, and reduces the changed-coverage scope to docs/test/CI-infra evidence only with `run_changed_coverage=false`
  - CI routing scripts compile under Python, and CI config check passed after advancing the default coverage baseline
  - OpenAPI drift guard, accounting portal scope guard, knowledgebase lint, high-risk guard, Spotless, conflict-marker scan, whitespace diff check, and Gate Core front-door catalog/flaky guards passed
  - no bearer tokens, passwords, activation links, reset links, token digests, provider credentials, or `.env` values were printed in evidence

## Addendum — `m6-owner-setup-corridor`

- Scope: owner first-login setup corridor after activation, including setup step persistence on companies, tenant-workflow setup-required gating, owner/admin setup APIs, invite-role bounds, and OpenAPI/test coverage.
- Risk trigger: touches high-risk company/auth boundary code and Flyway v2 schema under `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**`, and `erp-domain/src/main/resources/db/migration_v2/**`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary expansion, destructive migration, or secret-handling relaxation was introduced. The schema adds nullable setup-step timestamps only; setup APIs are tenant owner/admin-only and platform Super Admin/staff actors are denied.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun compile, Spotless, targeted owner setup/Super Admin/OpenAPI tests, OpenAPI drift guard, high-risk guard, and mission-safe baseline validators.
- Expiry: 2026-05-05.
- Verification evidence:
  - activation completion still moves tenants to `SETUP_PENDING`, with setup steps limited to company details, GST when enabled, accounting, invite team, and finish
  - owner setup APIs persist ordered step completion and reject out-of-order accounting, immutable company mutation probes, branch/warehouse payload fields via strict input handling, and `ROLE_SUPER_ADMIN` invites
  - invite-team setup accepts tenant-bounded roles, creates the invited tenant user once, and idempotent invite replay returns the stable setup status without duplicate users or emails
  - setup-required tenants can use auth/setup routes but receive safe `TENANT_SETUP_REQUIRED` denial for representative tenant workflows until finish
  - finish is idempotent, sets onboarding completion once, and transitions the read model to `TRIAL_ACTIVE` or `ACTIVE`
  - accepted setup mutations write required audit evidence with safe step/action metadata and no passwords, tokens, activation links, or tenant-private business data
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=SuperAdminControllerIT#ownerFirstLoginSetupCorridorIsOrderedResumableIdempotentAndAuthorized test`
  - `cd erp-domain && MIGRATION_SET=v2 ERP_OPENAPI_SNAPSHOT_VERIFY=true ERP_OPENAPI_SNAPSHOT_REFRESH=true mvn -Djacoco.skip=true -Dtest=OpenApiSnapshotIT test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile && MIGRATION_SET=v2 mvn spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=SuperAdminControllerIT,OpenApiSnapshotIT,SuperAdminTenantControlPlaneServiceTest,CompanyContextFilterControlPlaneBindingTest test`
  - `bash scripts/guard_openapi_contract_drift.sh && bash ci/check-high-risk-changes.sh`
  - `bash scripts/guard_accounting_portal_scope_contract.sh && bash scripts/validate_m0_validation_harness_gates.sh --static && bash ci/lint-knowledgebase.sh`
- Result summary:
  - targeted owner setup canary reported 1 test run, 0 failures/errors/skips
  - OpenAPI snapshot refresh suite reported 15 tests run, 0 failures/errors/skips
  - impacted Super Admin/OpenAPI/filter suite reported 99 tests run, 0 failures/errors/skips
  - compile, Spotless, OpenAPI drift guard, high-risk guard, accounting portal scope guard, M0 static gates, and knowledgebase lint passed
  - broad `scripts/gate_fast.sh` was attempted after targeted validators and remains blocked by unrelated legacy truth-lane failures outside this packet; see worker handoff for details
  - no bearer tokens, passwords, activation links, token digests, provider credentials, or `.env` values are included in evidence

## Addendum — `m5-fix-activation-reset-digest-metadata`

- Scope: activation and password-reset token persistence metadata hardening for digest algorithm/version.
- Risk trigger: touches high-risk auth token persistence, Super Admin activation token issuance, and Flyway v2 schema under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**`, and `erp-domain/src/main/resources/db/migration_v2/**`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary change, destructive migration, or secret-handling relaxation was introduced. The schema only adds/backfills/enforces non-secret digest metadata for existing digest-only token rows and keeps raw token/link material out of persistence.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun compile, Spotless, targeted digest-token guard/auth reset/activation tests, high-risk guard, and mission-safe baseline validators.
- Expiry: 2026-05-05.
- Verification evidence:
  - `password_reset_tokens` and `tenant_activation_tokens` now persist `digest_algorithm='SHA-256'` and `digest_version=1` alongside the existing one-way `token_digest`
  - Flyway v2 migration `V192__token_digest_metadata.sql` adds, backfills, marks non-null, and constrains digest metadata for both tables without adding any raw token, activation-link, or reset-link fields
  - token issuance for password reset and tenant activation writes metadata from centralized `AuthTokenDigests` constants
  - activation lookup and reset lookup still hash caller-supplied token material before repository lookup
  - non-black-box truth-suite guard covers entities, migration metadata, centralized digest constants, repository lookup paths, and token/audit metadata-only proof
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TS_AuthDigestTokenStorageGuardTest,AuthPasswordResetPublicContractIT,PasswordResetServiceTest,SuperAdminTenantControlPlaneServiceTest,SuperAdminControllerIT test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/lint-knowledgebase.sh`
- Result summary:
  - targeted digest-token/auth/activation suite reported 94 tests run, 0 failures/errors/skips
  - high-risk guard and knowledgebase lint both passed for the updated R2 evidence
  - password-reset and activation integration proofs now assert persisted digest metadata values and absence of raw token columns
  - no bearer tokens, passwords, activation links, reset links, token digests, provider credentials, or `.env` values were printed in evidence

## Addendum — `m5-fix-activation-email-after-audit-commit`

- Scope: activation create-with-send, send, and resend email side-effect ordering after required platform audit persistence and transaction commit.
- Risk trigger: touches high-risk company/Super Admin activation control-plane code and activation regression tests under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**` and `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/**`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary change, data migration, or secret-handling relaxation was introduced. The change narrows activation email delivery so required audit persistence and the surrounding transaction commit happen before SMTP delivery, and audit failure fails closed without calling the activation email sender.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun compile, Spotless, targeted Super Admin activation/controller tests, high-risk guard, and mission-safe baseline validators.
- Expiry: 2026-05-05.
- Verification evidence:
  - create-with-send, send, and resend activation paths now persist token/company state and call the required audit writer before registering activation email delivery after commit
  - SMTP delivery is registered with Spring transaction synchronization and runs only after the transaction containing token/company/audit state commits
  - audit persistence/signing failure propagates before SMTP delivery for all three email-sending paths
  - copy-link remains an explicit response/audit boundary and does not call the activation email sender
  - regression tests verify a simulated required-audit failure sends no activation email for create-with-send, send, or resend
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=SuperAdminTenantControlPlaneServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -DskipTests compile`
  - `cd erp-domain && MIGRATION_SET=v2 mvn spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=SuperAdminTenantControlPlaneServiceTest,SuperAdminControllerIT,SuperAdminControllerTest test`
  - `bash ci/lint-knowledgebase.sh`
  - `bash ci/check-high-risk-changes.sh`
  - mission-safe manifest test command with compile, Spotless, targeted Super Admin/auth/OpenAPI/runtime tests, OpenAPI drift guard, high-risk guard, and M0 static guards
- Result summary:
  - targeted service regression suite reported 41 tests run, 0 failures/errors/skips
  - targeted controller/service suite reported 61 tests run, 0 failures/errors/skips
  - mission-safe validator reported 161 tests run, 0 failures/errors/skips plus OpenAPI drift, high-risk, and M0 static guards OK
  - no bearer tokens, passwords, activation links, token digests, provider credentials, or `.env` values were printed in evidence

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
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuditServiceTest,TenantOnboardingControllerTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuditServiceTest,TenantOnboardingControllerTest,CriticalFixtureServiceTest test`
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
  - frontend contract: `docs/frontend-portals/tenant-admin/identity-iam.md`
  - migration and rollback docs: `docs/runbooks/migrations.md`, `docs/runbooks/rollback.md`
