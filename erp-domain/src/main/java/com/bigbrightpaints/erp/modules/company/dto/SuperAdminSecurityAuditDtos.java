package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SuperAdminSecurityAuditDtos {

  private SuperAdminSecurityAuditDtos() {}

  public record SecurityEventResponse(
      Long eventId,
      String eventType,
      String category,
      String severity,
      Instant occurredAt,
      Long tenantId,
      String tenantCode,
      String actorIdentifier,
      String requestMethod,
      String requestPath,
      String traceId,
      Map<String, String> metadata,
      RemediationResponse remediation) {}

  public record RemediationRequest(@NotBlank @Size(max = 300) String reason) {}

  public record RemediationResponse(
      Long remediationId,
      Long eventId,
      String status,
      String severity,
      String reason,
      String updatedBy,
      Instant createdAt,
      Instant updatedAt,
      Long auditEventId) {}
}
