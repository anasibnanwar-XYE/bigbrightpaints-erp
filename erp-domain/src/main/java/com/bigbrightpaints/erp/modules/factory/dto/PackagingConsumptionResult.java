package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;

public record PackagingConsumptionResult(
        Long rawMaterialId,
        Long inventoryAccountId,
        BigDecimal quantityConsumed,
        BigDecimal cost,
        String rawMaterialSku,
        String warning
) {
    public boolean isConsumed() {
        return quantityConsumed != null && quantityConsumed.compareTo(BigDecimal.ZERO) > 0
                && cost != null && cost.compareTo(BigDecimal.ZERO) >= 0;
    }

    public BigDecimal totalCost() {
        return cost == null ? BigDecimal.ZERO : cost;
    }

    public BigDecimal quantity() {
        return quantityConsumed == null ? BigDecimal.ZERO : quantityConsumed;
    }
}
