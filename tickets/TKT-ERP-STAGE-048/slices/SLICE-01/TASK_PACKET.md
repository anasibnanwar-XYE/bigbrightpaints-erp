# Task Packet

Ticket: `TKT-ERP-STAGE-048`
Slice: `SLICE-01`
Primary Agent: `auth-rbac-company`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-048/auth-rbac-company`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_exec_worktrees/TKT-ERP-STAGE-048/auth-rbac-company`

## Objective
Close M18-S2A gap by enforcing superadmin-only tenant configuration updates

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/`

## Requested Focus Paths
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthTenantAuthorityIT.java`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/CompanyControllerIT.java`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/service/CompanyServiceTest.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java`

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain, purchasing-invoice-p2p, reports-admin-portal, sales-domain
- Contract edges:
  - downstream-external -> accounting-domain: finance/admin authority boundaries
  - downstream-external -> factory-production: tenant-scoped manufacturing operations
  - downstream-external -> hr-domain: payroll/PII access boundaries
  - downstream-external -> inventory-domain: tenant context and role checks
  - downstream-external -> purchasing-invoice-p2p: tenant-scoped supplier/AP access rules
  - downstream-external -> reports-admin-portal: admin/report access boundaries
  - downstream-external -> sales-domain: tenant and role boundary contract

## Required Checks Before Done
- `bash ci/check-architecture.sh`
- `cd erp-domain && mvn -B -ntp -Dtest='AuthTenantAuthorityIT,CompanyControllerIT,CompanyServiceTest' test`
- `bash scripts/verify_local.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `auth-rbac-company`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
