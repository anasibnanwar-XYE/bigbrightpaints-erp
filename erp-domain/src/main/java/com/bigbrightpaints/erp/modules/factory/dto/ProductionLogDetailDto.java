package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductionLogDetailDto(
        Long id,
        UUID publicId,
        String productionCode,
        Instant producedAt,
        String brandName,
        String productName,
        String skuCode,
        String batchColour,
        BigDecimal batchSize,
        String unitOfMeasure,
        BigDecimal producedQuantity,
        BigDecimal materialCostTotal,
        BigDecimal unitCost,
        String finishedGoodBatchCode,
        UUID finishedGoodBatchPublicId,
        String notes,
        String createdBy,
        List<ProductionLogMaterialDto> materials
) {}
