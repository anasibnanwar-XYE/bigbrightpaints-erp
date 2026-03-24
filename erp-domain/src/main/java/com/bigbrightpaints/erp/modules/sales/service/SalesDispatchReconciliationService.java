package com.bigbrightpaints.erp.modules.sales.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmRequest;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmResponse;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchMarkerReconciliationResponse;

@Service
public class SalesDispatchReconciliationService {

  private final SalesCoreEngine salesCoreEngine;

  public SalesDispatchReconciliationService(SalesCoreEngine salesCoreEngine) {
    this.salesCoreEngine = salesCoreEngine;
  }

  public DispatchConfirmResponse confirmDispatch(DispatchConfirmRequest request) {
    return salesCoreEngine.confirmDispatch(request);
  }

  public DispatchMarkerReconciliationResponse reconcileStaleOrderLevelMarkers(int limit) {
    return salesCoreEngine.reconcileStaleOrderLevelMarkers(limit);
  }
}
