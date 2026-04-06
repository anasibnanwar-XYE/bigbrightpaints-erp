package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationApplication;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementPaymentRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;

@Service
class SettlementRequestResolutionService {

  private static final String SETTLEMENT_APPLICATION_PREFIX = "[SETTLEMENT-APPLICATION:";
  private static final BigDecimal ALLOCATION_TOLERANCE = new BigDecimal("0.01");

  private final InvoiceRepository invoiceRepository;
  private final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;
  private final JournalReplayService journalReplayService;
  private final AccountResolutionService accountResolutionService;

  SettlementRequestResolutionService(
      InvoiceRepository invoiceRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      JournalReplayService journalReplayService,
      AccountResolutionService accountResolutionService) {
    this.invoiceRepository = invoiceRepository;
    this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    this.journalReplayService = journalReplayService;
    this.accountResolutionService = accountResolutionService;
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

  BigDecimal normalizeNonNegative(BigDecimal value, String field) {
    BigDecimal normalized = MoneyUtils.zeroIfNull(value);
    if (normalized.compareTo(BigDecimal.ZERO) < 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Value for " + field + " cannot be negative");
    }
    return normalized;
  }

  void validatePaymentAllocations(
      List<SettlementAllocationRequest> allocations,
      BigDecimal amount,
      String label,
      boolean dealer) {
    if (allocations == null || allocations.isEmpty()) {
      return;
    }
    BigDecimal totalApplied = BigDecimal.ZERO;
    for (SettlementAllocationRequest allocation : allocations) {
      BigDecimal applied =
          ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount");
      BigDecimal discount = normalizeNonNegative(allocation.discountAmount(), "discountAmount");
      BigDecimal writeOff = normalizeNonNegative(allocation.writeOffAmount(), "writeOffAmount");
      BigDecimal fxAdjustment = MoneyUtils.zeroIfNull(allocation.fxAdjustment());
      SettlementAllocationApplication applicationType =
          resolveSettlementApplicationType(allocation);
      if (applicationType.isUnapplied()
          && (discount.compareTo(BigDecimal.ZERO) > 0
              || writeOff.compareTo(BigDecimal.ZERO) > 0
              || fxAdjustment.compareTo(BigDecimal.ZERO) != 0)) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Discount/write-off/FX adjustments are not supported for " + label + " allocations");
      }
      if (dealer && allocation.invoiceId() == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Invoice allocation is required for dealer settlements");
      }
      totalApplied = totalApplied.add(applied);
    }
    if (totalApplied.subtract(amount).abs().compareTo(ALLOCATION_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Allocation total must equal payment amount")
          .withDetail("allocationTotal", totalApplied)
          .withDetail("paymentAmount", amount);
    }
  }

  void validateDealerSettlementAllocations(List<SettlementAllocationRequest> allocations) {
    if (allocations == null) {
      return;
    }
    Set<Long> seenInvoiceIds = new HashSet<>();
    Set<SettlementAllocationApplication> seenUnappliedApplications = new HashSet<>();
    for (SettlementAllocationRequest allocation : allocations) {
      SettlementAllocationApplication applicationType =
          resolveSettlementApplicationType(allocation);
      if (applicationType.isUnapplied()) {
        if (allocation.invoiceId() != null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Unapplied dealer settlement rows cannot reference an invoice");
        }
        if (!seenUnappliedApplications.add(applicationType)) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Dealer settlements cannot include duplicate unapplied allocation rows");
        }
      } else {
        if (allocation.invoiceId() == null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Invoice allocation is required for dealer settlements");
        }
        if (allocation.purchaseId() != null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Dealer settlements cannot allocate to purchases");
        }
        if (!seenInvoiceIds.add(allocation.invoiceId())) {
          throw new ApplicationException(
                  ErrorCode.VALIDATION_INVALID_INPUT,
                  "Dealer settlements cannot include duplicate invoice allocations")
              .withDetail("invoiceId", allocation.invoiceId());
        }
      }
      normalizeNonNegative(allocation.discountAmount(), "discountAmount");
      normalizeNonNegative(allocation.writeOffAmount(), "writeOffAmount");
      ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount");
    }
  }

