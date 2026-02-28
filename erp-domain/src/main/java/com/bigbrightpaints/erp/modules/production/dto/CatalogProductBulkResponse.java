package com.bigbrightpaints.erp.modules.production.dto;

import java.util.List;

public record CatalogProductBulkResponse(
        int total,
        int succeeded,
        int failed,
        List<CatalogProductBulkItemResult> results
) {
}
