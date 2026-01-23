# Continuous End-to-End Hardening Plan (All Modules)

## Goal
Deliver a unified, correct, and secure ERP backend by exhaustively validating every module, every cross-module flow, and every core algorithm. The process is continuous: scan → test → fix → verify → commit → repeat.

## Operating rules
- After each epic or milestone, commit with clear scope and verification notes.
- Never skip cross-module checks when modifying any module.
- Prefer smallest-possible fixes with explicit tests for regressions.
- Keep multi-company isolation and RBAC boundaries intact at all times.
- Record stack traces and failing tests before any fix.
- All changes must preserve accounting correctness and auditability.

## Global invariants (must hold everywhere)
- Double-entry integrity: every journal entry balances in base currency with defined FX rounding rules.
- Idempotency: repeated calls with same idempotency key/reference never double-post.
- Multi-company isolation: no cross-company data leakage in queries, caches, or joins.
- Period control: postings respect period lock settings and override rules.
- Status-driven flows: state transitions are valid and deterministic.
- Reference number sequencing: unique, stable, and traceable across modules.
- Role boundaries: read/write access consistent with policy; accounting read-only where specified.

## Epic backlog (end-to-end, exhaustive)

### Epic A — Auth + RBAC correctness (auth, rbac)
- Verify login flows, JWT issuance/expiry, refresh/invalidations.
- Ensure role claims propagate to controllers/services.
- Validate permission annotations on every endpoint.
- Confirm dealer portal isolation (dealer-only access).
- Add tests for forbidden roles on sensitive endpoints.
- Milestone commit: `audit: auth-rbac invariants`

### Epic B — Company + configuration integrity (company, admin)
- Validate company context resolution across services.
- Confirm default accounts configuration is required where used.
- Verify system settings toggle behavior (period lock, GST config, etc.).
- Ensure admin endpoints are not exposed to non-admin roles.
- Milestone commit: `audit: company-admin settings`

### Epic C — Accounting core algorithm audit (accounting)
- Journal entry balancing, FX handling, rounding tolerance.
- Reference uniqueness and duplicate handling.
- Period close/lock/reopen behavior and reversals.
- Reconciliation checks: GL vs subledger totals.
- Statement/aging algorithms (buckets, running balance).
- Tax (GST) computations and account mapping.
- Accruals, write-offs, credit/debit notes logic.
- Milestone commit: `audit: accounting core algorithms`

### Epic D — Sales workflow integrity (sales, invoice)
- Dealer lifecycle, credit limits, status changes.
- Sales order creation, updates, cancellation, and confirmations.
- Dispatch confirmation and invoice generation.
- Revenue recognition, discounts, taxes, and AR postings.
- Sales returns and reversal logic.
- Sales order dealer filtering and list pagination correctness.
- Milestone commit: `audit: sales workflow`

### Epic E — Purchasing workflow integrity (purchasing)
- Supplier lifecycle, credit limits, status changes.
- Purchase order creation, goods receipt, raw material purchases.
- Supplier filtering on lists and multi-company scoping.
- AP postings for purchases and payments.
- Purchase returns and reversal logic.
- Milestone commit: `audit: purchasing workflow`

### Epic F — Inventory integrity (inventory, production, factory)
- Raw material and finished goods valuation.
- Inventory movement journaling (COGS, inventory accounts).
- Batch tracking, FIFO/LIFO/avg logic (if configured).
- Production consumption and finished goods output.
- Ensure stock movements are idempotent.
- Milestone commit: `audit: inventory-production linkage`

### Epic G — Payroll + HR integration (hr, accounting)
- Payroll calculations, deductions, and postings.
- Employee lifecycle and payroll approvals.
- Ensure payroll entries respect period locks.
- Milestone commit: `audit: payroll-hr linkage`

### Epic H — Portal + external access isolation (portal)
- Dealer portal access only to dealer-owned records.
- Validate portal endpoints for leakage.
- Ensure portal actions don’t bypass accounting controls.
- Milestone commit: `audit: portal isolation`

### Epic I — Reporting correctness (reports)
- Financial statements rollups match GL balances.
- Trial balance correctness by period.
- Aging vs statement vs subledger alignment.
- Report performance and caching correctness.
- Milestone commit: `audit: reporting accuracy`

### Epic J — Data quality + migrations (admin, accounting, data)
- Seed data correctness and consistency.
- Migrations are idempotent and safe across environments.
- Existing records remain compatible with new DTO fields.
- Milestone commit: `chore: data compatibility`

