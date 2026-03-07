# Remediation Packet Template

Use this template for every lane slice, PR, or delegated mission packet.

## 1. Header
- lane
- slice name
- finding IDs
- implementer
- reviewer
- QA owner
- release approver
- branch
- target environment

## 2. Lane Start Gate
- prerequisite lane outputs linked
- current failure or drift reproduced
- validation-first bundle linked if required
- slice still matches current remediation-backlog row

## 3. Why This Slice Exists
- the exact risk or blocker it closes
- why it belongs in this lane now
- what downstream lane it unlocks

## 4. Scope
- what changes
- what must not change
- routes, jobs, migrations, or feature flags touched

## 5. Caller Map
- controllers
- schedulers or listeners
- imports or bootstrap flows
- admin repair tools
- frontend or operator surfaces

## 6. Invariant Pack
- accounting invariants
- workflow invariants
- tenant or auth invariants
- audit or traceability invariants

## 7. Implementation Plan
1. code or config step
2. test or probe step
3. contract or OpenAPI step
4. docs or operator handoff step

## 8. Proof Pack
- exact automated tests
- exact runtime probes
- exact OpenAPI diff or parity check
- exact data or migration verification

## 9. Validation-First Evidence
- exact commands run
- exact artifact paths saved
- verdict: backend defect, contract cleanup, frontend drift, unpublished route, or environment-only constraint
- reviewer sign-off for the verdict

## 10. Rollback Pack
- what gets reverted first
- what data is forward-only
- what wrapper, flag, or alias remains available during rollback
- rollback trigger threshold
- rollback rehearsal evidence
- expected RTO and RPO

## 11. Stop Rule
- the condition that forces the slice to split instead of widening

## 12. Exit Gate
- what must be true before merge
- what must be true before the next lane consumes this slice

## 13. Handoff
- next lane
- remaining transitional paths
- operator or frontend note if the contract changed
- compatibility window and wrapper duration
- consumer sign-off needed before cutover
- deprecation or removal cutoff
