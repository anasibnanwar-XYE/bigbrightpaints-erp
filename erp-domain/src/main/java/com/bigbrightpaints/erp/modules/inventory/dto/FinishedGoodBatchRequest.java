package com.bigbrightpaints.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record FinishedGoodBatchRequest(
        @NotNull Long finishedGoodId,
        String batchCode,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @PositiveOrZero BigDecimal unitCost,
        Instant manufacturedAt,
        LocalDate expiryDate
) {}