### Epic K — Performance + query shaping (all modules)
- Validate pagination ordering with filters.
- Detect N+1 patterns; add EntityGraphs as needed.
- Validate index usage for hot tables.
- Milestone commit: `perf: query tuning`

### Epic L — API contract verification (openapi)
- Sync `openapi.json` with new params/fields.
- Ensure backward compatibility where expected.
- Add contract tests for critical endpoints.
- Milestone commit: `chore: openapi sync`

## Cross-module flow validation (end-to-end)

### O2C (Order-to-Cash)
- Sales order → dispatch → invoice → AR posting → receipt/settlement.
- Validate revenue + tax + discount entries match invoice totals.
- Confirm dealer balance updates and aging buckets.
- Verify statement running balance aligns with GL and subledger.

### P2P (Procure-to-Pay)
- PO → goods receipt → raw material purchase → AP posting → supplier payment.
- Validate inventory valuation + AP postings align.
- Confirm supplier balance updates and aging buckets.

### Production (Plan-to-Produce)
- BOM → production order → raw material consumption → finished goods output.
- Validate inventory movement entries and COGS postings.

### R2R (Record-to-Report)
- Period close flows + reversals + reopen.
- Reconcile GL vs subledgers vs statements.

### Returns + Reversals
- Sales returns, purchase returns, credit/debit notes.
- Validate references and reversal entries are consistent.

## Algorithm & business-logic checkpoints (always-on)
- Pricing and discount allocation (line vs order, rounding rules).
- Tax calculation (GST inclusive/exclusive, rate caps).
- FX conversion and cash settlement math.
- Aging bucket boundaries and due-date logic.
- Ledger running balance correctness.
- Idempotency key + reference number generation.
- Period lock validation (including overrides).

## Module-by-module deep checklist (every endpoint)

### accounting
- `journal-entries` list filters: dealer/supplier mutual exclusivity.
- `journal-entries` create: validates lines, accounts, period lock.
- `receipts`/`settlements`: cash calculation and allocation correctness.
- `statements`/`aging`: ledger math, bucket logic, PDF output.
- `periods` close/lock/reopen: entries and reversal handling.
- `gst` return accuracy and account linkage.

### sales
- `dealers` CRUD: RBAC + receivable account mapping exposure.
- `orders`: dealer filter, status transitions, idempotency.
- `dispatch` confirm: invoice numbering, postings, inventory updates.
- `promotions`/`targets`/`credit-requests`: RBAC + integrity.

### invoice
- Invoice generation and numbering.
- Balance updates and AR linkages.
- Refunds/credit notes integration.

### purchasing
- `purchase-orders` list filter by supplier.
- `goods-receipts` list filter by supplier.
- `raw-material-purchases` list filter by supplier.
- Purchase returns and AP reconciliation.

### inventory
- Raw material movements and valuation.
- Finished goods valuation and COGS postings.
- Batch updates and movement integrity.

### production / factory
- Production order status transitions.
- Consumption vs output quantity balancing.
- Work-in-progress account impacts.

### hr / payroll
- Payroll posting entry accuracy.
- Employee status and payroll access checks.

### auth / rbac / portal
- Access boundaries for dealer users.
- Admin-only endpoints protected.
- Token scopes and expiry handling.

### reports
- Trial balance and P&L totals reconcile with GL.
- Aging and statements align with subledgers.

## Testing matrix (repeat for each epic)
- Unit tests for algorithmic changes.
- Integration tests for flow integrity.
- Regression tests for fixed bugs.
- Contract tests for API params/payloads.
- Targeted performance checks for list endpoints.

## Never-ending loop (operational)
1. Scan: find new or adjacent logic paths.
2. Test: run focused tests or small integration flows.
3. Fix: implement minimal corrections + tests.
4. Verify: re-run tests, reconcile with accounting checklist.
5. Commit: milestone commit with verification notes.
6. Repeat indefinitely.

## Milestone commit rule
- After each epic or milestone, commit with a clear prefix and short verification notes.
- No milestone proceeds without a commit and updated notes.

## Execution log (2026-01-24)
- Async full suite: `mvn -B -ntp verify` → BUILD SUCCESS (268 run, 0 failures, 0 errors, 4 skipped, 08:54).
- Manual API smoke (seed profile): created dealer/supplier, verified dealer list includes receivable account fields, posted dealer/supplier journal entries, validated journal entry filters, and confirmed sales/purchasing list filters accept dealerId/supplierId.
