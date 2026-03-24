package com.bigbrightpaints.erp.modules.inventory.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Combined stock summary used by raw materials (aggregate counts) and finished goods (per-item snapshot).
 * Fields not relevant to a context can be null.
 */
public record StockSummaryDto(
    Long id,
    UUID publicId,
    String code,
    String name,
    BigDecimal currentStock,
    BigDecimal reservedStock,
    BigDecimal availableStock,
    BigDecimal weightedAverageCost,
    Long totalMaterials,
    Long lowStockMaterials,
    Long criticalStockMaterials,
    Long totalBatches) {}
