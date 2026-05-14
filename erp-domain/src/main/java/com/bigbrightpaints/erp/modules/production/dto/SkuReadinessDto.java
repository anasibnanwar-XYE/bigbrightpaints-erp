package com.bigbrightpaints.erp.modules.production.dto;

import java.util.List;

public record SkuReadinessDto(
    String sku,
    Stage masterReady,
    Stage inventoryReady,
    Stage productionReady,
    Stage packingReady,
    Stage salesReady,
    Stage accountingReady) {

  public Stage catalog() {
    return masterReady;
  }

  public Stage inventory() {
    return inventoryReady;
  }

  public Stage production() {
    return productionReady;
  }

  public Stage packing() {
    return packingReady;
  }

  public Stage sales() {
    return salesReady;
  }

  public Stage accounting() {
    return accountingReady;
  }

  public record Stage(boolean ready, List<String> blockers) {}
}
