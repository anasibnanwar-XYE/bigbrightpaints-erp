package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record GoodsReceiptRequest(
    @NotNull Long purchaseOrderId,
    @NotBlank String receiptNumber,
    @NotNull LocalDate receiptDate,
    String memo,
    String idempotencyKey,
    @NotEmpty List<@Valid GoodsReceiptLineRequest> lines) {}
