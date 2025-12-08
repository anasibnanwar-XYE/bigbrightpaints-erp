package com.bigbrightpaints.erp.modules.production.dto;

import java.util.List;

public record BulkVariantResponse(
        int created,
        int skippedExisting,
        List<ProductionProductDto> variants
) {}
