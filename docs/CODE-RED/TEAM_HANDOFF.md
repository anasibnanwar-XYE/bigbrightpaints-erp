# CODE-RED Team Handoff (Execution Order + Next P0 Work)

Last updated: 2026-02-03

Purpose: one place for the team to see **what’s already hardened**, what is still **P0**, and the exact **execution order**
to reach a safe, enterprise-grade deploy (no “mind fookin” scattered fixes).

Start here:
- System map + gates: `docs/CODE-RED/START_HERE.md`
- Program plan (detailed): `docs/CODE-RED/plan-v2.md`
- P0 blockers list: `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`
- De-dup backlog: `docs/CODE-RED/DEDUPLICATION_BACKLOG.md`
- Orchestrator strong-arm contract: `docs/CODE-RED/ORCHESTRATOR_STRONG_ARM_SPEC.md`
- Cross-cutting “code smell → centralization” plan: `docs/CODE-RED/CROSS_CUTTING_CENTRALIZATION.md`
- Decision log (locking behavior / truth boundaries): `docs/CODE-RED/decision-log.md`
- Evidence maps: `docs/cross-module-trace-map.md`, `docs/idempotency-inventory.md`

---

## 1) What’s Already Implemented (Baseline We Build On)

Do NOT redo these; treat as the CODE-RED baseline.

Orchestrator “strong arm” (no parallel truth)
- Orchestrator dispatch endpoints (`/api/v1/orchestrator/dispatch*`) are hard deprecated (410 + canonicalPath).
- Orchestrator fulfillment rejects `SHIPPED/DISPATCHED` (fail closed) and points to `POST /api/v1/sales/dispatch/confirm`.
- Orchestrator write commands require `Idempotency-Key` and reserve `(company, commandName, key)` in `orchestrator_commands`.
- Orchestrator outbox is company-scoped (`orchestrator_outbox.company_id`).

Payroll payment correctness (no double-expense)
- Payroll payment is liability clearing: **Dr SALARY-PAYABLE / Cr CASH** (not another expense).
- Payment journal is stored separately (`payroll_runs.payment_journal_entry_id`).
- HR mark-as-paid fails closed unless a payment journal exists.

Performance quick wins (safe, semantics-preserving)
- Inventory adjustment locks finished goods in one query (deterministic order) and avoids redundant per-line saves.
- Payroll auto-calculation prefetches attendance and bulk-inserts payroll run lines.

---

## 2) Execution Order (P0 → Ship)

Rule: finish each block with (1) tests, (2) doc updates, (3) any needed predeploy scans. No “drive-by” fixes.

### Block A (P0) — Tenant Isolation + Public Attack Surface Hardening

Why this is P0
- Multi-tenant privacy is your differentiator; any cross-company enumeration or public admin surfaces will kill trust.

Work to implement (see `docs/CODE-RED/plan-v2.md` and `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`)
- Canonical identity vocabulary (remove “companyId means companyCode” confusion):
  - Adopt `docs/CODE-RED/IDENTITY_AND_NAMING.md` and align code + OpenAPI accordingly.
  - Standardize on `companyCode` for tenant context (string) and reserve `companyId` for numeric DB ids.
  - Deprecate `X-Company-Id` → `X-Company-Code` (keep backwards compatible parsing during deprecation window).
- Swagger/OpenAPI must be intentional in prod:
  - disable `/swagger-ui/**` + `/v3/**` in prod OR move to secured management port (auth required).
- Actuator must be intentional in prod:
  - expose minimal safe endpoints (prefer `health` + `info` only) and avoid leaking details.
- CORS must be safe-by-default:
  - reject wildcard origins (`*`) when credentials are enabled; require explicit HTTPS origins.
  - validate admin-updated origins; block invalid schemes/hosts.
- Defense-in-depth membership enforcement:
  - “multi-company switch” and any company update/delete must enforce membership in the service layer too (not only controllers).
- System settings scoping clarity:
  - system settings are global today; confirm and document whether the product is currently single-tenant-by-install or true multi-tenant.
  - if multi-tenant, plan the migration to company-scoped settings (or hard-scope admin endpoints to “system super-admin” only).
- Observability identifiers (auditability is part of enterprise privacy):
  - Adopt `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md` so privileged writes are traceable end-to-end.
  - Standardize requestId/traceId propagation (ingress → audit → outbox → errors).

Definition of done
- Security tests exist and pass (see plan-v2 test names like `CR_SEC_*`).
- GO/NO-GO checklist includes the new NO-GO conditions (actuator/docs/cors).
- Docs updated (decision log if behavior is locked).

### Block B (P0) — Sales Dispatch-Truth: Eliminate AR/COGS Double-Posting

