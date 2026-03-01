package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.GstService;
import com.bigbrightpaints.erp.modules.accounting.service.ReferenceNumberService;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.inventory.service.RawMaterialService;
import com.bigbrightpaints.erp.modules.purchasing.domain.GoodsReceiptRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrderRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.dto.RawMaterialPurchaseRequest;
import com.bigbrightpaints.erp.modules.purchasing.dto.RawMaterialPurchaseResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PurchaseInvoiceService {

    private final PurchaseInvoiceEngine purchaseInvoiceEngine;

    public PurchaseInvoiceService(PurchaseInvoiceEngine purchaseInvoiceEngine) {
        this.purchaseInvoiceEngine = purchaseInvoiceEngine;
    }

    public PurchaseInvoiceService(CompanyContextService companyContextService,
                                  RawMaterialPurchaseRepository purchaseRepository,
                                  PurchaseOrderRepository purchaseOrderRepository,
                                  GoodsReceiptRepository goodsReceiptRepository,
                                  RawMaterialRepository rawMaterialRepository,
                                  RawMaterialBatchRepository rawMaterialBatchRepository,
                                  RawMaterialService rawMaterialService,
                                  RawMaterialMovementRepository movementRepository,
                                  AccountingFacade accountingFacade,
                                  CompanyEntityLookup companyEntityLookup,
                                  ReferenceNumberService referenceNumberService,
                                  CompanyClock companyClock,
                                  GstService gstService,
                                  PurchaseResponseMapper responseMapper,
                                  PurchaseTaxPolicy purchaseTaxPolicy) {
        this(new PurchaseInvoiceEngine(
                companyContextService,
                purchaseRepository,
                purchaseOrderRepository,
                goodsReceiptRepository,
                rawMaterialRepository,
                rawMaterialBatchRepository,
                rawMaterialService,
                movementRepository,
                accountingFacade,
                companyEntityLookup,
                referenceNumberService,
                companyClock,
                gstService,
                responseMapper,
                purchaseTaxPolicy
        ));
    }

    public List<RawMaterialPurchaseResponse> listPurchases() {
        return purchaseInvoiceEngine.listPurchases();
    }

    public List<RawMaterialPurchaseResponse> listPurchases(Long supplierId) {
        return purchaseInvoiceEngine.listPurchases(supplierId);
    }

    public RawMaterialPurchaseResponse getPurchase(Long id) {
        return purchaseInvoiceEngine.getPurchase(id);
    }

    public RawMaterialPurchaseResponse createPurchase(RawMaterialPurchaseRequest request) {
        return purchaseInvoiceEngine.createPurchase(request);
    }
}
