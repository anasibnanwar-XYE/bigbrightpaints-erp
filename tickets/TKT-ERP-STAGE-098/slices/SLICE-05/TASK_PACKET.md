# Task Packet

Ticket: `TKT-ERP-STAGE-098`
Slice: `SLICE-05`
Primary Agent: `sales-domain`
Reviewers: `qa-reliability, security-governance`
Lane: `w1`
Branch: `tickets/tkt-erp-stage-098/sales-domain`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-098/sales-domain`

## Objective
Raise anchored gate_fast changed-files line/branch coverage by adding deterministic tests for highest-deficit tenant runtime, company, admin/portal, purchasing, sales, and accounting-period services.

## Custom Multi-Agent Role (Codex)
- role: `cross_module_high`
- config_file: `.codex/agents/cross_module_high.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/sales/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/o2c/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`

## Cross-Workflow Dependencies
- Upstream slices: SLICE-02
- Downstream slices: SLICE-01
- External upstream agents to watch: orchestrator-runtime
- External downstream agents to watch: inventory-domain
- Contract edges:
  - downstream -> accounting-domain (slice SLICE-01): o2c posting and receivable linkage
  - downstream-external -> inventory-domain: dispatch and stock movement linkage
  - upstream -> auth-rbac-company (slice SLICE-02): tenant and role boundary contract
  - upstream-external -> orchestrator-runtime: async orchestration command contract

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='*Sales*' test`
- `bash ci/check-architecture.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am sales-domain and I own SLICE-05.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `sales-domain`.
Start your first line with: `I am sales-domain and I own SLICE-05.`
Use Codex custom multi-agent role `cross_module_high` from `.codex/agents/cross_module_high.toml`.
Implement this slice with minimal safe patching and proof-backed output.

Required output:
- identity
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
