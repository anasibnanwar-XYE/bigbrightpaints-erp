package com.bigbrightpaints.erp.modules.factory.controller;

import com.bigbrightpaints.erp.modules.factory.dto.*;
import com.bigbrightpaints.erp.modules.factory.service.BulkPackingService;
import com.bigbrightpaints.erp.modules.factory.service.PackingService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/factory")
@PreAuthorize("hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')")
public class PackingController {

    private final PackingService packingService;
    private final BulkPackingService bulkPackingService;

    public PackingController(PackingService packingService, BulkPackingService bulkPackingService) {
        this.packingService = packingService;
        this.bulkPackingService = bulkPackingService;
    }

    @PostMapping("/packing-records")
    public ResponseEntity<ApiResponse<ProductionLogDetailDto>> recordPacking(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PackingRequest request) {
        PackingRequest resolved = applyIdempotencyKey(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Packing recorded", packingService.recordPacking(resolved)));
    }

    @PostMapping("/packing-records/{productionLogId}/complete")
    public ResponseEntity<ApiResponse<ProductionLogDetailDto>> completePacking(@PathVariable Long productionLogId) {
        return ResponseEntity.ok(ApiResponse.success("Packing completed", packingService.completePacking(productionLogId)));
    }

    @GetMapping("/unpacked-batches")
    public ResponseEntity<ApiResponse<List<UnpackedBatchDto>>> listUnpackedBatches() {
        return ResponseEntity.ok(ApiResponse.success(packingService.listUnpackedBatches()));
    }

    @GetMapping("/production-logs/{productionLogId}/packing-history")
    public ResponseEntity<ApiResponse<List<PackingRecordDto>>> packingHistory(@PathVariable Long productionLogId) {
        return ResponseEntity.ok(ApiResponse.success(packingService.packingHistory(productionLogId)));
    }

    // ===== Bulk-to-Size Packaging =====

    /**
     * Pack a bulk FG batch into sized child SKUs.
     * Converts parent SKU (e.g., Safari-WHITE) into child SKUs (Safari-WHITE-1L, Safari-WHITE-4L).
     */
    @PostMapping("/pack")
    @PreAuthorize("hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<BulkPackResponse>> packBulkToSizes(@Valid @RequestBody BulkPackRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bulk packed into sizes", bulkPackingService.pack(request)));
    }

    /**
     * List available bulk batches for a finished good.
     */
    @GetMapping("/bulk-batches/{finishedGoodId}")
    @PreAuthorize("hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<BulkPackResponse.ChildBatchDto>>> listBulkBatches(
            @PathVariable Long finishedGoodId) {
        return ResponseEntity.ok(ApiResponse.success(bulkPackingService.listBulkBatches(finishedGoodId)));
    }

    /**
     * List child batches created from a parent bulk batch.
     */
    @GetMapping("/bulk-batches/{parentBatchId}/children")
    @PreAuthorize("hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<BulkPackResponse.ChildBatchDto>>> listChildBatches(
            @PathVariable Long parentBatchId) {
        return ResponseEntity.ok(ApiResponse.success(bulkPackingService.listChildBatches(parentBatchId)));
    }

    private PackingRequest applyIdempotencyKey(PackingRequest request, String headerKey) {
        if (request == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Packing request is required");
        }
        String bodyKey = request.idempotencyKey();
        if (!StringUtils.hasText(bodyKey) && !StringUtils.hasText(headerKey)) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Idempotency-Key header or request.idempotencyKey is required");
        }
        if (StringUtils.hasText(bodyKey)) {
            if (StringUtils.hasText(headerKey) && !bodyKey.trim().equals(headerKey.trim())) {
                throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                        "Idempotency key mismatch between header and request body")
                        .withDetail("headerKey", headerKey)
                        .withDetail("bodyKey", bodyKey);
            }
            return request;
        }
        if (!StringUtils.hasText(headerKey)) {
            return request;
        }
        return new PackingRequest(
                request.productionLogId(),
                request.packedDate(),
                request.packedBy(),
                headerKey.trim(),
                request.lines()
        );
    }
}
