package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RawMaterialPurchaseRequest(
    @NotNull Long supplierId,
    @NotBlank String invoiceNumber,
    @NotNull LocalDate invoiceDate,
    String memo,
    Long purchaseOrderId,
    @NotNull Long goodsReceiptId,
    @PositiveOrZero BigDecimal taxAmount,
    @NotEmpty List<@Valid RawMaterialPurchaseLineRequest> lines) {}
