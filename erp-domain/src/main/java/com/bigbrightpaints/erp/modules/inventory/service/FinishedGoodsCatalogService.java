package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodDto;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FinishedGoodsCatalogService {

    private final FinishedGoodsWorkflowService workflowService;

    public FinishedGoodsCatalogService(FinishedGoodsWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public List<FinishedGoodDto> listFinishedGoods() {
        return workflowService.listFinishedGoods();
    }

    public FinishedGoodDto getFinishedGood(Long id) {
        return workflowService.getFinishedGood(id);
    }

    public FinishedGoodDto updateFinishedGood(Long id, FinishedGoodRequest request) {
        return workflowService.updateFinishedGood(id, request);
    }

    public FinishedGoodDto createFinishedGood(FinishedGoodRequest request) {
        return workflowService.createFinishedGood(request);
    }

    public Map<String, FinishedGoodsService.FinishedGoodAccountingProfile> accountingProfiles(List<String> productCodes) {
        return workflowService.accountingProfiles(productCodes);
    }
}
