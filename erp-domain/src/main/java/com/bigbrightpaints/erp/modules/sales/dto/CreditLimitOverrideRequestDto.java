package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditLimitOverrideRequestDto(
        Long id,
        UUID publicId,
        Long dealerId,
        String dealerName,
        Long packagingSlipId,
        Long salesOrderId,
        BigDecimal dispatchAmount,
        BigDecimal currentExposure,
        BigDecimal creditLimit,
        BigDecimal requiredHeadroom,
        String status,
        String reason,
        String requestedBy,
        String reviewedBy,
        Instant reviewedAt,
        Instant expiresAt,
        Instant createdAt
) {}
