package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record RawMaterialPurchaseRequest(
        @NotNull Long supplierId,
        @NotBlank String invoiceNumber,
        @NotNull LocalDate invoiceDate,
        String memo,
        Long purchaseOrderId,
        @NotNull Long goodsReceiptId,
        @PositiveOrZero BigDecimal taxAmount,
        @NotEmpty List<@Valid RawMaterialPurchaseLineRequest> lines
) {

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
            @JsonProperty("lines") List<@Valid RawMaterialPurchaseLineRequest> lines
    ) {
        String resolvedInvoiceNumber = resolveCanonicalWithAliases(
                "invoiceNumber",
                invoiceNumber,
                "invoiceNo",
                invoiceNo,
                "invoice_no",
                invoiceNoSnakeCase);
        Long resolvedGoodsReceiptId = resolveCanonicalWithAliases(
                "goodsReceiptId",
                goodsReceiptId,
                "goodsReceiptID",
                goodsReceiptIdUpperCase,
                "goods_receipt_id",
                goodsReceiptIdSnakeCase,
                "goodsReceipt",
                goodsReceiptLegacy,
                "grnId",
                grnId);

        return new RawMaterialPurchaseRequest(
                supplierId,
                resolvedInvoiceNumber,
                invoiceDate,
                memo,
                purchaseOrderId,
                resolvedGoodsReceiptId,
                taxAmount,
                lines
        );
    }

    private static String resolveCanonicalWithAliases(String canonicalName,
                                                      String canonicalValue,
                                                      String aliasAName,
                                                      String aliasAValue,
                                                      String aliasBName,
                                                      String aliasBValue) {
        String resolved = canonicalValue;
        resolved = mergeValue(canonicalName, resolved, aliasAName, aliasAValue);
        return mergeValue(canonicalName, resolved, aliasBName, aliasBValue);
    }

    private static Long resolveCanonicalWithAliases(String canonicalName,
                                                    Long canonicalValue,
                                                    String aliasAName,
                                                    Long aliasAValue,
                                                    String aliasBName,
                                                    Long aliasBValue,
                                                    String aliasCName,
                                                    Long aliasCValue,
                                                    String aliasDName,
                                                    Long aliasDValue) {
        Long resolved = canonicalValue;
        resolved = mergeValue(canonicalName, resolved, aliasAName, aliasAValue);
        resolved = mergeValue(canonicalName, resolved, aliasBName, aliasBValue);
        resolved = mergeValue(canonicalName, resolved, aliasCName, aliasCValue);
        return mergeValue(canonicalName, resolved, aliasDName, aliasDValue);
    }

    private static <T> T mergeValue(String canonicalName,
                                    T currentValue,
                                    String incomingField,
                                    T incomingValue) {
        if (incomingValue == null) {
            return currentValue;
        }
        if (currentValue == null) {
            return incomingValue;
        }
        if (!Objects.equals(currentValue, incomingValue)) {
            throw new IllegalArgumentException(
                    "Conflicting values provided for " + canonicalName + " and " + incomingField);
        }
        return currentValue;
    }
}
