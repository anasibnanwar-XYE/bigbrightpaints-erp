package com.bigbrightpaints.erp.modules.purchasing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PurchaseOrderLineRequest(
        @NotNull Long rawMaterialId,
        @NotNull @Positive BigDecimal quantity,
        String unit,
        @NotNull @Positive BigDecimal costPerUnit,
        String notes
) {}
