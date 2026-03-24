package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AutoSettlementRequest(
    Long cashAccountId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    String referenceNumber,
    String memo,
    String idempotencyKey) {}
