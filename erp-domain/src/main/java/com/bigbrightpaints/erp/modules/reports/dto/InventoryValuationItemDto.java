package com.bigbrightpaints.erp.modules.reports.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InventoryValuationItemDto(
    Long inventoryItemId,
    String inventoryType,
    String code,
    String name,
    String category,
    String brand,
    BigDecimal quantityOnHand,
    BigDecimal reservedQuantity,
    BigDecimal availableQuantity,
    BigDecimal unitCost,
    BigDecimal totalValue,
    boolean lowStock) {

  @JsonProperty("itemId")
  public Long itemId() {
    return inventoryItemId;
  }

  @JsonProperty("quantity")
  public BigDecimal quantity() {
    return quantityOnHand;
  }
}
