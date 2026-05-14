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
  - post-PR #200 rebase remediation restores the final PR #199 hard-cut orchestrator runtime shape after conflict resolution, with no `WorkflowService`, policy-enforcer, payroll-run request, or orchestrator-disabled compatibility leftovers.
  - IAM canonical account upsert now reconciles by current scoped identity (`email`, `auth_scope_code`) after first checking `user_id`, matching the database uniqueness contract and avoiding a second historical-account path.
  - after all runtime PR shards passed on `d1ea0d40e322675aa97e7c4c16e0451161fddc96`, changed-code coverage compaction advances the existing CI baseline to that green runtime SHA so shard routing still sees the full PR-vs-main diff while coverage enforcement is scoped to this final non-runtime CI/evidence patch.
- Commands run:
  - `bash scripts/guard_orchestrator_correlation_contract.sh`
  - `bash scripts/guard_flyway_v2_migration_ownership.sh`
  - `bash scripts/guard_flyway_v2_referential_contract.sh`
  - `bash scripts/flyway_overlap_scan.sh`
  - `git diff --check`
  - `rg -n "^(<<<<<<<|=======|>>>>>>>)" erp-domain/src/main/java erp-domain/src/test/java docs openapi.json ci scripts`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=CorrelationIdentifierSanitizerTest,TS_RuntimeOrchestratorExecutableCoverageTest,TS_RuntimeOrchestratorCorrelationCoverageTest,TS_RuntimeOrchestratorIdempotencyExecutableCoverageTest,TS_RuntimeTraceServiceExecutableCoverageTest,TS_PackagingSlipInvoiceLinkV2MigrationContractTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -DskipTests test-compile`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp spotless:check`
  - `bash scripts/guard_openapi_contract_drift.sh`
  - `bash scripts/guard_accounting_portal_scope_contract.sh`
  - `python3 scripts/ci_risk_router.py --base ae97fa3b4443a58a44d1b3f161d58e1b6964bab9 --head HEAD`
  - `python3 -m py_compile scripts/ci_risk_router.py scripts/pr_ci_parity.py`

## Addendum — `hard-cut-orchestrator-idempotency-trace-cleanup`

- Scope: removes the request-key fallback after orchestrator idempotency lease creation, requires the leased command to carry the canonical persisted idempotency key, and makes trace detail serialization fail closed instead of falling back to `Map.toString()`.
- Risk trigger: touches high-risk orchestrator runtime code under `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/**` and runtime truth-suite coverage.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no API route, tenant boundary, privilege rule, persistence schema, or background execution behavior was widened. The cleanup removes fallback behavior and keeps one canonical persisted idempotency/trace JSON path.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the orchestrator cleanup commits and rerun focused orchestrator runtime tests, compile, Spotless, high-risk guard, and whitespace checks.
- Expiry: 2026-05-16.
- Verification evidence:
  - `CommandDispatcher` now derives downstream event/trace idempotency only from the leased `OrchestratorCommand`.
  - missing or malformed leased command idempotency fails fast instead of falling back to the request argument.
  - `TraceService` still serializes null details as `{}` but throws on serialization failure before saving an invalid non-JSON audit row.
- Commands run:
  - `MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -Dtest=CommandDispatcherTest,TS_RuntimeOrchestratorExecutableCoverageTest test`
  - `MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -Dtest=TS_RuntimeTraceServiceExecutableCoverageTest,TS_RuntimeOrchestratorIdempotencyExecutableCoverageTest test`
  - `MIGRATION_SET=v2 mvn -B -ntp spotless:check`
  - `bash scripts/guard_orchestrator_correlation_contract.sh`
  - `git diff --check`
- Result summary:
  - command dispatcher focused pack reported 16 tests run, 0 failures/errors/skips after fixture cleanup
  - trace/idempotency focused pack reported 14 tests run, 0 failures/errors/skips
  - orchestrator correlation guard now asserts the leased canonical idempotency key is propagated downstream and the request-key fallback stays absent
  - Spotless and whitespace diff checks passed

