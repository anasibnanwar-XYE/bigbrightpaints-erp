package com.bigbrightpaints.erp.modules.sales.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderRequest;

@Service
public class SalesOrderCrudService {

  private final SalesCoreEngine salesCoreEngine;
  private final SalesIdempotencyService salesIdempotencyService;

  public SalesOrderCrudService(
      SalesCoreEngine salesCoreEngine, SalesIdempotencyService salesIdempotencyService) {
    this.salesCoreEngine = salesCoreEngine;
    this.salesIdempotencyService = salesIdempotencyService;
  }

  public List<SalesOrderDto> listOrders(String status, int page, int size) {
    return salesCoreEngine.listOrders(status, page, size);
  }

  public List<SalesOrderDto> listOrders(String status, Long dealerId, int page, int size) {
    return salesCoreEngine.listOrders(status, dealerId, page, size);
  }

  public List<SalesOrderDto> listOrders(String status) {
    return salesCoreEngine.listOrders(status);
  }

  public List<SalesOrderDto> listOrders(String status, Long dealerId) {
    return salesCoreEngine.listOrders(status, dealerId);
  }

  public SalesOrderDto createOrder(SalesOrderRequest request) {
    return salesIdempotencyService.createOrderWithIdempotency(request);
  }

  public SalesOrderDto updateOrder(Long id, SalesOrderRequest request) {
    return salesCoreEngine.updateOrder(id, request);
  }

  public void deleteOrder(Long id) {
    salesCoreEngine.deleteOrder(id);
  }

  public SalesOrder getOrderWithItems(Long id) {
    return salesCoreEngine.getOrderWithItems(id);
  }
}
