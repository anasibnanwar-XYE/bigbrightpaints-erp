package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public record CreditLimitOverrideRequestCreateRequest(
        Long dealerId,
        Long packagingSlipId,
        Long salesOrderId,
        @NotNull @Positive BigDecimal dispatchAmount,
        String reason,
        Instant expiresAt
) {}
