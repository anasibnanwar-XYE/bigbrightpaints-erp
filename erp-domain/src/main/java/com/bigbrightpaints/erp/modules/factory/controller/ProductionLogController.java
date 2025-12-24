package com.bigbrightpaints.erp.modules.factory.controller;

import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogDetailDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogRequest;
import com.bigbrightpaints.erp.modules.factory.service.ProductionLogService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/factory/production/logs")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')")
public class ProductionLogController {

    private final ProductionLogService productionLogService;

    public ProductionLogController(ProductionLogService productionLogService) {
        this.productionLogService = productionLogService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductionLogDetailDto>> create(@Valid @RequestBody ProductionLogRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Production logged", productionLogService.createLog(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductionLogDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(productionLogService.recentLogs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductionLogDetailDto>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productionLogService.getLog(id)));
    }
}
