package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrder;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrderLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrderRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseOrderLineRequest;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseOrderResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseOrderRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseOrderService {

    private final CompanyContextService companyContextService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final CompanyEntityLookup companyEntityLookup;
    private final PurchaseResponseMapper responseMapper;

    public PurchaseOrderService(CompanyContextService companyContextService,
                                PurchaseOrderRepository purchaseOrderRepository,
                                RawMaterialRepository rawMaterialRepository,
                                CompanyEntityLookup companyEntityLookup,
                                PurchaseResponseMapper responseMapper) {
        this.companyContextService = companyContextService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.companyEntityLookup = companyEntityLookup;
        this.responseMapper = responseMapper;
    }

    public List<PurchaseOrderResponse> listPurchaseOrders() {
        return listPurchaseOrders(null);
    }

    public List<PurchaseOrderResponse> listPurchaseOrders(Long supplierId) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = supplierId != null ? companyEntityLookup.requireSupplier(company, supplierId) : null;
        List<PurchaseOrder> orders = supplier == null
                ? purchaseOrderRepository.findByCompanyWithLinesOrderByOrderDateDesc(company)
                : purchaseOrderRepository.findByCompanyAndSupplierWithLinesOrderByOrderDateDesc(company, supplier);
        return orders.stream()
                .map(responseMapper::toPurchaseOrderResponse)
                .toList();
    }

    public PurchaseOrderResponse getPurchaseOrder(Long id) {
        Company company = companyContextService.requireCurrentCompany();
        PurchaseOrder order = purchaseOrderRepository.findByCompanyAndId(company, id)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Purchase order not found"));
        return responseMapper.toPurchaseOrderResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = companyEntityLookup.requireSupplier(company, request.supplierId());

        String orderNumber = request.orderNumber().trim();
        purchaseOrderRepository.lockByCompanyAndOrderNumberIgnoreCase(company, orderNumber)
                .ifPresent(existing -> {
                    throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Order number already used for this company");
                });

        List<PurchaseOrderLineRequest> sortedLines = request.lines().stream()
                .sorted(java.util.Comparator.comparing(PurchaseOrderLineRequest::rawMaterialId))
                .toList();

        Map<Long, RawMaterial> lockedMaterials = new HashMap<>();
        Set<Long> seenMaterialIds = new HashSet<>();
        for (PurchaseOrderLineRequest lineRequest : sortedLines) {
            RawMaterial rawMaterial = requireMaterial(company, lineRequest.rawMaterialId());
            if (!seenMaterialIds.add(rawMaterial.getId())) {
                throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                        "Purchase order has duplicate raw material lines")
                        .withDetail("rawMaterialId", rawMaterial.getId());
            }
            lockedMaterials.put(rawMaterial.getId(), rawMaterial);
        }

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setCompany(company);
        purchaseOrder.setSupplier(supplier);
        purchaseOrder.setOrderNumber(orderNumber);
        purchaseOrder.setOrderDate(request.orderDate());
        purchaseOrder.setMemo(clean(request.memo()));

        for (PurchaseOrderLineRequest lineRequest : request.lines()) {
            RawMaterial rawMaterial = lockedMaterials.get(lineRequest.rawMaterialId());
            if (rawMaterial == null) {
                throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material not found");
            }
            BigDecimal quantity = positive(lineRequest.quantity(), "quantity");
            BigDecimal costPerUnit = positive(lineRequest.costPerUnit(), "costPerUnit");
            String unit = StringUtils.hasText(lineRequest.unit())
                    ? lineRequest.unit().trim()
                    : rawMaterial.getUnitType();
            BigDecimal lineTotal = currency(MoneyUtils.safeMultiply(quantity, costPerUnit));

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setPurchaseOrder(purchaseOrder);
            line.setRawMaterial(rawMaterial);
            line.setQuantity(quantity);
            line.setUnit(unit);
            line.setCostPerUnit(costPerUnit);
            line.setLineTotal(lineTotal);
            line.setNotes(clean(lineRequest.notes()));
            purchaseOrder.getLines().add(line);
        }

        PurchaseOrder saved = purchaseOrderRepository.save(purchaseOrder);
        return responseMapper.toPurchaseOrderResponse(saved);
    }

    private RawMaterial requireMaterial(Company company, Long rawMaterialId) {
        return rawMaterialRepository.lockByCompanyAndId(company, rawMaterialId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material not found"));
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Value for " + field + " must be greater than zero");
        }
        return value;
    }

    private BigDecimal currency(BigDecimal value) {
        return MoneyUtils.roundCurrency(value);
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
