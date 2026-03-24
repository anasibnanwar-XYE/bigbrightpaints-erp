package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    @NotEmpty List<@Valid RawMaterialPurchaseLineRequest> lines) {

  private static final String INVOICE_NUMBER_FIELD = "invoiceNumber";
  private static final String GOODS_RECEIPT_ID_FIELD = "goodsReceiptId";

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public static RawMaterialPurchaseRequest fromJson(
      @JsonProperty("supplierId") Long supplierId,
      @JsonProperty("invoiceNumber") String invoiceNumber,
      @JsonProperty("invoiceNo") String invoiceNo,
      @JsonProperty("invoice_no") String invoiceNoSnakeCase,
      @JsonProperty("invoiceDate") LocalDate invoiceDate,
      @JsonProperty("memo") String memo,
      @JsonProperty("purchaseOrderId") Long purchaseOrderId,
      @JsonProperty("goodsReceiptId") Long goodsReceiptId,
      @JsonProperty("goodsReceiptID") Long goodsReceiptIdUpperCase,
      @JsonProperty("goods_receipt_id") Long goodsReceiptIdSnakeCase,
      @JsonProperty("goodsReceipt") Long goodsReceiptLegacy,
      @JsonProperty("grnId") Long grnId,
      @JsonProperty("taxAmount") BigDecimal taxAmount,
      @JsonProperty("lines") List<@Valid RawMaterialPurchaseLineRequest> lines) {
    rejectLegacyAliasValue("invoiceNo", invoiceNo, INVOICE_NUMBER_FIELD);
    rejectLegacyAliasValue("invoice_no", invoiceNoSnakeCase, INVOICE_NUMBER_FIELD);
    rejectLegacyAliasValue("goodsReceiptID", goodsReceiptIdUpperCase, GOODS_RECEIPT_ID_FIELD);
    rejectLegacyAliasValue("goods_receipt_id", goodsReceiptIdSnakeCase, GOODS_RECEIPT_ID_FIELD);
    rejectLegacyAliasValue("goodsReceipt", goodsReceiptLegacy, GOODS_RECEIPT_ID_FIELD);
    rejectLegacyAliasValue("grnId", grnId, GOODS_RECEIPT_ID_FIELD);

    return new RawMaterialPurchaseRequest(
        supplierId,
        invoiceNumber,
        invoiceDate,
        memo,
        purchaseOrderId,
        goodsReceiptId,
        taxAmount,
        lines);
  }

  private static void rejectLegacyAliasValue(
      String aliasField, String aliasValue, String canonicalField) {
    if (aliasValue == null || aliasValue.isBlank()) {
      return;
    }
    throw new IllegalArgumentException(
        "Legacy field " + aliasField + " is not supported; use " + canonicalField);
  }

  private static void rejectLegacyAliasValue(
      String aliasField, Long aliasValue, String canonicalField) {
    if (aliasValue != null) {
      throw new IllegalArgumentException(
          "Legacy field " + aliasField + " is not supported; use " + canonicalField);
    }
  }
}
