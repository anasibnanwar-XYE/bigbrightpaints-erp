package com.bigbrightpaints.erp.modules.accounting.dto;

import jakarta.validation.constraints.NotNull;

public record PeriodStatusChangeRequest(
    @NotNull PeriodStatusAction action, Boolean force, String reason) {
  public PeriodStatusChangeRequest(Boolean force, String reason) {
    this(PeriodStatusAction.CLOSE, force, reason);
  }

  public PeriodStatusChangeRequest(String reason) {
    this(PeriodStatusAction.LOCK, null, reason);
  }

  public enum PeriodStatusAction {
    CLOSE,
    LOCK
  }
}
