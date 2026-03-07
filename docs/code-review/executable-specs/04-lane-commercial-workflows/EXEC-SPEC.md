# Lane 04 Exec Spec

## Covers
- Backlog row 4
- `O2C-02`, `O2C-03`, `O2C-04`, `O2C-05`, `O2C-06`, `O2C-07`, `O2C-08`, `O2C-09`, `P2P-02`, `P2P-03`, `P2P-04`, `P2P-05`, `P2P-06`

## Why This Lane Waits
It hardens the commercial workflows that feed the accounting boundary. Doing it before Lane 03 risks polishing the wrong semantics.

## Primary Review Sources
- `flows/order-to-cash.md`
- `flows/procure-to-pay.md`

## Primary Code Hotspots
- `SalesCoreEngine`
- `SalesOrderRepository`
- `CreditLimitOverrideService`
- `DealerLedgerService`
- `PurchasingService`
- `InvoiceSettlementPolicy`

## Entry Criteria
- Lane 03 boundary decisions are written and approved before workflow hardening starts
- `O2C-09` and the current reservation prerequisite behavior are reproducible on PostgreSQL-backed flows
- dealer and supplier settlement parity baselines are captured before behavior changes begin
- no dispatch-boundary or inventory-accounting redesign is sharing the same slice

## Produces For Other Lanes
- deterministic sales search, confirm, approval, and settlement behavior that frontend and ops can trust
- a normalized commercial vocabulary for status, retry, and approval semantics
- cleaner inputs for finance-control and reporting lanes

## Packet Sequence

### Packet 0 - repair `O2C-09` and freeze sales confirm preconditions
- fix the PostgreSQL search break on `/api/v1/sales/orders/search`
- preserve the reservation prerequisite as an explicit, tested contract instead of hidden backend behavior
- output: runtime regression proof and contract note for sales search and confirm

### Packet 1 - narrow approval and credit side effects
- separate what a credit or override approval is allowed to authorize
- prevent approval flows from silently mutating unrelated dealer master data beyond the intended business decision
- output: approval semantics matrix and regression coverage

### Packet 2 - converge status and idempotency semantics
- normalize order-status vocabulary across create, search, history, and fulfillment
- reduce retry and idempotency asymmetry between adjacent sales and purchasing workflows
- output: status vocabulary note and idempotency proof pack

### Packet 3 - re-prove settlement and sub-ledger parity
- verify dealer and supplier settlement allocations, invoice policy, and sub-ledger sync remain consistent
- keep fail-closed parity checks intact where they protect financial correctness
- output: settlement and ledger parity evidence

## Frontend And Operator Handoff
- frontend gets one documented status vocabulary and one documented order-confirm prerequisite
- operator and support notes explain whether a search failure, hold, approval, or settlement block is contractual or exceptional
- no UI cutover should rely on implicit alias-status handling after this lane lands

## Stop-The-Line Triggers
- dispatch or posting-boundary semantics are changed inside this lane
- new workflow endpoints are added instead of narrowing the current semantics
- supplier privacy or admin-governance changes drift into this lane
- `O2C-09` is fixed together with unrelated sales redesign that makes rollback hard

## Must Not Mix With
- dispatch accounting-boundary redesign
- tenant lifecycle or auth work
- manufacturing authority cleanup

## Must-Pass Evidence
- order idempotency coverage
- dispatch and approval truth suites
- settlement and ledger parity checks
- targeted regression for `/api/v1/sales/orders/search`

## Rollback
- revert the narrow workflow change without changing canonical dispatch or invoice boundary decisions already locked in Lane 03

## Exit Gate
- sales search and order confirm semantics are deterministic
- approval surfaces mutate only the intended business state
- settlement and sub-ledger parity are re-proven after changes
