package com.bigbrightpaints.erp.modules.portal.dto;

import java.util.List;

public record OperationsInsights(
        OperationsSummary summary,
        List<SupplyAlert> supplyAlerts,
        List<AutomationRun> automationRuns
) {
    public record OperationsSummary(
            double productionVelocity,
            double logisticsSla,
            String workingCapital
    ) {}

    public record SupplyAlert(
            String material,
            String status,
            String detail
    ) {}

    public record AutomationRun(
            String name,
            String state,
            String description
    ) {}
}
