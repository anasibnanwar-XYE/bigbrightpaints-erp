# Core Infrastructure: Orchestrator

This document covers all orchestrator classes in `com.bigbrightpaints.erp.orchestrator`.

## Overview

The orchestrator coordinates command dispatch, event publishing, and integration coordination across modules. It implements the outbox pattern for reliable event delivery and provides a unified command interface for business workflows.

---

## CommandDispatcher

| Field | Value |
|-------|-------|
| **Name** | CommandDispatcher |
| **Type** | Service (Spring Bean) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/CommandDispatcher.java |
| **Responsibility** | Central command dispatcher for business workflows with idempotency, policy enforcement, and tracing |
| **Use when** | Orchestrating business commands (order approval, dispatch, payroll) |
| **Do not use when** | Direct module service calls are sufficient |
| **Public methods** | `String approveOrder(ApproveOrderRequest request, String idempotencyKey, String requestId, String companyId, String userId)`<br>`String autoApproveOrder(String orderId, BigDecimal totalAmount, String companyId, String idempotencyKey, String requestId)`<br>`String updateOrderFulfillment(String orderId, OrderFulfillmentRequest request, String idempotencyKey, String requestId, String companyId, String userId)`<br>`String dispatchBatch(DispatchRequest request, String idempotencyKey, String requestId, String companyId, String userId)`<br>`String runPayroll(PayrollRunRequest request, String idempotencyKey, String requestId, String companyId, String userId)`<br>`Map<String, Object> integrationHealth()`<br>`Map<String, Object> eventHealth()`<br>`Map<String, Object> traceSummary(String traceId)`<br>`String generateTraceId()` |
| **Callers** | OrchestratorController, internal services |
    **Dependencies** | WorkflowService, IntegrationCoordinator, EventPublisherService, TraceService, PolicyEnforcer, OrchestratorIdempotencyService, OrchestratorFeatureFlags |
    **Side effects** | Database writes (idempotency, events); RabbitMQ publishes |
    **Invariants protected** | Command idempotency; feature flags; positive posting amounts |
    **Status** | Canonical |

### Command Flow

1. Policy enforcement (permissions check)
2. Acquire idempotency lease
3. Execute integration logic
4. Enqueue domain event
5. Record trace
6. Mark success/failure on lease

### Feature-Guarded Commands

| Command | Feature Flag | Fallback |
|---------|-------------|----------|
| runPayroll | isPayrollEnabled | Throws OrchestratorFeatureDisabledException |
| dispatchBatch | (deprecated) | Always throws - use /api/v1/sales/dispatch/confirm |

---

## EventPublisherService

| Field | Value |
|-------|-------|
| **Name** | EventPublisherService |
| **Type** | Service (Spring Bean) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/EventPublisherService.java |
| **Responsibility** | Publishes domain events via outbox pattern with RabbitMQ |
| **Use when** | Understanding event delivery mechanism |
| **Do not use when** | N/A |
| **Public methods** | `void enqueue(DomainEvent event)`<br>`@Scheduled void publishPendingEvents()`<br>`Map<String, Object> healthSnapshot()` |
| **Callers** | CommandDispatcher, other services |
    **Dependencies** | OutboxEventRepository, RabbitTemplate, CompanyContextService, ObjectMapper, MeterRegistry |
    **Side effects** | Database writes; RabbitMQ publishes; metrics |
    **Invariants protected** | At-least-once delivery; ambiguous state reconciliation; backoff retry |
    **Status** | Canonical |

### Outbox States

| State | Description |
|-------|-------------|
| PENDING | Ready to be published |
| PUBLISHING | Currently being processed (with lease timeout) |
| PUBLISHED | Successfully delivered |
| FAILED | Dead-lettered after max retries |

### Retry Configuration

```properties
orchestrator.outbox.publish-lease-seconds=120
orchestrator.outbox.ambiguous-recheck-seconds=300
orchestrator.outbox.lock-at-most-for=PT5M
```

### Error Prefixes

| Prefix | Meaning |
|--------|---------|
| AMBIGUOUS_PUBLISH: | Broker send failed, outcome unknown |
| FINALIZE_FAILURE: | Broker succeeded, DB update failed |
| STALE_LEASE_UNCERTAIN: | Lease expired, held for reconciliation |

---

## IntegrationCoordinator

