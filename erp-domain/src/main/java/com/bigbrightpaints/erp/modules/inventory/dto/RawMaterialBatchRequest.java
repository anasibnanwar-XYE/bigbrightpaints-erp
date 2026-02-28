package com.bigbrightpaints.erp.modules.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RawMaterialBatchRequest(
        String batchCode,
        @NotNull BigDecimal quantity,
        @NotBlank String unit,
        @NotNull BigDecimal costPerUnit,
        @NotNull Long supplierId,
        LocalDate manufacturingDate,
        LocalDate expiryDate,
        String notes
) {

    public RawMaterialBatchRequest(String batchCode,
                                   BigDecimal quantity,
                                   String unit,
                                   BigDecimal costPerUnit,
                                   Long supplierId,
                                   String notes) {
        this(batchCode, quantity, unit, costPerUnit, supplierId, null, null, notes);
    }
}
