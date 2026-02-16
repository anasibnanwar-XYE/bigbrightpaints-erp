# Task Packet

Ticket: `TKT-ERP-STAGE-001`
Slice: `SLICE-01`
Primary Agent: `auth-rbac-company`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-001/auth-rbac-company`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/TKT-ERP-STAGE-001/auth-rbac-company`

## Objective
M18-S2 tenant authority and bootstrap hardening with fail-closed tenant isolation enforcement.

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`

## Acceptance Criteria
- SUPER_ADMIN-only tenant bootstrap and tenant-admin creation paths are enforced in service-layer checks.
- ADMIN role cannot create or manage other tenants outside assigned company scope.
- Header/JWT/company mismatch fails closed with explicit 401/403 behavior.
- Cross-tenant IDOR attempts are blocked on critical tenant and auth endpoints.
- Audit trail captures actor, reason, and tenant scope for authority-sensitive actions.

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: SLICE-02
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain, purchasing-invoice-p2p, reports-admin-portal
- Contract edges:
  - downstream -> sales-domain (slice SLICE-02): tenant and role boundary contract
  - downstream-external -> accounting-domain: finance/admin authority boundaries
  - downstream-external -> factory-production: tenant-scoped manufacturing operations
  - downstream-external -> hr-domain: payroll/PII access boundaries
  - downstream-external -> inventory-domain: tenant context and role checks
  - downstream-external -> purchasing-invoice-p2p: tenant-scoped supplier/AP access rules
  - downstream-external -> reports-admin-portal: admin/report access boundaries

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='*Auth*' test`
- `cd erp-domain && mvn -B -ntp -Dtest='*Company*' test`
- `bash ci/check-architecture.sh`
- `bash ci/check-enterprise-policy.sh`
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
