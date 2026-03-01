package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.modules.sales.dto.CreditRequestDto;
import com.bigbrightpaints.erp.modules.sales.dto.CreditRequestRequest;
import com.bigbrightpaints.erp.modules.sales.dto.DealerDto;
import com.bigbrightpaints.erp.modules.sales.dto.DealerRequest;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SalesDealerCrudService {

    private final SalesCoreEngine salesCoreEngine;

    public SalesDealerCrudService(SalesCoreEngine salesCoreEngine) {
        this.salesCoreEngine = salesCoreEngine;
    }

    public List<DealerDto> listDealers() {
        return salesCoreEngine.listDealers();
    }

    public DealerDto createDealer(DealerRequest request) {
        return salesCoreEngine.createDealer(request);
    }

    public DealerDto updateDealer(Long id, DealerRequest request) {
        return salesCoreEngine.updateDealer(id, request);
    }

    public void deleteDealer(Long id) {
        salesCoreEngine.deleteDealer(id);
    }

    public List<CreditRequestDto> listCreditRequests() {
        return salesCoreEngine.listCreditRequests();
    }

    public CreditRequestDto createCreditRequest(CreditRequestRequest request) {
        return salesCoreEngine.createCreditRequest(request);
    }

    public CreditRequestDto updateCreditRequest(Long id, CreditRequestRequest request) {
        return salesCoreEngine.updateCreditRequest(id, request);
    }

    public CreditRequestDto approveCreditRequest(Long id, String decisionReason) {
        return salesCoreEngine.approveCreditRequest(id, decisionReason);
    }

    public CreditRequestDto rejectCreditRequest(Long id, String decisionReason) {
        return salesCoreEngine.rejectCreditRequest(id, decisionReason);
    }
}
