package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record SuperAdminActivationCopyResponse(
    Long tenantId,
    String companyCode,
    String activationStatus,
    Instant expiresAt,
    Long tokenId,
    String activationUrl,
    Long auditEventId,
    List<String> redactedFields) {}
