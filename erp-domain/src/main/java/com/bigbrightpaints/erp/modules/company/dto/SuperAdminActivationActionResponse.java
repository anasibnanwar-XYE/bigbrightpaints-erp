package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record SuperAdminActivationActionResponse(
    Long tenantId,
    String companyCode,
    String activationStatus,
    Instant sentAt,
    Instant expiresAt,
    Long tokenId,
    String deliveryStatus,
    Long auditEventId,
    List<String> redactedFields) {}
