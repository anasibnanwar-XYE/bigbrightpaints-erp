package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.util.List;

import com.bigbrightpaints.erp.shared.dto.ImportRowErrorDto;

public record SupplierImportResponse(
    int successCount, int failureCount, List<ImportRowErrorDto> errors) {

  public SupplierImportResponse {
    errors = errors == null ? List.of() : List.copyOf(errors);
  }

  public int rowsProcessed() {
    return successCount + failureCount;
  }
}
