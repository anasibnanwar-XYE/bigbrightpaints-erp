# Lane 07 Exec Spec

## Covers
- Backlog row 7
- `ORCH-01`, `ORCH-03`, `ORCH-04`, `ORCH-05`, `ORCH-06`, `ORCH-07`, `ORCH-10`, `FIN-07`, `OPS-01`, `OPS-04`, `OPS-05`, `OPS-08`, `SA-06`, `ENV-01`, `ENV-02`, `ENV-03`, `ENV-04`, `ENV-05`

## Why This Lane Is Wave B
The next remediation wave becomes unsafe if operators cannot tell health truth, replay stuck work, or trust the management surfaces. This lane makes the rest of the program measurable and recoverable.

## Primary Review Sources
- `flows/orchestrator-background-integration.md`
- `ops-deployment-runtime.md`
- `static-analysis-triage.md`
- `README.md`

## Primary Code Hotspots
- `EventPublisherService`
- `OutboxPublisherJob`
- `SchedulerService`
- `IntegrationCoordinator`
- `EnterpriseAuditTrailService`
- `SupportTicketGitHubSyncService`

## Entry Criteria
- current app-port, management-port, and controller health probes are captured for the branch and environment under review
- mission environment constraints are written separately from product defects before implementation starts
- `ORCH-10` is re-checked against current controller code and published contracts
- no new business-workflow semantics are sharing the same slice

## Produces For Other Lanes
- one trustworthy operator health story for the broader program
- replay and recovery posture that other lanes can depend on during rollout
- cleaner release packets that separate environment noise from product regressions

## Packet Sequence

### Packet 0 - health-truth matrix and validation-first operator-route proof
- compare app-port, management-port, and orchestrator controller health surfaces
- decide which surface is authoritative for operators
- re-prove `ORCH-10` before any new admin operator-status work is opened
- output: health matrix and the completed `ORCH-10` validation-first bundle

### Packet 1 - replay and manual recovery posture
- build the operator-facing replay, reconciliation, or manual recovery path for ambiguous or stuck outbox work
- ensure recovery is documented, testable, and not just tribal knowledge
- output: replay and recovery packet with runbook evidence

### Packet 2 - scheduler, backlog, and tenant-scope truth
- codify scheduler singleton posture for non-outbox jobs
- add backlog-age or equivalent queue truth where needed
- scope tenant-visible event-health counters correctly
- output: scheduler and event-health packet

### Packet 3 - release evidence and ops handoff discipline
- separate environment instability from product defects in validation packets
- publish the operator evidence bundle that later lanes must attach before promotion
- output: release-ops handoff packet for the remediation program

## Frontend And Operator Handoff
- operators get one health URL matrix, one replay runbook, and one statement of which counters are authoritative
- admin or frontend consumers only use surviving operator surfaces after payload shape and auth scope are pinned
- release packets must state clearly whether a failed check is an environment constraint or a product defect

## Stop-The-Line Triggers
- orchestrator work starts changing domain workflow semantics instead of operator or recovery posture
- environment instability is used to waive product-correctness evidence
- a new operator route is built before `ORCH-10` is re-proved
- health-surface cleanup ships without a runbook saying which probe operators should trust

## Must Not Mix With
- new domain workflow semantics
- export or changelog governance changes
- auth-secret migration

## Must-Pass Evidence
- orchestrator service tests
- management-port health probe evidence
- replay or reconciliation operator runbook
- explicit notes on which findings are environment constraints versus product defects

## Rollback
- revert operator-surface additions or health-route wiring without losing durable outbox or audit evidence already captured

## Exit Gate
- operators have one trustworthy health story
- replay or manual recovery is no longer guesswork
- validation packets stop mixing environment instability with product regressions
- `ORCH-10` is either re-proven as a backend gap or downgraded to contract cleanup
