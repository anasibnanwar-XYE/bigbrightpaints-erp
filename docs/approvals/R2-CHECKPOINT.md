# R2 Checkpoint

## Scope
- Feature: `pr119-canary-readiness`
- Runtime candidate SHA: `2fba82e91748f60872727c8f530369e66d95c0a8`
- Readiness packet SHA: `ef2e56284aa38dc0799144765d8b91ba610c0ab5`
- Branch: `feature/pr119-canary-readiness-clean`
- Why this is R2: this packet closes the post-merge deploy-readiness gap on live `main`, fixes accounting-portal handoff RBAC/readiness drift, and records the canary ownership/rollback decision on the exact proved runtime SHA.

## Risk Trigger
- Triggered by post-merge deploy-readiness closure on live `main`, where incorrect portal handoff scope could have exposed accountant or sales users to deterministic `403` failures on admin-only exports during canary.
- Contract surfaces affected: accounting portal route ownership, portal/frontend handoff RBAC truth, audit-trail route guidance, release gate documentation, and canary operator ownership.
- Failure mode if wrong: canary proceeds without a named owner/rollback decision, or frontend/operator handoff treats admin-only exports as accountant-required flows and hits deterministic authorization failures under live traffic.

## Approval Authority
- Mode: human
- Approver: `Anas ibn Anwar`
- Approval status: `approved`
- Basis: runtime and staging proof are green on the exact merged runtime SHA, the docs/readiness packet has been corrected and re-proved on `ef2e56284aa38dc0799144765d8b91ba610c0ab5`, and rollback rehearsal is already artifact-backed.

## Escalation Decision
- Human escalation required: no further escalation before canary entry
- Reason: explicit canary owner and rollback owner are now recorded here, together with rollback trigger thresholds, telemetry watch, and proof references.

## Canary Owner
- Lane owner: `Anas ibn Anwar`
- Lane-owner acknowledgement: `Anas ibn Anwar approved canary entry for runtime candidate 2fba82e91748f60872727c8f530369e66d95c0a8 based on the proof set referenced below.`

## Rollback Owner
- Owner: `Anas ibn Anwar`
- Rollback method: stop canary expansion, revert to the previous backend artifact, and use the proven rollback window already exercised in `artifacts/gate-release/rollback-rehearsal-evidence.json` (`162 -> 161`, then re-upgrade path validated).
- Rollback trigger:
  - actuator readiness goes `DOWN`
  - auth or tenant-isolation probe fails
  - minimum O2C probe fails
  - minimum P2P/accounting probe fails
  - sustained unexpected `5xx` or authorization anomaly on canary traffic

## Telemetry Signals
- Actuator readiness and liveness
- Application `5xx` rate on canary slice
- Auth/authorization anomalies, including unexpected `403` on intended canary flows
- O2C dispatch-confirm, invoice visibility, and dealer receipt outcomes
- P2P goods-receipt, raw-material purchase, and supplier-payment outcomes
- Database migration/connection errors and rollback readiness posture

## Minimum Canary Probes
- O2C:
  - `POST /api/v1/sales/dispatch/confirm`
  - verify downstream invoice/journal truth
  - `POST /api/v1/accounting/receipts/dealer`
- P2P/accounting:
  - `POST /api/v1/purchasing/goods-receipts`
  - `POST /api/v1/purchasing/raw-material-purchases`
  - `POST /api/v1/accounting/suppliers/payments`

## Expiry
- Valid until: `2026-03-25`
- Re-evaluate if: runtime candidate SHA changes, release proof is superseded, rollback rehearsal changes, or canary scope expands beyond the narrow probes above.

## Verification Evidence
- Exact-main proof on live runtime SHA `2fba82e91748f60872727c8f530369e66d95c0a8`:
  - `scripts/gate_core.sh` passed at `2026-03-18 17:45:54 +05:30`
  - `scripts/gate_release.sh` passed at `2026-03-18 17:47:32 +05:30`
  - `scripts/verify_local.sh` passed at `2026-03-18 17:49:01 +05:30`
- Current readiness packet proof on `ef2e56284aa38dc0799144765d8b91ba610c0ab5`:
  - `scripts/gate_core.sh` passed; see `artifacts/gate-core/gate-core-traceability.json` (`finished_at_utc=2026-03-18T13:33:28Z`)
  - `scripts/gate_release.sh` passed; see `artifacts/gate-release/release-gate-traceability.json` (`finished_at_utc=2026-03-18T13:35:03Z`)
  - standalone `scripts/verify_local.sh` passed at `2026-03-18T19:06:19+05:30` with `439` tests, `0` failures, `0` errors
- Docs/readiness closure:
  - `docs/accounting-portal-frontend-engineer-handoff.md` corrected for RBAC and legacy audit-export scope
  - `scripts/guard_accounting_portal_scope_contract.sh` hardened to fail on those contradictions
- Rollback rehearsal:
  - `artifacts/gate-release/rollback-rehearsal-evidence.json` reports `status=pass`, rollback target version `161`, and post-rehearsal upgrade max version `162`

## Artifacts
- `artifacts/gate-core/gate-core-traceability.json`
- `artifacts/gate-release/release-gate-traceability.json`
- `artifacts/gate-release/rollback-rehearsal-evidence.json`
- `artifacts/gate-core/accounting-portal-scope-guard.txt`
- `artifacts/gate-core/audit-trail-ownership-guard.txt`
