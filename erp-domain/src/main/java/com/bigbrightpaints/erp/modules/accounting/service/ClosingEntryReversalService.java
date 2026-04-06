package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;

@Service
class ClosingEntryReversalService {

  private final AccountingCoreSupport accountingCoreSupport;

  ClosingEntryReversalService(AccountingCoreSupport accountingCoreSupport) {
    this.accountingCoreSupport = accountingCoreSupport;
  }

  JournalEntryDto reverseClosingEntryForPeriodReopen(
      JournalEntry entry, AccountingPeriod period, String reason) {
    return accountingCoreSupport.reverseClosingEntryForPeriodReopen(entry, period, reason);
  }
}
