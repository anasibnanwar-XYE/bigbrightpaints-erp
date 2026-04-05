package com.bigbrightpaints.erp.modules.accounting.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

final class AccountingDateParameters {

  private AccountingDateParameters() {}

  static LocalDate parseOptionalDate(String rawDate, String fieldName) {
    if (!StringUtils.hasText(rawDate)) {
      return null;
    }
    return parseRequiredDate(rawDate, fieldName);
  }

  static LocalDate parseRequiredDate(String rawDate, String fieldName) {
    String trimmed = rawDate == null ? null : rawDate.trim();
    try {
      return LocalDate.parse(trimmed);
    } catch (DateTimeParseException ex) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_DATE,
              "Invalid " + fieldName + " date format; expected ISO date yyyy-MM-dd")
          .withDetail(fieldName, rawDate);
    }
  }
}
