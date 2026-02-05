package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.company.domain.Company;

/**
 * Test hook for coordinating period close concurrency scenarios.
 * Default implementation is a no-op.
 */
public interface PeriodCloseHook {
    void onPeriodCloseLocked(Company company, AccountingPeriod period);
}
