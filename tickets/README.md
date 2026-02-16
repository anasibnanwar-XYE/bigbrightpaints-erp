# Local Ticket Ledger

This directory is a local, git-tracked operations ledger for harness-engineering execution.

## Purpose
- Replace ad-hoc status notes with machine-readable ticket artifacts.
- Track per-slice worktree assignments, reviewer evidence, and merge readiness.
- Keep execution legible for orchestrator + human oversight.

## Ticket Layout
For each ticket id (`TKT-*`), the orchestrator writes:

- `ticket.yaml`: source of truth for slices, lanes, branch/worktree mapping, and status.
- `SUMMARY.md`: compact board view.
- `TIMELINE.md`: append-only activity log.
- `commands/tmux-launch.sh`: tmux send-keys block for lane dispatch.
- `slices/<slice-id>/TASK_PACKET.md`: copy/paste instructions for the assigned agent.
- `slices/<slice-id>/reviews/<reviewer>.md`: review evidence files.
- `slices/<slice-id>/orchestrator-review.md`: orchestrator senior pre-merge review result.
- `reports/verify-*.md`: verification + merge reports.

## Core Commands
Create + plan + worktrees:
- `python3 scripts/harness_orchestrator.py bootstrap --title "<title>" --goal "<goal>" --paths "path1,path2"`

Regenerate tmux launch block:
- `python3 scripts/harness_orchestrator.py dispatch --ticket-id <TKT-ID>`

Record review result:
- `python3 scripts/harness_orchestrator.py review --ticket-id <TKT-ID> --slice-id SLICE-01 --reviewer qa-reliability --status approved --findings "none" --commands "bash scripts/verify_local.sh" --artifacts "artifacts/gate-fast/..."`

Verify all slices:
- `python3 scripts/harness_orchestrator.py verify --ticket-id <TKT-ID>`

Verify and merge ready slices:
- `python3 scripts/harness_orchestrator.py verify --ticket-id <TKT-ID> --merge`

Verify + merge + remove merged slice worktrees:
- `python3 scripts/harness_orchestrator.py verify --ticket-id <TKT-ID> --merge --cleanup-worktrees`

## Guardrails
- Primary module agents are blocked from merging if branch changes exceed their `scope_paths` permission boundaries.
- Reviewer evidence is mandatory before merge.
- Cross-slice overlaps across different implementation agents are blocked as `coordination_required` until orchestrator replans/consolidates.
- Merged slice worktrees are deleted by default if `automation.cleanup_worktrees_on_merge: true` in `agents/orchestrator-layer.yaml`.
- Human approval is reserved for R3 irreversible production actions only.
