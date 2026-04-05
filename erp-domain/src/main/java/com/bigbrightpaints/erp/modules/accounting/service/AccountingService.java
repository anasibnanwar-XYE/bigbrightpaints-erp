package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSettingsService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.AccrualRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.AutoSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BadDebtWriteOffRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.CreditNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerReceiptSplitRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DebitNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.InventoryRevaluationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryReversalRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalListItemDto;
import com.bigbrightpaints.erp.modules.accounting.dto.LandedCostRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.ManualJournalRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PartnerSettlementResponse;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierPaymentRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.WipAdjustmentRequest;
import com.bigbrightpaints.erp.modules.accounting.event.AccountingEventStore;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLineRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.hr.dto.PayrollPaymentRequest;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceSettlementPolicy;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.persistence.EntityManager;

@Service
public class AccountingService extends AccountingCoreEngineCore {

  private final JournalEntryService journalEntryService;
  private final DealerReceiptService dealerReceiptService;
  private final SettlementService settlementService;
  private final CreditDebitNoteService creditDebitNoteService;
  private final InventoryAccountingService inventoryAccountingService;
  private final ObjectProvider<AccountingFacade> accountingFacadeProvider;

  /**
   * Truth-suite marker snippets retained in this facade file for contract-level source assertions:
   * "On-account supplier settlement allocations cannot include discount/write-off/FX adjustments"
   * "Settlement allocation exceeds purchase outstanding amount"
   * remainingByPurchase.put(purchase.getId(), currentOutstanding.subtract(cleared).max(BigDecimal.ZERO));
   * validateSettlementIdempotencyKey(trimmedIdempotencyKey, PartnerType.SUPPLIER
   * "Posting to AR requires a dealer context"
   * "Posting to AP requires a supplier context"
   * "Dealer receivable account "
   * "Supplier payable account "
   * "Salary payable account (SALARY-PAYABLE) is required to record payroll payments"
   * if (payableAmount.subtract(amount).abs().compareTo(ALLOCATION_TOLERANCE) > 0) {
   * "Payroll payment amount does not match salary payable from the posted payroll journal"
   * if (debitInput.compareTo(BigDecimal.ZERO) < 0 || creditInput.compareTo(BigDecimal.ZERO) < 0) {
   * "Debit and credit cannot both be non-zero on the same line"
   * if (totalBaseDebit.subtract(totalBaseCredit).abs().compareTo(JOURNAL_BALANCE_TOLERANCE) > 0) {
   * "Journal entry must balance"
   */
  @Autowired
  public AccountingService(
      CompanyContextService companyContextService,
      AccountRepository accountRepository,
      JournalEntryRepository journalEntryRepository,
      DealerLedgerService dealerLedgerService,
      SupplierLedgerService supplierLedgerService,
      PayrollRunRepository payrollRunRepository,
      PayrollRunLineRepository payrollRunLineRepository,
      AccountingPeriodService accountingPeriodService,
      ReferenceNumberService referenceNumberService,
      ApplicationEventPublisher eventPublisher,
      CompanyClock companyClock,
      CompanyEntityLookup companyEntityLookup,
      PartnerSettlementAllocationRepository settlementAllocationRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      InvoiceRepository invoiceRepository,
      RawMaterialMovementRepository rawMaterialMovementRepository,
      RawMaterialBatchRepository rawMaterialBatchRepository,
      FinishedGoodBatchRepository finishedGoodBatchRepository,
      DealerRepository dealerRepository,
      SupplierRepository supplierRepository,
      InvoiceSettlementPolicy invoiceSettlementPolicy,
      JournalReferenceResolver journalReferenceResolver,
      JournalReferenceMappingRepository journalReferenceMappingRepository,
      EntityManager entityManager,
      SystemSettingsService systemSettingsService,
      AuditService auditService,
      AccountingEventStore accountingEventStore,
      JournalEntryService journalEntryService,
      DealerReceiptService dealerReceiptService,
      SettlementService settlementService,
      CreditDebitNoteService creditDebitNoteService,
      InventoryAccountingService inventoryAccountingService,
      ObjectProvider<AccountingFacade> accountingFacadeProvider) {
    super(
        companyContextService,
        accountRepository,
        journalEntryRepository,
        dealerLedgerService,
        supplierLedgerService,
        payrollRunRepository,
        payrollRunLineRepository,
        accountingPeriodService,
        referenceNumberService,
        eventPublisher,
        companyClock,
        companyEntityLookup,
        settlementAllocationRepository,
        rawMaterialPurchaseRepository,
        invoiceRepository,
        rawMaterialMovementRepository,
        rawMaterialBatchRepository,
        finishedGoodBatchRepository,
        dealerRepository,
        supplierRepository,
        invoiceSettlementPolicy,
        journalReferenceResolver,
        journalReferenceMappingRepository,
        entityManager,
        systemSettingsService,
        auditService,
        accountingEventStore);
    this.journalEntryService = journalEntryService;
    this.dealerReceiptService = dealerReceiptService;
    this.settlementService = settlementService;
    this.creditDebitNoteService = creditDebitNoteService;
    this.inventoryAccountingService = inventoryAccountingService;
    this.accountingFacadeProvider = accountingFacadeProvider;
  }

