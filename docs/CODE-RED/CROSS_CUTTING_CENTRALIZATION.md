# Cross-Cutting Centralization Plan (CODE-RED)

Last updated: 2026-02-02

Goal: eliminate “duplicate code smell” where cross-cutting logic is duplicated/scattered (often with inconsistent domain
vocabulary), by converging onto a **single source of truth** per concern. In CODE-RED we prioritize changes that reduce
data-corruption risk (idempotency, tenant isolation, posting boundaries) over stylistic refactors.

## P0 – Must Centralize Before We Claim “Enterprise”

### 1) Idempotency: one contract across the system
Problem
- Multiple idempotency implementations exist (some payload-hash based, some reference-based, some missing entirely), which
  creates uneven retry safety.

Current anchors
- Orchestrator idempotency ledger: `erp-domain/src/main/resources/db/migration/V118__orchestrator_command_idempotency.sql`
- Sales order idempotency: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/domain/SalesOrder.java`
- Payroll run idempotency: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRun.java`
- Manual journals reserve-first pattern: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java`

Plan
- Define a single “idempotency contract” (key scope + request hash rules + conflict behavior) and apply it to every write
  endpoint that mutates stock/ledger.
- Add DB uniqueness guards where a “same key” must converge on one document row.
- Make idempotency mismatch-safe everywhere:
  - same key/reference + different payload (amount/accounts/lines) must fail closed with a conflict (409), never “return existing”.

### 2) Posting boundaries: one canonical writer per business event
Problem
- Duplicate endpoints and internal helpers can create “parallel truth” (double-posting or status drift).

Current anchors
- Canonical sales dispatch truth: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesService.java`
- Canonical accounting wrapper (enterprise posting boundary): `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingFacade.java`
- De-dup backlog: `docs/CODE-RED/DEDUPLICATION_BACKLOG.md`

Plan
- For every business event, document the one canonical service method and force every alias/legacy endpoint to call it.
- Any unsafe bypass becomes admin-only repair tooling or returns 410 + canonicalPath in prod.
- Treat `AccountingFacade` as the “posting firewall”:
  - all module-level posting must flow through it (or a deliberately reviewed alternative)
  - it must dedupe across reference namespaces (canonical vs custom references) to prevent double-posting
  - it must enforce mismatch-safe idempotency for any “return existing” behavior

Known code-smell examples to eliminate (P0)
- AR/Revenue journal dedupe must check both canonical `INV-<orderNumber>` and any invoice-number-based reference.
- COGS journal dedupe must be standardized to the slip boundary (`COGS-<slipNumber>`), and order-level helpers must not create
  a second `COGS-<orderNumber>` journal for the same dispatch event.

### 3) Tenant isolation: one way to look up entities
Problem
- “findById then check company” patterns can leak cross-tenant existence and create inconsistent error behavior.

Current anchor
- Company-scoped lookup utility: `erp-domain/src/main/java/com/bigbrightpaints/erp/core/util/CompanyEntityLookup.java`

Plan
- Replace ad-hoc company checks with `CompanyEntityLookup` + repository `findByCompanyAndId(...)` / pessimistic lock variants.
- Standardize error codes/messages for “not found” vs “forbidden” outcomes.

### 3.1) Identity vocabulary: companyCode vs companyId must be unambiguous
Problem
- In some layers, variables/headers/claims named like “companyId” actually carry the company code string. This is a high-risk
  code smell because engineers will eventually pass the wrong identifier into lookups, audits, or security checks.

Plan
- Adopt the canonical identity vocabulary in `docs/CODE-RED/IDENTITY_AND_NAMING.md`.
- Standardize tenant context as `companyCode` (string) and reserve `companyId` for numeric DB ids.
- Deprecate confusing legacy header/claim names with backward-compatible parsing during a defined window.

### 4) Payroll: posting journal vs payment journal must be explicit
Problem
- Mixing “posting journal” and “payment journal” into one field causes loss of audit trail and enables double-expense.

Current anchors
- Payroll posting (expense/liability): `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java`
- Payroll payment (liability clearing): `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java`
- Payment journal link: `erp-domain/src/main/resources/db/migration/V119__payroll_payment_journal_link.sql`

Plan
- Keep payroll run posting and payment as two distinct journals with explicit linkage fields.
- Make “mark paid” fail closed unless a payment journal exists (no “paid with no evidence”).

### 5) Prod gating: one contract for feature flags
Problem
- Production safety cannot rely on controller-only gates; internal callers can bypass them accidentally.

Plan
- Any prod-gated endpoint must enforce gating in the service layer too (defense-in-depth).
- Denied attempts should still be auditable (who/when/company/command) without performing side effects.

### 6) Observability identifiers: one envelope across modules
Problem
- Trace/audit/outbox/accounting events use overlapping identifiers with inconsistent meaning (`traceId`, `correlationId`,
  `requestId`, `idempotencyKey`, `referenceNumber`). This blocks enterprise-grade audit and incident response.

Plan
- Adopt `docs/CODE-RED/OBSERVABILITY_IDENTIFIERS.md`.
- Standardize ingress header handling + MDC + response echo.
- Persist identifiers in outbox/audit in queryable columns (no “parse payload to debug”).

## P1 – High Value Centralization (After P0 Gate Is Green)

### Hashing / request signature utilities
Problem
- Multiple SHA-256 helper implementations exist with different truncation/fallback behaviors.

Examples
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingService.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/OpeningStockImportService.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/SalesReturnService.java`

Plan
- Introduce a shared utility (e.g., `core/util/Hashing`) and migrate call sites one module at a time with parity tests.

### Domain vocabulary (statuses, reference namespaces)
Problem
- Stringly-typed statuses and inconsistent naming (“DISPATCHED” vs “SHIPPED”, etc.) increases bypass risk and makes audit harder.

Plan
- Centralize status enums per bounded context (sales dispatch, inventory slips, payroll workflow) and forbid free-form updates.
- Centralize reference namespace rules (already partially handled in `AccountingFacade`).

## Acceptance Criteria
- Every “write” endpoint documents its canonical service method + idempotency scope.
- Every “mark final truth” action is impossible without creating the required accounting/inventory linkage.
- Every cross-module automation command is auditable: company, actor, traceId, and journal/movement ids are queryable.