  void validateSupplierSettlementAllocations(List<SettlementAllocationRequest> allocations) {
    if (allocations == null) {
      return;
    }
    Set<Long> seenPurchaseIds = new HashSet<>();
    Set<SettlementAllocationApplication> seenUnappliedApplications = new HashSet<>();
    for (SettlementAllocationRequest allocation : allocations) {
      if (allocation.invoiceId() != null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Supplier settlements cannot allocate to invoices");
      }
      SettlementAllocationApplication applicationType =
          resolveSettlementApplicationType(allocation);
      if (applicationType.isUnapplied()) {
        if (allocation.purchaseId() != null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Unapplied supplier settlement rows cannot reference a purchase");
        }
        if (!seenUnappliedApplications.add(applicationType)) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Supplier settlements cannot include duplicate unapplied allocation rows");
        }
      } else if (allocation.purchaseId() == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Purchase allocation is required for supplier settlements unless unapplied");
      } else if (!seenPurchaseIds.add(allocation.purchaseId())) {
        throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_INPUT,
                "Supplier settlements cannot include duplicate purchase allocations")
            .withDetail("purchaseId", allocation.purchaseId());
      }
      normalizeNonNegative(allocation.discountAmount(), "discountAmount");
      normalizeNonNegative(allocation.writeOffAmount(), "writeOffAmount");
      ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount");
    }
  }

  SettlementTotals computeSettlementTotals(List<SettlementAllocationRequest> allocations) {
    BigDecimal totalApplied = BigDecimal.ZERO;
    BigDecimal totalDiscount = BigDecimal.ZERO;
    BigDecimal totalWriteOff = BigDecimal.ZERO;
    BigDecimal totalFxGain = BigDecimal.ZERO;
    BigDecimal totalFxLoss = BigDecimal.ZERO;
    if (allocations == null) {
      return new SettlementTotals(
          totalApplied, totalDiscount, totalWriteOff, totalFxGain, totalFxLoss);
    }
    for (SettlementAllocationRequest allocation : allocations) {
      BigDecimal applied =
          ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount");
      BigDecimal discount = normalizeNonNegative(allocation.discountAmount(), "discountAmount");
      BigDecimal writeOff = normalizeNonNegative(allocation.writeOffAmount(), "writeOffAmount");
      BigDecimal fxAdjustment = MoneyUtils.zeroIfNull(allocation.fxAdjustment());
      totalApplied = totalApplied.add(applied);
      totalDiscount = totalDiscount.add(discount);
      totalWriteOff = totalWriteOff.add(writeOff);
      if (fxAdjustment.compareTo(BigDecimal.ZERO) > 0) {
        totalFxGain = totalFxGain.add(fxAdjustment);
      } else if (fxAdjustment.compareTo(BigDecimal.ZERO) < 0) {
        totalFxLoss = totalFxLoss.add(fxAdjustment.abs());
      }
    }
    return new SettlementTotals(
        totalApplied, totalDiscount, totalWriteOff, totalFxGain, totalFxLoss);
  }

  SettlementAllocationApplication resolveSettlementApplicationType(
      SettlementAllocationRequest allocation) {
    if (allocation == null) {
      return SettlementAllocationApplication.DOCUMENT;
    }
    if (allocation.applicationType() != null) {
      return allocation.applicationType();
    }
    if (allocation.invoiceId() == null && allocation.purchaseId() == null) {
      return SettlementAllocationApplication.ON_ACCOUNT;
    }
    return SettlementAllocationApplication.DOCUMENT;
  }

  SettlementLineDraft buildDealerSettlementLines(
      Company company,
      PartnerSettlementRequest request,
      Account receivableAccount,
      SettlementTotals totals,
      String memo,
      boolean requireActiveCashAccounts) {
    Account discountAccount =
        totals.totalDiscount().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.discountAccountId())
            : null;
    Account writeOffAccount =
        totals.totalWriteOff().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.writeOffAccountId())
            : null;
    Account fxGainAccount =
        totals.totalFxGain().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.fxGainAccountId())
            : null;
    Account fxLossAccount =
        totals.totalFxLoss().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.fxLossAccountId())
            : null;
    BigDecimal cashAmount =
        totals
            .totalApplied()
            .add(totals.totalFxGain())
            .subtract(totals.totalFxLoss())
            .subtract(totals.totalDiscount())
            .subtract(totals.totalWriteOff());
    List<JournalEntryRequest.JournalLineRequest> paymentLines = new ArrayList<>();
    if (request.payments() == null || request.payments().isEmpty()) {
      if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
        Account cashAccount =
            accountResolutionService.requireCashAccountForSettlement(
                company, request.cashAccountId(), "dealer settlement", requireActiveCashAccounts);
        paymentLines.add(
            new JournalEntryRequest.JournalLineRequest(
                cashAccount.getId(), memo, cashAmount, BigDecimal.ZERO));
      }
    } else {
      BigDecimal paymentTotal = BigDecimal.ZERO;
      for (SettlementPaymentRequest payment : request.payments()) {
        BigDecimal amount = ValidationUtils.requirePositive(payment.amount(), "payment amount");
        Account account =
            accountResolutionService.requireCashAccountForSettlement(
                company,
                payment.accountId(),
                "dealer settlement payment line",
                requireActiveCashAccounts);
        paymentLines.add(
            new JournalEntryRequest.JournalLineRequest(
                account.getId(), memo, amount, BigDecimal.ZERO));
        paymentTotal = paymentTotal.add(amount);
      }
      if (cashAmount.compareTo(paymentTotal) != 0) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Payment total must equal net cash required for dealer settlement");
      }
    }
    List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>(paymentLines);
    if (discountAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              discountAccount.getId(),
              "Settlement discount",
              totals.totalDiscount(),
              BigDecimal.ZERO));
    }
    if (writeOffAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              writeOffAccount.getId(),
              "Settlement write-off",
              totals.totalWriteOff(),
              BigDecimal.ZERO));
    }
    if (fxLossAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxLossAccount.getId(),
              "FX loss on settlement",
              totals.totalFxLoss(),
              BigDecimal.ZERO));
    }
    lines.add(
        new JournalEntryRequest.JournalLineRequest(
            receivableAccount.getId(), memo, BigDecimal.ZERO, totals.totalApplied()));
    if (fxGainAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxGainAccount.getId(),
              "FX gain on settlement",
              BigDecimal.ZERO,
              totals.totalFxGain()));
    }
    return new SettlementLineDraft(lines, cashAmount);
  }

  SettlementLineDraft buildSupplierSettlementLines(
      Company company,
      PartnerSettlementRequest request,
      Account payableAccount,
      SettlementTotals totals,
      String memo,
      boolean requireActiveCashAccount) {
    Account discountAccount =
        totals.totalDiscount().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.discountAccountId())
            : null;
    Account writeOffAccount =
        totals.totalWriteOff().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.writeOffAccountId())
            : null;
    Account fxGainAccount =
        totals.totalFxGain().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.fxGainAccountId())
            : null;
    Account fxLossAccount =
        totals.totalFxLoss().compareTo(BigDecimal.ZERO) > 0
            ? accountResolutionService.requireAccount(company, request.fxLossAccountId())
            : null;
    BigDecimal cashAmount =
        totals
            .totalApplied()
            .add(totals.totalFxLoss())
            .subtract(totals.totalFxGain())
            .subtract(totals.totalDiscount())
            .subtract(totals.totalWriteOff());
    List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>();
    lines.add(
        new JournalEntryRequest.JournalLineRequest(
            payableAccount.getId(), memo, totals.totalApplied(), BigDecimal.ZERO));
    if (fxLossAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxLossAccount.getId(),
              "FX loss on settlement",
              totals.totalFxLoss(),
              BigDecimal.ZERO));
    }
    if (cashAmount.compareTo(BigDecimal.ZERO) > 0) {
      Account cashAccount =
          accountResolutionService.requireCashAccountForSettlement(
              company, request.cashAccountId(), "supplier settlement", requireActiveCashAccount);
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              cashAccount.getId(), memo, BigDecimal.ZERO, cashAmount));
    }
    if (discountAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              discountAccount.getId(),
              "Settlement discount received",
              BigDecimal.ZERO,
              totals.totalDiscount()));
    }
    if (writeOffAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              writeOffAccount.getId(),
              "Settlement write-off",
              BigDecimal.ZERO,
              totals.totalWriteOff()));
    }
    if (fxGainAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxGainAccount.getId(),
              "FX gain on settlement",
              BigDecimal.ZERO,
              totals.totalFxGain()));
    }
    return new SettlementLineDraft(lines, cashAmount);
  }

  String encodeSettlementAllocationMemo(
      SettlementAllocationApplication applicationType, String memo) {
    SettlementAllocationApplication resolved =
        applicationType != null ? applicationType : SettlementAllocationApplication.DOCUMENT;
    String visibleMemo = memo != null && !memo.isBlank() ? memo.trim() : null;
    if (!resolved.isUnapplied()) {
      return visibleMemo;
    }
    String prefix = SETTLEMENT_APPLICATION_PREFIX + resolved.name() + "]";
    return visibleMemo != null ? prefix + " " + visibleMemo : prefix;
  }

  boolean settlementOverrideRequested(SettlementTotals totals) {
    if (totals == null) {
      return false;
    }
    return totals.totalDiscount().compareTo(BigDecimal.ZERO) > 0
        || totals.totalWriteOff().compareTo(BigDecimal.ZERO) > 0
        || totals.totalFxGain().compareTo(BigDecimal.ZERO) > 0
        || totals.totalFxLoss().compareTo(BigDecimal.ZERO) > 0;
  }

  private List<SettlementAllocationRequest> replayAllocations(
      Company company, String replayIdempotencyKey) {
    if (!org.springframework.util.StringUtils.hasText(replayIdempotencyKey)) {
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
    BigDecimal requestedAmount =
        request.amount() != null
            ? ValidationUtils.requirePositive(request.amount(), "amount")
            : null;
    BigDecimal paymentTotal = null;
    if (request.payments() != null && !request.payments().isEmpty()) {
      paymentTotal = BigDecimal.ZERO;
      for (SettlementPaymentRequest payment : request.payments()) {
        paymentTotal =
            paymentTotal.add(ValidationUtils.requirePositive(payment.amount(), "payment amount"));
      }
    }
    if (requestedAmount != null) {
      return requestedAmount;
    }
    if (paymentTotal != null && paymentTotal.compareTo(BigDecimal.ZERO) > 0) {
      return paymentTotal;
    }
    throw new ApplicationException(
        ErrorCode.VALIDATION_INVALID_INPUT,
        "Provide allocations or an amount (or payment lines) for dealer settlements");
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
    BigDecimal totalOutstanding = BigDecimal.ZERO;
    List<SettlementAllocationRequest> allocations = new ArrayList<>();
    for (Invoice invoice : openInvoices) {
      BigDecimal outstanding = MoneyUtils.zeroIfNull(invoice.getOutstandingAmount());
      if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      totalOutstanding = totalOutstanding.add(outstanding);
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
    BigDecimal totalOutstanding = BigDecimal.ZERO;
    List<SettlementAllocationRequest> allocations = new ArrayList<>();
    for (RawMaterialPurchase purchase : openPurchases) {
      BigDecimal outstanding = MoneyUtils.zeroIfNull(purchase.getOutstandingAmount());
      if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }
      totalOutstanding = totalOutstanding.add(outstanding);
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

  record SettlementTotals(
      BigDecimal totalApplied,
      BigDecimal totalDiscount,
      BigDecimal totalWriteOff,
      BigDecimal totalFxGain,
      BigDecimal totalFxLoss) {}

  record SettlementLineDraft(
      List<JournalEntryRequest.JournalLineRequest> lines, BigDecimal cashAmount) {}
}
