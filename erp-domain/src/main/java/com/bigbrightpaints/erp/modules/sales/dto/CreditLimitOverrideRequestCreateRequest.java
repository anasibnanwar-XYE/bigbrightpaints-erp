package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.Positive;

public record CreditLimitOverrideRequestCreateRequest(
    Long dealerId,
    Long packagingSlipId,
    Long salesOrderId,
    @Positive BigDecimal requestedAmount,
    String reason,
    Instant expiresAt) {}
