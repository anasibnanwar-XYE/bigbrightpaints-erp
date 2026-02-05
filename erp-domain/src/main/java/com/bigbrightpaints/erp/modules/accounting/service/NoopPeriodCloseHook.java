package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.stereotype.Component;

@Component
public class NoopPeriodCloseHook implements PeriodCloseHook {
    @Override
    public void onPeriodCloseLocked(Company company, AccountingPeriod period) {
        // no-op (used by tests to coordinate concurrency)
    }
}
