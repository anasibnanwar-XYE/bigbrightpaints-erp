package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;

public record InventoryReconciliationDashboardDto(
    BigDecimal ledgerValue, BigDecimal physicalValue, BigDecimal variance, boolean balanced) {}
