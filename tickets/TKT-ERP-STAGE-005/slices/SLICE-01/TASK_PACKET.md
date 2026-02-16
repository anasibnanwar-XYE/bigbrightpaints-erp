# Task Packet

Ticket: `TKT-ERP-STAGE-005`
Slice: `SLICE-01`
Primary Agent: `auth-rbac-company`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-005/auth-rbac-company`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo_worktrees/TKT-ERP-STAGE-005/auth-rbac-company`

## Objective
M18-S2A tenant hold/block controls with super-admin authority and fail-closed runtime enforcement

Acceptance criteria:
- Tenant hold/block actions are super-admin-only and reject tenant-admin attempts.
- Hold/block writes require explicit reason metadata and are immutable-audited.
- Company lifecycle state persists with deterministic values consumed by runtime enforcement.

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`

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
- `cd erp-domain && mvn -B -ntp -Dtest='AuthTenantAuthorityIT,*Company*' test`
- `bash ci/check-architecture.sh`
- `bash ci/check-enterprise-policy.sh`

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

Start your response with identity exactly:
- `I am auth-rbac-company and I own SLICE-01.`

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
