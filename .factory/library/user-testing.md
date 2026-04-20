# User Testing

Worker-facing validation contract for the accounting hard-cut mission.

Keep validation focused, backend-only, and tied to the mission's actual acceptance bar.

---

## What Counts as Validation

This mission is **backend-only**. The required proof stack is:

1. **Characterization tests** to lock current accounting behavior before hard-cuts
2. **Integration tests** for accounting truth, RLS, and cross-module financial seams
3. **curl runtime proof** against the dry-run runtime when live boundary evidence is required

Frontend/browser/UAT flows are not part of mission validation unless a later packet explicitly changes the mission contract.

## Runtime Policy

- Start runtime **only** when a test or curl proof actually needs it.
- Docs-only packets should not start runtime.
- Do not keep the backend running while doing pure analysis or documentation work.

## Approved Dry-Run Runtime Boundary

Use these validated host ports when runtime proof is necessary:

- App: `http://localhost:18081`
- Actuator: `http://localhost:19090/actuator/health`
- MailHog UI/API: `http://localhost:18025`
- Postgres: `5433`
- RabbitMQ AMQP: `15673`
- RabbitMQ management: `15674`

If older guidance mentions `8081`, `9090`, `8025`, or `5672`, treat that as stale for this mission's dry-run host proof.

## High-Signal Proof Targets

Prioritize proof that answers these questions:

- Does accounting remain the single financial truth owner?
- Are **all** accounting tables covered by fail-closed tenant isolation / RLS expectations?
- Do dealer/supplier ledger and settlement flows still reconcile correctly?
- Do period close and reconciliation behaviors stay correct after simplification?
- Do sensitive financial disclosures remain approval-gated?
- Does the paid anomaly/review feature stay manual-superadmin-toggle-first, default-off, and warn-only?
- Do docs deliverables still match the behavior being shipped?

## Minimum Evidence Rules

- **Accounting logic changed:** run characterization plus targeted integration coverage.
- **Access control / disclosure gate / runtime contract changed:** add curl runtime proof.
- **Docs-only packet in `.factory/library/**` or approved docs-only lanes:** run `bash ci/lint-knowledgebase.sh` only.

## Execution Notes

- Run Maven from `erp-domain/`.
- Keep JVM test execution serialized in a shared checkout.
- Use repo-static inspection for docs deliverables and contract alignment.
- When capturing curl proof, prefer one health/readiness probe plus one representative accounting or gate-protected endpoint rather than broad exploratory traffic.

## Worker Reminder

Mission docs are part of the deliverable. If tests pass but the architecture/environment/user-testing guidance still teaches the wrong accounting boundary, runtime ports, or approval model, validation is not complete.
