# Remediation Release Gate

No lane should be promoted toward production until this bundle exists for the exact branch and environment.

## Must-Pass Checks
- the lane `Exit Gate` is satisfied
- CODE-RED, invariant, and smoke checks for the touched area are green
- changed-files or targeted regression checks are green
- OpenAPI parity is updated for changed routes
- validation-first findings were either re-proved or downgraded before implementation
- validation-first findings carry a completed `VALIDATION-FIRST-BUNDLE.md`
- rollback steps are written and owned
- implementer, reviewer, QA owner, and release approver are named

## Data And Migration Controls
- forward-only migrations are reviewed
- dual-read or dual-write migrations have a cleanup plan
- data backfill scope is explicit
- no destructive migration ships without a recovery story
- rollback trigger threshold is written
- rollback rehearsal evidence is attached
- expected RTO and RPO are stated for the slice

## Runtime Evidence
- management health probe result
- user-facing route probe result
- queue or scheduler health evidence if background work changed
- log or audit evidence for the changed control surface

Fallback order when runtime surfaces are degraded:
1. live management or user-facing probes on the target branch and environment
2. targeted integration, truth-suite, or CODE-RED evidence for the same surface
3. controller, service, and `openapi.json` proof plus an explicit note that runtime evidence is degraded

Waiver rule:
- degraded runtime evidence can lower confidence, but it cannot be used to waive product-correctness proof

## Frontend And Operator Controls
- frontend has the sample response or contract diff for changed surfaces
- old routes stay wrapped or feature-flagged through cutover
- operator runbook is updated if health, replay, or support behavior changed
- compatibility window and deprecation cutoff are written for changed contracts
- consumer sign-off is attached before frontend cutover or route removal

## No-Go Conditions
- unresolved validation-first drift is still being treated as implementation truth
- mixed-ownership PRs changed unrelated boundaries together
- rollback is missing or depends on hand editing production data
- environment instability is being used to waive product correctness checks
- release roles are not assigned
- compatibility window or consumer sign-off is missing for a changed contract
