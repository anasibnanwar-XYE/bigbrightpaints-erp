package com.bigbrightpaints.erp.modules.sales.domain;

import java.math.BigDecimal;

public record DealerCreditExposureView(Long dealerId, BigDecimal exposure) {}
