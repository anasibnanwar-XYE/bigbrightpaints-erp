package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.stereotype.Component;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Component
public class NoopPeriodCloseHook implements PeriodCloseHook {
  @Override
  public void onPeriodCloseLocked(Company company, AccountingPeriod period) {
    // no-op (used by tests to coordinate concurrency)
  }
}
