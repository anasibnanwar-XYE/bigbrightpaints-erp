package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

public record PurchaseOrderLineResponse(
    Long rawMaterialId,
    String rawMaterialName,
    BigDecimal quantity,
    String unit,
    BigDecimal costPerUnit,
    BigDecimal lineTotal,
    String notes) {}
