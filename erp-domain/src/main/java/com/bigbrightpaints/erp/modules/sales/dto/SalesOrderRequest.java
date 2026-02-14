package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;

public record SalesOrderRequest(
        Long dealerId,
        @NotNull BigDecimal totalAmount,
        String currency,
        String notes,
        @NotEmpty List<@Valid SalesOrderItemRequest> items,
        String gstTreatment,
        BigDecimal gstRate,
        Boolean gstInclusive,
        String idempotencyKey,
        String paymentMode
) {
    public SalesOrderRequest(
            Long dealerId,
            BigDecimal totalAmount,
            String currency,
            String notes,
            List<@Valid SalesOrderItemRequest> items,
            String gstTreatment,
            BigDecimal gstRate,
            Boolean gstInclusive,
            String idempotencyKey
    ) {
        this(dealerId, totalAmount, currency, notes, items, gstTreatment, gstRate, gstInclusive, idempotencyKey, null);
    }

    public String normalizedPaymentMode() {
        if (paymentMode == null || paymentMode.isBlank()) {
            return "CREDIT";
        }
        return paymentMode.trim().toUpperCase(Locale.ROOT);
    }

    public String resolveIdempotencyKey() {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey.trim();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(dealerId == null ? "null" : dealerId)
                .append('|').append(totalAmount)
                .append('|').append(currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT))
                .append('|').append(normalizedPaymentMode());
        for (SalesOrderItemRequest item : items) {
            sb.append('|')
                    .append(item.productCode() == null ? "" : item.productCode().trim().toUpperCase(Locale.ROOT))
                    .append(':').append(item.quantity())
                    .append(':').append(item.unitPrice());
        }
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(sb.toString());
    }
}
