package com.bigbrightpaints.erp.modules.sales.dto;

import java.util.List;

import com.bigbrightpaints.erp.shared.dto.ImportRowErrorDto;

public record DealerImportResponse(
    int successCount, int failureCount, List<ImportRowErrorDto> errors) {

  public DealerImportResponse {
    errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public int rowsProcessed() {
    return successCount + failureCount;
  }
}