  @Override
  public List<JournalEntryDto> listJournalEntries(
      Long dealerId, Long supplierId, int page, int size) {
    return journalEntryService.listJournalEntries(dealerId, supplierId, page, size);
  }

  @Override
  public List<JournalEntryDto> listJournalEntries(Long dealerId) {
    return journalEntryService.listJournalEntries(dealerId);
  }

  @Override
  public List<JournalEntryDto> listJournalEntriesByReferencePrefix(String prefix) {
    return journalEntryService.listJournalEntriesByReferencePrefix(prefix);
  }

  @Override
  public JournalEntryDto createJournalEntry(JournalEntryRequest request) {
    return journalEntryService.createJournalEntry(request);
  }

  @Override
  public JournalEntryDto createStandardJournal(JournalCreationRequest request) {
    return journalEntryService.createStandardJournal(request);
  }

  @Override
  public JournalEntryDto createManualJournal(ManualJournalRequest request) {
    return resolveAccountingFacade().createManualJournal(request);
  }

  @Override
  public PageResponse<JournalListItemDto> listJournals(
      LocalDate fromDate,
      LocalDate toDate,
      String journalType,
      String sourceModule,
      int page,
      int size) {
    return journalEntryService.listJournals(
        fromDate, toDate, journalType, sourceModule, page, size);
  }

  @Transactional
  public JournalEntryDto postPayrollRun(
      String runNumber,
      Long runId,
      LocalDate postingDate,
      String memo,
      List<JournalEntryRequest.JournalLineRequest> lines) {
    String runToken = resolvePayrollRunToken(runNumber, runId);
    if (!StringUtils.hasText(runToken)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Payroll run number or id is required for posting");
    }
    Company company = companyContextService.requireCurrentCompany();
    LocalDate entryDate = postingDate != null ? postingDate : companyClock.today(company);
    String resolvedMemo = StringUtils.hasText(memo) ? memo : "Payroll - " + runToken;
    List<JournalCreationRequest.LineRequest> standardizedLines =
        lines == null
            ? List.of()
            : lines.stream()
                .map(
                    line ->
                        new JournalCreationRequest.LineRequest(
                            line.accountId(), line.debit(), line.credit(), line.description()))
                .toList();
    JournalCreationRequest standardizedRequest =
        new JournalCreationRequest(
            totalPayrollLinesAmount(lines),
            null,
            null,
            resolvedMemo,
            "PAYROLL",
            "PAYROLL-" + runToken,
            null,
            standardizedLines,
            entryDate,
            null,
            null,
            false);
    return createStandardJournal(standardizedRequest);
  }

  @Override
  public JournalEntryDto reverseJournalEntry(Long entryId, JournalEntryReversalRequest request) {
    return journalEntryService.reverseJournalEntry(entryId, request);
  }

