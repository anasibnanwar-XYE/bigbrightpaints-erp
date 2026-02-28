package com.bigbrightpaints.erp.modules.invoice.dto;

import java.math.BigDecimal;

public record InvoiceLineDto(Long id,
                             String productCode,
                             String description,
                             BigDecimal quantity,
                             BigDecimal unitPrice,
                             BigDecimal taxRate,
                             BigDecimal lineTotal,
                             BigDecimal taxableAmount,
                             BigDecimal taxAmount,
                             BigDecimal discountAmount,
                             BigDecimal cgstAmount,
                             BigDecimal sgstAmount,
                             BigDecimal igstAmount) {

    public InvoiceLineDto(Long id,
                          String productCode,
                          String description,
                          BigDecimal quantity,
                          BigDecimal unitPrice,
                          BigDecimal taxRate,
                          BigDecimal lineTotal,
                          BigDecimal taxableAmount,
                          BigDecimal taxAmount,
                          BigDecimal discountAmount) {
        this(id, productCode, description, quantity, unitPrice, taxRate, lineTotal, taxableAmount, taxAmount,
                discountAmount, null, null, null);
    }
}
