# Architecture

Architectural decisions, patterns discovered, and conventions.

**What belongs here:** Module structure, patterns, conventions, entity relationships, cross-module contracts.

---

## Module Structure
Base package: `com.bigbrightpaints.erp`
- `core/` - Shared infrastructure (security, config, audit, exceptions, utilities)
- `modules/` - Domain modules (accounting, admin, auth, company, factory, hr, inventory, invoice, portal, production, purchasing, rbac, reports, sales)
- `orchestrator/` - Cross-module orchestration (command dispatch, event publishing, workflows, scheduling)
- `shared/dto/` - Shared DTOs (ApiResponse, PageResponse, ErrorResponse)
- `config/` - App-level config (CORS, RabbitMQ, Jackson)

## Per-Module Convention
Each module follows: `domain/` (entities + repos), `service/`, `controller/`, `dto/`, optionally `event/`, `config/`

## Key Patterns
- Multi-tenant via `CompanyContextHolder` (thread-local company context)
- Idempotency via signature hashing + DB unique constraints
- Outbox pattern for reliable event publishing (EventPublisherService)
- ShedLock for distributed scheduler coordination
- Flyway v2 is the active migration path in prod profile (`prod` includes `flyway-v2`, locations `classpath:db/migration_v2`)
- MapStruct for DTO mapping (CentralMapperConfig)
- JaCoCo coverage gates (Tier A packages + bundle minimum)

## Entity Base
- `VersionedEntity` - base class with optimistic locking
- Company-scoped entities have `company` field for tenant isolation

## Error Handling
- `ApplicationException` with `ErrorCode` enum for domain errors
- `GlobalExceptionHandler` maps exceptions to HTTP responses
- **CONVENTION**: Always use ApplicationException for business errors, never raw IllegalArgumentException/IllegalStateException

## Tenant & Admin Runtime Conventions
- Tenant lifecycle states are `ACTIVE`, `SUSPENDED`, `DEACTIVATED`.
- `DEACTIVATED` tenants are denied all API access.
- `SUSPENDED` tenants are read-only (write operations are denied).
- Module access gates treat `AUTH`, `ACCOUNTING`, `SALES`, and `INVENTORY` as always-on core modules.
- Optional modules are controlled via company `enabled_modules` and enforced by `ModuleGatingInterceptor` + `ModuleGatingService`.

## ERP Truth-Stabilization Mission Notes
- Highest-risk O2C/P2P hotspots are `SalesCoreEngine`, `InvoiceService`, `GoodsReceiptService`, `PurchaseInvoiceEngine`, `InventoryAccountingEventListener`, `SupplierService`, and `DealerService`.
- For this mission, workflow state and accounting state must stay separate in touched documents.
- Posting truth must have one canonical trigger per touched workflow boundary; duplicate-truth listeners and dead fallback paths should be removed when a feature makes them obsolete.
- O2C dispatch posting has one allowed accounting path: `SalesCoreEngine.confirmDispatch -> AccountingFacade.postCogsJournal/postSalesJournal`. Orchestrator batch-dispatch and fulfillment endpoints must fail closed or redirect callers to the canonical sales dispatch confirm endpoint; they must never mint `DISPATCH-*` journals.
- The mission normalizes linked business references across order or proforma, production requirement, packaging slip, dispatch, invoice, journal, settlement, return, note, and reversal artifacts.
- Flyway `migration_v2` is the only valid migration track for this mission.
