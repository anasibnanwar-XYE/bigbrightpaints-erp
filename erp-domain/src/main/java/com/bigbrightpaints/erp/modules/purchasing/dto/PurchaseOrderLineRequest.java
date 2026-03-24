package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseOrderLineRequest(
    @NotNull Long rawMaterialId,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    String notes) {}
