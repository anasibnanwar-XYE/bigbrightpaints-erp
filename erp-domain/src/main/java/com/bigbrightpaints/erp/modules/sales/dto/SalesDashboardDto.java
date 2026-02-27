package com.bigbrightpaints.erp.modules.sales.dto;

import java.util.Map;

public record SalesDashboardDto(
        long activeDealers,
        long totalOrders,
        Map<String, Long> orderStatusBuckets,
        long pendingCreditRequests
) {}
