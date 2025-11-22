package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;

public record AgingBucketDto(String label, int fromDays, Integer toDays, BigDecimal amount) {}
