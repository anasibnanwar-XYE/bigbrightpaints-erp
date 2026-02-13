package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RawMaterialPurchaseRequest(
        @NotNull Long supplierId,
        @JsonAlias({"invoiceNo", "invoice_no"}) @NotBlank String invoiceNumber,
        @NotNull LocalDate invoiceDate,
        String memo,
        Long purchaseOrderId,
        @JsonAlias({"goodsReceiptID", "goods_receipt_id", "goodsReceipt", "grnId"}) @NotNull Long goodsReceiptId,
        @PositiveOrZero BigDecimal taxAmount,
        @NotEmpty List<@Valid RawMaterialPurchaseLineRequest> lines
) {}
