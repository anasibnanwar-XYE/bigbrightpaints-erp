package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryReconciliationReportDto(
    BigDecimal systemQuantityTotal,
    BigDecimal physicalQuantityTotal,
    BigDecimal quantityVarianceTotal,
    BigDecimal ledgerInventoryValue,
    BigDecimal physicalInventoryValue,
    BigDecimal valueVariance,
    List<InventoryReconciliationItemDto> items) {}
