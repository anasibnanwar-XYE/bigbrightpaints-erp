package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderStatusHistoryDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SalesOrderLifecycleService {

    private final SalesCoreEngine salesCoreEngine;

    public SalesOrderLifecycleService(SalesCoreEngine salesCoreEngine) {
        this.salesCoreEngine = salesCoreEngine;
    }

    public SalesOrderDto confirmOrder(Long id) {
        return salesCoreEngine.confirmOrder(id);
    }

    public SalesOrderDto cancelOrder(Long id, String reason) {
        return salesCoreEngine.cancelOrder(id, reason);
    }

    public SalesOrderDto updateStatus(Long id, String status) {
        return salesCoreEngine.updateStatus(id, status);
    }

    public SalesOrderDto updateStatusInternal(Long id, String status) {
        return salesCoreEngine.updateStatusInternal(id, status);
    }

    public void updateOrchestratorWorkflowStatus(Long id, String status) {
        salesCoreEngine.updateOrchestratorWorkflowStatus(id, status);
    }

    public boolean hasDispatchConfirmation(Long id) {
        return salesCoreEngine.hasDispatchConfirmation(id);
    }

    public void attachTraceId(Long id, String traceId) {
        salesCoreEngine.attachTraceId(id, traceId);
    }

    public List<SalesOrderStatusHistoryDto> orderTimeline(Long id) {
        return salesCoreEngine.orderTimeline(id);
    }
}
