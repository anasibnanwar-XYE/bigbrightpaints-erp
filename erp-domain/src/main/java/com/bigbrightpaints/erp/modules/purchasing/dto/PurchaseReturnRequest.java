package com.bigbrightpaints.erp.modules.purchasing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PurchaseReturnRequest(
        @NotNull Long supplierId,
        @NotNull Long purchaseId,
        @NotNull Long rawMaterialId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal unitCost,
        String referenceNumber,
        LocalDate returnDate,
        String reason) {}
