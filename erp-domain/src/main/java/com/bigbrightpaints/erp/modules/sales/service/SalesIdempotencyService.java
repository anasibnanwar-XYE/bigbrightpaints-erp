package com.bigbrightpaints.erp.modules.sales.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderRequest;

@Service
public class SalesIdempotencyService {

  private final SalesCoreEngine salesCoreEngine;

  public SalesIdempotencyService(SalesCoreEngine salesCoreEngine) {
    this.salesCoreEngine = salesCoreEngine;
  }

  public SalesOrderDto createOrderWithIdempotency(SalesOrderRequest request) {
    return salesCoreEngine.createOrder(request);
  }
}
