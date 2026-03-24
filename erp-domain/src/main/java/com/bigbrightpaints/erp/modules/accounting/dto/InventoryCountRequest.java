package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record InventoryCountRequest(
    @NotNull InventoryCountTarget target,
    @NotNull Long itemId,
    @NotNull BigDecimal physicalQuantity,
    @NotNull BigDecimal unitCost,
    @NotNull Long adjustmentAccountId,
    LocalDate countDate,
    Long accountingPeriodId,
    Boolean markAsComplete,
    String note) {}
