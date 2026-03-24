package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RawMaterialPurchaseLineRequest(
    @NotNull Long rawMaterialId,
    String batchCode,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    BigDecimal taxRate,
    Boolean taxInclusive,
    String notes) {}
