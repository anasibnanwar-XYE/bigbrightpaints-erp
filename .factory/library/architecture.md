# Architecture

Worker-facing architecture guidance for the accounting hard-cut mission.

This file is not a public docs entrypoint. It exists to keep workers aligned on the mission's actual truth boundary and the constraints that cannot drift during cleanup.

---

## Mission Truth

- **Accounting is the top-level truth boundary.** Sales, inventory, invoice, purchasing, and reporting may initiate or consume business flows, but accounting owns journals, ledgers, settlements, period state, reconciliation, and financial disclosure policy.
- **There must be one financial owner per flow.** Do not leave duplicate posting paths, shadow ledgers, or report-side financial truth reconstruction behind.
- **Docs are deliverables in this mission.** Architecture guidance, backend contract docs, and worker-facing library docs are part of done, not follow-up polish.

## Scope Boundaries

- In scope: accounting plus direct financial seams in inventory, invoice, purchasing, sales, and reports.
- Out of scope: HR/payroll feature work unless a shared guard, test, or doc surface must stay consistent.
- Preserve frontend-usable API contract shapes where they matter, but do **not** preserve bad internal architecture for its own sake.

## Implementation Mode

- This mission is **characterization-first and contract-preserving at public edges**. Workers may hard-cut internals aggressively, but named public surfaces must keep their documented behavior unless the packet deliberately updates docs/contracts in the same change.
- Treat the validation contract as the authoritative checklist for worker-visible accounting/reporting/workflow behavior. If a refactor changes a path, role rule, response-shape expectation, or workflow step named there, the packet is incomplete until the contract-facing docs and proofs move with it.
- Prefer consolidation into one accounting owner per flow over compatibility wrappers. Delete duplicate or shadow financial paths once characterization coverage and replacement ownership are in place.

## Non-Negotiable Invariants

- **RLS rollout applies to all accounting tables.** This is not optional and not partial.
- **Tenant isolation must hold at both service and database layers.** Missing or wrong tenant context must fail closed.
- **Sensitive financial disclosures require approval gates.** Full ledgers, transaction-heavy reports, cashflow-style disclosures, and similar high-sensitivity outputs cannot become open reads by accident.
- **Anomaly/review is a paid feature.** Rollout order is manual superadmin toggle first, default off, warn-only. It may surface guidance or queue review, but it must not hard-block accounting writes.
- **Accounting auditability must remain explicit.** Financial events need traceable workflow linkage and reviewable reasoning.

## Canonical Accounting Seams

Workers should assume these are the main hard-cut seams:

- journal creation, numbering, reversal, and posting ownership
- chart of accounts and default-account readiness
- dealer/supplier settlements and ledger truth
- period close, reopen, and reconciliation flows
- inventory-accounting and invoice/purchasing-accounting bridges
- reporting reads that expose accounting truth

Worker-facing expectations on those seams:

- **Period close is maker-checker, not a convenience toggle.** The canonical close path is request-close → approve-close, with distinct actors and explicit notes/reasoning; do not reintroduce a direct close shortcut.
- **Draft support is opt-in and side-effect free until promotion.** If a workflow is declared draft-capable, save/resume must preserve a stable draft artifact and must not post journals, settle balances, or mutate period state before explicit submit/post.
- **Partner truth must stay filterable and reconcilable.** Dealer/supplier journal, statement, settlement, and aging surfaces must remain aligned so workers do not split partner truth across disconnected implementations.
- **Reports are read models over accounting truth, not alternate ledgers.** Closed/open branching, snapshot usage, export approval, requester-owned downloads, and statement/PDF exceptions must live at the reporting/read boundary without creating a second accounting owner.

## Cross-Module Dependency Map

The expected dependency direction is:

- `sales -> accounting` for receivables, revenue, credits, and settlement truth
- `inventory -> accounting` for valuation, opening stock, adjustments, and stock-linked financial effects
- `invoice -> accounting` for invoice posting and downstream ledger impacts
- `purchasing -> accounting` for payables, supplier settlement, and purchase-return truth
- `reports -> accounting` for financial read models and disclosure policy
- `admin/company/auth -> accounting` only for access control, company binding, approvals, and runtime admission

If a cleanup changes accounting ownership, workers must re-check the dependent module flow in the same packet.

## Worker Implications

- Prefer deletion and consolidation over new wrapper layers.
- Do not let reports become an alternate accounting owner.
- Do not weaken approval gates just to simplify a read path.
- Keep export/report approvals on the canonical admin approval surfaces, and preserve requester-owned export downloads even after approval.
- Do not accidentally route statement/aging PDF downloads through the export-approval gate; those are separate audited surfaces.
- Preserve role boundaries at public edges: accounting/report surfaces stay on the documented admin/accounting policies, with audit/approval exceptions only where the contract says so.
- Do not treat anomaly/review as a blocking control path; it is warning/review assistance only, behind a superadmin-controlled entitlement gate, default off.
- If a packet changes financial truth, RLS behavior, sensitive-disclosure access, or a named workflow shortcut, docs/tests/contracts must move with it because mission docs are a shipped output.
