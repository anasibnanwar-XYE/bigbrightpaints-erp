package com.bigbrightpaints.erp.core.audittrail.web;

import com.bigbrightpaints.erp.core.audittrail.AuditActionEventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventIngestItemRequest(
        @NotBlank @Size(max = 64) String module,
        @NotBlank @Size(max = 128) String action,
        @Size(max = 32) String interactionType,
        @Size(max = 128) String screen,
        @Size(max = 256) String targetId,
        @Size(max = 128) String referenceNumber,
        AuditActionEventStatus status,
        @Size(max = 512) String failureReason,
        UUID correlationId,
        @Size(max = 128) String requestId,
        @Size(max = 128) String traceId,
        Map<String, Object> trainingPayload,
        Map<String, String> metadata,
        Instant occurredAt
) {
}
