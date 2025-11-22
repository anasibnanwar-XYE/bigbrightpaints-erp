package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.util.List;

public record AgingSummaryResponse(Long partnerId,
                                   String partnerName,
                                   BigDecimal totalOutstanding,
                                   List<AgingBucketDto> buckets) {}
