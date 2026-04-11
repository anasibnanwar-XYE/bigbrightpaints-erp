package com.bigbrightpaints.erp.modules.accounting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

class JournalEntryStatusTest {

  @Test
  void from_blankDefaultsToDraft() {
    assertThat(JournalEntryStatus.from(" ")).isEqualTo(JournalEntryStatus.DRAFT);
  }

  @Test
  void from_invalidValueThrowsValidationException() {
    assertThatThrownBy(() -> JournalEntryStatus.from("legacy"))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex -> {
              ApplicationException applicationException = (ApplicationException) ex;
              assertThat(applicationException.getErrorCode())
                  .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
              assertThat(applicationException.getUserMessage())
                  .isEqualTo("Invalid journal entry status 'legacy'");
              assertThat(applicationException.getDetails()).containsEntry("status", "legacy");
            });
  }
}
