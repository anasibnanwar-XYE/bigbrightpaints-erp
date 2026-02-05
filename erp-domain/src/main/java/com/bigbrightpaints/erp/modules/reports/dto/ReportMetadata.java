package com.bigbrightpaints.erp.modules.reports.dto;

import java.time.LocalDate;

public record ReportMetadata(
        LocalDate asOfDate,
        ReportSource source,
        Long accountingPeriodId,
        String accountingPeriodStatus,
        Long snapshotId
) {
}