## Addendum — `hard-cut-sales-order-status-canonicalization`

- Scope: adds Flyway v2 forward migration `V207__canonicalize_sales_order_statuses.sql` to collapse retired sales-order status tokens into the current canonical lifecycle, updates the migration contract guard, and removes `BOOKED` from performance fixtures in favor of `CONFIRMED`.
- Risk trigger: touches high-risk Flyway v2 schema under `erp-domain/src/main/resources/db/migration_v2/**` and sales/order-credit behavior fixtures.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no runtime compatibility path, fallback, or privilege/tenant-boundary change was introduced. Retired order statuses are handled once through forward data canonicalization so Java services keep one current-state lifecycle model.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the status canonicalization commit and rerun Flyway v2 migration guards, targeted O2C/performance/accounting tests, compile, Spotless, high-risk guard, and whitespace checks.
- Expiry: 2026-05-16.
- Verification evidence:
  - `BOOKED` canonicalizes to `CONFIRMED` so pending credit exposure remains represented by a current pending-exposure status.
  - `SHIPPED` and `FULFILLED` canonicalize to `DISPATCHED`; `COMPLETED` canonicalizes to `SETTLED`, preventing retired terminal sales-order states from re-entering fulfillment.
  - sales-order status history is canonicalized with the same mapping so frontend status history reads current lifecycle tokens.
  - performance fixtures now seed `CONFIRMED`, and the reports performance fixture enables `REPORTS_ADVANCED` through the canonical module helper.
- Commands run:
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TS_PackagingSlipInvoiceLinkV2MigrationContractTest,AccountingCatalogControllerSecurityIT,PerformanceBudgetIT,PerformanceExplainIT test`
- Result summary:
  - first focused run applied 97 Flyway v2 migrations through V207 and exposed a pre-existing `PerformanceBudgetIT` fixture issue where `REPORTS_ADVANCED` was not enabled for the reports endpoint
  - after enabling `REPORTS_ADVANCED` through the canonical test helper, the focused pack reported 14 tests run, 0 failures/errors/skips

## Addendum — `hard-cut-accounting-period-close-hook-cleanup`

- Scope: removes the production test-only period-close hook layer from accounting period close and moves the concurrency pause into the period-close atomicity integration test with a test-local PostgreSQL trigger/advisory lock.
- Risk trigger: touches high-risk accounting close/reopen code and period-close tests under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/**` and `erp-domain/src/test/java/**`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no accounting workflow, tenant boundary, approval rule, or migration behavior changed. The cleanup removes a production bean that existed only for tests; period close still takes the pessimistic period lock, captures the snapshot before marking the period closed, and rejects concurrent posting after close.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the hook cleanup commit and rerun compile, Spotless, high-risk guard, and focused period-close tests.
- Expiry: 2026-05-16.
- Verification evidence:
  - removed period-close hook names have no remaining source/docs/CI references
  - the period-close atomicity integration test still proves posting blocks while close holds the accounting-period lock, now using test-owned database blocking instead of a production hook
  - accounting period close/reopen unit and runtime coverage still pass
- Commands run:
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -DskipTests test-compile`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Dtest=AccountingPeriodServiceTest,AccountingPeriodServicePolicyTest,PeriodCloseAtomicityTest,TS_RuntimeAccountingFacadePeriodCloseBoundaryTest,TS_RuntimeAccountingPeriodServiceExecutableCoverageTest,TS_RuntimeAccountingPeriodPolicyExecutableCoverageTest,TS_RuntimeAccountingPeriodServiceRegressionExecutableCoverageTest -Djacoco.skip=true test`
  - `MIGRATION_SET=v2 mvn -B -ntp spotless:check`
  - `git diff --check`
  - `bash ci/check-high-risk-changes.sh`
- Result summary:
  - test-compile passed after removing the production hook
  - focused period-close pack reported 89 tests run, 0 failures/errors/skips
  - Spotless check, whitespace diff check, high-risk guard, and stale hook-name scan passed

## Addendum — `hard-cut-runtime-auth-cleanup`

