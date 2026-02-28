package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

public record RawMaterialPurchaseLineResponse(Long rawMaterialId,
                                              String rawMaterialName,
                                              Long rawMaterialBatchId,
                                              String batchCode,
                                              BigDecimal quantity,
                                              String unit,
                                              BigDecimal costPerUnit,
                                              BigDecimal lineTotal,
                                              BigDecimal taxRate,
                                              BigDecimal taxAmount,
                                              String notes,
                                              BigDecimal cgstAmount,
                                              BigDecimal sgstAmount,
                                              BigDecimal igstAmount) {

    public RawMaterialPurchaseLineResponse(Long rawMaterialId,
                                           String rawMaterialName,
                                           Long rawMaterialBatchId,
                                           String batchCode,
                                           BigDecimal quantity,
                                           String unit,
                                           BigDecimal costPerUnit,
                                           BigDecimal lineTotal,
                                           BigDecimal taxRate,
                                           BigDecimal taxAmount,
                                           String notes) {
        this(rawMaterialId, rawMaterialName, rawMaterialBatchId, batchCode, quantity, unit, costPerUnit, lineTotal,
                taxRate, taxAmount, notes, null, null, null);
    }
}
