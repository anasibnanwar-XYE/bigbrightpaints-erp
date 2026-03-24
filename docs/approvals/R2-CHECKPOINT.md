# R2 Checkpoint

## Scope
- Feature: `ERP-33 pause HR/payroll behind tenant module gate`
- PR: `#135`
- PR branch: `erp-33-pause-hr-payroll-module`
- Review candidate: hard-cut `HR_PAYROLL` behind the tenant module gate while refreshing the branch onto latest `main` and preserving the reviewed pause behavior.
- Why this is R2: this packet changes tenant-scoped module-gated runtime surfaces, admin approval routing, payroll/accounting month-end behavior, portal/orchestrator HR visibility, and a `migration_v2` schema change that auto-pauses existing tenants.

## Risk Trigger
- Triggered by changes under `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/portal/`, `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/`, and `erp-domain/src/main/resources/db/migration_v2/V165__pause_hr_payroll_module.sql`.
- Contract surfaces affected: `GET /api/v1/admin/approvals`, `/api/v1/payroll/**`, `/api/v1/accounting/payroll/**`, `/api/v1/portal/workforce`, portal dashboard HR metrics, orchestrator admin/health HR snapshots, and accounting period-close diagnostics that previously counted payroll backlog.
- Failure mode if wrong: paused tenants can still see or act on payroll surfaces, month-end close can be blocked by stranded payroll diagnostics, or rollback/re-enable can leave tenant module state inconsistent across existing companies.

## Approval Authority
- Mode: human
- Approver: `Anas ibn Anwar`
- Canary owner: `Anas ibn Anwar`
- Approval status: `pending green CI and explicit merge approval`
- Basis: resolved review threads on PR `#135`, refreshed merge from latest `main`, focused local unit/integration proof on Colima-backed Testcontainers, and explicit acceptance of the hard-cut decision to hide paused payroll approvals until super-admin re-enables the module.

## Escalation Decision
- Human escalation required: yes
- Reason: this packet changes tenant runtime gating plus schema-driven default behavior for existing tenants, so merge remains gated on explicit human approval after CI settles on PR `#135`.

## Rollback Owner
- Owner: `Anas ibn Anwar`
- Rollback method: revert PR `#135` (including merge-refresh follow-ups through `e97ceaaa`), redeploy the prior backend build, then follow the `erp-33.pause-hr-payroll-module` rollback entry in `docs/runbooks/rollback.md` to restore the pre-pause module default and tenant enablement state.
- Rollback trigger:
  - paused tenants can still access payroll, accounting-payroll, workforce, or orchestrator HR surfaces
  - paused tenants lose the documented super-admin re-enable recovery path
  - period close still counts payroll unposted/unlinked backlog after the hard cut
  - the migration leaves tenant `enabled_modules` defaults or existing rows inconsistent after rollback

## Expiry
- Valid until: `2026-03-31`
- Re-evaluate if: PR `#135` head changes beyond commit `e97ceaaa`, CI reruns against a materially different candidate, or scope expands beyond the ERP-33 payroll-pause hard-cut and branch-refresh packet.

## Verification Evidence
- Branch refresh proof:
  - `cd "/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-33-merge-fix" && git merge origin/main`
  - result: merge refresh applied on top of reviewed ERP-33 changes; remote PR head updated to `e97ceaaa`
- Focused ERP-33 merge-validation proof:
  - `cd "/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-33-merge-fix/erp-domain" && DOCKER_HOST=unix:///Users/anas/.colima/default/docker.sock TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock TESTCONTAINERS_HOST_OVERRIDE=192.168.64.2 mvn clean -Dtest=AccountingPeriodServiceTest,IntegrationCoordinatorTest,ModuleGatingInterceptorTest,ModuleGatingServiceTest,AdminSettingsControllerApprovalsContractTest,AdminSettingsControllerTenantRuntimeContractTest,AdminApprovalRbacIT,HrPayrollModulePauseIT test`
  - result: `BUILD SUCCESS`
- Policy / review proof:
  - PR review threads on `#135` were checked after refresh and all review threads were confirmed resolved
  - local secret scan over staged merge-freshness diff found no credential/private-key patterns before commit/push
- Artifacts/links:
  - PR: `https://github.com/anasibnanwar-XYE/bigbrightpaints-erp/pull/135`
  - Head commit: `e97ceaaa`
  - Worktree: `/Users/anas/Documents/Factory/bigbrightpaints-erp_worktrees/erp-33-merge-fix`
