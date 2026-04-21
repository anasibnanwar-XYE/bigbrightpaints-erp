# User Testing

Worker-facing validation guidance for the accounting hard-cut mission.

Keep validation backend-first, contract-driven, and focused on the surviving ERP truth.

---

## What Counts as Validation

This mission is backend-first. The required proof stack is:
1. **Characterization tests** for current accounting seams before hard-cuts
2. **Integration / contract tests** for centralized accounting truth, dealer/shared-master convergence, period/reconciliation, inventory/cross-module posting, and disclosure policy
3. **curl runtime proof** on the approved local boundary when live evidence is required
4. **repo-static / OpenAPI / DB inspection** when the validation contract explicitly requires those proof types

Frontend/browser validation is not part of this mission unless a later packet explicitly changes that contract.

## Runtime Policy

- Start runtime only when a test or curl proof actually needs it.
- Docs-only packets should not start runtime.
- Do not leave the backend running after proof-only work.

## Approved Local Validation Boundary

Use these host ports when runtime proof is necessary:
- App: `http://localhost:18081`
- Actuator: `http://localhost:19090/actuator/health`
- MailHog API/UI: `http://localhost:18025`
- Postgres: `5433`
- RabbitMQ AMQP: `15673`
- RabbitMQ management: `15674`

## High-Signal Proof Targets

Prioritize proofs that answer these questions:
- Does `AccountingFacade` remain the stable external boundary while one clear internal owner survives per touched flow?
- Are payment events created before journals on the touched receipt/payment paths?
- Do allocation rows remain explicit and distinct from payment truth and journal lines?
- Do party summaries reconcile to document/payment/allocation truth?
- Does dealer creation converge to one shared dealer master across admin, sales, accounting, and dealer-facing finance reads?
- Do hold/suspend flows preserve dealer visibility and accounting history without a hard-delete path?
- Do tenant isolation and approval gates stay fail-closed at both DB and application surfaces?
- After the catalog milestone, do tenant-scoped brand/product/variant reads and canonical SKU reuse stay ERP-native and cross-module consistent?

## Sequence-Sensitive Proof Targets

The mission is intentionally ordered. Validators should respect that:
- boundary / tenant-safety proofs come before deeper accounting hard-cuts
- accounting core and payment/settlement proofs come before catalog-central-master proofs
- catalog proofs should assume accounting-readiness blockers remain valid until explicitly cleared
- docs proofs should happen only after the code paths they describe are stable

## Minimum Evidence Rules

- **Accounting truth changed:** run characterization or targeted integration proof plus compile.
- **Dealer/shared-master truth changed:** prove admin/sales/accounting/dealer-facing reads resolve the same dealer identity.
- **Tenant isolation or disclosure policy changed:** add targeted security proof and curl or DB evidence per the contract.
- **Catalog central master changed:** prove tenant scoping, readiness blockers, and canonical SKU reuse where applicable.
- **Docs-only packet in approved docs lanes or `.factory/library/**`:** run `bash ci/lint-knowledgebase.sh` only unless the feature explicitly requires stronger contract proof.

## Execution Notes

- Run Maven from `erp-domain/`.
- Keep JVM-heavy validation serialized in the shared checkout.
- Prefer the exact command aliases from `.factory/services.yaml` over ad-hoc broad test runs.
- When capturing curl proof, use one health/readiness check plus the smallest set of representative business-route probes needed to satisfy the contract.
- Treat runtime proof as not-ready until the reset harness has verified seeded actors and fixture presence, including the validation super-admin's required auth scope, rather than merely printing guidance.
- Treat RLS proof as incomplete until live app datasource sessions demonstrate the tenant/company context binding that the PostgreSQL policy depends on.

## Validation Concurrency

- **api-and-db:** max concurrent validators = 1
- Reasoning: this milestone uses one shared compose-backed runtime, seeded tenants that can be mutated by validation probes, direct DB inspection against the same Postgres instance, and JVM-heavy regression suites in the shared checkout. Keep this surface serialized to avoid cross-validator interference and noisy state races.
- Current machine assessment (2026-04-21): 25 GiB RAM with an active local VM and multiple `droid` processes already consuming meaningful memory/CPU. Treat serialized validation as the safe ceiling even though the runtime itself is healthy.

## Flow Validator Guidance: api-and-db

- Stay on the approved local boundary only: app `http://localhost:18081`, actuator `http://localhost:19090/actuator/health`, Postgres `127.0.0.1:5433`, MailHog `http://localhost:18025`.
- Use the seeded validation runtime created by `scripts/reset_final_validation_runtime.sh`; do not restart services from the subagent.
- If the delegated `user-testing-flow-validator` Task helper exits without producing a report, fall back to direct in-session validation, record the Task failure as a tooling friction, and continue with the same isolation guidance rather than burning the round.
- Allowed seeded actors for this milestone group are:
  - `validation.admin@example.com` / `QaFixture!2026` / company `MOCK`
  - `validation.rival.admin@example.com` / `QaFixture!2026` / company `RIVAL`
  - `validation.superadmin@example.com` / `QaFixture!2026` / platform scope only when a control-plane comparison is explicitly needed
- Use `X-Company-Code` headers that match the login scope on every authenticated API probe.
- Prefer read probes first. Only use write probes when they are fail-closed by construction and do not require cleanup (for example, foreign-tenant ids that should be rejected before mutation).
- For `VAL-CROSS-023`, capture representative cross-tenant evidence across dealer master, catalog/stock visibility, accounting journals, and dealer-finance statement/aging/ledger surfaces. The goal is to prove the authenticated tenant cannot read or mutate rival-tenant truth.
- For `VAL-CROSS-027`, pair repo-static evidence (the canonical accounting RLS table list and policies) with live DB/app-session evidence showing `app.current_company_id` binding, zero visibility without valid context, own-tenant visibility with valid context, and blocked foreign-tenant writes.
- DB inspection may use `docker exec -i erp_db psql ...` against the local compose database only. Do not alter schema, disable RLS, or grant broader privileges.
- Keep all outputs inside `.factory/validation/accounting-boundary-and-tenant-safety/user-testing/flows/` and the mission evidence directory assigned by the parent validator.

## Worker Reminder

Validation is not complete if tests pass but the shared-state docs still teach the wrong accounting boundary, dealer/shared-master model, runtime ports, approval model, or catalog sequencing.
