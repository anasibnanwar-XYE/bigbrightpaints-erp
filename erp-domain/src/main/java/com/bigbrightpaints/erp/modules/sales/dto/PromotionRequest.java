package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromotionRequest(
    @NotBlank String name,
    String description,
    @Size(max = 1024) String imageUrl,
    @NotBlank String discountType,
    @NotNull BigDecimal discountValue,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    String status) {
  public PromotionRequest(
      String name,
      String description,
      String discountType,
      BigDecimal discountValue,
      LocalDate startDate,
      LocalDate endDate,
      String status) {
    this(name, description, null, discountType, discountValue, startDate, endDate, status);
  }
}
