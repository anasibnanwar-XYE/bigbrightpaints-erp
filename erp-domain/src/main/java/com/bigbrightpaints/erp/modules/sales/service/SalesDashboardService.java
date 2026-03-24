package com.bigbrightpaints.erp.modules.sales.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.sales.dto.SalesDashboardDto;

@Service
public class SalesDashboardService {

  private final SalesCoreEngine salesCoreEngine;

  public SalesDashboardService(SalesCoreEngine salesCoreEngine) {
    this.salesCoreEngine = salesCoreEngine;
  }

  public SalesDashboardDto getDashboard() {
    return salesCoreEngine.getDashboard();
  }
}
