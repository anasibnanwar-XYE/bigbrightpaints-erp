package com.bigbrightpaints.erp.truthsuite.manufacturing;

import com.bigbrightpaints.erp.truthsuite.support.TruthSuiteFileAssert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("critical")
@Tag("reconciliation")
class TS_FactoryRawMaterialWacFallbackContractTest {

    private static final String PACKAGING_MATERIAL_SERVICE =
            "src/main/java/com/bigbrightpaints/erp/modules/factory/service/PackagingMaterialService.java";
    private static final String BULK_PACKING_SERVICE =
            "src/main/java/com/bigbrightpaints/erp/modules/factory/service/BulkPackingService.java";
    private static final String PRODUCTION_LOG_SERVICE =
            "src/main/java/com/bigbrightpaints/erp/modules/factory/service/ProductionLogService.java";

    @Test
    void packagingBulkAndProductionUseSharedWacSelectorWithNullFallback() {
        assertWacNullFallbackPattern(PACKAGING_MATERIAL_SERVICE);
        assertWacNullFallbackPattern(BULK_PACKING_SERVICE);
        assertWacNullFallbackPattern(PRODUCTION_LOG_SERVICE);
    }

    @Test
    void packagingAndProductionFallbackToBatchCostWhenWacUnavailable() {
        TruthSuiteFileAssert.assertContains(
                PACKAGING_MATERIAL_SERVICE,
                "weightedAverageCost != null",
                "Optional.ofNullable(batch.getCostPerUnit()).orElse(BigDecimal.ZERO)");
        TruthSuiteFileAssert.assertContains(
                PRODUCTION_LOG_SERVICE,
                "weightedAverageCost != null",
                "Optional.ofNullable(batch.getCostPerUnit()).orElse(BigDecimal.ZERO)");
        TruthSuiteFileAssert.assertContains(
                BULK_PACKING_SERVICE,
                "weightedAverageCost != null",
                "(batch.getCostPerUnit() != null ? batch.getCostPerUnit() : BigDecimal.ZERO)");
    }

    private void assertWacNullFallbackPattern(String relativePath) {
        TruthSuiteFileAssert.assertContains(
                relativePath,
                "CostingMethodUtils.selectWeightedAverageValue(",
                "() -> rawMaterialBatchRepository.calculateWeightedAverageCost(",
                "() -> null);");
    }
}