| Field | Value |
|-------|-------|
| **Name** | IntegrationCoordinator |
| **Type** | Service (Spring Bean) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java |
| **Responsibility** | Coordinates cross-module operations with transaction management |
| **Use when** | Understanding cross-module workflow execution |
| **Do not use when** | Single-module operations |
| **Public methods** | `InventoryReservationResult reserveInventory(String orderId, String companyId, String traceId, String idempotencyKey)`<br>`void queueProduction(String orderId, String companyId)`<br>`AutoApprovalResult autoApproveOrder(String orderId, BigDecimal amount, String companyId, String traceId, String idempotencyKey)`<br>`void updateProductionStatus(String planId, String companyId, String traceId, String idempotencyKey)`<br>`AutoApprovalResult updateFulfillment(String orderId, String requestedStatus, String companyId, String traceId, String idempotencyKey)`<br>`void syncEmployees(String companyId, String traceId, String idempotencyKey)`<br>`JournalEntryDto recordPayrollPayment(...)`<br>`Map<String, Object> health()`<br>`Map<String, Object> fetchAdminDashboard(String companyId)`<br>`Map<String, Object> fetchFactoryDashboard(String companyId)`<br>`Map<String, Object> fetchFinanceDashboard(String companyId)` |
| **Callers** | CommandDispatcher |
    **Dependencies** | SalesService, FactoryService, FinishedGoodsService, AccountingService, HrService, ReportService, OrderAutoApprovalStateRepository, AccountingFacade, CompanyRepository, CompanyClock, OrchestratorFeatureFlags |
    **Side effects** | Database writes; cross-module state changes |
    **Invariants protected** | Company context propagation; transaction isolation; auto-approval state machine |
    **Status** | Canonical |

### AutoApprovalResult Record

| Field | Type | Description |
|-------|------|-------------|
| orderStatus | String | Final order status |
| awaitingProduction | boolean | True if waiting for production |

---

## OrchestratorIdempotencyService

| Field | Value |
|-------|-------|
| **Name** | OrchestratorIdempotencyService |
| **Type** | Service (Spring Bean) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/OrchestratorIdempotencyService.java |
| **Responsibility** | Manages command idempotency with workflow correlation |
| **Use when** | Understanding orchestrator idempotency |
| **Do not use when** | N/A |
| **Public methods** | `CommandLease start(String commandName, String idempotencyKey, Object payload, Supplier<String> workflowStarter)`<br>`void markSuccess(OrchestratorCommand command)`<br>`void markFailed(OrchestratorCommand command, Throwable error)` |
| **Callers** | CommandDispatcher |
    **Dependencies** | OrchestratorCommandRepository |
    **Side effects** | Database writes |
    **Invariants protected** | Command uniqueness; workflow correlation; trace ID generation |
    **Status** | Canonical |

### CommandLease Record

| Field | Type | Description |
|-------|------|-------------|
| shouldExecute | boolean | True if this request should execute |
| traceId | String | Correlation trace ID |
| command | OrchestratorCommand | The command entity |

---

## TraceService

| Field | Value |
|-------|-------|
| **Name** | TraceService |
| **Type** | Service (Spring Bean) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/TraceService.java |
| **Responsibility** | Records trace events for debugging and auditing |
| **Use when** | Understanding trace/debug capabilities |
| **Do not use when** | N/A |
| **Public methods** | `void record(String traceId, String eventType, String companyId, Map<String, Object> data, String requestId, String idempotencyKey)`<br>`List<Map<String, Object>> getTrace(String traceId)` |
| **Callers** | CommandDispatcher |
    **Dependencies** | None (in-memory) |
    **Side effects** | In-memory trace storage |
    **Invariants protected** | Bounded trace history (100 events max) |
    **Status** | Canonical |

---

## OutboxEvent

| Field | Value |
|-------|-------|
| **Name** | OutboxEvent |
| **Type** | Entity |
    **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.repository |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/repository/OutboxEvent.java |
| **Responsibility** | Entity for outbox pattern event storage |
| **Use when** | Understanding event persistence |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters; `void markPublishingUntil(Instant leaseExpiry)`<br>`void markPublished()`<br>`void scheduleRetry(String errorMessage, int maxAttempts, long delaySeconds)`<br>`void deferPublishing(String marker, long delaySeconds)` |
| **Callers** | EventPublisherService |
    **Dependencies** | None |
    **Side effects** | Database writes |
    **Invariants protected** | Status transitions; retry counting |
    **Status** | Canonical |

