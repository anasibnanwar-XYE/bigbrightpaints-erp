package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ManualJournalRequest(
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String narration,
    String idempotencyKey,
    Boolean adminOverride,
    List<LineRequest> lines,
    List<String> attachmentReferences) {
  public ManualJournalRequest(
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
      String narration,
      String idempotencyKey,
      Boolean adminOverride,
      List<LineRequest> lines) {
    this(entryDate, narration, idempotencyKey, adminOverride, lines, List.of());
  }

  public record LineRequest(
      Long accountId, BigDecimal amount, String narration, EntryType entryType) {}

  public enum EntryType {
    DEBIT,
    CREDIT
  }
}
