# R2 Checkpoint

Last reviewed: 2026-04-15

## Scope
- Feature: `tenant-admin-backend-hard-cut` slice 1 (role-assignment boundary hardening)
- Branch: codex/tenant-admin-hardcut-s1 (base: `fc3266800`)
- PR: pending
- Review candidate:
  - keep tenant-admin user assignment constrained to `ROLE_ACCOUNTING`, `ROLE_FACTORY`, `ROLE_SALES`, `ROLE_DEALER`
  - keep tenant-admin `ROLE_ADMIN` / `ROLE_SUPER_ADMIN` assignment denied with explicit access-denied auditing
  - keep tenant-admin custom/unknown role creation removed from the admin users workflow surface
- Why this is R2: this packet changes high-risk auth/RBAC runtime behavior under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`.

## Risk Trigger
- Triggered by:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java`
- Contract surfaces affected:
  - tenant-admin create/update user role validation and assignment behavior
  - role-escalation denial semantics for tenant-admin actors
  - audit failure metadata for blocked privileged role attempts
- Failure mode if wrong:
  - tenant-admin could assign privileged or unknown roles
  - tenant-admin user updates could silently accept unsupported roles
  - audit trail for blocked role escalation could become inconsistent

## Approval Authority
- Mode: orchestrator
- Approver: Droid mission orchestrator
- Canary owner: Droid mission orchestrator
- Approval status: branch-local integration candidate pending PR review
- Basis: this is a hard-cut tenant-admin RBAC tightening with no compatibility bridge; rollout remains pre-deployment but still requires explicit R2 evidence because auth/RBAC semantics changed.

## Escalation Decision
- Human escalation required: no
- Reason: this packet narrows tenant-admin privileges and rejects unsupported roles; it does not widen tenant boundaries or introduce destructive migration behavior.

## Rollback Owner
- Owner: Droid mission orchestrator
- Rollback method:
  - before merge: revert the packet if tenant-admin role assignment contracts regress
  - after merge: revert packet and rerun focused user-management tests plus policy gate
- Rollback trigger:
  - any non-allowlisted role can be assigned from the admin users API surface
  - tenant-admin privileged role denial/audit behavior diverges from the contract
  - policy gate fails after integrating this packet

## Expiry
- Valid until: 2026-04-22
- Re-evaluate if: scope expands into broader auth, company-control-plane, or migration-path changes.

## Verification Evidence
- Commands run:
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q spotless:check`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AdminUserServiceTest test`
  - `cd erp-domain && MIGRATION_SET=v2 mvn -q -Dtest=AuthTenantAuthorityIT#admin_cannot_create_tenant_admin_user+tenant_admin_can_still_create_non_privileged_user test`
  - curl validation harness (Colima + Postgres container + local app boot + seeded tenant-admin principal):
    - login `POST /api/v1/auth/login` with tenant-admin credentials
    - role escalation probe against the admin users create endpoint with `roles=[ROLE_ADMIN]` => `403`
    - unknown-role probe against the admin users create endpoint with `roles=[ROLE_CUSTOM]` => `400`
- Result summary:
  - formatting and focused unit/integration tests passed for this slice
  - curl probes confirmed privileged-role assignment remains denied and custom-role assignment is fail-fast validation
  - enterprise policy gate required this checkpoint update before final pass
- Artifact note:
  - runtime curl responses captured in shell output for this lane; no additional artifact file required.
