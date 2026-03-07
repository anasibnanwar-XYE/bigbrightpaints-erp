# Executable Remediation Specs

This folder turns the review set into an implementation system.

Use the review docs for evidence and traceability. Use this package for execution order, packet shape, guardrails, rollback, and narrow workflow planning.

Do not jump from a backlog row straight into code. The lane `EXEC-SPEC` is the contract for how a slice is shaped, what it is allowed to touch, and what proof it owes before frontend or operators depend on it.

## Read Order
1. `00-program-map.md`
2. `00-current-auth-merge-gate.md` while the current auth branch gate is still open
3. `PACKET-TEMPLATE.md`
4. `VALIDATION-FIRST-BUNDLE.md` when the lane carries prove-first findings
5. The lane `EXEC-SPEC.md` for the row you are executing
6. The linked source review docs and tests from that lane
7. `RELEASE-GATE.md` before merge or promotion
8. `remediation-backlog.md` again before merge so the lane still matches current mission order

## Lane Map
- Row 1 -> `01-lane-control-plane-runtime/EXEC-SPEC.md`
- Row 2 -> `02-lane-auth-secrets-incident/EXEC-SPEC.md`
- Row 3 -> `03-lane-accounting-truth-boundary/EXEC-SPEC.md`
- Row 4 -> `04-lane-commercial-workflows/EXEC-SPEC.md`
- Row 5 -> `05-lane-catalog-manufacturing/EXEC-SPEC.md`
- Row 6 -> `06-lane-governance-finance/EXEC-SPEC.md`
- Row 7 -> `07-lane-orchestrator-ops/EXEC-SPEC.md`
- Row 8 -> `08-lane-quality-governance/EXEC-SPEC.md`

## Companion Packages
- Row 5 also consumes `/home/realnigga/Desktop/mission-control-refactor-specs/catalog-materials-refactor/README.md`, which remains the detailed authority-migration package for the catalog/material/manufacturing refactor

## Execution Rules
- one lane owns the slice
- one slice moves one boundary, guardrail, or validation packet at a time
- validation-first findings must be re-proved before backend work is opened
- validation-first proof uses `VALIDATION-FIRST-BUNDLE.md`, not free-form notes
- frontend cutover waits for packet proof plus the shared release gate
- rollback is written before merge, not after a staging surprise
- the packet owner, reviewer, QA owner, and release approver must all be named on the slice

## What This Package Solves
- broad mission order
- narrow packet-by-packet execution
- validation-first handling for drift-prone findings
- rollback and merge gates
- proof expectations before frontend or operators depend on the result
- a reusable packet template so delegated work stays at the same quality bar

## Later Work
Rows 9-11 remain later cleanup and ratchet work. They should not overtake lanes 1-8 unless a fresh production incident changes the priority.
