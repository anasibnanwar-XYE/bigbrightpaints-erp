package com.bigbrightpaints.erp.modules.accounting.domain;

import java.math.BigDecimal;

public record DealerBalanceView(Long dealerId, BigDecimal balance) {}
