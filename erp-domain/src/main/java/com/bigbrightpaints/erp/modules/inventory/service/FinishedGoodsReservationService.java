package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import org.springframework.stereotype.Service;

@Service
public class FinishedGoodsReservationService {

    private final FinishedGoodsWorkflowService workflowService;

    public FinishedGoodsReservationService(FinishedGoodsWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public FinishedGoodsService.InventoryReservationResult reserveForOrder(SalesOrder order) {
        return workflowService.reserveForOrder(order);
    }

    public void releaseReservationsForOrder(Long orderId) {
        workflowService.releaseReservationsForOrder(orderId);
    }
}
