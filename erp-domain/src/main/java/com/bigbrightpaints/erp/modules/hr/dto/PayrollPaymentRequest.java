package com.bigbrightpaints.erp.modules.hr.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PayrollPaymentRequest(
    @NotNull Long payrollRunId,
    @NotNull Long cashAccountId,
    @NotNull Long expenseAccountId,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    String referenceNumber,
    String memo) {}