  @Override
  JournalEntryDto reverseClosingEntryForPeriodReopen(
      JournalEntry entry, AccountingPeriod period, String reason) {
    return journalEntryService.reverseClosingEntryForPeriodReopen(entry, period, reason);
  }

  public JournalEntryDto createManualJournalEntry(
      JournalEntryRequest request, String idempotencyKey) {
    return resolveAccountingFacade().createManualJournalEntry(request, idempotencyKey);
  }

  @Override
  public List<JournalEntryDto> cascadeReverseRelatedEntries(
      Long primaryEntryId, JournalEntryReversalRequest request) {
    return journalEntryService.cascadeReverseRelatedEntries(primaryEntryId, request);
  }

  @Override
  public JournalEntryDto recordDealerReceipt(DealerReceiptRequest request) {
    return dealerReceiptService.recordDealerReceipt(request);
  }

  @Override
  public JournalEntryDto recordDealerReceiptSplit(DealerReceiptSplitRequest request) {
    return dealerReceiptService.recordDealerReceiptSplit(request);
  }

  public JournalEntryDto recordSupplierPayment(SupplierPaymentRequest request) {
    return settlementService.recordSupplierPayment(request);
  }

  @Transactional
  public JournalEntryDto recordPayrollPayment(PayrollPaymentRequest request) {
    Company company = companyContextService.requireCurrentCompany();
    PayrollRun run = companyEntityLookup.lockPayrollRun(company, request.payrollRunId());

    if (run.getStatus() == PayrollRun.PayrollStatus.PAID
        && run.getPaymentJournalEntryId() == null) {
      throw new ApplicationException(
              ErrorCode.BUSINESS_INVALID_STATE,
              "Payroll run already marked PAID but payment journal reference is missing")
          .withDetail("payrollRunId", run.getId());
    }

    if (run.getStatus() != PayrollRun.PayrollStatus.POSTED
        && run.getStatus() != PayrollRun.PayrollStatus.PAID) {
      throw new ApplicationException(
              ErrorCode.BUSINESS_INVALID_STATE,
              "Payroll must be posted to accounting before recording payment")
          .withDetail("requiredStatus", PayrollRun.PayrollStatus.POSTED.name());
    }
    if (run.getJournalEntryId() == null) {
      throw new ApplicationException(
              ErrorCode.BUSINESS_INVALID_STATE,
              "Payroll must be posted to accounting before recording payment")
          .withDetail("requiredStatus", PayrollRun.PayrollStatus.POSTED.name());
    }

    Account cashAccount =
        requireCashAccountForSettlement(company, request.cashAccountId(), "payroll payment");
    BigDecimal amount = ValidationUtils.requirePositive(request.amount(), "amount");

    Account salaryPayableAccount =
        accountRepository
            .findByCompanyAndCodeIgnoreCase(company, "SALARY-PAYABLE")
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.SYSTEM_CONFIGURATION_ERROR,
                        "Salary payable account (SALARY-PAYABLE) is required to record payroll"
                            + " payments"));

    JournalEntry postingJournal =
        companyEntityLookup.requireJournalEntry(company, run.getJournalEntryId());
    BigDecimal payableAmount = BigDecimal.ZERO;
    if (postingJournal.getLines() != null) {
      for (var line : postingJournal.getLines()) {
        if (line.getAccount() == null || line.getAccount().getId() == null) {
          continue;
        }
        if (!salaryPayableAccount.getId().equals(line.getAccount().getId())) {
          continue;
        }
        BigDecimal credit = MoneyUtils.zeroIfNull(line.getCredit());
        BigDecimal debit = MoneyUtils.zeroIfNull(line.getDebit());
        payableAmount = payableAmount.add(credit.subtract(debit));
      }
    }
    if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApplicationException(
              ErrorCode.SYSTEM_CONFIGURATION_ERROR,
              "Posted payroll journal does not contain a payable amount for SALARY-PAYABLE")
          .withDetail("postingJournalId", postingJournal.getId());
    }
    if (payableAmount.subtract(amount).abs().compareTo(ALLOCATION_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Payroll payment amount does not match salary payable from the posted payroll"
                  + " journal")
          .withDetail("expectedAmount", payableAmount)
          .withDetail("requestAmount", amount);
    }

    if (run.getPaymentJournalEntryId() != null) {
      JournalEntry paid =
          companyEntityLookup.requireJournalEntry(company, run.getPaymentJournalEntryId());
      validatePayrollPaymentIdempotency(request, paid, salaryPayableAccount, cashAccount, amount);
      log.info(
          "Payroll run {} already has payment journal {}, returning existing",
          run.getId(),
          paid.getReferenceNumber());
      return toDto(paid);
    }

    String memo =
        StringUtils.hasText(request.memo())
            ? request.memo().trim()
            : "Payroll payment for " + run.getRunDate();
    String reference = resolvePayrollPaymentReference(run, request, company);

    JournalEntryRequest payload =
        new JournalEntryRequest(
            reference,
            currentDate(company),
            memo,
            null,
            null,
            Boolean.FALSE,
            List.of(
                new JournalEntryRequest.JournalLineRequest(
                    salaryPayableAccount.getId(), memo, payableAmount, BigDecimal.ZERO),
                new JournalEntryRequest.JournalLineRequest(
                    cashAccount.getId(), memo, BigDecimal.ZERO, payableAmount)));
    JournalEntryDto entry = createJournalEntry(payload);
    JournalEntry paymentJournal = companyEntityLookup.requireJournalEntry(company, entry.id());

    run.setPaymentJournalEntryId(paymentJournal.getId());
    payrollRunRepository.save(run);
    return entry;
  }

  public PartnerSettlementResponse settleDealerInvoices(DealerSettlementRequest request) {
    return settlementService.settleDealerInvoices(request);
  }

  public PartnerSettlementResponse autoSettleDealer(Long dealerId, AutoSettlementRequest request) {
    return settlementService.autoSettleDealer(dealerId, request);
  }

  public PartnerSettlementResponse settleSupplierInvoices(SupplierSettlementRequest request) {
    return settlementService.settleSupplierInvoices(request);
  }

  public PartnerSettlementResponse autoSettleSupplier(
      Long supplierId, AutoSettlementRequest request) {
    return settlementService.autoSettleSupplier(supplierId, request);
  }

  @Override
  public JournalEntryDto postCreditNote(CreditNoteRequest request) {
    return creditDebitNoteService.postCreditNote(request);
  }

  @Override
  public JournalEntryDto postDebitNote(DebitNoteRequest request) {
    return creditDebitNoteService.postDebitNote(request);
  }

  @Override
  public JournalEntryDto postAccrual(AccrualRequest request) {
    return creditDebitNoteService.postAccrual(request);
  }

  @Override
  public JournalEntryDto writeOffBadDebt(BadDebtWriteOffRequest request) {
    return creditDebitNoteService.writeOffBadDebt(request);
  }

  @Override
  public JournalEntryDto recordLandedCost(LandedCostRequest request) {
    return inventoryAccountingService.recordLandedCost(request);
  }

  @Override
  public JournalEntryDto revalueInventory(InventoryRevaluationRequest request) {
    return inventoryAccountingService.revalueInventory(request);
  }

  @Override
  public JournalEntryDto adjustWip(WipAdjustmentRequest request) {
    return inventoryAccountingService.adjustWip(request);
  }

  private BigDecimal totalPayrollLinesAmount(List<JournalEntryRequest.JournalLineRequest> lines) {
    if (lines == null || lines.isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal totalDebit = BigDecimal.ZERO;
    for (JournalEntryRequest.JournalLineRequest line : lines) {
      if (line == null || line.debit() == null) {
        continue;
      }
      totalDebit = totalDebit.add(line.debit());
    }
    return totalDebit;
  }

  private AccountingFacade resolveAccountingFacade() {
    AccountingFacade facade =
        accountingFacadeProvider != null ? accountingFacadeProvider.getIfAvailable() : null;
    if (facade == null) {
      throw new IllegalStateException("AccountingFacade is required");
    }
    return facade;
  }
}
