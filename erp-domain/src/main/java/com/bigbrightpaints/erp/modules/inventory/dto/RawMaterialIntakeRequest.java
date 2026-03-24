package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RawMaterialIntakeRequest(
    @NotNull Long rawMaterialId,
    String batchCode,
    @NotNull BigDecimal quantity,
    @NotBlank String unit,
    @NotNull BigDecimal costPerUnit,
    @NotNull Long supplierId,
    LocalDate manufacturingDate,
    LocalDate expiryDate,
    String notes) {

  public RawMaterialIntakeRequest(
      Long rawMaterialId,
      String batchCode,
      BigDecimal quantity,
      String unit,
      BigDecimal costPerUnit,
      Long supplierId,
      String notes) {
    this(rawMaterialId, batchCode, quantity, unit, costPerUnit, supplierId, null, null, notes);
  }
}
