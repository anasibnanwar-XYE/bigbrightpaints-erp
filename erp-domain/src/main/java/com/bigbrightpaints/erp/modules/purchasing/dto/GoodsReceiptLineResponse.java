package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;

public record GoodsReceiptLineResponse(Long rawMaterialId,
                                       String rawMaterialName,
                                       String batchCode,
                                       BigDecimal quantity,
                                       String unit,
                                       BigDecimal costPerUnit,
                                       BigDecimal lineTotal,
                                       String notes) {}
