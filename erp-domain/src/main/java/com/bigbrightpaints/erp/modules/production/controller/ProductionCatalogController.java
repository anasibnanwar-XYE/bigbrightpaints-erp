package com.bigbrightpaints.erp.modules.production.controller;

import com.bigbrightpaints.erp.modules.production.dto.ProductionBrandDto;
import com.bigbrightpaints.erp.modules.production.dto.ProductionProductDto;
import com.bigbrightpaints.erp.modules.production.service.ProductionCatalogService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')")
public class ProductionCatalogController {

    private final ProductionCatalogService productionCatalogService;

    public ProductionCatalogController(ProductionCatalogService productionCatalogService) {
        this.productionCatalogService = productionCatalogService;
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<ProductionBrandDto>>> listBrands() {
        return ResponseEntity.ok(ApiResponse.success(productionCatalogService.listBrands()));
    }

    @GetMapping("/brands/{brandId}/products")
    public ResponseEntity<ApiResponse<List<ProductionProductDto>>> listBrandProducts(@PathVariable Long brandId) {
        return ResponseEntity.ok(ApiResponse.success(productionCatalogService.listBrandProducts(brandId)));
    }
}
