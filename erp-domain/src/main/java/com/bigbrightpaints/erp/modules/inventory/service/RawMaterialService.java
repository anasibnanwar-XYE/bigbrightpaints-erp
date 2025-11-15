package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.*;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RawMaterialService {

    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialBatchRepository batchRepository;
    private final CompanyContextService companyContextService;
    private final ProductionProductRepository productionProductRepository;
    private final ProductionBrandRepository productionBrandRepository;

    public RawMaterialService(RawMaterialRepository rawMaterialRepository,
                              RawMaterialBatchRepository batchRepository,
                              CompanyContextService companyContextService,
                              ProductionProductRepository productionProductRepository,
                              ProductionBrandRepository productionBrandRepository) {
        this.rawMaterialRepository = rawMaterialRepository;
        this.batchRepository = batchRepository;
        this.companyContextService = companyContextService;
        this.productionProductRepository = productionProductRepository;
        this.productionBrandRepository = productionBrandRepository;
    }

    public List<RawMaterialDto> listRawMaterials() {
        Company company = companyContextService.requireCurrentCompany();
        return rawMaterialRepository.findByCompanyOrderByNameAsc(company).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RawMaterialDto createRawMaterial(RawMaterialRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        RawMaterial material = new RawMaterial();
        material.setCompany(company);
        material.setName(request.name());
        material.setSku(request.sku());
        material.setUnitType(request.unitType());
        material.setReorderLevel(request.reorderLevel());
        material.setMinStock(request.minStock());
        material.setMaxStock(request.maxStock());
        material.setInventoryAccountId(request.inventoryAccountId());
        RawMaterial saved = rawMaterialRepository.save(material);
        syncProductFromMaterial(company, saved);
        return toDto(saved);
    }

    @Transactional
    public RawMaterialDto updateRawMaterial(Long id, RawMaterialRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        RawMaterial material = rawMaterialRepository.findByCompanyAndId(company, id)
                .orElseThrow(() -> new IllegalArgumentException("Raw material not found"));
        material.setName(request.name());
        material.setSku(request.sku());
        material.setUnitType(request.unitType());
        material.setReorderLevel(request.reorderLevel());
        material.setMinStock(request.minStock());
        material.setMaxStock(request.maxStock());
        material.setInventoryAccountId(request.inventoryAccountId());
        RawMaterial saved = rawMaterialRepository.save(material);
        syncProductFromMaterial(company, saved);
        return toDto(saved);
    }

    @Transactional
    public void deleteRawMaterial(Long id) {
        Company company = companyContextService.requireCurrentCompany();
        RawMaterial material = rawMaterialRepository.findByCompanyAndId(company, id)
                .orElseThrow(() -> new IllegalArgumentException("Raw material not found"));
        rawMaterialRepository.delete(material);
    }

    public StockSummaryDto summarizeStock() {
        Company company = companyContextService.requireCurrentCompany();
        List<RawMaterial> materials = rawMaterialRepository.findByCompanyOrderByNameAsc(company);
        long total = materials.size();
        long lowStock = materials.stream().filter(this::isLowStock).count();
        long criticalStock = materials.stream().filter(this::isCriticalStock).count();
        long batches = materials.stream()
                .map(rawMaterial -> batchRepository.findByRawMaterial(rawMaterial).size())
                .mapToLong(Integer::longValue)
                .sum();
        return new StockSummaryDto(total, lowStock, criticalStock, batches);
    }

    public List<InventoryStockSnapshot> listInventory() {
        Company company = companyContextService.requireCurrentCompany();
        return rawMaterialRepository.findByCompanyOrderByNameAsc(company).stream()
                .map(this::toSnapshot)
                .toList();
    }

    public List<InventoryStockSnapshot> listLowStock() {
        return listInventory().stream()
                .filter(snapshot -> "LOW_STOCK".equals(snapshot.status()) || "CRITICAL".equals(snapshot.status()))
                .toList();
    }

    public List<RawMaterialBatchDto> listBatches(Long rawMaterialId) {
        RawMaterial material = requireMaterial(rawMaterialId);
        return batchRepository.findByRawMaterial(material).stream()
                .sorted(Comparator.comparing(RawMaterialBatch::getReceivedAt).reversed())
                .map(this::toBatchDto)
                .toList();
    }

    @Transactional
    public RawMaterialBatchDto createBatch(Long rawMaterialId, RawMaterialBatchRequest request) {
        RawMaterial material = requireMaterial(rawMaterialId);
        RawMaterialBatch batch = new RawMaterialBatch();
        batch.setRawMaterial(material);
        batch.setBatchCode(request.batchCode());
        batch.setQuantity(request.quantity());
        batch.setUnit(request.unit());
        batch.setCostPerUnit(request.costPerUnit());
        batch.setSupplier(request.supplier());
        batch.setNotes(request.notes());
        material.setCurrentStock(material.getCurrentStock().add(request.quantity()));
        rawMaterialRepository.save(material);
        return toBatchDto(batchRepository.save(batch));
    }

    @Transactional
    public RawMaterialBatchDto intake(RawMaterialIntakeRequest request) {
        return createBatch(request.rawMaterialId(), new RawMaterialBatchRequest(
                request.batchCode(),
                request.quantity(),
                request.unit(),
                request.costPerUnit(),
                request.supplier(),
                request.notes()
        ));
    }

    private RawMaterial requireMaterial(Long rawMaterialId) {
        Company company = companyContextService.requireCurrentCompany();
        return rawMaterialRepository.findByCompanyAndId(company, rawMaterialId)
                .orElseThrow(() -> new IllegalArgumentException("Raw material not found"));
    }

    private RawMaterialDto toDto(RawMaterial material) {
        return new RawMaterialDto(material.getId(), material.getPublicId(), material.getName(), material.getSku(),
                material.getUnitType(), material.getReorderLevel(), material.getCurrentStock(),
                material.getMinStock(), material.getMaxStock(), stockStatus(material), material.getInventoryAccountId());
    }

    private RawMaterialBatchDto toBatchDto(RawMaterialBatch batch) {
        return new RawMaterialBatchDto(batch.getId(), batch.getPublicId(), batch.getBatchCode(), batch.getQuantity(),
                batch.getUnit(), batch.getCostPerUnit(), batch.getSupplier(), batch.getReceivedAt(), batch.getNotes());
    }

    private InventoryStockSnapshot toSnapshot(RawMaterial material) {
        return new InventoryStockSnapshot(material.getName(), material.getSku(), material.getCurrentStock(),
                material.getReorderLevel(), stockStatus(material));
    }

    private String stockStatus(RawMaterial material) {
        if (material.getCurrentStock().compareTo(material.getMinStock()) <= 0) {
            return "CRITICAL";
        }
        if (isLowStock(material)) {
            return "LOW_STOCK";
        }
        return "IN_STOCK";
    }

    private boolean isLowStock(RawMaterial material) {
        return material.getCurrentStock().compareTo(material.getReorderLevel()) < 0;
    }

    private boolean isCriticalStock(RawMaterial material) {
        return material.getCurrentStock().compareTo(material.getMinStock()) <= 0;
    }

    private void syncProductFromMaterial(Company company, RawMaterial material) {
        if (!StringUtils.hasText(material.getSku())) {
            return;
        }
        ProductionProduct product = productionProductRepository.findByCompanyAndSkuCode(company, material.getSku())
                .orElseGet(() -> {
                    ProductionProduct created = new ProductionProduct();
                    created.setCompany(company);
                    created.setBrand(resolveRawMaterialBrand(company));
                    created.setProductName(material.getName());
                    created.setCategory("RAW_MATERIAL");
                    created.setUnitOfMeasure(material.getUnitType());
                    created.setSkuCode(material.getSku());
                    created.setBasePrice(BigDecimal.ZERO);
                    created.setGstRate(BigDecimal.ZERO);
                    created.setMinDiscountPercent(BigDecimal.ZERO);
                    created.setMinSellingPrice(BigDecimal.ZERO);
                    created.setMetadata(new HashMap<>());
                    return created;
                });
        product.setProductName(material.getName());
        product.setCategory("RAW_MATERIAL");
        product.setUnitOfMeasure(material.getUnitType());
        if (product.getBrand() == null) {
            product.setBrand(resolveRawMaterialBrand(company));
        }
        Map<String, Object> metadata = product.getMetadata() == null ? new HashMap<>() : new HashMap<>(product.getMetadata());
        metadata.put("linkedRawMaterialId", material.getId());
        metadata.put("linkedRawMaterialSku", material.getSku());
        product.setMetadata(metadata);
        productionProductRepository.save(product);
    }

    private ProductionBrand resolveRawMaterialBrand(Company company) {
        return productionBrandRepository.findByCompanyAndCodeIgnoreCase(company, "RAW-MATERIALS")
                .orElseGet(() -> {
                    ProductionBrand brand = new ProductionBrand();
                    brand.setCompany(company);
                    brand.setCode("RAW-MATERIALS");
                    brand.setName("Raw Materials");
                    return productionBrandRepository.save(brand);
                });
    }
}
