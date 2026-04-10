package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.Locale;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

public enum JournalEntryStatus {
  DRAFT,
  POSTED,
  PAID,
  SETTLED,
  CLOSED,
  REVERSED,
  VOID,
  VOIDED,
  CANCELLED,
  BLOCKED,
  FAILED;

  public static JournalEntryStatus from(String value) {
    if (value == null || value.isBlank()) {
      return DRAFT;
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    try {
      return JournalEntryStatus.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Invalid journal entry status '" + value + "'")
          .withDetail("status", value);
    }
  }
}
