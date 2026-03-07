# Remediation Program Map

This is the broader picture for the current review set. It is intentionally stricter than a normal backlog because several lanes can break ERP truth or tenant isolation if they are mixed casually.

## Execution Waves

### Wave A: foundation contracts
1. Lane 01 control plane and runtime policy
2. Lane 02 auth, reset, secrets, and incident-response controls
3. Lane 03 accounting truth boundary

### Wave B: proof and recovery rails
4. Lane 07 orchestrator, ops, and recovery truth
5. Lane 08 quality governance and hard gates

### Wave C: domain hardening on top of stable contracts
6. Lane 04 commercial workflows
7. Lane 05 catalog and manufacturing guardrails
8. Lane 06 governance, finance controls, and exfiltration closure

## Program Entry Criteria
- the active slice has a packet built from [PACKET-TEMPLATE.md](./PACKET-TEMPLATE.md)
- the lane owner can state which earlier lane outputs it depends on
- current failures or drift were reproduced before code changes start
- validation-first items are explicitly marked as `prove first` rather than `build first`
- validation-first items attach a completed [VALIDATION-FIRST-BUNDLE.md](./VALIDATION-FIRST-BUNDLE.md) packet before backend scope opens
- frontend is not already depending on an unpublished or unproven route change

## Merge Gate Before The Broader Wave
The open auth-hardening branch must close these first:
- `TEN-10`
- `AUTH-09`
- `ADMIN-14`

Do not start broad remediation with those regressions still open, because they undermine the control-plane and auth lanes directly.

## Validation-First Findings
These items must be re-proved against current code, tests, and `openapi.json` before backend implementation work is opened:
- `TEN-09`
- `ADMIN-07`
- `ADMIN-13`
- `ORCH-10`

Planning rule:
- if current code and OpenAPI already satisfy the surface, treat the work as contract cleanup or frontend drift cleanup, not as a new backend endpoint build
- a prove-first decision is incomplete until commands, artifact paths, verdict, and reviewer sign-off are written in the validation bundle

## Cross-Package References
- Lane 05 is also constrained by `/home/realnigga/Desktop/mission-control-refactor-specs/catalog-materials-refactor/README.md`
- the external catalog/material package owns the detailed authority-migration sequence for product, material, receipt, packaging, and frontend cutover work
- the local remediation package still decides when that refactor is allowed to start relative to control-plane, auth, accounting, and ops lanes

## Shared Packet Shape
Every PR or mission slice must carry:
- `Scope`
- `Non-goals`
- `Caller map`
- `Invariant pack`
- `Verification pack`
- `Rollback pack`
- `Stop rule`
- `Exit gate`

Use [PACKET-TEMPLATE.md](./PACKET-TEMPLATE.md) instead of inventing a fresh packet shape in each lane.

## Non-Negotiables
- Do not mix control-plane or auth storage redesign with accounting-boundary redesign in the same PR.
- Do not enable inventory-accounting listener automation as part of this remediation wave.
- Do not weaken `CompanyDefaultAccountsService.requireDefaults()` to make seed or mock flows pass.
- Do not treat unpublished routes as backend defects until controller code, tests, and `openapi.json` agree they are supposed to exist.
- Do not move frontend to a new surface until the old route is a wrapper or a feature flag protects the cutover.
- Do not rewrite dispatch truth, purchase-invoice truth, or period-close maker-checker rules as collateral cleanup.

## Shared Proof Expectations
- changed invariants have automated coverage
- operator or runtime probes exist for user-facing paths
- OpenAPI parity is updated for any changed contract
- rollback is written before merge
- release notes say what stayed transitional

## Stop-The-Line Triggers
- a slice starts mixing lane ownership or unrelated boundary changes
- a validation-first finding is treated as confirmed backend work without current proof
- a public contract changes without tests plus OpenAPI parity
- a migration or secret-storage change has no rollback story
- frontend cutover is requested before wrapper or feature-flag protection exists
- environment noise is being used to waive product-correctness evidence

## Release Discipline
Before anything in this package is promoted beyond local or staging validation, run the checks in [RELEASE-GATE.md](./RELEASE-GATE.md).

## Dependency Notes
- Lane 04 must not precede Lane 03 because dispatch and settlement semantics depend on the accounting boundary.
- Lane 05 must not start authority cleanup before Lane 02 closes default-account and reset/control prerequisites that affect bootstrap and admin repair.
- Lane 06 should consume the stable boundaries from Lanes 01-03 rather than redefining them.
- Lane 07 and Lane 08 can overlap with early foundation work, but they must not be used to hide unresolved product regressions.