### Key Fields

| Field | Type | Description |
|-------|------|-------------|
| id | UUID | Event ID |
| entity | String | Entity type |
| entityId | String | Entity ID |
| eventType | String | Event type name |
| payload | String | JSON payload |
| companyId | Long | Company ID |
| traceId | String | Trace ID |
| requestId | String | Request ID |
| idempotencyKey | String | Idempotency key |
| status | Status | PENDING, PUBLISHING, PUBLISHED, FAILED |
| retryCount | int | Number of retry attempts |
| deadLetter | boolean | True if dead-lettered |
| nextAttemptAt | Instant | Next attempt time |
| lastError | String | Last error message |

---

## OrchestratorCommand

| Field | Value |
|-------|-------|
| **Name** | OrchestratorCommand |
| **Type** | Entity |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.repository |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/repository/OrchestratorCommand.java |
| **Responsibility** | Entity for orchestrator command idempotency tracking |
| **Use when** | Understanding command persistence |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters |
    **Callers** | OrchestratorIdempotencyService |
    **Dependencies** | None |
| **Side effects** | Database writes |
    **Invariants protected** | Unique constraint on (commandName, idempotencyKey) |
| **Status** | Canonical |

### Key Fields

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Command ID |
| commandName | String | Command type name |
| idempotencyKey | String | Idempotency key |
| workflowId | String | Workflow correlation ID |
| traceId | String | Trace ID |
| status | Status | PENDING, COMPLETED, FAILED |
| payloadHash | String | SHA-256 hash of payload |
| error | String | Error message if failed |

---

## OrderAutoApprovalState

| Field | Value |
|-------|-------|
| **Name** | OrderAutoApprovalState |
| **Type** | Entity |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.repository |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/repository/OrderAutoApprovalState.java |
| **Responsibility** | Tracks auto-approval state machine per order |
| **Use when** | Understanding order auto-approval flow |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters; `boolean isCompleted()`<br>`boolean isInventoryReserved()`<br>`boolean isOrderStatusUpdated()`<br>`void startAttempt()`<br>`void markInventoryReserved()`<br>`void markOrderStatusUpdated()`<br>`void markCompleted()`<br>`void markFailed(String reason)` |
| **Callers** | IntegrationCoordinator |
    **Dependencies** | None |
    **Side effects** | Database writes |
    **Invariants protected** | State machine transitions |
| **Status** | Canonical |

### State Machine

```
PENDING → IN_PROGRESS → COMPLETED
                    ↘ FAILED
```

### State Tracking Fields

| Field | Description |
|-------|-------------|
| inventoryReserved | Inventory reservation completed |
| orderStatusUpdated | Order status updated |
| dispatchFinalized | Dispatch completed |
| completed | Overall completion flag |

---

## CorrelationIdentifierSanitizer

| Field | Value |
|-------|-------|
| **Name** | CorrelationIdentifierSanitizer |
| **Type** | Utility (final class) |
| **Module** | orchestrator |
| **Package** | com.bigbrightpaints.erp.core.orchestrator.service |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/CorrelationIdentifierSanitizer.java |
| **Responsibility** | Sanitizes correlation identifiers for safe logging and storage |
| **Use when** | Handling trace IDs, request IDs, idempotency keys |
| **Do not use when** | N/A |
| **Public methods** | `static String sanitizeRequiredTraceId(String traceId)`<br>`static String sanitizeOptionalTraceId(String traceId)`<br>`static String sanitizeRequiredRequestId(String requestId)`<br>`static String sanitizeOptionalRequestId(String requestId)`<br>`static String sanitizeRequiredIdempotencyKey(String idempotencyKey)`<br>`static String sanitizeOptionalIdempotencyKey(String idempotencyKey)`<br>`static String normalizeRequestId(String requestId, String idempotencyKey)`<br>`static String safeTraceForLog(String traceId)`<br>`static String safeIdempotencyForLog(String idempotencyKey)`<br>`static String safeIdentifierForLog(String identifier)` |
| **Callers** | CommandDispatcher, EventPublisherService, IntegrationCoordinator |
    **Dependencies** | None |
    **Side effects** | None |
    **Invariants protected** | Max length 128; no control characters; safe for logging |
| **Status** | Canonical |
