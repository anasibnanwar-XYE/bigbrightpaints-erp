package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;
import java.util.Map;

public record PackagingConsumptionResult(
        boolean mappingFound,
        BigDecimal totalCost,
        BigDecimal totalQuantity,
        Map<Long, BigDecimal> accountTotals,
        String warning
) {
    public boolean isConsumed() {
        return totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) > 0
                && totalCost != null && totalCost.compareTo(BigDecimal.ZERO) >= 0;
    }

    public BigDecimal totalCost() {
        return totalCost == null ? BigDecimal.ZERO : totalCost;
    }

    public BigDecimal quantity() {
        return totalQuantity == null ? BigDecimal.ZERO : totalQuantity;
    }

    public Map<Long, BigDecimal> accountTotalsOrEmpty() {
        return accountTotals == null ? Map.of() : accountTotals;
    }
}
