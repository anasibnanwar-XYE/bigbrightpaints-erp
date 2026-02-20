# Task Packet

Ticket: `TKT-ERP-STAGE-097`
Slice: `SLICE-02`
Primary Agent: `purchasing-invoice-p2p`
Reviewers: `qa-reliability, security-governance`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-097/purchasing-invoice-p2p`
Worktree: `/Users/anas/Documents/orchestrator_erp/bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-097/purchasing-invoice-p2p`

## Objective
Eliminate gate_fast release-validation coverage_skipped_files for anchor 06d85e792d2a80cd9fc1f8e5dc15d6dfa15dd93e and establish first-step coverage closure toward threshold pass.

## Custom Multi-Agent Role (Codex)
- role: `cross_module_high`
- config_file: `.codex/agents/cross_module_high.toml`
- runtime_profile: `resolved at runtime from role config`

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/purchasing/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/invoice/`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/truthsuite/p2p/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/purchasing/dto/RawMaterialPurchaseRequest.java`

## Cross-Workflow Dependencies
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, inventory-domain
- Contract edges:
  - downstream-external -> accounting-domain: ap/posting and settlement linkage
  - downstream-external -> inventory-domain: grn/stock intake coupling
  - upstream -> auth-rbac-company (slice SLICE-01): tenant-scoped supplier/AP access rules

## Required Checks Before Done
- `bash ci/check-architecture.sh`

## Reviewer Contract
- Review-only agents do not commit code.
- Add one review file per reviewer under `tickets/<id>/slices/<slice>/reviews/`.
- Mark review status as `approved` only with concrete evidence.

## Agent Identity Contract
- First output line must be: `I am purchasing-invoice-p2p and I own SLICE-02.`

## Shipability Bar
- The patch must be minimal, deterministic, and test-backed.
- Do not change behavior outside explicit scope without evidence and rationale.
- If any safety invariant is uncertain, fail closed and document blocker with evidence.

## Agent Prompt (Copy/Paste)
```text
You are `purchasing-invoice-p2p`.
Start your first line with: `I am purchasing-invoice-p2p and I own SLICE-02.`
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
