package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationApplication;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;

@Service
class SettlementAllocationResolutionService {

  private static final BigDecimal ALLOCATION_TOLERANCE = new BigDecimal("0.01");

  private final InvoiceRepository invoiceRepository;
  private final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;
  private final JournalReplayService journalReplayService;

  SettlementAllocationResolutionService(
      InvoiceRepository invoiceRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      JournalReplayService journalReplayService) {
    this.invoiceRepository = invoiceRepository;
    this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    this.journalReplayService = journalReplayService;
  }

  List<SettlementAllocationRequest> resolveDealerSettlementAllocations(
      Company company,
      Dealer dealer,
      PartnerSettlementRequest request,
      String replayIdempotencyKey) {
    List<SettlementAllocationRequest> provided = request != null ? request.allocations() : null;
    if (provided != null && !provided.isEmpty()) {
      validateOptionalHeaderSettlementAmount("dealer", request.amount(), provided);
      return provided;
    }
    List<SettlementAllocationRequest> replayAllocations =
        replayAllocations(company, replayIdempotencyKey);
    if (!replayAllocations.isEmpty()) {
      return replayAllocations;
    }
    BigDecimal amount = resolveDealerHeaderSettlementAmount(request);
    SettlementAllocationApplication unappliedApplication =
        request != null
            ? normalizeRequestedUnappliedApplication(request.unappliedAmountApplication())
            : null;
    return buildDealerHeaderSettlementAllocations(company, dealer, amount, unappliedApplication);
  }

  List<SettlementAllocationRequest> resolveSupplierSettlementAllocations(
      Company company,
      Supplier supplier,
      PartnerSettlementRequest request,
      String replayIdempotencyKey) {
    List<SettlementAllocationRequest> provided = request != null ? request.allocations() : null;
    if (provided != null && !provided.isEmpty()) {
      validateOptionalHeaderSettlementAmount("supplier", request.amount(), provided);
      return provided;
    }
    List<SettlementAllocationRequest> replayAllocations =
        replayAllocations(company, replayIdempotencyKey);
    if (!replayAllocations.isEmpty()) {
      return replayAllocations;
    }
    BigDecimal amount = resolveSupplierHeaderSettlementAmount(request);
    SettlementAllocationApplication unappliedApplication =
        request != null
            ? normalizeRequestedUnappliedApplication(request.unappliedAmountApplication())
            : null;
    return buildSupplierHeaderSettlementAllocations(
        company, supplier, amount, unappliedApplication);
  }

  List<SettlementAllocationRequest> buildDealerAutoSettlementAllocations(
      Company company, Dealer dealer, BigDecimal amount) {
    return buildDealerHeaderSettlementAllocations(company, dealer, amount, null);
  }

  List<SettlementAllocationRequest> buildSupplierAutoSettlementAllocations(
      Company company, Supplier supplier, BigDecimal amount) {
    return buildSupplierHeaderSettlementAllocations(company, supplier, amount, null);
  }

  private List<SettlementAllocationRequest> replayAllocations(
      Company company, String replayIdempotencyKey) {
    if (!StringUtils.hasText(replayIdempotencyKey)) {
      return List.of();
    }
    List<PartnerSettlementAllocation> existingAllocations =
        journalReplayService.findAllocationsByIdempotencyKey(company, replayIdempotencyKey.trim());
    if (existingAllocations.isEmpty()) {
      return List.of();
    }
    return existingAllocations.stream()
        .map(
            allocation ->
                new SettlementAllocationRequest(
                    allocation.getInvoice() != null ? allocation.getInvoice().getId() : null,
                    allocation.getPurchase() != null ? allocation.getPurchase().getId() : null,
                    allocation.getAllocationAmount(),
                    allocation.getDiscountAmount(),
                    allocation.getWriteOffAmount(),
                    allocation.getFxDifferenceAmount(),
                    allocation.getInvoice() == null && allocation.getPurchase() == null
                        ? SettlementAllocationApplication.ON_ACCOUNT
                        : SettlementAllocationApplication.DOCUMENT,
                    allocation.getMemo()))
        .toList();
  }

  private BigDecimal resolveDealerHeaderSettlementAmount(PartnerSettlementRequest request) {
    if (request == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Dealer settlement request is required");
    }
    if (request.amount() != null) {
      return ValidationUtils.requirePositive(request.amount(), "amount");
    }
    throw new ApplicationException(
        ErrorCode.VALIDATION_INVALID_INPUT,
        "Provide allocations or an amount for dealer settlements");
  }

