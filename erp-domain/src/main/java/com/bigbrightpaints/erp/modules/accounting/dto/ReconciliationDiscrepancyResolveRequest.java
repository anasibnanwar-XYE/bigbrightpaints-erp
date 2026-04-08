package com.bigbrightpaints.erp.modules.accounting.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.ReconciliationDiscrepancyResolution;

import jakarta.validation.constraints.NotNull;

public record ReconciliationDiscrepancyResolveRequest(
    @NotNull ReconciliationDiscrepancyResolution resolution,
    String note,
    Long adjustmentAccountId,
    Long journalEntryId) {

  public ReconciliationDiscrepancyResolveRequest(
      ReconciliationDiscrepancyResolution resolution, String note, Long adjustmentAccountId) {
    this(resolution, note, adjustmentAccountId, null);
  }
}
