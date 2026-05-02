package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record ActivationVerifyResponse(
    String companyCode,
    String companyName,
    String ownerDisplayName,
    Instant expiresAt,
    List<String> requiredSetupSteps,
    List<String> redactedFields) {}
