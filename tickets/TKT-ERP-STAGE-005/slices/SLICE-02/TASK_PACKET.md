# Task Packet

Ticket: `TKT-ERP-STAGE-005`
Slice: `SLICE-02`
Primary Agent: `refactor-techdebt-gc`
Reviewers: `qa-reliability`
Lane: `w2`
Branch: `tickets/tkt-erp-stage-005/refactor-techdebt-gc`
Worktree: `/home/realnigga/Desktop/orchestrator_erp_worktrees/_tmp_orch_cleanrepo_worktrees/TKT-ERP-STAGE-005/refactor-techdebt-gc`

## Objective
M18-S2A tenant hold/block controls with super-admin authority and fail-closed runtime enforcement

Acceptance criteria:
- Runtime access fails closed with deterministic `403` when tenant lifecycle state is hold/blocked.
- Unauthenticated requests remain `401`; tenant-state denial applies only after authentication context is valid.
- AuthTenantAuthorityIT contains coverage for tenant hold/block runtime behavior.

## Agent Write Boundary (Enforced)
- `erp-domain/src/main/java/`
- `erp-domain/src/test/java/`
- `docs/`

## Requested Focus Paths
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/CompanyContextFilter.java`
- `erp-domain/src/test/java/com/bigbrightpaints/erp/modules/auth/AuthTenantAuthorityIT.java`

## Required Checks Before Done
- `cd erp-domain && mvn -B -ntp -Dtest='AuthTenantAuthorityIT' test`
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
You are `refactor-techdebt-gc`.
Implement this slice with minimal safe patching and proof-backed output.

Start your response with identity exactly:
- `I am refactor-techdebt-gc and I own SLICE-02.`

Required output:
- files_changed
- commands_run
- harness_results
- residual_risks
- blockers_or_next_step
```
