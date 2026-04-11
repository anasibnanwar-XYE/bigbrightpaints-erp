package com.bigbrightpaints.erp.modules.accounting.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.CostingMethod;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AccountingPeriodRequest(
    @Min(1900) @Max(9999) Integer year,
    @Min(1) @Max(12) Integer month,
    CostingMethod costingMethod) {
  public AccountingPeriodRequest(CostingMethod costingMethod) {
    this(null, null, costingMethod);
  }
}
