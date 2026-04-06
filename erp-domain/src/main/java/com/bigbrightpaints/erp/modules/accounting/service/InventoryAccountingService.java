package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.LandedCostRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.WipAdjustmentRequest;

@Service
public class InventoryAccountingService {

  @SuppressWarnings("unused")
  private Environment environment;

  private final AccountingCoreSupport accountingCoreSupport;

  @Autowired
  public InventoryAccountingService(AccountingCoreSupport accountingCoreSupport) {
    this.accountingCoreSupport = accountingCoreSupport;
  }

  public JournalEntryDto recordLandedCost(LandedCostRequest request) {
    return accountingCoreSupport.recordLandedCost(normalizeLandedCostRequest(request));
  }

  public JournalEntryDto revalueInventory(InventoryRevaluationRequest request) {
    return accountingCoreSupport.revalueInventory(normalizeInventoryRevaluationRequest(request));
  }

  public JournalEntryDto adjustWip(WipAdjustmentRequest request) {
    return accountingCoreSupport.adjustWip(normalizeWipAdjustmentRequest(request));
  }

  JournalEntryDto createJournalEntry(JournalEntryRequest request) {
    return accountingCoreSupport.createJournalEntry(request);
  }

  private LandedCostRequest normalizeLandedCostRequest(LandedCostRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.rawMaterialPurchaseId(), "rawMaterialPurchaseId");
    ValidationUtils.requireNotNull(request.inventoryAccountId(), "inventoryAccountId");
    ValidationUtils.requireNotNull(request.offsetAccountId(), "offsetAccountId");
    return new LandedCostRequest(
        request.rawMaterialPurchaseId(),
        ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.inventoryAccountId(),
        request.offsetAccountId(),
        request.entryDate(),
        normalizeText(request.memo()),
        normalizeText(request.referenceNumber()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private InventoryRevaluationRequest normalizeInventoryRevaluationRequest(
      InventoryRevaluationRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.inventoryAccountId(), "inventoryAccountId");
    ValidationUtils.requireNotNull(request.revaluationAccountId(), "revaluationAccountId");
    ValidationUtils.requireNotNull(request.deltaAmount(), "deltaAmount");
    ValidationUtils.requirePositive(request.deltaAmount().abs(), "deltaAmount");
    return new InventoryRevaluationRequest(
        request.inventoryAccountId(),
        request.revaluationAccountId(),
        request.deltaAmount(),
        normalizeText(request.memo()),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private WipAdjustmentRequest normalizeWipAdjustmentRequest(WipAdjustmentRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.productionLogId(), "productionLogId");
    ValidationUtils.requireNotNull(request.wipAccountId(), "wipAccountId");
    ValidationUtils.requireNotNull(request.inventoryAccountId(), "inventoryAccountId");
    ValidationUtils.requireNotNull(request.direction(), "direction");
    return new WipAdjustmentRequest(
        request.productionLogId(),
        ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.wipAccountId(),
        request.inventoryAccountId(),
        request.direction(),
        normalizeText(request.memo()),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private String normalizeText(String value) {
    String normalized = IdempotencyUtils.normalizeToken(value);
    return normalized.isBlank() ? null : normalized;
  }
}