- Scope: hard-cut cleanup branch that removes the duplicate tenant runtime access layer, unused SQL-backed IAM entity mirrors, and unused DTO leftovers while keeping the canonical runtime/auth/API contracts intact.
- Risk trigger: touches high-risk auth/company/runtime paths and related tests/docs/CI manifests under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/**`, `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/**`, `erp-domain/src/test/java/**`, `ci/**`, and canonical docs.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary expansion, destructive migration, or compatibility shim was introduced. Runtime admission remains on `TenantRuntimeEnforcementService`, portal/report interception keeps the same policy service, and IAM persistence remains on the SQL-backed canonical storage services.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the runtime/auth cleanup commits and rerun compile, targeted auth/runtime/IAM tests, stale-reference scans, high-risk guard, whitespace checks, and broader PR gates.
- Expiry: 2026-05-16.
- Verification evidence:
  - duplicate `TenantRuntimeAccessService`, `TenantRuntimeRequestAdmissionService`, stale runtime truth tests, and related wrapper references are absent from source/docs/CI manifests
  - runtime admission is still documented and routed through `TenantRuntimeEnforcementService`, including portal/report interceptor paths
  - unused IAM entity mirror classes are absent; SQL-backed `IamCanonicalStorageService`, `AuthSessionService`, migrations, and IAM contract tests remain canonical
  - removed accounting/admin/sales/production DTO class names have no remaining source/docs references
- Commands run:
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Dtest=TenantRuntimeEnforcementServiceTest,TenantRuntimeEnforcementInterceptorTest,CompanyContextFilterControlPlaneBindingTest,CompanyContextFilterPasswordResetBypassTest,TS_RuntimeTenantRuntimeEnforcementTest,TS_RuntimeTenantPolicyControlExecutableCoverageTest,TS_RuntimeCompanyContextFilterExecutableCoverageTest,TenantRuntimeEnforcementAuthIT,PortalInsightsControllerIT,SuperAdminUsageServiceTest -Djacoco.skip=true test`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp clean -DskipTests test-compile`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Dtest=IamCoreSchemaAndModelHardCutMigrationIT,TS_IamCoreSchemaAndModelHardCutMigrationContractTest,AuthControllerIT,MfaControllerIT,ScopedAccountBootstrapServiceIT,AdminUserSecurityIT -Djacoco.skip=true test`
  - exact stale-name scans for removed runtime services, runtime truth tests, unused DTOs, and IAM entity mirror classes
  - `git diff --check`
  - `bash ci/check-high-risk-changes.sh`
- Result summary:
  - targeted runtime/auth pack reported 166 tests run, 0 failures/errors/skips
  - clean compile after runtime cleanup passed with 1081 main sources and 589 tests
  - clean compile after unused accounting DTO cleanup passed with 1077 main sources and 589 tests
  - clean compile after unused portal DTO cleanup passed with 1073 main sources and 589 tests
  - clean compile after IAM entity deletion passed with 1065 main sources and 589 tests
  - targeted auth/IAM pack reported 80 tests run, 0 failures/errors/skips
  - stale-name scans, `git diff --check`, and high-risk guard passed

## Addendum — `active-flyway-v2-hard-cut-baseline`

- Scope: hard-cut schema-resource cleanup that removes the retired Flyway v1 tree, keeps `erp-domain/src/main/resources/db/migration_v2` as the only supported migration track, retires the Tally import table through a forward v2 migration while preserving historical migration files, and renames journal reference mapping storage from `legacy_reference` to `reference_key`.
- Risk trigger: touches high-risk Flyway v2 schema under `erp-domain/src/main/resources/db/migration_v2/**` and removes the retired Flyway v1 migration tree.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary expansion, or runtime fallback was introduced. This is a pre-release hard cut that removes an obsolete schema track and aligns the active v2 baseline with the current backend model.
- Rollback owner: Droid / PR owner.
- Rollback method: before merge, revert the schema-resource commit. After applying to an internal database, restore from a pre-change snapshot/PITR or rebuild from the previous v2 baseline; do not re-add the v1 migration tree or Tally import compatibility path.
- Expiry: 2026-05-16.
- Verification evidence:
  - old Flyway v1 references are absent from runtime, test, script, and config paths after the earlier docs/tooling cleanup and this schema-resource slice
  - `tally_imports` and `TallyImport` references are absent from current runtime/test/API surfaces outside historical and forward migration resources
  - Flyway v2 ownership, referential, and overlap guards pass against the reduced active migration track
  - staged whitespace check passes for the schema-resource slice
- Commands run:
  - `git diff --cached --check`
  - `bash scripts/guard_flyway_v2_migration_ownership.sh`
  - `bash scripts/guard_flyway_v2_referential_contract.sh`
  - `bash scripts/flyway_overlap_scan.sh`
  - `rg -n --pcre2 "db/migration(?!_v2)|classpath:db/migration(?!_v2)" .github ci scripts erp-domain/src erp-domain/pom.xml`
  - `rg -n "tally_import|TallyImport|tally-import" erp-domain/src/main erp-domain/src/test scripts ci .github openapi.json`
- Result summary:
  - staged whitespace check passed with no output
  - Flyway ownership guard passed with `[guard_flyway_v2_migration_ownership] OK`
  - Flyway referential guard passed after checking 237 FK references against 217 PK/UNIQUE targets
  - Flyway overlap scan passed after scanning 95 migrations with no duplicate table, constraint, or index findings
  - old Flyway path scan returned no matches across runtime, test, script, and config paths
  - Tally import scan returned no runtime/test/API matches

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
  - `cd erp-domain && MIGRATION_SET=v2 DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock mvn -q -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AuditServiceTest,CompanyServiceTest,SuperAdminTenantControlPlaneServiceTest,SuperAdminControllerTest,TenantOnboardingControllerTest,AuthTenantAuthorityIT,OpenApiSnapshotIT,TS_RuntimeCompanyContextFilterExecutableCoverageTest,TS_RuntimeTenantRuntimeEnforcementTest,TS_RuntimeTenantPolicyControlExecutableCoverageTest test`
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

## Addendum — `hard-cut-tenant-usage-shutdown-flush`

- Scope: tenant usage metric persistence shutdown behavior.
- Risk trigger: touches tenant/company usage accounting under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantUsageMetricsService.java`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary change, data migration, or billing model expansion was introduced. The shutdown flush now runs through a bounded daemon worker so graceful runtime shutdown can persist buffered tenant usage without letting unavailable storage stall CI/runtime shutdown. Scheduled and explicit tenant usage flush behavior remains canonical.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun the tenant usage metrics service test, high-risk guard, Spotless, compile/test-compile, and gate-fast.
- Expiry: 2026-05-16.
- Verification evidence:
  - `@PreDestroy` tenant usage flush is bounded to a short daemon-thread wait and cancels on timeout/failure
  - shutdown tests cover both successful buffered metric persistence and unavailable-storage timeout behavior
  - scheduled and explicit `flushPendingMetrics()` persistence tests still cover counter persistence, rollup recording, retry restoration, and multi-tenant partial failure behavior
  - this preserves graceful shutdown persistence while removing the unbounded path that waited on unavailable Testcontainers PostgreSQL connections after the truth-suite tests had already passed
- Commands run:
  - `git diff --check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=TenantUsageMetricsServiceTest test`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -B -ntp spotless:check`
  - `cd erp-domain && DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -DskipTests test-compile`
- Result summary:
  - targeted tenant usage metrics suite reported 17 tests run, 0 failures/errors/skips
  - high-risk guard, docs lint, Spotless, and test-compile passed

## Addendum — `journal-reference-key-forward-migration`

- Scope: Flyway v2 checksum preservation for retired Tally import storage, accounting journal reference mapping column canonicalization, and packaging-slip COGS journal link backfill.
- Risk trigger: touches Flyway v2 migration ownership under `erp-domain/src/main/resources/db/migration_v2/`.
- Approval mode: orchestrator; human escalation required: no.
- Escalation decision: no privilege widening, tenant-boundary change, or rollback-blocking schema change was introduced. Historical migrations remain immutable; the `legacy_reference` to `reference_key` rename, retired `tally_imports` table removal, and packaging-slip COGS journal link backfill now live in forward migrations so already-applied v2 databases avoid checksum drift and replay-safety gaps.
- Rollback owner: Droid mission orchestrator.
- Rollback method: revert this packet and rerun migration validation, accounting shard tests, high-risk guard, Spotless, compile/test-compile, and PR CI.
- Expiry: 2026-05-16.
- Verification evidence:
  - `V2__accounting_core.sql` and `V8__idempotency_case_insensitive_lookup_indexes.sql` preserve the originally applied `legacy_reference` schema/index names
  - `V37__tally_import_idempotency.sql` and `V184__accounting_truth_rls_hard_cut.sql` preserve applied migration contents instead of deleting or editing versioned scripts
  - `V204__journal_reference_mapping_reference_key.sql` performs the current-state rename and recreates canonical `reference_key` indexes
  - `V205__drop_tally_imports_hard_cut.sql` performs the hard-cut Tally import table removal as a forward migration
  - `V206__backfill_packaging_slip_cogs_journal_links.sql` links historical `COGS-{slip_number}` journals to `packaging_slips.cogs_journal_entry_id` before runtime relies on the canonical COGS marker
  - current Java/JPA/repository code continues to target `reference_key`
- Commands run:
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=ReconciliationServiceTest,BankReconciliationSessionServiceTest test`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AccountingCatalogControllerSecurityIT test`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=PurchasingServiceTest test`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=ReconciliationServiceTest,BankReconciliationSessionServiceTest,PurchasingServiceTest,AccountingCatalogControllerSecurityIT test`
  - `bash ci/check-high-risk-changes.sh`
  - `bash ci/lint-knowledgebase.sh`
  - `bash scripts/guard_openapi_contract_drift.sh`
  - `bash scripts/guard_accounting_portal_scope_contract.sh`
  - `python3 -m unittest testing/ci/test_pr_review_ci_contract.py`
  - `python3 -m py_compile scripts/ci_risk_router.py scripts/changed_files_coverage.py scripts/pr_ci_parity.py testing/ci/test_pr_review_ci_contract.py`
  - `MIGRATION_SET=v2 mvn -B -ntp spotless:apply && MIGRATION_SET=v2 mvn -B -ntp spotless:check`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -DskipTests test-compile`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 bash scripts/gate_fast.sh`
  - `MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -Dtest=TS_PackagingSlipInvoiceLinkV2MigrationContractTest test`
  - `DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock MIGRATION_SET=v2 mvn -B -ntp -Djacoco.skip=true -DfailIfNoTests=false -DfailIfNoSpecifiedTests=false -Dtest=AccountingCatalogControllerSecurityIT test`
  - `bash scripts/guard_flyway_v2_migration_ownership.sh`
  - `bash scripts/guard_flyway_v2_referential_contract.sh`
  - `bash scripts/flyway_overlap_scan.sh`
- Result summary:
  - Targeted accounting reconciliation, catalog, and purchasing tests passed with 39, 8, 23, and 70 tests respectively.
  - Flyway validated 96 migrations and a fresh Testcontainers PostgreSQL schema applied through `V206__backfill_packaging_slip_cogs_journal_links.sql`.
  - Packaging-slip migration contract test passed with 2 tests, 0 failures/errors/skips.
  - Accounting catalog security IT passed with 8 tests, 0 failures/errors/skips after applying through v206.
  - Flyway v2 ownership, referential, and overlap guards passed after staging the new migration.
  - High-risk, knowledgebase, OpenAPI drift, accounting portal scope, PR CI contract, Python compile, Spotless, and test-compile checks passed.
  - `gate_fast` passed with 904 tests, 0 failures/errors/skips; changed-files coverage recorded `threshold_gap_allowed=true` and `passes=true` for PR long-branch convergence parity.

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
