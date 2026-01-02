# Module Flow Map

## ADMIN
- Auth: `/api/v1/auth/*` -> JWT issuance/refresh -> method-level authorization.
- Users & roles: `/api/v1/admin/*` + `/api/v1/admin/roles/{roleKey}` -> RBAC enforcement.
- Company context: `/api/v1/multi-company/companies/switch` -> `X-Company-Id` header used across services.
- Orchestrator dashboards: `/api/v1/orchestrator/dashboard/*` aggregate status across modules.

## ACCOUNTING (Reports, Purchasing, Inventory, HR)
- Chart of accounts -> journal entries -> periods/close/lock -> statements/aging.
- Purchasing: raw material purchases -> inventory movement -> supplier settlements -> accounting postings.
- Inventory: raw materials + finished goods stock -> dispatch confirmations -> COGS/Inventory postings.
- HR/Payroll: payroll runs -> calculations -> approvals -> post to accounting -> payments.
- Reports: balance sheet, P&L, trial balance, inventory valuation, reconciliation dashboards.

## FACTORY_PRODUCTION
- Production plans -> batches -> production logs -> packing records -> finished goods batches.
- Packaging mappings -> packing records -> inventory updates.
- Cost allocation -> feeds production cost reporting and accounting postings.

## SALES
- Dealers -> order creation -> order confirmation -> inventory reservation.
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
