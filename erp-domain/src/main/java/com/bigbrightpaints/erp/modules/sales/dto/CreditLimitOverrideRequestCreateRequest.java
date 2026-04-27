package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

public record CreditLimitOverrideRequestCreateRequest(
    @Schema(
            description =
                "Dealer identity for headroom evaluation; required when neither salesOrderId nor"
                    + " packagingSlipId can resolve the dealer")
        Long dealerId,
    Long packagingSlipId,
    Long salesOrderId,
    @Schema(
            description =
                "Canonical requested amount for temporary credit headroom. Required headroom is"
                    + " computed against outstanding + pending-order exposure")
        @Positive
        BigDecimal requestedAmount,
    String reason,
    Instant expiresAt) {}
