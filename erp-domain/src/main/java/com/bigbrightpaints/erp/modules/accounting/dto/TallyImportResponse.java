package com.bigbrightpaints.erp.modules.accounting.dto;

import java.util.List;

public record TallyImportResponse(
    int ledgersProcessed,
    int mappedLedgers,
    int accountsCreated,
    int openingVoucherEntriesProcessed,
    int openingBalanceRowsProcessed,
    List<String> unmappedGroups,
    List<String> unmappedItems,
    List<ImportError> errors) {
  public record ImportError(String context, String message) {}
}
