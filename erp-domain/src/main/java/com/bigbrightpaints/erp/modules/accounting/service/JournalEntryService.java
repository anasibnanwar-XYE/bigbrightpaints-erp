package com.bigbrightpaints.erp.modules.accounting.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryReversalRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalListItemDto;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@Service
public class JournalEntryService {

  @SuppressWarnings("unused")
  private Environment environment;

  private final JournalQueryService journalQueryService;
  private final JournalPostingService journalPostingService;
  private final JournalReversalService journalReversalService;
  private final PeriodValidationService periodValidationService;
  private final ManualJournalService manualJournalService;
  private final ClosingEntryReversalService closingEntryReversalService;

  @Autowired
  public JournalEntryService(
      JournalQueryService journalQueryService,
      JournalPostingService journalPostingService,
      JournalReversalService journalReversalService,
      PeriodValidationService periodValidationService,
      ManualJournalService manualJournalService,
      ClosingEntryReversalService closingEntryReversalService) {
    this.journalQueryService = journalQueryService;
    this.journalPostingService = journalPostingService;
    this.journalReversalService = journalReversalService;
    this.periodValidationService = periodValidationService;
    this.manualJournalService = manualJournalService;
    this.closingEntryReversalService = closingEntryReversalService;
  }

  public List<JournalEntryDto> listJournalEntries(
      Long dealerId, Long supplierId, int page, int size) {
    return journalQueryService.listJournalEntries(dealerId, supplierId, page, size);
  }

  public List<JournalEntryDto> listJournalEntries(Long dealerId) {
    return journalQueryService.listJournalEntries(dealerId);
  }

  public List<JournalEntryDto> listJournalEntriesByReferencePrefix(String prefix) {
    return journalQueryService.listJournalEntriesByReferencePrefix(prefix);
  }

  @Transactional
  public JournalEntryDto createJournalEntry(JournalEntryRequest request) {
    return journalPostingService.createJournalEntry(request);
  }

  public JournalEntryDto createStandardJournal(JournalCreationRequest request) {
    return journalPostingService.createStandardJournal(request);
  }

  public PageResponse<JournalListItemDto> listJournals(
      LocalDate fromDate,
      LocalDate toDate,
      String journalType,
      String sourceModule,
      int page,
      int size) {
    return journalQueryService.listJournals(
        fromDate, toDate, journalType, sourceModule, page, size);
  }

  @Transactional
  public JournalEntryDto createManualJournalEntry(
      JournalEntryRequest request, String idempotencyKey) {
    return manualJournalService.createManualJournalEntry(request, idempotencyKey);
  }

  public JournalEntryDto reverseJournalEntry(Long entryId, JournalEntryReversalRequest request) {
    return journalReversalService.reverseJournalEntry(entryId, request);
  }

  JournalEntryDto reverseClosingEntryForPeriodReopen(
      JournalEntry entry, AccountingPeriod period, String reason) {
    return closingEntryReversalService.reverseClosingEntryForPeriodReopen(entry, period, reason);
  }
}
