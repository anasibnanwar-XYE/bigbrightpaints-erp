package com.bigbrightpaints.erp.modules.purchasing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record GoodsReceiptLineRequest(
        @NotNull Long rawMaterialId,
        String batchCode,
        @NotNull @Positive BigDecimal quantity,
        String unit,
        @NotNull @Positive BigDecimal costPerUnit,
        String notes
) {}
