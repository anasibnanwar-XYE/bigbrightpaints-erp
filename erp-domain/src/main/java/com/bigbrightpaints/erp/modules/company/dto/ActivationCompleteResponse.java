package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record ActivationCompleteResponse(
    Long tenantId,
    String companyCode,
    Long ownerId,
    String ownerState,
    String tenantStatus,
    Instant completedAt,
    List<String> nextSetupSteps,
    List<String> redactedFields) {}
