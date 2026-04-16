# R2 Checkpoint

Last reviewed: 2026-04-16

## Scope
- Feature: `tenant-admin-backend-hard-cut` slices 1-2
- Branch: codex/tenant-admin-hardcut-s1 (base: `fc3266800`)
- PR: pending
- Review candidate:
  - keep tenant-admin user assignment constrained to `ROLE_ACCOUNTING`, `ROLE_FACTORY`, `ROLE_SALES`, `ROLE_DEALER`
  - keep tenant-admin `ROLE_ADMIN` / `ROLE_SUPER_ADMIN` assignment denied with explicit access-denied auditing
  - keep tenant-admin custom/unknown role creation removed from the admin users workflow surface
  - keep superadmin settings/roles/notify control-plane hosts platform-scope-only for superadmin callers
  - keep denied role-mutation audit body extraction fail-closed when request body cache is unavailable
- Why this is R2: this packet changes high-risk auth/RBAC and superadmin control-plane enforcement behavior under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`, `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java`, and `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/RequestBodyCachingFilter.java`.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/RequestBodyCachingFilter.java`
- Contract surfaces affected:
  - tenant-admin create/update user role validation and assignment behavior
  - role-escalation denial semantics for tenant-admin actors
  - audit failure metadata for blocked privileged role attempts
  - platform-scope host enforcement for superadmin settings/role/notify control-plane routes
  - denied-path role target extraction behavior on oversized/uncached request bodies
- Failure mode if wrong:
  - tenant-admin could assign privileged or unknown roles
  - tenant-scoped superadmin sessions could execute platform-only notify/settings/roles endpoints
  - denied-path auditing could attempt raw request-stream reads instead of failing closed
  - audit trail for blocked role escalation could become inconsistent

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is a hard-cut tenant-admin RBAC tightening with no compatibility bridge; rollout remains pre-deployment but still requires explicit R2 evidence because auth/RBAC semantics changed.

## Escalation Decision
- Human escalation required: no
- Reason: this packet narrows tenant-admin privileges, hardens platform-only superadmin boundaries, and keeps denied-path parsing fail-closed; it does not widen tenant boundaries or introduce destructive migration behavior.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert the packet if tenant-admin role assignment or superadmin platform-only host contracts regress
  - after merge: revert packet and rerun focused security/auth tests plus enterprise policy gates
- Rollback trigger:
  - any non-allowlisted role can be assigned from the admin users API surface
  - tenant-scoped superadmin can access superadmin settings/roles/notify control-plane hosts
  - denied role-mutation audit extraction no longer fails closed when request body cache is unavailable
  - tenant-admin privileged role denial/audit behavior diverges from the contract
  - policy gate fails after integrating this packet

## Expiry
- Valid until: 2026-04-23
- Re-evaluate if: scope expands into broader auth, company-control-plane, or migration-path changes.

## Verification Evidence
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=RequestBodyCachingFilterTest,CompanyContextFilterControlPlaneBindingTest,AuthTenantAuthorityIT#tenant_scoped_super_admin_cannot_access_platform_only_superadmin_hosts test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuthTenantAuthorityIT#tenant_scoped_super_admin_cannot_access_platform_only_superadmin_hosts test`
  - `bash ci/check-codex-review-guidelines.sh`
  - `bash ci/check-enterprise-policy.sh`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AdminUserServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuthTenantAuthorityIT#admin_cannot_create_tenant_admin_user+tenant_admin_can_still_create_non_privileged_user test`
  - curl validation harness (Colima + Postgres container + local app boot + seeded tenant-admin principal):
    - login `POST /api/v1/auth/login` with tenant-admin credentials
    - role escalation probe against the admin users create endpoint with `roles=[ROLE_ADMIN]` => `403`
    - unknown-role probe against the admin users create endpoint with `roles=[ROLE_CUSTOM]` => `400`
- Result summary:
  - focused security/auth tests passed for this slice (`RequestBodyCachingFilterTest`, `CompanyContextFilterControlPlaneBindingTest`, `AuthTenantAuthorityIT` targeted method)
  - tenant-scoped superadmin deny contract now explicitly covered on canonical notify POST call
  - policy gates (`check-codex-review-guidelines`, `check-enterprise-policy`) passed
  - prior role-assignment hard-cut tests and curl probes remained valid from slice 1
- Artifact note:
  - runtime curl responses and Maven/guard outputs captured in shell output for this lane; no additional artifact file required.