  private BigDecimal resolveSupplierHeaderSettlementAmount(PartnerSettlementRequest request) {
    if (request == null || request.amount() == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Provide allocations or an amount for supplier settlements");
    }
    return ValidationUtils.requirePositive(request.amount(), "amount");
  }

  private List<SettlementAllocationRequest> buildDealerHeaderSettlementAllocations(
      Company company,
      Dealer dealer,
      BigDecimal amount,
      SettlementAllocationApplication unappliedApplication) {
    List<Invoice> openInvoices = invoiceRepository.lockOpenInvoicesForSettlement(company, dealer);
    BigDecimal remaining = amount;
    List<SettlementAllocationRequest> allocations = new ArrayList<>();
    for (Invoice invoice : openInvoices) {
      BigDecimal outstanding = MoneyUtils.zeroIfNull(invoice.getOutstandingAmount());
      if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal applied = remaining.min(outstanding);
      allocations.add(
          new SettlementAllocationRequest(
              invoice.getId(),
              null,
              applied,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              SettlementAllocationApplication.DOCUMENT,
              "Header-level FIFO allocation"));
      remaining = remaining.subtract(applied);
    }
    if (remaining.compareTo(ALLOCATION_TOLERANCE) > 0 && unappliedApplication != null) {
      allocations.add(
          new SettlementAllocationRequest(
              null,
              null,
              remaining,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              unappliedApplication,
              unappliedApplication == SettlementAllocationApplication.FUTURE_APPLICATION
                  ? "Header-level future application"
                  : "Header-level on-account carry"));
    } else if (remaining.compareTo(ALLOCATION_TOLERANCE) > 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Header-level settlement amount exceeds open invoice outstanding total");
    }
    if (allocations.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "At least one dealer settlement allocation is required");
    }
    return allocations;
  }

  private List<SettlementAllocationRequest> buildSupplierHeaderSettlementAllocations(
      Company company,
      Supplier supplier,
      BigDecimal amount,
      SettlementAllocationApplication unappliedApplication) {
    List<RawMaterialPurchase> openPurchases =
        rawMaterialPurchaseRepository.lockOpenPurchasesForSettlement(company, supplier);
    BigDecimal remaining = amount;
    List<SettlementAllocationRequest> allocations = new ArrayList<>();
    for (RawMaterialPurchase purchase : openPurchases) {
      BigDecimal outstanding = MoneyUtils.zeroIfNull(purchase.getOutstandingAmount());
      if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      BigDecimal applied = remaining.min(outstanding);
      allocations.add(
          new SettlementAllocationRequest(
              null,
              purchase.getId(),
              applied,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              SettlementAllocationApplication.DOCUMENT,
              "Header-level oldest-open allocation"));
      remaining = remaining.subtract(applied);
    }
    if (remaining.compareTo(ALLOCATION_TOLERANCE) > 0 && unappliedApplication != null) {
      allocations.add(
          new SettlementAllocationRequest(
              null,
              null,
              remaining,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              BigDecimal.ZERO,
              unappliedApplication,
              unappliedApplication == SettlementAllocationApplication.FUTURE_APPLICATION
                  ? "Header-level future application"
                  : "Header-level on-account carry"));
    } else if (remaining.compareTo(ALLOCATION_TOLERANCE) > 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Header-level settlement amount exceeds open purchase outstanding total");
    }
    if (allocations.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "At least one supplier settlement allocation is required");
    }
    return allocations;
  }

  private void validateOptionalHeaderSettlementAmount(
      String label, BigDecimal requestedAmount, List<SettlementAllocationRequest> allocations) {
    if (requestedAmount == null || allocations == null || allocations.isEmpty()) {
      return;
    }
    BigDecimal totalApplied =
        allocations.stream()
            .map(SettlementAllocationRequest::appliedAmount)
            .map(MoneyUtils::zeroIfNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (requestedAmount.subtract(totalApplied).abs().compareTo(ALLOCATION_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Explicit " + label + " settlement allocations must add up to the request amount")
          .withDetail("amount", requestedAmount)
          .withDetail("allocationTotal", totalApplied);
    }
  }

  private SettlementAllocationApplication normalizeRequestedUnappliedApplication(
      SettlementAllocationApplication application) {
    if (application == null) {
      return null;
    }
    if (application == SettlementAllocationApplication.DOCUMENT) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Unapplied amount handling must be ON_ACCOUNT or FUTURE_APPLICATION");
    }
    return application;
  }
}
