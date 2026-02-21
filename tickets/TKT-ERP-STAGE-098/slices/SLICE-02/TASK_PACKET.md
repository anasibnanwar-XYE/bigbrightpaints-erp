# Task Packet

Ticket: `TKT-ERP-STAGE-098`
Slice: `SLICE-02`
Primary Agent: `auth-rbac-company`
Reviewers: `qa-reliability, security-governance`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-098/auth-rbac-company`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-098/auth-rbac-company`

## Objective
Raise anchored gate_fast changed-files line/branch coverage by adding deterministic tests for highest-deficit tenant runtime, company, admin/portal, purchasing, sales, and accounting-period services.

## Custom Multi-Agent Role (Codex)
- role: `cross_module`
- config_file: `.codex/agents/cross_module.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/rbac/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/TenantRuntimeEnforcementService.java`

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: SLICE-01, SLICE-03, SLICE-04, SLICE-05
- External upstream agents to watch: none
- External downstream agents to watch: factory-production, hr-domain, inventory-domain
- Contract edges:
  - downstream -> accounting-domain (slice SLICE-01): finance/admin authority boundaries
  - downstream -> purchasing-invoice-p2p (slice SLICE-03): tenant-scoped supplier/AP access rules
  - downstream -> reports-admin-portal (slice SLICE-04): admin/report access boundaries
  - downstream -> sales-domain (slice SLICE-05): tenant and role boundary contract
  - downstream-external -> factory-production: tenant-scoped manufacturing operations
  - downstream-external -> hr-domain: payroll/PII access boundaries
  - downstream-external -> inventory-domain: tenant context and role checks

## Required Checks Before Done
- `bash ci/check-architecture.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am auth-rbac-company and I own SLICE-02.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `auth-rbac-company`.
Start your first line with: `I am auth-rbac-company and I own SLICE-02.`
Use Codex custom multi-agent role `cross_module` from `.codex/agents/cross_module.toml`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- identity
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
