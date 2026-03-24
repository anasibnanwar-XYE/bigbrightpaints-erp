package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreditLimitOverrideRequestCreateRequest(
    Long dealerId,
    Long packagingSlipId,
    Long salesOrderId,
    @NotNull @Positive BigDecimal dispatchAmount,
    String reason,
    Instant expiresAt) {}
