package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;

public record InventoryReconciliationItemDto(
    Long inventoryItemId,
    String code,
    String name,
    BigDecimal systemQty,
    BigDecimal physicalQty,
    BigDecimal variance) {}
