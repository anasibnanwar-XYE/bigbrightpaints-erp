Plan for Incoming Bug Report (to be updated with dev findings)
==============================================================

Status Updates
- Inventory/Production: ✅ Implemented (RM atomic deduct & pessimistic locks, atomic packed-qty update, per-session FG receipt journal, costing snapshot before mutation, packing tests updated).
- AccountingService: ✅ duplicate journal refs; ✅ payroll orphans (batch + single); ✅ settlement races (pessimistic locks); ✅ balance contention (atomic UPSERT). Pending: DB unique constraint on journal ref; query perf/index migration.
- Purchasing/Supplier: ✅ Duplicate invoice race, receipt over-stock, return > stock, journal-first ordering, ledger sourced from ledger balances.

Context & Architecture Guardrails
- Multi-tenant by company: every query/update must scope by company and use locking helpers already in services/repositories.
- Dispatch flow: orders -> slips -> confirm dispatch/backorder -> journals; do not reintroduce duplicate slips or cross-slip locking.
- Idempotency: OrderAutoApprovalState and number sequences enforce single-run side effects; avoid side-effecting retries without state flags.
- Caching leniencies: short-lived account/WAC caches exist; invalidate on mutations, do not add global/shared caches.

When Bug List Arrives
- Triage each item: classify (data integrity, concurrency, validation, API contract, performance).
- Map to file/line and reproduce (unit/e2e) without altering architecture assumptions above.
- Decide minimal fix: prefer localized change over refactors; keep transactional boundaries intact.

Execution Steps
1) Read each bug description and reproduce (existing tests or add targeted small tests).
2) Patch code with minimal change respecting company scoping, lock order, and idempotency guards.
3) Run targeted tests for the touched modules; if time permits, rerun full `mvn -q test`.
4) Update this plan with per-bug status (Repro ?/?, Fix applied ?/?, Test coverage).
5) Sanity-check API/OpenAPI if any contract changes are involved.

Reporting Back
- For each bug: file path, method/line, cause, fix summary, tests executed.
- Call out any residual risk or required data cleanup.

Detailed Fix Plan (audit-driven, CA-grade controls)
---------------------------------------------------

AccountingService (journals/settlements/payroll)
- Settlement races (lines ~670-1143): ✅ PESSIMISTIC locks on invoice/dealer/supplier; @Version present.
- Payroll orphans (lines ~467-639): ✅ Batch DRAFT→PAID post-journal; single PROCESSING→PAID post-journal; rollback-safe.
- Duplicate journal refs (lines ~165-378): ✅ pre-save reference check; pending DB unique constraint.
- Balance contention (lines ~336-348): ✅ sorted locks + atomic UPSERT for balances.
- Ledger double-post risk (lines ~351-376): ✅ single tx boundary, partner locks, atomic balance updates.
- Query performance: pending indexes on `(company_id, entry_date, status)` for journal lines; enforce pagination.

SalesService (orders/credit/dispatch)
- Oversell on create (lines ~225-258): ✅ reserveForOrder immediately; REPEATABLE_READ isolation; dealer lock.
- Cancel rollback (lines ~296-302): ✅ releaseReservationsForOrder on cancel.
- Late credit check (lines ~924-931): ✅ dealer lock in confirmDispatch.
- Isolation: createOrder uses REPEATABLE_READ; consider optimistic/serializable if needed.
- Dealer Portal: ✅ DealerPortalService + DealerPortalController with self-only access; ledger/invoices/aging views.

Inventory / Production
- RM batch double-spend: ✅ pessimistic locks + atomic deduct; @Version.
- Packing lost update: ✅ atomic incrementPackedQuantityAtomic + pessimistic log lock.
- WIP/FG desync: ✅ per-session FG receipt journal; wastage on completion.
- Costing stale: ✅ snapshot cost before deduction under lock.

Orchestrator / Dashboard / Outbox
- Tx non-propagation: wrap dashboard delegates in @Transactional(readOnly = true, REQUIRES_NEW).
- Workflow UUID races: persist with lock check; reject dupes.
- Scheduler overlaps: ✅ AtomicBoolean mutex in EventPublisherService.publishPendingEvents() prevents concurrent runs.
- Eventual consistency drift: add CDC timestamps/read-your-writes where feasible.
- Note: For multi-instance deployments, add ShedLock dependency for distributed locking.

HR / Payroll
- Batch payment dupes: ✅ PayrollRun.idempotencyKey + findByCompanyAndIdempotencyKey pre-check.
- Employee status races: ✅ EmployeeRepository has @Lock(PESSIMISTIC_WRITE).
- Leave overlaps: ✅ existsOverlappingByEmployeeIdAndDates implemented.
- Premature COMPLETED: DRAFT→PROCESSING→COMPLETED; validate journal sum in @PreUpdate (pending).

