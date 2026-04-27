package com.bigbrightpaints.erp.modules.production.dto;

import java.util.List;

import com.bigbrightpaints.erp.shared.dto.ImportRowErrorDto;

public record CatalogImportResponse(
    int rowsProcessed,
    int brandsCreated,
    int productsCreated,
    int productsUpdated,
    int rawMaterialsSeeded,
    List<ImportRowErrorDto> errors) {}
