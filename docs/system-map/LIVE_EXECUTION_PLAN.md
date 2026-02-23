# Live Execution Plan (Async-Loop)

Last updated: 2026-02-23
Mode: Active (`gate_fast` changed-files coverage blocker; deployment blocked)

## Goal
- Restore gate freshness and re-close completion gates on canonical head.
- Completion-gate board remains pending until changed-files coverage passes (see `docs/system-map/COMPLETION_GATES_STATUS.md`).

## Current state (2026-02-23)
- Async-loop status: `ACTIVE`.
- Canonical base branch head: `6819522a09c00416a21d432e1a404b767d7b80d4`.
- Latest gate run head: `ab2e919839b92f43072566e6aa707268d9ee8538` (`tickets/tkt-erp-stage-104/release-ops`).
- `gate_fast`: `FAIL` (changed-files coverage below threshold; line_ratio `0.2223`, branch_ratio `0.1868`, files_considered `320`).
- `gate_core`: `PASS`.
- `gate_reconciliation`: `PASS`.
- `gate_release`: `PASS` (release migration matrix and predeploy scans).
- `check-architecture`: `PASS`.
- Full regression run: `PASS` (`1661` tests, `0` failures, `0` errors, `4` skipped).
- Evidence ledger:
  - gate refresh artifacts: `bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-104/release-ops/artifacts/`
  - full regression workspace: `bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-104/refactor-techdebt-gc/erp-domain`

## Active constraints (refresh required)
- `gate_fast` is evaluating a wide changed-file surface (`320` files), which keeps coverage ratios below closure threshold.
- Completion board cannot be marked safe-to-deploy until `gate_fast` passes on the intended anchor/diff base.
- Release gate local prerequisite is now codified: auto-starts `gate_release_pg` on `127.0.0.1:55432` unless disabled (`AUTO_START_GATE_RELEASE_PG=false`).

## Live plan lanes

### Lane A: Review queue drain (blocking lane)
1. Retry review dispatch each cycle.
2. While blocked, execute direct commit review fallback without timeout and log outcomes in `asyncloop`.
3. Record blocker + pending SHAs in `asyncloop`.
4. Apply fixes immediately for any reviewer finding once surfaced.

### Lane B: Gate freshness (safety lane)
1. Keep anchored `gate_fast` fresh on moving head.
2. Re-run `gate_core`, `gate_reconciliation`, and `gate_release` on cadence for staging evidence.
3. Record command outcomes in `asyncloop`.
4. Current note (2026-02-23): refreshed gate set on `ab2e919839b92f43072566e6aa707268d9ee8538`:
   - `gate_fast` FAIL (changed-files coverage below threshold; line_ratio `0.2223`, branch_ratio `0.1868`, files_considered `320`)
   - `gate_core` PASS
   - `gate_reconciliation` PASS
   - `gate_release` PASS

### Lane C: Consistency hardening (throughput lane)
1. Prefer small, evidence-backed slices with low blast radius.
2. Prioritize dedupe and role-parity consistency (dealer/supplier behavior symmetry).
3. Keep v2 contract stability: no public API naming drift.
4. Canonicalize internal mismatch metadata toward `partner*` while preserving external role-specific payload compatibility.
5. Last confirmed 2026-02-16: `c676bee9` dedupes async command lease bootstrap in `CommandDispatcher` through shared `startLease(...)`, reducing divergence risk across approval/fulfillment/dispatch/payroll flows while preserving exactly-once semantics and canonical idempotency propagation.

### Lane D: Completion-gate consolidation (closure lane)
1. Convert module-level evidence into explicit completion-gate closure packs.
2. Keep `docs/system-map/COMPLETION_GATES_STATUS.md` current after each gate-affecting tranche.
3. Do not claim safe-to-deploy until all `5/5` completion gates are marked `CLOSED`.

## Exit criteria
- Pending code-review queue is empty (all code commits reviewed).
- No unresolved high/critical findings remain.
- Final ledger gates pass on same closure evidence set.
- M8/M9 async hardening lane is complete and recorded in `asyncloop`.

## Latest gate evidence (2026-02-23)
1. Canonical base branch head: `6819522a09c00416a21d432e1a404b767d7b80d4`.
2. Latest gate run head: `ab2e919839b92f43072566e6aa707268d9ee8538`.
3. `gate_fast`: `FAIL` (changed-files coverage below threshold; line_ratio `0.2223`, branch_ratio `0.1868`, files_considered `320`).
4. `gate_core`: `PASS`.
5. `gate_reconciliation`: `PASS`.
6. `gate_release`: `PASS`.
7. `check-architecture`: `PASS`.
8. Full regression lane: `PASS` (`1661` tests, `0` failures, `0` errors, `4` skipped).
9. Evidence ledger:
   - gate refresh artifacts: `bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-104/release-ops/artifacts/`
   - full regression workspace: `bigbrightpaints-erp_worktrees/TKT-ERP-STAGE-104/refactor-techdebt-gc/erp-domain`
