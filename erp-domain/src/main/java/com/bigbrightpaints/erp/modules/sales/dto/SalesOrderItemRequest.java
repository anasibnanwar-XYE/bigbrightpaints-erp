package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SalesOrderItemRequest(
        @NotBlank String productCode,
        String description,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitPrice,
        BigDecimal gstRate
) {}
