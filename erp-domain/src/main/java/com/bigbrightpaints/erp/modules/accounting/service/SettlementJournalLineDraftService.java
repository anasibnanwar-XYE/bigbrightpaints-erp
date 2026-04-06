package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementPaymentRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Service
class SettlementJournalLineDraftService {

  private final AccountResolutionService accountResolutionService;

  SettlementJournalLineDraftService(AccountResolutionService accountResolutionService) {
    this.accountResolutionService = accountResolutionService;
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
              discountAccount.getId(), "Settlement discount", totals.totalDiscount(), BigDecimal.ZERO));
    }
    if (writeOffAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              writeOffAccount.getId(), "Settlement write-off", totals.totalWriteOff(), BigDecimal.ZERO));
    }
    if (fxLossAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxLossAccount.getId(), "FX loss on settlement", totals.totalFxLoss(), BigDecimal.ZERO));
    }
    lines.add(
        new JournalEntryRequest.JournalLineRequest(
            receivableAccount.getId(), memo, BigDecimal.ZERO, totals.totalApplied()));
    if (fxGainAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxGainAccount.getId(), "FX gain on settlement", BigDecimal.ZERO, totals.totalFxGain()));
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
              fxLossAccount.getId(), "FX loss on settlement", totals.totalFxLoss(), BigDecimal.ZERO));
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
              writeOffAccount.getId(), "Settlement write-off", BigDecimal.ZERO, totals.totalWriteOff()));
    }
    if (fxGainAccount != null) {
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              fxGainAccount.getId(), "FX gain on settlement", BigDecimal.ZERO, totals.totalFxGain()));
    }
    return new SettlementLineDraft(lines, cashAmount);
  }
}
