# Remediation Log

Track cleanup, duplicate-truth removals, dead-code retirement, and production-readiness fixes for this mission.

## Logging Rules

- Add entries in chronological order.
- Log packet-level cleanup only when backed by merged code or reviewable evidence.
- Describe what was cleaned, why it was risky, and what proof shows the cleanup is real.
- Do **not** describe planned cleanup as already completed.

## Entry Template

### YYYY-MM-DD — `<feature-id>`

- **Area:**
- **Risk addressed:**
- **Cleanup/remediation performed:**
- **Duplicate-truth or dead-code impact:**
- **Evidence:**
- **Follow-up:**

## 2026-03-08 — `truth-rails.docs-baseline`

- **Area:** mission documentation baseline
- **Risk addressed:** future packets needed a single approved reference for scope, truth boundaries, frontend notes, and cleanup logging; without that baseline, later workers could drift or overstate unimplemented behavior.
- **Cleanup/remediation performed:** initialized the mission remediation scaffold, definition-of-done reference, and frontend-v2 working notes.
- **Duplicate-truth or dead-code impact:** no application cleanup was performed in this docs-only packet; this entry creates the structure future packets must use when they remove duplicate-truth or dead code.
- **Evidence:** `.factory/library/erp-definition-of-done.md`, `.factory/library/frontend-v2.md`, and this file were aligned to the approved mission scope and Flyway v2-only rule.
- **Follow-up:** future truth-rails, O2C, P2P, control, and portal packets must append concrete remediation entries as code cleanup lands.

## Known Cleanup Watchlist For Upcoming Packets

- `SalesCoreEngine` — mixed workflow/accounting decisions make O2C truth boundaries hard to reason about.
- `InvoiceService` — invoice issuance still has dispatch-coupled risk to untangle.
- `GoodsReceiptService` — must stay stock-truth only and replay-safe.
- `PurchaseInvoiceEngine` — needs clean GRN linkage without overlapping AP truth.
- `InventoryAccountingEventListener` — duplicate-posting and side-channel accounting risk must be contained early.
