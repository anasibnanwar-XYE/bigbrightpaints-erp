package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PurchaseReturnRequest(
    @NotNull Long supplierId,
    @NotNull Long purchaseId,
    @NotNull Long rawMaterialId,
    @NotNull @Positive BigDecimal quantity,
    @NotNull @Positive BigDecimal unitCost,
    String referenceNumber,
    LocalDate returnDate,
    String reason) {}
