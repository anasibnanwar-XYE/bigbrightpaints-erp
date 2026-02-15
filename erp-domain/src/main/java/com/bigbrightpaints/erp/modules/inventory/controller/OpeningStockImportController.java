package com.bigbrightpaints.erp.modules.inventory.controller;

import com.bigbrightpaints.erp.core.util.IdempotencyHeaderUtils;
import com.bigbrightpaints.erp.modules.inventory.dto.OpeningStockImportResponse;
import com.bigbrightpaints.erp.modules.inventory.service.OpeningStockImportService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inventory")
public class OpeningStockImportController {

    private final OpeningStockImportService openingStockImportService;

    public OpeningStockImportController(OpeningStockImportService openingStockImportService) {
        this.openingStockImportService = openingStockImportService;
    }

    @PostMapping(value = "/opening-stock", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')")
    public ResponseEntity<ApiResponse<OpeningStockImportResponse>> importOpeningStock(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String legacyIdempotencyKey,
            @RequestPart("file") MultipartFile file) {
        String resolvedIdempotencyKey = IdempotencyHeaderUtils.resolveHeaderKey(idempotencyKey, legacyIdempotencyKey);
        OpeningStockImportResponse response = openingStockImportService.importOpeningStock(file, resolvedIdempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Opening stock import processed", response));
    }
}
