package com.bigbrightpaints.erp.modules.production.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.production.dto.SkuReadinessDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkuReadinessServiceTest {

    @Mock
    private ProductionProductRepository productRepository;
    @Mock
    private FinishedGoodRepository finishedGoodRepository;
    @Mock
    private FinishedGoodBatchRepository finishedGoodBatchRepository;
    @Mock
    private RawMaterialRepository rawMaterialRepository;

    private SkuReadinessService service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new SkuReadinessService(
                productRepository,
                finishedGoodRepository,
                finishedGoodBatchRepository,
                rawMaterialRepository
        );
        company = new Company();
        company.setCode("ACME");
        company.setTimezone("UTC");
    }

    @Test
    void forProduct_requiresProduct() {
        assertThatThrownBy(() -> service.forProduct(company, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("product is required");
    }

    @Test
    void forSku_finishedGoodHappyPath_isFullyReady() {
        ProductionProduct product = finishedGoodProduct(" fg-1 ");
        product.setMetadata(Map.of("wipAccountId", "44"));
        FinishedGood finishedGood = finishedGood("FG-1", 11L, 22L, 33L, 44L);
        FinishedGoodBatch saleReadyBatch = new FinishedGoodBatch();
        saleReadyBatch.setQuantityAvailable(new BigDecimal("5"));

        when(productRepository.findByCompanyAndSkuCode(company, "FG-1")).thenReturn(Optional.of(product));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(finishedGood));
        when(rawMaterialRepository.findByCompanyAndSku(company, "FG-1")).thenReturn(Optional.empty());
        when(finishedGoodBatchRepository.findByFinishedGoodOrderByManufacturedAtAsc(finishedGood))
                .thenReturn(List.of(saleReadyBatch));

        SkuReadinessDto readiness = service.forSku(company, " fg-1 ", null);

        assertThat(readiness.sku()).isEqualTo("FG-1");
        assertThat(readiness.catalog().ready()).isTrue();
        assertThat(readiness.inventory().ready()).isTrue();
        assertThat(readiness.production().ready()).isTrue();
        assertThat(readiness.sales().ready()).isTrue();
        verify(productRepository).findByCompanyAndSkuCode(company, "FG-1");
    }

    @Test
    void forSku_finishedGoodWithoutMirrorAccounts_reportsInventoryProductionAndSalesBlockers() {
        ProductionProduct product = finishedGoodProduct("FG-2");
        FinishedGood finishedGood = finishedGood("FG-2", null, null, null, null);
        FinishedGoodBatch emptyBatch = new FinishedGoodBatch();
        emptyBatch.setQuantityAvailable(BigDecimal.ZERO);

        when(productRepository.findByCompanyAndSkuCode(company, "FG-2")).thenReturn(Optional.of(product));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "FG-2")).thenReturn(Optional.of(finishedGood));
        when(rawMaterialRepository.findByCompanyAndSku(company, "FG-2")).thenReturn(Optional.empty());
        when(finishedGoodBatchRepository.findByFinishedGoodOrderByManufacturedAtAsc(finishedGood))
                .thenReturn(List.of(emptyBatch));

        SkuReadinessDto readiness = service.forSku(company, "FG-2", null);

        assertThat(readiness.inventory().ready()).isFalse();
        assertThat(readiness.inventory().blockers()).containsExactlyInAnyOrder(
                "FINISHED_GOOD_VALUATION_ACCOUNT_MISSING",
                "FINISHED_GOOD_COGS_ACCOUNT_MISSING",
                "FINISHED_GOOD_REVENUE_ACCOUNT_MISSING",
                "FINISHED_GOOD_TAX_ACCOUNT_MISSING"
        );
        assertThat(readiness.production().blockers()).contains("WIP_ACCOUNT_MISSING");
        assertThat(readiness.sales().blockers()).contains("NO_FINISHED_GOOD_BATCH_STOCK");
    }

    @Test
    void forSku_rawMaterialExpectedType_reportsCategoryAndInventoryAccountProblems() {
        ProductionProduct product = finishedGoodProduct("RM-1");
        RawMaterial rawMaterial = rawMaterial("RM-1", null);

        when(productRepository.findByCompanyAndSkuCode(company, "RM-1")).thenReturn(Optional.of(product));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "RM-1")).thenReturn(Optional.empty());
        when(rawMaterialRepository.findByCompanyAndSku(company, "RM-1")).thenReturn(Optional.of(rawMaterial));

        SkuReadinessDto readiness = service.forSku(company, "RM-1", SkuReadinessService.ExpectedStockType.RAW_MATERIAL);

        assertThat(readiness.catalog().ready()).isTrue();
        assertThat(readiness.inventory().blockers()).containsExactlyInAnyOrder(
                "RAW_MATERIAL_CATEGORY_REQUIRED",
                "RAW_MATERIAL_INVENTORY_ACCOUNT_MISSING"
        );
        assertThat(readiness.production().blockers()).containsExactlyInAnyOrder(
                "RAW_MATERIAL_CATEGORY_REQUIRED",
                "RAW_MATERIAL_INVENTORY_ACCOUNT_MISSING"
        );
        assertThat(readiness.sales().blockers()).containsExactly("RAW_MATERIAL_SKU_NOT_SALES_ORDERABLE");
    }

    @Test
    void forSku_usesRawMaterialMirrorToInferRawMaterialTypeWhenProductMissing() {
        RawMaterial rawMaterial = rawMaterial("RM-2", 77L);

        when(productRepository.findByCompanyAndSkuCode(company, "RM-2")).thenReturn(Optional.empty());
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "RM-2")).thenReturn(Optional.empty());
        when(rawMaterialRepository.findByCompanyAndSku(company, "RM-2")).thenReturn(Optional.of(rawMaterial));

        SkuReadinessDto readiness = service.forSku(company, "rm-2", null);

        assertThat(readiness.sku()).isEqualTo("RM-2");
        assertThat(readiness.catalog().blockers()).containsExactly("PRODUCT_MASTER_MISSING");
        assertThat(readiness.inventory().ready()).isTrue();
        assertThat(readiness.production().blockers()).containsExactly("PRODUCT_MASTER_MISSING");
        assertThat(readiness.sales().blockers()).containsExactly("RAW_MATERIAL_SKU_NOT_SALES_ORDERABLE");
    }

    @Test
    void forSku_blankSku_defaultsToMissingFinishedGoodSnapshot() {
        SkuReadinessDto readiness = service.forSku(company, "   ", null);

        assertThat(readiness.sku()).isNull();
        assertThat(readiness.catalog().blockers()).containsExactly("PRODUCT_MASTER_MISSING");
        assertThat(readiness.inventory().blockers()).containsExactly("FINISHED_GOOD_MIRROR_MISSING");
        assertThat(readiness.production().blockers())
                .containsExactly("PRODUCT_MASTER_MISSING", "FINISHED_GOOD_MIRROR_MISSING", "WIP_ACCOUNT_MISSING");
        assertThat(readiness.sales().blockers())
                .containsExactly("PRODUCT_MASTER_MISSING", "FINISHED_GOOD_MIRROR_MISSING", "NO_FINISHED_GOOD_BATCH_STOCK");
    }

    @Test
    void forSku_rawMaterialExpectedType_reportsMirrorMissingWhenRawMaterialAbsent() {
        ProductionProduct product = finishedGoodProduct("RM-3");
        product.setCategory("RAW MATERIAL");

        when(productRepository.findByCompanyAndSkuCode(company, "RM-3")).thenReturn(Optional.of(product));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "RM-3")).thenReturn(Optional.empty());
        when(rawMaterialRepository.findByCompanyAndSku(company, "RM-3")).thenReturn(Optional.empty());

        SkuReadinessDto readiness = service.forSku(company, "RM-3", null);

        assertThat(readiness.inventory().blockers()).containsExactly("RAW_MATERIAL_MIRROR_MISSING");
        assertThat(readiness.production().blockers()).containsExactly("RAW_MATERIAL_MIRROR_MISSING");
        assertThat(readiness.sales().blockers()).containsExactly("RAW_MATERIAL_SKU_NOT_SALES_ORDERABLE");
    }

    @Test
    void forSku_finishedGoodExpectedType_overridesRawMaterialCategoryAndFlagsFinishedGoodRequirements() {
        ProductionProduct product = finishedGoodProduct("FG-RAW-CAT");
        product.setCategory("RAW_MATERIAL");

        when(productRepository.findByCompanyAndSkuCode(company, "FG-RAW-CAT")).thenReturn(Optional.of(product));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "FG-RAW-CAT")).thenReturn(Optional.empty());
        when(rawMaterialRepository.findByCompanyAndSku(company, "FG-RAW-CAT")).thenReturn(Optional.empty());

        SkuReadinessDto readiness = service.forSku(
                company,
                "FG-RAW-CAT",
                SkuReadinessService.ExpectedStockType.FINISHED_GOOD);

        assertThat(readiness.inventory().blockers())
                .containsExactlyInAnyOrder("FINISHED_GOOD_CATEGORY_REQUIRED", "FINISHED_GOOD_MIRROR_MISSING");
        assertThat(readiness.production().blockers())
                .containsExactlyInAnyOrder("FINISHED_GOOD_CATEGORY_REQUIRED", "FINISHED_GOOD_MIRROR_MISSING", "WIP_ACCOUNT_MISSING");
        assertThat(readiness.sales().blockers())
                .containsExactlyInAnyOrder("FINISHED_GOOD_CATEGORY_REQUIRED", "FINISHED_GOOD_MIRROR_MISSING", "NO_FINISHED_GOOD_BATCH_STOCK");
    }

    @Test
    void forProduct_blankSkuAndInvalidWipMetadata_fallsBackToMissingFinishedGoodSnapshot() {
        ProductionProduct product = finishedGoodProduct(null);
        product.setMetadata(Map.of("wipAccountId", "not-a-number"));

        SkuReadinessDto readiness = service.forProduct(company, product);

        assertThat(readiness.sku()).isNull();
        assertThat(readiness.inventory().blockers()).containsExactly("FINISHED_GOOD_MIRROR_MISSING");
        assertThat(readiness.production().blockers())
                .containsExactly("FINISHED_GOOD_MIRROR_MISSING", "WIP_ACCOUNT_MISSING");
        assertThat(readiness.sales().blockers())
                .containsExactly("FINISHED_GOOD_MIRROR_MISSING", "NO_FINISHED_GOOD_BATCH_STOCK");
    }

    @Test
    void forProduct_handlesNumericWipMetadataAndInactiveProduct() {
        ProductionProduct product = finishedGoodProduct("FG-3");
        product.setActive(false);
        product.setMetadata(Map.of("wipAccountId", 88L));
        FinishedGood finishedGood = finishedGood("FG-3", 11L, 22L, 33L, 44L);
        FinishedGoodBatch nullQuantityBatch = new FinishedGoodBatch();
        nullQuantityBatch.setQuantityAvailable(null);

        when(finishedGoodRepository.findByCompanyAndProductCode(company, "FG-3")).thenReturn(Optional.of(finishedGood));
        when(rawMaterialRepository.findByCompanyAndSku(company, "FG-3")).thenReturn(Optional.empty());
        when(finishedGoodBatchRepository.findByFinishedGoodOrderByManufacturedAtAsc(finishedGood))
                .thenReturn(List.of(nullQuantityBatch));

        SkuReadinessDto readiness = service.forProduct(company, product);

        assertThat(readiness.catalog().blockers()).containsExactly("PRODUCT_INACTIVE");
        assertThat(readiness.production().blockers()).doesNotContain("WIP_ACCOUNT_MISSING");
        assertThat(readiness.sales().blockers()).contains("PRODUCT_INACTIVE", "NO_FINISHED_GOOD_BATCH_STOCK");
    }

    private ProductionProduct finishedGoodProduct(String sku) {
        ProductionProduct product = new ProductionProduct();
        product.setCompany(company);
        product.setSkuCode(sku);
        product.setCategory("FINISHED_GOOD");
        product.setActive(true);
        return product;
    }

    private FinishedGood finishedGood(String sku,
                                      Long valuationAccountId,
                                      Long cogsAccountId,
                                      Long revenueAccountId,
                                      Long taxAccountId) {
        FinishedGood finishedGood = new FinishedGood();
        finishedGood.setCompany(company);
        finishedGood.setProductCode(sku);
        finishedGood.setName("Gloss Paint");
        finishedGood.setValuationAccountId(valuationAccountId);
        finishedGood.setCogsAccountId(cogsAccountId);
        finishedGood.setRevenueAccountId(revenueAccountId);
        finishedGood.setTaxAccountId(taxAccountId);
        return finishedGood;
    }

    private RawMaterial rawMaterial(String sku, Long inventoryAccountId) {
        RawMaterial rawMaterial = new RawMaterial();
        rawMaterial.setCompany(company);
        rawMaterial.setSku(sku);
        rawMaterial.setName("Resin");
        rawMaterial.setInventoryAccountId(inventoryAccountId);
        return rawMaterial;
    }
}
