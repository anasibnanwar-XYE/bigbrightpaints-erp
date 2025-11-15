package com.bigbrightpaints.erp.modules.portal.dto;

import java.util.List;

public record DashboardInsights(
        List<HighlightMetric> highlights,
        List<PipelineStage> pipeline,
        List<HrPulseMetric> hrPulse
) {
    public record HighlightMetric(String label, String value, String detail) {}

    public record PipelineStage(String label, long count) {}

    public record HrPulseMetric(String label, String score, String context) {}
}
