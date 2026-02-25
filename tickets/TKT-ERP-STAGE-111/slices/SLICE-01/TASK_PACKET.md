# Task Packet

Ticket: `TKT-ERP-STAGE-111`
Slice: `SLICE-01`
Primary Agent: `auth-rbac-company`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-111/auth-rbac-company`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-111/auth-rbac-company`

## Ticket Context
- title: Superadmin frontend password reset and token/company-code alignment
- goal: Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch

## Problem Statement
Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch

## Task To Solve
- Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch
- Expected outcome: Required checks pass and acceptance criteria are satisfied.

## Objective
Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch

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
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company`

## Acceptance Criteria
- No explicit criteria in ticket YAML. Treat required checks and objective as DoD.

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

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am auth-rbac-company and I own SLICE-01.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `auth-rbac-company`.
Start your first line with: `I am auth-rbac-company and I own SLICE-01.`
Use Codex custom multi-agent role `cross_module` from `.codex/agents/cross_module.toml`.
Ticket title: Superadmin frontend password reset and token/company-code alignment
Problem statement: Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch
Task to solve: Implement superadmin forgot/reset-password UX/API flow where entering superadmin email sends reset mail and fix invalid-token behavior caused by tenant/company scoped token mismatch
Expected outcome: Required checks pass and acceptance criteria are satisfied.
Implement this slice with minimal safe patching and proof-backed output.

Execution minimum:
- diagnose current behavior in the requested focus paths
- implement the root-cause fix in allowed scope
- add/adjust tests that prove acceptance criteria
- run required checks and report evidence

Required output:
- identity
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
