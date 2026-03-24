package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record FinishedGoodBatchRequest(
    @NotNull Long finishedGoodId,
    String batchCode,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @PositiveOrZero BigDecimal unitCost,
    Instant manufacturedAt,
    LocalDate expiryDate) {}
