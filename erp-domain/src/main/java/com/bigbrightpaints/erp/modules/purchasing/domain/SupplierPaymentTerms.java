package com.bigbrightpaints.erp.modules.purchasing.domain;

public enum SupplierPaymentTerms {
  NET_30(30),
  NET_60(60),
  NET_90(90);

  private final int dueDays;

  SupplierPaymentTerms(int dueDays) {
    this.dueDays = dueDays;
  }

  public int dueDays() {
    return dueDays;
  }
}
