package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierPaymentRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;

@Service
class SupplierPaymentService {

  private static final Logger log = LoggerFactory.getLogger(SupplierPaymentService.class);

  private final AccountingCoreSupport accountingCoreSupport;
  private final JournalEntryService journalEntryService;

  SupplierPaymentService(
      AccountingCoreSupport accountingCoreSupport, JournalEntryService journalEntryService) {
    this.accountingCoreSupport = accountingCoreSupport;
    this.journalEntryService = journalEntryService;
  }

  @Retryable(
      value = DataIntegrityViolationException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 50, maxDelay = 250, multiplier = 2.0))
  @Transactional
  JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request) {
    return recordSupplierPaymentInternal(request);
  }

  JournalEntryDto recordSupplierPaymentInternal(SupplierPaymentRequest request) {
    Company company = accountingCoreSupport.companyContextService.requireCurrentCompany();
    Supplier supplier =
        accountingCoreSupport
            .supplierRepository
            .lockByCompanyAndId(company, request.supplierId())
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE, "Supplier not found"));
    Account payableAccount = accountingCoreSupport.requireSupplierPayable(supplier);
    BigDecimal amount = ValidationUtils.requirePositive(request.amount(), "amount");
    List<SettlementAllocationRequest> allocations = request.allocations();
    accountingCoreSupport.validatePaymentAllocations(
        allocations, amount, "supplier payment", false);
    Account cashAccount =
        accountingCoreSupport.requireCashAccountForSettlement(
            company, request.cashAccountId(), "supplier payment", false);
    String memo =
        StringUtils.hasText(request.memo())
            ? request.memo().trim()
            : "Payment to supplier " + supplier.getName();
    String idempotencyKey =
        accountingCoreSupport.resolveReceiptIdempotencyKey(
            request.idempotencyKey(), request.referenceNumber(), "supplier payment");
    String reference =
        accountingCoreSupport.resolveSupplierPaymentReference(
            company, supplier, request.referenceNumber(), idempotencyKey);
    AccountingCoreSupport.IdempotencyReservation reservation =
        accountingCoreSupport.reserveReferenceMapping(
            company, idempotencyKey, reference, AccountingCoreSupport.ENTITY_TYPE_SUPPLIER_PAYMENT);

    if (!reservation.leader()) {
      JournalEntry existingEntry =
          accountingCoreSupport.awaitJournalEntry(company, reference, idempotencyKey);
      List<PartnerSettlementAllocation> existingAllocations =
          accountingCoreSupport.awaitAllocations(company, idempotencyKey);
      if (!existingAllocations.isEmpty()) {
        JournalEntry entry =
            accountingCoreSupport.resolveReplayJournalEntry(
                idempotencyKey, existingEntry, existingAllocations);
        accountingCoreSupport.linkReferenceMapping(
            company, idempotencyKey, entry, AccountingCoreSupport.ENTITY_TYPE_SUPPLIER_PAYMENT);
        accountingCoreSupport.validateSupplierPaymentIdempotency(
            idempotencyKey,
            supplier,
            cashAccount,
            payableAccount,
            amount,
            memo,
            entry,
            existingAllocations,
            allocations);
        return accountingCoreSupport.toDto(entry);
      }
      throw accountingCoreSupport.missingReservedPartnerAllocation(
          "Supplier payment", idempotencyKey, PartnerType.SUPPLIER, supplier.getId());
    }

    List<PartnerSettlementAllocation> existingAllocations =
        accountingCoreSupport.findAllocationsByIdempotencyKey(company, idempotencyKey);
    if (!existingAllocations.isEmpty()) {
      JournalEntry entry =
          accountingCoreSupport.resolveReplayJournalEntryFromExistingAllocations(
              company, reference, idempotencyKey, existingAllocations);
      accountingCoreSupport.linkReferenceMapping(
          company, idempotencyKey, entry, AccountingCoreSupport.ENTITY_TYPE_SUPPLIER_PAYMENT);
      accountingCoreSupport.validateSupplierPaymentIdempotency(
          idempotencyKey,
          supplier,
          cashAccount,
          payableAccount,
          amount,
          memo,
          entry,
          existingAllocations,
          allocations);
      return accountingCoreSupport.toDto(entry);
    }

    supplier.requireTransactionalUsage("record supplier payments");
    cashAccount =
        accountingCoreSupport.requireCashAccountForSettlement(
            company, request.cashAccountId(), "supplier payment", true);
    JournalEntryRequest payload =
        new JournalEntryRequest(
            reference,
            accountingCoreSupport.currentDate(company),
            memo,
            null,
            supplier.getId(),
            Boolean.FALSE,
            List.of(
                new JournalEntryRequest.JournalLineRequest(
                    payableAccount.getId(), memo, amount, BigDecimal.ZERO),
                new JournalEntryRequest.JournalLineRequest(
                    cashAccount.getId(), memo, BigDecimal.ZERO, amount)));
    JournalEntryDto entryDto = journalEntryService.createJournalEntry(payload);
    JournalEntry entry =
        accountingCoreSupport.accountingLookupService.requireJournalEntry(company, entryDto.id());
    accountingCoreSupport.linkReferenceMapping(
        company, idempotencyKey, entry, AccountingCoreSupport.ENTITY_TYPE_SUPPLIER_PAYMENT);
    existingAllocations =
        accountingCoreSupport.findAllocationsByIdempotencyKey(company, idempotencyKey);
    if (!existingAllocations.isEmpty()) {
      accountingCoreSupport.validateSupplierPaymentIdempotency(
          idempotencyKey,
          supplier,
          cashAccount,
          payableAccount,
          amount,
          memo,
          entry,
          existingAllocations,
          allocations);
      return entryDto;
    }

    LocalDate entryDate = entry.getEntryDate();
    List<PartnerSettlementAllocation> settlementRows = new ArrayList<>();
    List<RawMaterialPurchase> touchedPurchases = new ArrayList<>();
    Map<Long, BigDecimal> remainingByPurchase = new HashMap<>();
    Map<Long, RawMaterialPurchase> purchaseById = new HashMap<>();

    for (SettlementAllocationRequest allocation : allocations) {
      if (allocation.invoiceId() != null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Supplier payments cannot allocate to invoices");
      }
      BigDecimal applied =
          ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount");
      RawMaterialPurchase purchase = null;
      if (allocation.purchaseId() != null) {
        purchase =
            accountingCoreSupport
                .rawMaterialPurchaseRepository
                .lockByCompanyAndId(company, allocation.purchaseId())
                .orElseThrow(
                    () ->
                        new ApplicationException(
                            ErrorCode.VALIDATION_INVALID_REFERENCE,
                            "Raw material purchase not found"));
        if (purchase.getSupplier() == null
            || !purchase.getSupplier().getId().equals(supplier.getId())) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_REFERENCE, "Purchase does not belong to the supplier");
        }
        BigDecimal currentOutstanding =
            remainingByPurchase.getOrDefault(
                purchase.getId(), MoneyUtils.zeroIfNull(purchase.getOutstandingAmount()));
        if (applied.compareTo(currentOutstanding) > 0) {
          throw new ApplicationException(
                  ErrorCode.VALIDATION_INVALID_INPUT,
                  "Allocation exceeds purchase outstanding amount")
              .withDetail("purchaseId", purchase.getId())
              .withDetail("outstanding", currentOutstanding)
              .withDetail("applied", applied);
        }
        remainingByPurchase.put(
            purchase.getId(), currentOutstanding.subtract(applied).max(BigDecimal.ZERO));
        purchaseById.put(purchase.getId(), purchase);
      }

      PartnerSettlementAllocation row = new PartnerSettlementAllocation();
      row.setCompany(company);
      row.setPartnerType(PartnerType.SUPPLIER);
      row.setSupplier(supplier);
      row.setPurchase(purchase);
      row.setJournalEntry(entry);
      row.setSettlementDate(entryDate);
      row.setAllocationAmount(applied);
      row.setDiscountAmount(BigDecimal.ZERO);
      row.setWriteOffAmount(BigDecimal.ZERO);
      row.setFxDifferenceAmount(BigDecimal.ZERO);
      row.setIdempotencyKey(idempotencyKey);
      row.setMemo(allocation.memo());
      settlementRows.add(row);
    }
    try {
      accountingCoreSupport.settlementAllocationRepository.saveAll(settlementRows);
    } catch (DataIntegrityViolationException ex) {
      log.info(
          "Concurrent supplier payment allocation conflict for idempotency key hash={} detected;"
              + " retrying in fresh transaction",
          accountingCoreSupport.sanitizeIdempotencyLogValue(idempotencyKey));
      throw ex;
    }
    for (Map.Entry<Long, BigDecimal> entryState : remainingByPurchase.entrySet()) {
      RawMaterialPurchase purchase = purchaseById.get(entryState.getKey());
      if (purchase == null) {
        continue;
      }
      purchase.setOutstandingAmount(entryState.getValue().max(BigDecimal.ZERO));
      accountingCoreSupport.updatePurchaseStatus(purchase);
      touchedPurchases.add(purchase);
    }
    if (!touchedPurchases.isEmpty()) {
      accountingCoreSupport.rawMaterialPurchaseRepository.saveAll(touchedPurchases);
    }
    return entryDto;
  }
}
