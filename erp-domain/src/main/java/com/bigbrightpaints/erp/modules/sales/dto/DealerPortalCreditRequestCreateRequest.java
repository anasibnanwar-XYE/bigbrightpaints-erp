package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DealerPortalCreditRequestCreateRequest(
    @NotNull @Positive BigDecimal amountRequested, String reason) {}