Purchasing / Supplier
- Duplicate invoice race: ✅ pessimistic lock on invoice number; unique constraint present.
- Receipt over-stock: ✅ lock raw material before loops.
- Return > stock: ✅ atomic deduct prevents negatives.
- Ledger stale: ✅ ledger balances only (no denorm fallback).
- Journal orphans: ✅ journal-first then link purchase/movements.

Reports (ReportService/Controller)
- No company filter: ✅ All methods call requireCurrentCompany() - company filtering implemented.
- Aggregation races: ✅ All public methods have @Transactional(readOnly = true).
- N+1/full scans: use native GROUP BY; add pagination/date params (optimization, can defer).

Invoice (InvoiceService / InvoiceSettlementPolicy / InvoicePdfService)
- Outstanding race: ✅ InvoiceSettlementPolicy.applySettlement() centralizes settlement; AccountingService uses policy with pessimistic invoice locks.
- PDF DoS/IDOR: ✅ sanitizeForPdf() escapes HTML; ownership enforced via companyEntityLookup.requireInvoice().
- Status drift: ✅ updateStatusFromOutstanding() centralizes status transitions; @Version via VersionedEntity.

RBAC / Admin
- Role assign race: ✅ RoleRepository.lockByName() with PESSIMISTIC_WRITE; RoleService uses lock.
- User status race: ✅ UserAccountRepository.lockById(); AdminUserService.suspend/unsuspend use pessimistic lock.
- Cache invalidation: ✅ TokenBlacklistService.revokeAllUserTokens() called on suspend/role change to force re-auth.

Security / Auth
- JWT jti claim: ✅ TokenBlacklistService uses jti claim for blacklist.
- Blacklist race: ✅ existsByTokenId check before insert.
- Rate limiting: ✅ SecurityMonitoringService.checkRateLimit() with per-identifier throttling.
- Filter races: ✅ JwtAuthenticationFilter checks isTokenBlacklisted() and isUserTokenRevoked().
- Logout blacklist: ✅ blacklistToken() called on logout.

Company Context
- ThreadLocal async loss: ✅ CompanyContextTaskDecorator + AsyncConfig propagate context to @Async threads.
- Filter validation: ✅ CompanyContextFilter validates user has access to requested company.
- Switch endpoint: pending (add /companies/{id}/switch).

Portal / Dashboard
- Aggregation races: single-query GROUP BY under read-only tx.
- Header leak: validate companyId against user’s companies.

Tx Propagation / Events / DB Consistency
- Partial commits: avoid inventory deduct before journals; use single tx or compensating saga.
- Denorm drifts: report from InventoryMovement/JournalEntry views; materialized views.
- Outbox races: distributed lock + idempotent handlers; expose DLQ metrics/alerts; expand beyond top10 polling.
- Missing SalesOrderCreatedEvent handler: add @EventListener in IntegrationCoordinator; publish via outbox.
- WIP/FG journal gaps: ✅ per packing session posting; no silent skips.
- Credit limit race: lock dealer; use optimistic version where appropriate.
- Denorm stock races: per-batch locks and centralized inventory service calls.

RBAC / Policy Enforcement
- PolicyEnforcer unused: inject/call in command paths (confirmDispatch, markSlipDispatched, createJournalEntry, createOrder, issueFromBatches); add role-aware guards (@PreAuthorize) and RBAC ITs.

Stock vs Journals Ordering
- Current risk: stock deducts before journals; fix by posting journals first (or compensating) and only then reducing stock; link journal IDs to movements/slips.

Outbox / Events Reliability
- At-least-once; risk of dupes/backlog/silent DLQ. Add lock/idempotent consumers, better polling, DLQ metrics/alerts. Tests for rollback, retry→DLQ, crash mid-publish, consumer idempotency.

Cost Booking (Accounting/Invoice/Purchasing/Inventory)
- COGS auto-post: ✅ IntegrationCoordinator.postCogsEntry() + SalesService.confirmDispatch() post COGS via AccountingFacade.postCOGS().
- WIP→FG relief: ✅ per packing session FG receipt; wastage on completion.
- Purchase return: ✅ journal-first, atomic stock restore via SalesReturnService.postCogsReversal().
- Reporting: prefer JournalEntry/InventoryMovement views.

Sales → Production/Dispatch Flow
- Status: BOOKED → CONFIRMED → RESERVED → PENDING_PRODUCTION | READY_TO_SHIP → DISPATCHED/SHIPPED; idempotency via keys/state locks.
- Tests: acceptance, shortage, idempotent approve/create.

Cross-cutting guardrails
- Multi-tenant scoping everywhere; deterministic lock order; constraints where noted; idempotent handlers.
