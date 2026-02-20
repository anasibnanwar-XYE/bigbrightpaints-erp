# Async Loop Operations Runbook

Last reviewed: 2026-02-18
Owner: Orchestrator Agent

This runbook defines the non-stop autonomous workflow used in this repository
to move the ERP toward staging/predeployment readiness.

## Mission
- Reach deployment-safe behavior with emphasis on:
  - accounting correctness and idempotency,
  - business-logic abuse resistance,
  - cross-module invariant safety,
  - security boundaries (auth/RBAC/company isolation),
  - migration/runtime release readiness.

## Hard Rules
- Work on branch `async-loop-predeploy-audit`.
- Keep all code/schema changes inside `erp-domain` unless a gate script/doc update
  is strictly required.
- Use Flyway V2 only for new migration work:
  - location: `erp-domain/src/main/resources/db/migration_v2`
  - history table: `flyway_schema_history_v2`
- Every change must be committed (no loose code changes left behind).
- After every runtime/config/schema/test commit:
  - run commit review (`codex review --commit <sha>` when available),
  - spawn one review-only subagent for deep regression/abuse review.
- Docs-only commit exception:
  - skip commit review/subagent only when no runtime/config/schema/test files changed in the same slice,
  - run `bash ci/lint-knowledgebase.sh` and log pass status.
- Lane alignment rule:
  - `fast_lane` is docs-only work and follows the docs-only commit exception.
  - `strict_lane` control-plane docs work (`docs/agents/`, `docs/ASYNC_LOOP_OPERATIONS.md`, `docs/system-map/REVIEW_QUEUE_POLICY.md`, `agents/orchestrator-layer.yaml`, `asyncloop`, `scripts/harness_orchestrator.py`, `ci/`) may skip reviewer-agent/commit-review only when no runtime/config/schema/test files changed and the strict trio passes:
    - `bash ci/lint-knowledgebase.sh`
    - `bash ci/check-architecture.sh`
    - `bash ci/check-enterprise-policy.sh`
  - `strict_lane` is required for accounting, auth/RBAC, migrations, orchestrator semantics, and any runtime/config/schema/test logic change.
  - `strict_lane` runtime/config/schema/test minimum harness evidence:
    - `bash ci/lint-knowledgebase.sh`
    - `bash ci/check-architecture.sh`
    - `bash ci/check-enterprise-policy.sh`
    - `bash ci/check-orchestrator-layer.sh`
    - `bash scripts/verify_local.sh`
- Subagents are for commit review only. Main implementation/audit work stays in
  the primary agent.
- Maintain backlog floor: at least 3 `ready` slices in `asyncloop`.
- After a completed slice, immediately add a new concrete slice.
- Orchestrator routing/review must follow `agents/orchestrator-layer.yaml`.
- Decisions must be proof-backed (tests/guards/traces), not assumption-backed.
- Scope priority source is `docs/system-map/Goal/ERP_STAGING_MASTER_PLAN.md`.

## Execution Loop (One Iteration)
1. Pick highest-risk `in_progress` or top `ready` slice from `asyncloop`.
2. Perform static abuse-oriented review on impacted services/controllers/repos.
3. Implement minimal safe patch (prefer invariants + idempotency + validation).
4. Add targeted negative and replay tests.
5. Run focused Maven test matrix for changed flows.
6. Commit with:
  - concise subject,
  - bullet comments describing exactly what changed and why.
7. For `strict_lane` runtime/config/schema/test commits, run commit review + review subagent.
8. If review finds issues:
  - fix immediately,
  - re-test,
  - commit follow-up,
  - re-run review.
9. Append evidence to `asyncloop`:
  - commit id,
  - tests run/results,
  - review findings/disposition,
  - active slice + replenished queue.
10. Move to next slice without waiting for user prompts.

## Planning Contract
- Keep deep plan slices; avoid generic tasks.
- Slice format:
  - module and service names,
  - abuse vector,
  - invariant expected,
  - tests to prove closure.
