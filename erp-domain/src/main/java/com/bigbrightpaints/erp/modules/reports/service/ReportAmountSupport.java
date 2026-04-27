package com.bigbrightpaints.erp.modules.reports.service;

import java.math.BigDecimal;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;

final class ReportAmountSupport {

  private ReportAmountSupport() {}

  static boolean hasSummaryKey(Object[] row) {
    return row != null && row.length >= 3 && row[0] != null;
  }

  static BigDecimal debit(Object[] row) {
    return zeroIfNull((BigDecimal) row[1]);
  }

  static BigDecimal credit(Object[] row) {
    return zeroIfNull((BigDecimal) row[2]);
  }

  static BigDecimal naturalBalance(AccountType type, BigDecimal debit, BigDecimal credit) {
    if (type == null || type.isDebitNormalBalance()) {
      return zeroIfNull(debit).subtract(zeroIfNull(credit));
    }
    return zeroIfNull(credit).subtract(zeroIfNull(debit));
  }

  static BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
