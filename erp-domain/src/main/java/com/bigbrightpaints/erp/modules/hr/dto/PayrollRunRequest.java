package com.bigbrightpaints.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record PayrollRunRequest(
    @NotNull LocalDate runDate,
    @DecimalMin(value = "0.00") BigDecimal totalAmount,
    String notes,
    String idempotencyKey) {}
