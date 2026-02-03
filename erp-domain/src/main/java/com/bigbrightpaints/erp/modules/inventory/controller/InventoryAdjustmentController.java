package com.bigbrightpaints.erp.modules.inventory.controller;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.inventory.dto.InventoryAdjustmentDto;
import com.bigbrightpaints.erp.modules.inventory.dto.InventoryAdjustmentRequest;
import com.bigbrightpaints.erp.modules.inventory.service.InventoryAdjustmentService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/adjustments")
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService inventoryAdjustmentService;

    public InventoryAdjustmentController(InventoryAdjustmentService inventoryAdjustmentService) {
        this.inventoryAdjustmentService = inventoryAdjustmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<List<InventoryAdjustmentDto>>> listAdjustments() {
        return ResponseEntity.ok(ApiResponse.success(inventoryAdjustmentService.listAdjustments()));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<ApiResponse<InventoryAdjustmentDto>> createAdjustment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InventoryAdjustmentRequest request) {
        InventoryAdjustmentRequest resolved = applyIdempotencyKey(request, idempotencyKey);
        return ResponseEntity.ok(ApiResponse.success("Inventory adjustment posted",
                inventoryAdjustmentService.createAdjustment(resolved)));
    }

    private InventoryAdjustmentRequest applyIdempotencyKey(InventoryAdjustmentRequest request, String headerKey) {
        if (request == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Inventory adjustment request is required");
        }
        String bodyKey = request.idempotencyKey();
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
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "Idempotency-Key header is required");
        }
        return new InventoryAdjustmentRequest(
                request.adjustmentDate(),
                request.type(),
                request.adjustmentAccountId(),
                request.reason(),
                request.adminOverride(),
                headerKey.trim(),
                request.lines()
        );
    }
}
