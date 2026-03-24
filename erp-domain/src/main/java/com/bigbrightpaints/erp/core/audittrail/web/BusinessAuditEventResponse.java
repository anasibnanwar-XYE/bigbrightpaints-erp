package com.bigbrightpaints.erp.core.audittrail.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.bigbrightpaints.erp.core.audittrail.AuditActionEventSource;
import com.bigbrightpaints.erp.core.audittrail.AuditActionEventStatus;

public record BusinessAuditEventResponse(
    Long id,
    Instant occurredAt,
    AuditActionEventSource source,
    String module,
    String action,
    String entityType,
    String entityId,
    String referenceNumber,
    AuditActionEventStatus status,
    String failureReason,
    BigDecimal amount,
    String currency,
    UUID correlationId,
    String requestId,
    String traceId,
    Long actorUserId,
    String actorIdentifier,
    Map<String, String> metadata) {}
