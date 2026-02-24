# Task Packet

Ticket: `TKT-ERP-STAGE-107`
Slice: `SLICE-02`
Primary Agent: `sales-domain`
Reviewers: `qa-reliability, security-governance`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-107/sales-domain`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-107/sales-domain`

## Ticket Context
- title: Dealer linkage integrity and journal invoice reference visibility
- goal: Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.

## Problem Statement
Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.

## Task To Solve
- Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.
- Expected outcome: Required checks pass and acceptance criteria are satisfied.

## Objective
Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.

## Custom Multi-Agent Role (Codex)
- role: `cross_module_high`
- config_file: `.codex/agents/cross_module_high.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/o2c/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales`

## Acceptance Criteria
- No explicit criteria in ticket YAML. Treat required checks and objective as DoD.

## Cross-Workflow Dependencies
- Upstream slices: none
- Downstream slices: SLICE-01
- External upstream agents to watch: auth-rbac-company, orchestrator-runtime
- External downstream agents to watch: inventory-domain
- Contract edges:
  - downstream -> accounting-domain (slice SLICE-01): o2c posting and receivable linkage
  - downstream-external -> inventory-domain: dispatch and stock movement linkage
  - upstream-external -> auth-rbac-company: tenant and role boundary contract
  - upstream-external -> orchestrator-runtime: async orchestration command contract

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='*Sales*' test`
- `bash ci/check-architecture.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am sales-domain and I own SLICE-02.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `sales-domain`.
Start your first line with: `I am sales-domain and I own SLICE-02.`
Use Codex custom multi-agent role `cross_module_high` from `.codex/agents/cross_module_high.toml`.
Ticket title: Dealer linkage integrity and journal invoice reference visibility
Problem statement: Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.
Task to solve: Prevent dealer-history orphaning on admin/sales dealer changes and surface invoice references in journal views without breaking accounting invariants.
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
