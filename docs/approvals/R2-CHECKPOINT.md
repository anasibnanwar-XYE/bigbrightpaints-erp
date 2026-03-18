# R2 Checkpoint

## Scope
- Feature: `pr119-canary-readiness`
- Runtime candidate SHA: `2fba82e91748f60872727c8f530369e66d95c0a8`
- Readiness packet SHA: `80aab18d04c65c4195868d2c0f42c96fc0d3eb7c`
- Branch: `feature/pr119-canary-readiness-clean`
- Why this is R2: this packet closes the post-merge deploy-readiness gap on live `main`, fixes accounting-portal handoff RBAC/readiness drift, and records the canary ownership/rollback decision on the exact proved runtime SHA.

## Risk Trigger
- Triggered by post-merge deploy-readiness closure on live `main`, where incorrect portal handoff scope could have exposed accountant or sales users to deterministic `403` failures on admin-only exports during canary.
- Contract surfaces affected: accounting portal route ownership, portal/frontend handoff RBAC truth, audit-trail route guidance, release gate documentation, and canary operator ownership.
- Failure mode if wrong: canary proceeds without a named owner/rollback decision, or frontend/operator handoff treats admin-only exports as accountant-required flows and hits deterministic authorization failures under live traffic.

## Approval Authority
- Mode: human
- Approver: `Anas ibn Anwar`
- Approval status: `approved for merge handoff; canary entry remains pending canonical-lane merge plus post-merge gate re-proof`
- Basis: runtime and staging proof are green on the exact merged runtime SHA, the docs/readiness packet is corrected through `80aab18d04c65c4195868d2c0f42c96fc0d3eb7c`, and canary may proceed only after the merged canonical-lane head re-proves the release gates listed below.

## Escalation Decision
- Human escalation required: no additional human approval after the packet is merged to the canonical lane and the post-merge gate sequence re-proves green on that merged head
- Reason: explicit canary owner and rollback owner are recorded here, but this file does not authorize canary from a local-only readiness branch. Canary remains blocked until merge/publication and fresh gate proof on the merged head.

## Canary Owner
- Lane owner: `Anas ibn Anwar`
- Lane-owner acknowledgement: `Anas ibn Anwar approved canary entry for runtime candidate 2fba82e91748f60872727c8f530369e66d95c0a8 only after this packet is merged to the canonical lane and scripts/gate_core.sh, scripts/gate_release.sh, and scripts/verify_local.sh are re-proved green on that merged head.`

## Rollback Owner
- Owner: `Anas ibn Anwar`
- Rollback method: stop canary expansion, revert to the previous backend artifact, then rerun `scripts/gate_release.sh` and `scripts/verify_local.sh` on the rollback target before resuming traffic expansion.
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
- Re-evaluate if: runtime candidate SHA changes, release proof is superseded, rollback procedure changes, or canary scope expands beyond the narrow probes above.

## Verification Evidence
- Exact-main proof on live runtime SHA `2fba82e91748f60872727c8f530369e66d95c0a8`:
  - `scripts/gate_core.sh` passed at `2026-03-18 17:45:54 +05:30`
  - `scripts/gate_release.sh` passed at `2026-03-18 17:47:32 +05:30`
  - `scripts/verify_local.sh` passed at `2026-03-18 17:49:01 +05:30`
- Current readiness packet proof inherited by `80aab18d04c65c4195868d2c0f42c96fc0d3eb7c`:
  - `scripts/gate_core.sh` passed on packet ancestor `ef2e56284aa38dc0799144765d8b91ba610c0ab5` at `2026-03-18T13:33:28Z`
  - `scripts/gate_release.sh` passed on packet ancestor `ef2e56284aa38dc0799144765d8b91ba610c0ab5` at `2026-03-18T13:35:03Z`
  - standalone `scripts/verify_local.sh` passed on packet ancestor `ef2e56284aa38dc0799144765d8b91ba610c0ab5` at `2026-03-18T19:06:19+05:30` with `439` tests, `0` failures, `0` errors
- Docs/readiness closure:
  - `docs/accounting-portal-frontend-engineer-handoff.md` corrected for RBAC and legacy audit-export scope
  - `scripts/guard_accounting_portal_scope_contract.sh` hardened to fail on those contradictions
- Reviewable replay contract:
  - reviewers must validate this packet from a clean checkout with rerunnable commands, not repo-local `artifacts/`
  - required replay commands: `scripts/gate_core.sh`, `scripts/gate_release.sh`, `scripts/verify_local.sh`, `scripts/guard_accounting_portal_scope_contract.sh`, `scripts/guard_audit_trail_ownership_contract.sh`
  - canary remains blocked until those commands are re-proved on the merged canonical-lane head for the runtime candidate above

## Reviewer Notes
- Local run outputs under `artifacts/` are non-canonical and intentionally excluded from this readiness packet.
- Merge/readiness review must rely on committed docs plus the rerunnable commands above.
