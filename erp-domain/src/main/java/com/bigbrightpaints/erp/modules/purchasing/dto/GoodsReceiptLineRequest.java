package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GoodsReceiptLineRequest(
    @NotNull Long rawMaterialId,
    String batchCode,
    @NotNull @Positive BigDecimal quantity,
    String unit,
    @NotNull @Positive BigDecimal costPerUnit,
    LocalDate manufacturingDate,
    LocalDate expiryDate,
    String notes) {

  public GoodsReceiptLineRequest(
      Long rawMaterialId,
      String batchCode,
      BigDecimal quantity,
      String unit,
      BigDecimal costPerUnit,
      String notes) {
    this(rawMaterialId, batchCode, quantity, unit, costPerUnit, null, null, notes);
  }
}
