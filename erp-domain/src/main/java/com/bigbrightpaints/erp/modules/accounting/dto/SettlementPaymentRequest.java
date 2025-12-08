package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SettlementPaymentRequest(
        @NotNull Long accountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String method // optional label e.g., CASH/BANK/CHEQUE
) {
}
