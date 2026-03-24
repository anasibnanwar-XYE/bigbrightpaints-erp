package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record FinishedGoodLowStockThresholdRequest(@NotNull @PositiveOrZero BigDecimal threshold) {}
