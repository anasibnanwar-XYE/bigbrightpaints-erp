package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;

@Service
class ClosingEntryReversalService {

  private final JournalReversalService journalReversalService;

  ClosingEntryReversalService(JournalReversalService journalReversalService) {
    this.journalReversalService = journalReversalService;
  }

  JournalEntryDto reverseClosingEntryForPeriodReopen(
      JournalEntry entry, AccountingPeriod period, String reason) {
    return journalReversalService.reverseClosingEntryForPeriodReopen(entry, period, reason);
  }
}
