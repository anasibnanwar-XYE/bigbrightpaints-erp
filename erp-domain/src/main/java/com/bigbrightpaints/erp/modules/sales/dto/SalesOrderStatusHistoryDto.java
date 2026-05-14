package com.bigbrightpaints.erp.modules.sales.dto;

import java.time.Instant;

public record SalesOrderStatusHistoryDto(
    Long id,
    String fromStatus,
    String toStatus,
    String reasonCode,
    String reason,
    String changedBy,
    Instant changedAt) {}
