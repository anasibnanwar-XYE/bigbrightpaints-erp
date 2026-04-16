# R2 Checkpoint

Last reviewed: 2026-04-17

## Scope
- Feature: `seed-uat-runtime` branch governance packet for local UAT runtime seeding + baseline gate-fast stabilization
- Branch: `seed-uat-runtime` (base: `6bdfef50af3340489134068f61220095792b7e60`)
- PR: pending
- Review candidate:
  - keep the `uat-seed` Spring profile opt-in and environment-driven so default runtime behavior is unchanged unless the profile is selected
  - keep the seeded platform admin bound to platform scope only, and keep seeded tenant actors (`ROLE_ADMIN`, `ROLE_ACCOUNTING`, `ROLE_SALES`, `ROLE_FACTORY`, `ROLE_DEALER`) scoped to tenant `UAT01`
  - keep tenant bootstrap/admin provisioning on canonical scoped bootstrap + canonical reset-link flows rather than temporary-password responses or ad hoc account creation
  - keep company/onboarding runtime envelope sync treating omitted quotas as runtime defaults (`null`) instead of fail-closed `1`-value throttles
  - keep logout token-blacklist failures warning-only with hashed token id + actor attribution instead of breaking logout
  - keep the branch baseline green under strict dispatch-confirmation slip-line coverage for `scripts/gate_fast.sh`
- Why this is R2: this branch changes high-risk auth and company-control-plane behavior in `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/TenantAdminProvisioningService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantBootstrapDefaults.java`, and `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantOnboardingService.java`; it also adds the `uat-seed` local-runtime bootstrap in `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/UatSeedDataInitializer.java` and `erp-domain/src/main/resources/application-uat-seed.yml`, so explicit branch-local approval evidence is required before scrutiny can pass.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/AuthService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/TenantAdminProvisioningService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantBootstrapDefaults.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantOnboardingService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/UatSeedDataInitializer.java`
  - `erp-domain/src/main/resources/application-uat-seed.yml`
- Contract surfaces affected:
  - local UAT seed bootstrap and platform/tenant account seeding behavior
  - tenant initial-admin provisioning and canonical reset-link recovery behavior
  - company/onboarding runtime quota envelope synchronization for omitted quotas
  - logout error-handling/audit attribution when token blacklist storage is unavailable
  - branch baseline integrity for dispatch/factory slip coverage under gate-fast
- Failure mode if wrong:
  - seed runtime could create privileged accounts outside intended scopes or unexpectedly alter default runtime startup
  - tenant onboarding/company updates could clamp runtime quotas to `1` and break request admission for valid tenants
  - initial admin bootstrap/reset flows could diverge from canonical password-reset semantics
  - logout could fail closed on blacklist-store outages instead of returning success with warning-only audit evidence
  - branch scrutiny could be blocked by a non-green baseline or inaccurate approval evidence

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this packet is a compatibility-preserving, pre-deployment local/UAT bootstrap change set with auth/company impact; it needs explicit R2 evidence but does not currently justify a human-only gate.

## Escalation Decision
- Human escalation required: no
- Reason: the new seed path is profile-gated, environment-overridable, and intended for local/UAT bootstrap only; the branch adds no destructive migrations and does not widen tenant data boundaries in the default runtime path.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert `c087ba1e2` and `b1e4bf288` if auth/company bootstrap semantics or the baseline gate regress
  - after merge: revert the packet and rerun focused auth/company tests, `scripts/gate_fast.sh`, and enterprise policy gates
- Rollback trigger:
  - seeded platform or tenant actors bind to the wrong auth scope
  - omitted quota values are materialized as fail-closed `1` limits instead of runtime defaults
  - tenant-admin bootstrap/reset diverges from the canonical scoped-account flow
  - logout fails instead of emitting warning-only blacklist telemetry when the token store is unavailable
  - `scripts/gate_fast.sh` or enterprise policy gates fail after integration

## Expiry
- Valid until: 2026-04-24
- Re-evaluate if: scope expands into production seed enablement, broader superadmin privilege semantics, or migration-path changes.

## Verification Evidence
- Scope-to-evidence mapping:
  - focused auth/company/runtime-envelope proof: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/focused-auth-company-uat-tests.txt` covering `AuthServiceAuditAttributionTest`, `TenantAdminProvisioningServiceTest`, `CompanyServiceTest`, and `TenantOnboardingServiceTest`
  - branch baseline compatibility + strict slip-line confirmation proof: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/gate-fast.txt`
  - enterprise governance proof: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/check-enterprise-policy.txt`
  - review-governance proof: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/check-codex-review-guidelines.txt`
  - docs/governance lint proof: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/lint-knowledgebase.txt`
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuthServiceAuditAttributionTest,TenantAdminProvisioningServiceTest,CompanyServiceTest,TenantOnboardingServiceTest test`
  - `bash scripts/gate_fast.sh`
  - `bash ci/check-enterprise-policy.sh`
  - `bash ci/check-codex-review-guidelines.sh`
  - `bash ci/lint-knowledgebase.sh`
- Result summary:
  - focused auth/company tests passed, covering logout warning-only token-blacklist behavior, canonical tenant-admin provisioning/reset flow, and runtime-default quota envelope synchronization
  - `scripts/gate_fast.sh` passed and preserved the strict dispatch-confirmation slip-line baseline after the baseline-stabilization fix commit
  - `check-enterprise-policy`, `check-codex-review-guidelines`, and `lint-knowledgebase` all passed for this docs refresh
  - `gate_fast` changed-files coverage still reports `erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/UatSeedDataInitializer.java` as unmapped, so this checkpoint claims indirect composed-service proof for the new seed runner plus a green branch baseline, not dedicated line-level coverage of the initializer itself
- Artifact note:
  - evidence bundle index: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/README.md`
  - test evidence: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/focused-auth-company-uat-tests.txt`
  - gate evidence: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/gate-fast.txt`
  - policy evidence: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/check-enterprise-policy.txt`
  - policy evidence: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/check-codex-review-guidelines.txt`
  - docs evidence: `docs/approvals/evidence/2026-04-17-seed-uat-runtime-r2/lint-knowledgebase.txt`
