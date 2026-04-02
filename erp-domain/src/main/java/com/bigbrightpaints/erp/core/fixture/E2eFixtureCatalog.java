package com.bigbrightpaints.erp.core.fixture;

import java.math.BigDecimal;

public final class E2eFixtureCatalog {

  public static final String ORDER_PRIMARY_SKU = "E2E-PAINT-001";
  public static final String ORDER_PRIMARY_NAME = "E2E Primary Paint";
  public static final String ORDER_PRIMARY_BATCH_CODE = "E2E-PAINT-001-BATCH-1";
  public static final BigDecimal ORDER_PRIMARY_BASE_PRICE = new BigDecimal("100.00");
  public static final BigDecimal ORDER_PRIMARY_GST_RATE = BigDecimal.ZERO;
  public static final BigDecimal ORDER_PRIMARY_STOCK_QUANTITY = new BigDecimal("250");
  public static final BigDecimal ORDER_PRIMARY_UNIT_COST = new BigDecimal("12.50");

  private E2eFixtureCatalog() {}
}
