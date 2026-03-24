package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SettlementPaymentRequest(
    @NotNull Long accountId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    String method // optional label e.g., CASH/BANK/CHEQUE
    ) {}
