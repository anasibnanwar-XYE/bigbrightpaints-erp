# Module Flow Map

## ADMIN
- Auth: `/api/v1/auth/*` -> JWT issuance/refresh -> method-level authorization.
- Users & roles: `/api/v1/admin/*` + `/api/v1/admin/roles/{roleKey}` -> RBAC enforcement.
- Company context: derived from authenticated JWT company context (`cid` → `CompanyContextHolder`).
  - Clients may send `X-Company-Id` (for “switch company” UX), but it must match the JWT `cid` or the request is rejected.
- Orchestrator dashboards: `/api/v1/orchestrator/dashboard/*` aggregate status across modules.

## ACCOUNTING (Reports, Purchasing, Inventory, HR)
- Chart of accounts -> journal entries -> periods/close/lock -> statements/aging.
- Purchasing: purchase orders -> goods receipts (GRN) -> raw material batches + stock movements -> supplier invoice (raw material purchase) -> accounting postings -> supplier settlements/payments.
- Purchasing (limitation): PO/GRN do not post GL journals; inventory + GST + AP post at the purchase invoice step.
- Purchasing (guardrail): manual `/raw-materials/intake` and batch creation are disabled by default and reserved for internal adjustments.
- Inventory: raw materials + finished goods stock -> dispatch confirmations -> COGS/Inventory postings.
- HR/Payroll: payroll runs -> calculations -> approvals -> post to accounting -> payments.
- Reports: balance sheet, P&L, trial balance, inventory valuation, reconciliation dashboards.

## FACTORY_PRODUCTION
- Production plans -> batches -> production logs -> packing records -> finished goods batches.
- Packaging mappings -> packing records -> inventory updates.
- Cost allocation -> feeds production cost reporting and accounting postings.
- Current behavior state machines: `erp-domain/docs/PRODUCTION_TO_PACK_STATE_MACHINES.md`

## SALES
- Dealers (search/create) -> order creation -> order confirmation -> inventory reservation.
  - Dealer creation also creates the dealer’s AR receivable account and portal access (dealer role) when applicable.
- Dispatch confirmation -> inventory issue + AR/Invoice + COGS journals.
- Promotions/targets/credit requests managed within sales domain.
- Sales return -> accounting sales return journals.

## DEALERS
- Dealer lifecycle: create/update/list -> ledger/aging/invoices.
- Dealer portal: self-service view of ledger/invoices/orders.
- Settlements and receipts flow into accounting journals and dealer ledger.

## Cross-Module Connections (Key)
- Sales -> Accounting: AR postings, dealer ledger updates, sales returns.
- Sales -> Inventory: reservations + dispatch confirmations.
- Factory -> Inventory: finished goods batches, packing logs.
- Inventory -> Accounting: inventory valuation, landed costs, revaluation, COGS.
- HR/Payroll -> Accounting: payroll posting and payment journals.
- Orchestrator -> Sales/Factory/Accounting: command dispatch, approvals, payroll runs.
