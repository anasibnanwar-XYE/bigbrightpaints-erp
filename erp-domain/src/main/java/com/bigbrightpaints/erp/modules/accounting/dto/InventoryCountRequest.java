package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotNull;

public record InventoryCountRequest(
    @NotNull InventoryCountTarget target,
    @NotNull Long itemId,
    @NotNull BigDecimal physicalQuantity,
    @NotNull BigDecimal unitCost,
    @NotNull Long adjustmentAccountId,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate countDate,
    Long accountingPeriodId,
    Boolean markAsComplete,
    String note) {}
