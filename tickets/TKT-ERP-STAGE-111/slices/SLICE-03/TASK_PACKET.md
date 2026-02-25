# Task Packet

Ticket: `TKT-ERP-STAGE-111`
Slice: `SLICE-03`
Primary Agent: `refactor-techdebt-gc`
Reviewers: `qa-reliability`
Lane: `w3`
Branch: `tickets/tkt-erp-stage-111/refactor-techdebt-gc`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-111/refactor-techdebt-gc`

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
- role: `cross_module_high`
- config_file: `.codex/agents/cross_module_high.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/`
- `erp-domain/src/test/java/`
- `docs/`

## Requested Focus Paths
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth`

## Acceptance Criteria
- No explicit criteria in ticket YAML. Treat required checks and objective as DoD.

## Required Checks Before Done
- `bash ci/check-architecture.sh`
- `cd erp-domain && mvn -B -ntp test`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am refactor-techdebt-gc and I own SLICE-03.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `refactor-techdebt-gc`.
Start your first line with: `I am refactor-techdebt-gc and I own SLICE-03.`
Use Codex custom multi-agent role `cross_module_high` from `.codex/agents/cross_module_high.toml`.
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
