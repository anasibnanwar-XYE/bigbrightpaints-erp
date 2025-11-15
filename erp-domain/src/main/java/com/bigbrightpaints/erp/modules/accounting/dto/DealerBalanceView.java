package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;

public record DealerBalanceView(Long dealerId, BigDecimal balance) {}
