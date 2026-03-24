package com.bigbrightpaints.erp.modules.sales.dto;

import java.time.Instant;

public record SalesOrderSearchFilters(
    String status,
    Long dealerId,
    String orderNumber,
    Instant fromDate,
    Instant toDate,
    int page,
    int size) {}
