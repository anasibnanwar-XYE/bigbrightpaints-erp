# Core Infrastructure: Audit Framework

This document covers all audit classes in `com.bigbrightpaints.erp.core.audit` and `com.bigbrightpaints.erp.core.audittrail`.

## Overview

The audit framework provides comprehensive audit logging with two layers:
1. **core.audit**: Basic audit logging for security and business events (AuditService, AuditLog)
2. **core.audittrail**: Enterprise audit trail with ML training data support and business event tracking

---

## Part 1: Core Audit Package (`com.bigbrightpaints.erp.core.audit`)

### AuditService

| Field | Value |
|-------|-------|
| **Name** | AuditService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditService.java |
| **Responsibility** | Service for comprehensive audit logging with async writes and context capture |
| **Use when** | Recording security events, business operations, data access events |
| **Do not use when** | Enterprise audit trail with ML tracking (use EnterpriseAuditTrailService) |
| **Public methods** | `void logEvent(AuditEvent event, AuditStatus status, Map<String, String> metadata)`<br>`void logAuthSuccess(AuditEvent event, String username, String companyCode, Map<String, String> metadata)`<br>`void logAuthFailure(AuditEvent event, String username, String companyCode, Map<String, String> metadata)`<br>`void logSuccess(AuditEvent event)`<br>`void logSuccess(AuditEvent event, Map<String, String> metadata)`<br>`void logFailure(AuditEvent event, String reason)`<br>`void logFailure(AuditEvent event, Map<String, String> metadata)`<br>`void logWarning(AuditEvent event, String message)`<br>`void logInfo(AuditEvent event, String message)`<br>`void logSecurityAlert(String alertType, String description, Map<String, String> details)`<br>`void logDataAccess(String resourceType, String resourceId, String operation)`<br>`void logSensitiveDataAccess(String dataType, String reason)`<br>`AuditContext createContext()` |
| **Callers** | Controllers, services, exception handlers |
| **Dependencies** | AuditLogRepository, CompanyRepository |
| **Side effects** | Async database writes; request context capture |
| **Invariants protected** | Self-reference via @Lazy for @Async/@Transactional proxy chaining |
| **Status** | Canonical |

### AuditLog

