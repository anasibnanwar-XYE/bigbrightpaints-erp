package com.bigbrightpaints.erp.core.audittrail.web;

import com.bigbrightpaints.erp.core.audittrail.AuditActionEventStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record MlInteractionEventResponse(
        Long id,
        Instant occurredAt,
        String module,
        String action,
        String interactionType,
        String screen,
        String targetId,
        AuditActionEventStatus status,
        String failureReason,
        UUID correlationId,
        String requestId,
        String traceId,
        Long actorUserId,
        String actorIdentifier,
        boolean actorAnonymized,
        boolean consentOptIn,
        String trainingSubjectKey,
        String payload,
        Map<String, String> metadata
) {
}