- Example slice naming:
  - `M5-S3 dispatch/invoice/accounting exact-link invariant sweep`
  - `M2-S11 settlement replay mismatch matrix parity`

## Evidence Logging Contract (`asyncloop`)
For each commit, append:
- `Completed Slice (Committed)` with commit SHA and summary bullets.
- `Verification` commands and pass/fail totals.
- `Post-Commit Review Evidence`:
  - `codex review --commit <sha>` summary,
  - subagent id and findings.
- `Loop Update`:
  - `tracked_now_ts`,
  - `tracked_elapsed`,
  - current `active_in_progress`.
- `Queue Rotation (Auto-Replenish)` with at least 3 `ready` items.

## Section 14.3 Anchor Gate Closure Procedure (Canonical Base)
Use this exact runbook for final ledger closure. Do not execute Section 14.3 from a slice branch.

1. Sync canonical base and freeze the closure target SHA:
  - `CANONICAL_BASE_BRANCH=harness-engineering-orchestrator`
  - `git fetch origin`
  - `git checkout "$CANONICAL_BASE_BRANCH"`
  - `git pull --ff-only origin "$CANONICAL_BASE_BRANCH"`
  - `CANONICAL_HEAD_SHA="$(git rev-parse HEAD)"`
2. Set immutable anchor on the same lineage:
  - choose `RELEASE_ANCHOR_SHA` from the active hardening train start (or prior closure anchor retained in `asyncloop`).
  - `git merge-base --is-ancestor "$RELEASE_ANCHOR_SHA" "$CANONICAL_HEAD_SHA"`
3. Run all ledger gates on the same frozen canonical SHA:
  - `git checkout --detach "$CANONICAL_HEAD_SHA"`
  - `mkdir -p "artifacts/gate-ledger/$CANONICAL_HEAD_SHA"`
  - `DIFF_BASE="$RELEASE_ANCHOR_SHA" GATE_FAST_RELEASE_VALIDATION_MODE=true bash scripts/gate_fast.sh 2>&1 | tee "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/gate_fast.log"`
  - `bash scripts/gate_core.sh 2>&1 | tee "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/gate_core.log"`
  - `bash scripts/gate_reconciliation.sh 2>&1 | tee "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/gate_reconciliation.log"`
  - `bash scripts/gate_release.sh 2>&1 | tee "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/gate_release.log"`
4. Capture immutable evidence before any anchor movement:
  - `shasum -a 256 "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/"*.log > "artifacts/gate-ledger/$CANONICAL_HEAD_SHA/SHA256SUMS"`
  - append `CANONICAL_BASE_BRANCH`, `CANONICAL_HEAD_SHA`, `RELEASE_ANCHOR_SHA`, exact commands, and artifact paths in `asyncloop`.
5. Fail closed + anchor rotation rule:
  - if any gate fails, stop closure and keep the same anchor.
  - rotate `RELEASE_ANCHOR_SHA` only after all four gates pass on one `CANONICAL_HEAD_SHA` and evidence is recorded.

## Recovery If Session Interrupts
1. Open `asyncloop` first.
2. Resume latest `active_in_progress`.
3. Check uncommitted changes:
  - if owned by current slice, continue and finish.
  - if unrelated unexpected edits appear, stop and report.
4. Continue commit/review cadence from latest recorded point.
5. If pausing mid-slice, append a `Pause Checkpoint` block in `asyncloop` with:
  - owned uncommitted file paths,
  - last targeted verification command and concrete failure line,
  - immediate next action to resume the same slice.

## Current High-Priority Continuation Targets
- M5 dispatch/invoice/accounting replay and double-post defenses.
- M2 settlement replay mismatch parity across all replay call sites.
- M6 duplicate idempotency helper consolidation map.
- M9 accounting query hotspot/index validation under replay-like load.
- M7 report-to-journal linkage traceability assertions.
