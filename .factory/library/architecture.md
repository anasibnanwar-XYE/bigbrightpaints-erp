# Architecture

Worker-facing architecture guidance for the accounting hard-cut mission.

This file exists to keep workers aligned on the surviving business truth, ownership boundaries, and deletion intent during the hard-cut.

---

## Mission Truth

- **Accounting is the financial truth boundary.** Sales, purchasing, inventory, factory, dealer master, and reports may initiate or consume flows, but accounting owns journal truth, settlement truth, period state, reconciliation truth, and sensitive financial disclosure policy.
- **`AccountingFacade` stays.** The mission does not remove the facade as the external accounting boundary; it hard-cuts the internals behind it.
- **Flows must be explainable as business flows.** The target state is not “random posting paths that happen to balance”; it is a connected ERP flow model that accountants and non-accountants can follow.
- **Delete duplicate owners.** If a packet lands a canonical owner, remove wrapper/delegate/side-channel/report-reconstruction paths that would leave two truths alive.

## Surviving Internal Layer Model

The target internal layering behind `AccountingFacade` is:
1. **Policy layer** — validation, role/policy checks, tenant/period/disclosure rules
2. **Orchestration layer** — connected business-flow coordination across party, inventory, purchasing, dispatch, period, and report corridors
3. **Posting layer** — canonical journal creation, reversal, numbering, and replay-safe ledger mutation
4. **Account-resolution layer** — COA, default accounts, readiness, and semantic account-role resolution
5. **Reporting layer** — read models over centralized accounting truth, approval-gated disclosures, and operator guidance

Workers should use these layers to decide what survives and what gets deleted.

## Party / Payment Truth

- **Party-first, payment-first, allocation-next, accounting-derived** is the target model for receipts and settlements.
- Persist a distinct payment truth first.
- Keep allocation rows explicit and separate from both payment truth and journal lines.
- Derive balances and summaries from invoice/purchase, payment, and allocation truth.
- Keep current public receipt/payment/settlement routes stable while internal ownership is unified.

## Shared Dealer Master

- Dealer identity is one shared ERP truth across admin, sales, accounting, portal-finance, and dealer self-service.
- Dealer creation from tenant-admin role assignment or sales onboarding must converge to one preserved dealer master.
- Dealer accounting wiring must exist on the surviving dealer truth; accounting should not need a second hidden dealer/account record.
- Dealers are never hard-deleted in this mission. Hold/suspend/block states may exist, but finance visibility and history remain.

## Central Stock / Catalog Master

- The catalog/master-data track is the **second track** after accounting truth is stable.
- The target model is tenant-scoped **Brand -> Parent Product -> Variant**, with variant/SKU as the stock truth.
- Raw materials and packaging stock items also belong to the central master.
- Catalog readiness may succeed before full accounting readiness, but accounting-owned actions must fail closed until valuation/COGS/revenue/tax/default-account blockers are cleared.
- The central master remains ERP-native. Do not create a second public catalog system.

## Non-Negotiable Invariants

- **All accounting truth tables get database-enforced tenant isolation.**
- **Application-surface tenant isolation must also fail closed.** Dealer-master, catalog, journal, settlement, statement, aging, report, and pricing reads must stay tenant-scoped.
- **Sensitive disclosures stay approval-gated.**
- **Anomaly/review stays default-off, superadmin-controlled, warn-only.**
- **Accounting auditability stays explicit.** Financial events need workflow linkage and reviewable reasoning.
- **Money math must reconcile exactly under the system’s rounding/scale rules.**

## Canonical Hard-Cut Seams

Workers should assume these are the main seams:
- journal posting, reversal, numbering, and audit visibility
- chart of accounts, default accounts, and readiness/config health
- payment-event, allocation, receipt, and settlement orchestration
- dealer shared-master preservation and accounting-facing party visibility
- period-close, reopen, month-end checklist, and reconciliation ownership
- inventory/opening-stock/costing/event bridges into accounting truth
- dispatch / invoice / purchase / return / note accounting linkage
- report read models, disclosure gates, workflow shortcuts, and review toggle surfaces
- catalog central master, bulk variant generation, and canonical SKU reuse

## Dependency Direction

Expected dependency direction is:
- `sales -> accounting`
- `purchasing -> accounting`
- `inventory/factory -> accounting`
- `dealer master -> accounting` for finance visibility and account wiring
- `reports -> accounting`
- `catalog central master -> accounting readiness` (after accounting core is stable)
- `admin/company/auth -> accounting` only for access control, approvals, and tenant binding

If a packet changes one of these seams, re-check the downstream corridors in the same packet.

## Worker Implications

- Prefer consolidation and deletion over wrappers.
- Do not reintroduce a second public accounting or catalog host.
- Do not let reports or portal reads become alternate truth owners.
- Do not hide dealer/accounting convergence behind manual sync assumptions.
- Do not preserve obsolete shadow records just because multiple modules currently disagree.
- When a packet changes truth, tenant isolation, or public route expectations, update the shared-state docs and contract-facing artifacts with it.
