package com.bigbrightpaints.erp.core.audittrail;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.company.domain.Company;

public record AuditActionEventCommand(
    Company company,
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
    String ipAddress,
    String userAgent,
    UserAccount actorUserOverride,
    Boolean mlEligible,
    String trainingPayload,
    Map<String, String> metadata,
    Instant occurredAt) {}