Why this is P0
- Double-posting is a market-ending accounting bug. Dispatch confirmation is the financial truth; references must converge.

Work to implement (see `docs/CODE-RED/plan-v2.md` EPIC 01)
- Make slip lookup deterministic + read-only (no “most recent slip”, no GET side effects).
- AR/Revenue dedupe must work across reference namespaces:
  - dedupe across canonical `INV-<orderNumber>` and any invoice-number reference.
  - enforce mismatch-safe idempotency (conflict on payload mismatch; no silent reuse).
- COGS must be slip-scoped single truth:
  - canonical COGS reference is `COGS-<slipNumber>`.
  - remove/prod-gate any order-level helper that can create `COGS-<orderNumber>`, or force it to reuse slip-based journals.
- Link movements to COGS by slip boundary (not by order id).

Definition of done
- New tests exist and pass (`CR_O2C_ArJournal_DedupesAcrossCanonicalAndInvoiceReferencesIT`, `CR_O2C_CogsJournal_DedupesAcrossSlipAndOrderReferencesIT`, etc.).
- Predeploy scans detect unlinked/missing artifacts (dispatched slips without invoice/journals).
- `docs/cross-module-trace-map.md` and `docs/idempotency-inventory.md` updated if reference rules change.

### Block C (P0) — Orchestrator: Defense-in-Depth + Auditability (Strong Arm, Not Second Brain)

Why this is P0
- Orchestrator is your differentiator; it must be “enterprise automation with proof”, not “another way to mutate state”.

Work to implement (see `docs/CODE-RED/ORCHESTRATOR_STRONG_ARM_SPEC.md`)
- Enforce feature flags in service layer too (`CommandDispatcher` / `IntegrationCoordinator`).
- Ensure denied attempts are trace/audit-visible (who/when/company/command), with **zero business side effects**.
- Fulfillment updates must route through canonical state-machine guards (no free-form status setters).
- Mismatch-safe idempotency:
  - reference-based “return existing” must validate amount/accounts or fail closed with 409.
- Remove/rename “false security” policy hooks:
  - if a class is named like it enforces policy, it must enforce real checks or be removed to avoid engineer confusion.
- Observability identifiers contract:
  - adopt `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md` so every privileged write is traceable end-to-end
  - standardize `requestId/traceId/correlationId/idempotencyKey/referenceNumber` propagation to outbox + audit + errors

Definition of done
- P0 orchestrator tests exist and pass (`CR_ORCH_FlagOff_FailsClosed_EvenInternalInvocationIT`, etc.).
- Orchestrator contract doc updated with the tests + invariants (kept in sync).

### Block D (P0) — Dealer AR Receipts/Settlements Exactly-Once

Why this is P0
- Receipts/settlements are high-frequency; concurrency duplicates are guaranteed in the wild.

Work to implement (see `docs/CODE-RED/plan-v2.md` EPIC 01B)
- Reserve-first idempotency for dealer receipts/settlements (exactly-once journals + allocations).
- Add DB uniqueness constraints for allocations (forward-only migration + deterministic backfill/dedupe plan).

Definition of done
- Idempotency + concurrency tests exist and pass.
- Predeploy scan(s) detect duplicate allocations or inconsistent settlement rows.

### Block E (P0/P1) — Manufacturing/Inventory Write Idempotency

Why this matters
- Any retryable manufacturing endpoint can double-consume materials or double-post journals.

Work (see plan-v2 EPIC 06 + EPIC 02)
- Packing, production logs, bulk pack: define required idempotency key scopes + dedupe rules.
- Ensure posting flows go through `AccountingFacade` and are mismatch-safe.

Definition of done
- Retry/concurrency tests cover packing/production critical endpoints.
- Any unsafe legacy endpoint is prod-gated.

---

## 3) Rules For Every PR (So We Stay Centralized)

For each “fix” PR:
1) Pick the epic/task in `docs/CODE-RED/plan-v2.md` and link it in the PR description.
2) Update the evidence docs if semantics changed:
   - `docs/cross-module-trace-map.md`, `docs/idempotency-inventory.md`
3) Update the program docs if a blocker was closed:
   - `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`, `docs/CODE-RED/DEDUPLICATION_BACKLOG.md`, `docs/CODE-RED/decision-log.md`
4) Add/extend tests first, then code.
5) Run the gate: `bash scripts/verify_local.sh`

---

## 4) Deploy Gate (Non-Negotiable)

- Local: `bash scripts/verify_local.sh`
- Release-commit strict mode: `FAIL_ON_FINDINGS=true bash scripts/verify_local.sh`
- Staging: `scripts/db_predeploy_scans.sql` must return zero rows.
