package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalesTargetRequest(
    @NotBlank String name,
    @NotNull LocalDate periodStart,
    @NotNull LocalDate periodEnd,
    @NotNull BigDecimal targetAmount,
    BigDecimal achievedAmount,
    @NotBlank String assignee,
    @NotBlank String changeReason) {}
