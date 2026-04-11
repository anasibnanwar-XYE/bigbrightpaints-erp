package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;

record SettlementTotals(
    BigDecimal totalApplied,
    BigDecimal totalDiscount,
    BigDecimal totalWriteOff,
    BigDecimal totalFxGain,
    BigDecimal totalFxLoss) {}
