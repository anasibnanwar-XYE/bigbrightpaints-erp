package com.bigbrightpaints.erp.modules.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
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
        String idempotencyKey
) {
    public String resolveIdempotencyKey() {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            return idempotencyKey.trim();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(dealerId == null ? "null" : dealerId)
                .append('|').append(totalAmount)
                .append('|').append(currency == null ? "" : currency.trim().toUpperCase());
        for (SalesOrderItemRequest item : items) {
            sb.append('|')
                    .append(item.productCode() == null ? "" : item.productCode().trim().toUpperCase())
                    .append(':').append(item.quantity())
                    .append(':').append(item.unitPrice());
        }
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(sb.toString());
    }
}
