package com.bigbrightpaints.erp.modules.factory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductionLogDto(
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
        String createdBy,
        BigDecimal unitCost
) {}
