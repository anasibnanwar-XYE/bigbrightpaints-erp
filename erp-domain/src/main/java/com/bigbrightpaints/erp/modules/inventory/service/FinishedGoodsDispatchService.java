package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.dto.DispatchConfirmationRequest;
import com.bigbrightpaints.erp.modules.inventory.dto.DispatchConfirmationResponse;
import com.bigbrightpaints.erp.modules.inventory.dto.DispatchPreviewDto;
import com.bigbrightpaints.erp.modules.inventory.dto.PackagingSlipDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FinishedGoodsDispatchService {

    private final FinishedGoodsWorkflowEngineService workflowService;

    public FinishedGoodsDispatchService(FinishedGoodsWorkflowEngineService workflowService) {
        this.workflowService = workflowService;
    }

    public List<PackagingSlipDto> listPackagingSlips() {
        return workflowService.listPackagingSlips();
    }

    public List<FinishedGoodsService.DispatchPosting> markSlipDispatched(Long salesOrderId) {
        return workflowService.markSlipDispatched(salesOrderId);
    }

    public List<FinishedGoodsService.DispatchPosting> markSlipDispatched(Long salesOrderId, PackagingSlip slip) {
        return workflowService.markSlipDispatched(salesOrderId, slip);
    }

    public DispatchPreviewDto getDispatchPreview(Long packagingSlipId) {
        return workflowService.getDispatchPreview(packagingSlipId);
    }

    public DispatchConfirmationResponse confirmDispatch(DispatchConfirmationRequest request, String username) {
        return workflowService.confirmDispatch(request, username);
    }

    public DispatchConfirmationResponse getDispatchConfirmation(Long packagingSlipId) {
        return workflowService.getDispatchConfirmation(packagingSlipId);
    }

    public PackagingSlipDto getPackagingSlip(Long slipId) {
        return workflowService.getPackagingSlip(slipId);
    }

    public PackagingSlipDto getPackagingSlipByOrder(Long salesOrderId) {
        return workflowService.getPackagingSlipByOrder(salesOrderId);
    }

    public PackagingSlipDto updateSlipStatus(Long slipId, String newStatus) {
        return workflowService.updateSlipStatus(slipId, newStatus);
    }

    public PackagingSlipDto cancelBackorderSlip(Long slipId, String username, String reason) {
        return workflowService.cancelBackorderSlip(slipId, username, reason);
    }

    public void linkDispatchMovementsToJournal(Long packingSlipId, Long journalEntryId) {
        workflowService.linkDispatchMovementsToJournal(packingSlipId, journalEntryId);
    }
}
