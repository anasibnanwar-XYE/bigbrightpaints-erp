# Lane 06 Exec Spec

## Covers
- Backlog row 6
- `ADMIN-02`, `ADMIN-03`, `ADMIN-04`, `ADMIN-05`, `ADMIN-06`, `ADMIN-07`, `ADMIN-10`, `ADMIN-11`, `ADMIN-13`, `FIN-02`, `FIN-03`, `FIN-04`, `FIN-05`, `FIN-06`, `FIN-08`, `ORCH-02`

## Why This Lane Comes After Foundations
It closes the remaining high-value governance and finance-control gaps once control-plane, auth, and accounting boundaries are already stable.

## Primary Review Sources
- `flows/admin-governance.md`
- `flows/finance-reporting-audit.md`
- `flows/orchestrator-background-integration.md`

## Primary Code Hotspots
- `AdminSettingsController`
- `ExportApprovalService`
- `ChangelogService`
- `SupportTicketService`
- `SupportTicketGitHubSyncService`
- `EnterpriseAuditTrailController`

## Entry Criteria
- Lane 01 control-plane scope and Lane 03 accounting-boundary posture are already stable for surfaces touched here
- `FIN-08` is reproducible and the current `ADMIN-07` and `ADMIN-13` contract state is checked against code and `openapi.json`
- support-ticket and changelog owners agree on the minimum acceptable privacy and governance posture
- no auth-secret storage migration or orchestrator replay buildout is sharing the same slice

## Produces For Other Lanes
- durable governance and finance-control behavior for admin, reporting, and audit surfaces
- a clean contract diff for frontend and operator consumers of admin or audit routes
- reduced exfiltration and unsynced-support risk before the next release wave

## Packet Sequence

### Packet 0 - validation-first proof and early runtime repair
- repair `FIN-08` business-event browse reliability before it is reused as proof
- re-prove `ADMIN-07` and `ADMIN-13` against current code and `openapi.json`
- output: completed validation-first bundles plus a contract-proof packet saying what is backend debt, what is drift cleanup, and what is not a backend route at all

### Packet 1 - durable export governance
- make export approval outcomes durable even when flags change
- emit audit evidence for request, approval, rejection, and download flows
- output: export-governance packet with approval and audit proof

### Packet 2 - changelog and support-ticket governance hardening
- restrict changelog writers to the intended governance boundary and preserve revision semantics
- redact and minimize support-ticket GitHub payloads
- add retry or operator recovery posture for unsynced tickets
- output: governance packet for changelog and support externalization

### Packet 3 - remaining finance-control and visibility cleanup
- close the remaining report-window, payroll, and tenant-visible counter issues in this row
- keep operator-style event counters scoped correctly so tenant admins do not see global backlog truth
- output: finance-control packet with counter-scope proof

## Frontend And Operator Handoff
- frontend receives a contract diff and sample payloads only for the surviving admin and audit surfaces
- support and governance owners get a clear runbook for unsynced tickets, approval evidence, and changelog revision behavior
- validation-first items must be called out as such in the handoff instead of being phrased as implemented backend work

## Stop-The-Line Triggers
- a validation-first item is converted into a backend build ticket without current proof
- support-ticket privacy or changelog governance changes drift into auth-secret or control-plane redesign
- `FIN-08` is bundled with broad audit model changes instead of a narrow runtime repair
- admin or finance contract changes ship without audit and OpenAPI parity proof

## Must Not Mix With
- token storage migration
- canonical posting-boundary redesign
- orchestrator replay control-plane buildout

## Must-Pass Evidence
- approvals contract tests
- support-ticket integration coverage
- changelog security tests
- export approval tests
- business-event browse regression coverage

## Rollback
- revert governance decision-path changes independently; do not roll back finance-control fixes by undoing unrelated control-plane work

## Exit Gate
- export and changelog governance is durable and auditable
- support-ticket externalization is minimized and recoverable
- business-event browse is reliable
- `ADMIN-07` and `ADMIN-13` are either re-proven as backend defects or downgraded to contract cleanup
