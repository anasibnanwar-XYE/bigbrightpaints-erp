package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PackagingSlipLineDto(
    Long id,
    UUID batchPublicId,
    String batchCode,
    String productCode,
    String productName,
    BigDecimal orderedQuantity,
    BigDecimal shippedQuantity,
    BigDecimal backorderQuantity,
    BigDecimal quantity,
    BigDecimal unitCost,
    String notes) {}
