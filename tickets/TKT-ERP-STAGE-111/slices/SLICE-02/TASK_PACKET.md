# Task Packet

Ticket: `TKT-ERP-STAGE-111`
Slice: `SLICE-02`
Primary Agent: `frontend-documentation`
Reviewers: `repo-cartographer`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-111/frontend-documentation`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-111/frontend-documentation`

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
- role: `frontend_documentation`
- config_file: `.codex/agents/frontend_arch.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `docs/accounting-portal-endpoint-map.md`
- `docs/admin-portal-endpoint-map.md`
- `docs/dealer-portal-endpoint-map.md`
- `docs/manufacturing-portal-endpoint-map.md`
- `docs/sales-portal-endpoint-map.md`
- `docs/*portal*handoff*.md`

## Requested Focus Paths
- `docs/accounting-portal-endpoint-map.md`

## Acceptance Criteria
- No explicit criteria in ticket YAML. Treat required checks and objective as DoD.

## Required Checks Before Done
- `bash ci/lint-knowledgebase.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am frontend-documentation and I own SLICE-02.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `frontend-documentation`.
Start your first line with: `I am frontend-documentation and I own SLICE-02.`
Use Codex custom multi-agent role `frontend_documentation` from `.codex/agents/frontend_arch.toml`.
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