| Field | Value |
|-------|-------|
| **Name** | AuditLog |
| **Type** | Entity (JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditLog.java |
| **Responsibility** | Entity for storing audit log entries with comprehensive tracking |
| **Use when** | Understanding audit log storage structure |
| **Do not use when** | N/A |
| **Public methods** | Builder pattern for construction; getters/setters |
| **Callers** | AuditService, AuditLogRepository |
| **Dependencies** | VersionedEntity |
| **Side effects** | Database writes |
| **Invariants protected** | PrePersist sets timestamps and default status |
| **Status** | Canonical |

### AuditEvent

| Field | Value |
|-------|-------|
| **Name** | AuditEvent |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditEvent.java |
| **Responsibility** | Enumeration of all audit event types |
| **Use when** | Specifying audit event type |
| **Do not use when** | N/A |
| **Public methods** | `String getDescription()` |
| **Callers** | AuditService, all audited services |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Event Categories

| Category | Events |
|----------|--------|
| Authentication | LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, TOKEN_REFRESH, TOKEN_REVOKED, PASSWORD_CHANGED, PASSWORD_RESET_REQUESTED, PASSWORD_RESET_COMPLETED |
| MFA | MFA_ENROLLED, MFA_ACTIVATED, MFA_DISABLED, MFA_SUCCESS, MFA_FAILURE, MFA_RECOVERY_CODE_USED |
| Authorization | ACCESS_GRANTED, ACCESS_DENIED, PERMISSION_CHANGED, ROLE_ASSIGNED, ROLE_REMOVED |
| Data Access | DATA_CREATE, DATA_READ, DATA_UPDATE, DATA_DELETE, DATA_EXPORT, SENSITIVE_DATA_ACCESSED |
| Administrative | USER_CREATED, USER_UPDATED, USER_DELETED, USER_ACTIVATED, USER_DEACTIVATED, USER_LOCKED, USER_UNLOCKED |
| System | SYSTEM_STARTUP, SYSTEM_SHUTDOWN, CONFIGURATION_CHANGED, SECURITY_ALERT, INTEGRATION_SUCCESS, INTEGRATION_FAILURE |
| Business | REFERENCE_GENERATED, ORDER_NUMBER_GENERATED, JOURNAL_ENTRY_POSTED, JOURNAL_ENTRY_REVERSED, DISPATCH_CONFIRMED, SETTLEMENT_RECORDED, PAYROLL_POSTED |
| Financial | TRANSACTION_CREATED, TRANSACTION_APPROVED, TRANSACTION_REJECTED, PAYMENT_PROCESSED, REFUND_ISSUED |
| Compliance | AUDIT_LOG_ACCESSED, AUDIT_LOG_EXPORTED, COMPLIANCE_CHECK, DATA_RETENTION_ACTION |

### AuditStatus

| Field | Value |
|-------|-------|
| **Name** | AuditStatus |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditLog.java (inner) |
| **Responsibility** | Status of audited events |
| **Use when** | Setting event status |
| **Do not use when** | N/A |
| **Public methods** | None (enum) |
| **Callers** | AuditService |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Values

| Value | Description |
|-------|-------------|
| SUCCESS | Operation completed successfully |
| FAILURE | Operation failed |
| WARNING | Warning condition |
| INFO | Informational event |

### IntegrationFailureAlertRoutingPolicy

| Field | Value |
|-------|-------|
| **Name** | IntegrationFailureAlertRoutingPolicy |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/IntegrationFailureAlertRoutingPolicy.java |
| **Responsibility** | Routes integration failures to appropriate alert channels |
| **Use when** | Determining alert routing for integration failures |
| **Do not use when** | N/A |
| **Public methods** | `static String resolveRoute(String failureCode, String category)` |
| **Callers** | AuditExceptionRoutingService, exception handlers |
| **Dependencies** | IntegrationFailureAlertRoute |
| **Side effects** | None |
| **Invariants protected** | Normalized failure codes and categories |
| **Status** | Canonical |

### IntegrationFailureMetadataSchema

| Field | Value |
|-------|-------|
| **Name** | IntegrationFailureMetadataSchema |
| **Type** | Utility (final class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/IntegrationFailureMetadataSchema.java |
| **Responsibility** | Shared schema contract for integration failure metadata |
| **Use when** | Building integration failure audit metadata |
| **Do not use when** | N/A |
| **Public methods** | `static void applyRequiredFields(Map<String, String> metadata, String failureCode, String errorCategory, String alertRoutingVersion, String alertRoute)` |
| **Callers** | AuditExceptionRoutingService, SettlementExceptionHandler |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | Required field population; value sanitization |
| **Status** | Canonical |

### AuditLogRepository

| Field | Value |
|-------|-------|
| **Name** | AuditLogRepository |
| **Type** | Repository (Spring Data JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditLogRepository.java |
| **Responsibility** | JPA repository for AuditLog entity |
| **Use when** | Persisting or querying audit logs |
| **Do not use when** | N/A |
| **Public methods** | Standard Spring Data JPA methods |
| **Callers** | AuditService |
| **Dependencies** | AuditLog entity |
| **Side effects** | Database writes |
| **Invariants protected** | None |
| **Status** | Canonical |

### IntegrationFailureAlertRoute

| Field | Value |
|-------|-------|
| **Name** | IntegrationFailureAlertRoute |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/IntegrationFailureAlertRoute.java |
| **Responsibility** | Defines alert routing destinations |
| **Use when** | Routing integration failure alerts |
| **Do not use when** | N/A |
| **Public methods** | None (enum) |
| **Callers** | IntegrationFailureAlertRoutingPolicy |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### JournalEntryPostedAuditListener

| Field | Value |
|-------|-------|
| **Name** | JournalEntryPostedAuditListener |
| **Type** | Entity Listener |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audit |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/JournalEntryPostedAuditListener.java |
| **Responsibility** | Listens for journal entry posted events and records audit |
| **Use when** | Understanding automatic journal entry auditing |
| **Do not use when** | N/A |
| **Public methods** | `void onPostPersist(Object entity)` |
| **Callers** | JPA lifecycle |
| **Dependencies** | AuditService |
| **Side effects** | Audit log entries |
| **Invariants protected** | Automatic audit of journal posting |
| **Status** | Canonical |

---

## Part 2: Enterprise Audit Trail Package (`com.bigbrightpaints.erp.core.audittrail`)

### EnterpriseAuditTrailService

| Field | Value |
|-------|-------|
| **Name** | EnterpriseAuditTrailService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/EnterpriseAuditTrailService.java |
| **Responsibility** | Enterprise audit trail with ML interaction tracking, async persistence, retry queues |
| **Use when** | Recording business or ML audit events |
| **Do not use when** | Simple AuditService events suffice |
| **Public methods** | `void recordBusinessEvent(AuditActionEventCommand command)`<br>`MfaAuditIngestResponse ingestMlInteractions(UserAccount actor, List<AuditEventIngestItemRequest> items, HttpServletRequest request)`<br>`PageResponse<BusinessAuditEventResponse> queryBusinessEvents(...)`<br>`PageResponse<MlInteractionEventResponse> queryMlEvents(...)` |
| **Callers** | Controllers, services |
| **Dependencies** | AuditActionEventRepository, AuditActionEventRetryRepository, MlInteractionEventRepository, CompanyContextService, ObjectMapper |
| **Side effects** | Async database writes; retry scheduling |
| **Invariants protected** | Max batch size 200; max metadata entries 40; actor anonymization via HMAC-SHA256 |
| **Status** | Canonical |

### AuditActionEvent

| Field | Value |
|-------|-------|
| **Name** | AuditActionEvent |
| **Type** | Entity (JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/AuditActionEvent.java |
| **Responsibility** | Entity for business audit events |
| **Use when** | Understanding audit event storage |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters for all fields |
| **Callers** | EnterpriseAuditTrailService |
| **Dependencies** | VersionedEntity |
| **Side effects** | Database writes |
| **Invariants protected** | PrePersist sets timestamps |
| **Status** | Canonical |

### AuditActionEventCommand

| Field | Value |
|-------|-------|
| **Name** | AuditActionEventCommand |
| **Type** | Record (immutable DTO) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/AuditActionEventCommand.java |
| **Responsibility** | Command object for recording business events |
| **Use when** | Creating business event records |
| **Do not use when** | N/A |
| **Public methods** | Record accessor methods |
| **Callers** | Services recording business events |
| **Dependencies** | Company, UserAccount |
| **Side effects** | None |
| **Invariants protected** | Immutable |
| **Status** | Canonical |

### MlInteractionEvent

| Field | Value |
|-------|-------|
| **Name** | MlInteractionEvent |
| **Type** | Entity (JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/MlInteractionEvent.java |
| **Responsibility** | Entity for ML/UI interaction events |
| **Use when** | Understanding ML training data |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters for all fields |
| **Callers** | EnterpriseAuditTrailService |
| **Dependencies** | None |
| **Side effects** | Database writes |
| **Invariants protected** | PrePersist sets timestamps |
| **Status** | Canonical |

### AuditActionEventRetry

| Field | Value |
|-------|-------|
| **Name** | AuditActionEventRetry |
| **Type** | Entity (JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/AuditActionEventRetry.java |
| **Responsibility** | Persistent retry queue for failed business events |
| **Use when** | Understanding audit retry mechanism |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters |
| **Callers** | EnterpriseAuditTrailService |
| **Dependencies** | None |
| **Side effects** | Database writes |
| **Invariants protected** | Backoff scheduling |
| **Status** | Canonical |

### AuditActionEventStatus

| Field | Value |
|-------|-------|
| **Name** | AuditActionEventStatus |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/AuditActionEventStatus.java |
| **Responsibility** | Defines status values for audit events |
| **Use when** | Setting event status |
| **Do not use when** | N/A |
| **Public methods** | None (enum) |
| **Callers** | EnterpriseAuditTrailService, AuditActionEvent, MlInteractionEvent |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Values

| Value | Description |
|-------|-------------|
| SUCCESS | Operation completed successfully |
| FAILURE | Operation failed |
| INFO | Informational event |

### AuditActionEventSource

| Field | Value |
|-------|-------|
| **Name** | AuditActionEventSource |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.audittrail |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/audittrail/AuditActionEventSource.java |
| **Responsibility** | Defines source values for audit events |
| **Use when** | Setting event source |
| **Do not use when** | N/A |
| **Public methods** | None (enum) |
| **Callers** | EnterpriseAuditTrailService, AuditActionEventCommand |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Values

| Value | Description |
|-------|-------------|
| BACKEND | Server-side event |
| UI | Frontend/UI event |
| SYSTEM | System-generated event |

### Repositories

| Repository | Entity |
|------------|--------|
| AuditActionEventRepository | AuditActionEvent |
| AuditActionEventRetryRepository | AuditActionEventRetry |
| MlInteractionEventRepository | MlInteractionEvent |

### Web Layer

| Class | Type | Purpose |
|-------|------|---------|
| EnterpriseAuditTrailController | Controller | REST endpoints for audit trail access |
| AuditEventIngestRequest | DTO | Request for ML interaction ingestion |
| AuditEventIngestItemRequest | DTO | Single ML interaction item |
| BusinessAuditEventResponse | DTO | Response for business event queries |
| MlInteractionEventResponse | DTO | Response for ML event queries |
| MlAuditIngestResponse | DTO | Response for ML ingestion |
