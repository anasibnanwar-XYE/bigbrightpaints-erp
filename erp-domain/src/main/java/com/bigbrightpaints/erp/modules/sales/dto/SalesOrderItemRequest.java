package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SalesOrderItemRequest(
    @NotBlank String productCode,
    String description,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal unitPrice,
    BigDecimal gstRate) {}
