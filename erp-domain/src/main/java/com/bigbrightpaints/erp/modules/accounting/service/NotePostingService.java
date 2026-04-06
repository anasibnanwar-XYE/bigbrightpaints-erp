package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalCorrectionType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.dto.CreditNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DebitNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;

@Service
class NotePostingService {

  private final CompanyContextService companyContextService;
  private final InvoiceRepository invoiceRepository;
  private final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final CompanyScopedAccountingLookupService accountingLookupService;
  private final JournalEntryService journalEntryService;
  private final JournalReplayService journalReplayService;
  private final SettlementReferenceService settlementReferenceService;
  private final SettlementOutcomeService settlementOutcomeService;
  private final JournalReferenceService journalReferenceService;
  private final AccountingDtoMapperService dtoMapperService;

  NotePostingService(
      CompanyContextService companyContextService,
      InvoiceRepository invoiceRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      JournalEntryRepository journalEntryRepository,
      CompanyScopedAccountingLookupService accountingLookupService,
      JournalEntryService journalEntryService,
      JournalReplayService journalReplayService,
      SettlementReferenceService settlementReferenceService,
      SettlementOutcomeService settlementOutcomeService,
      JournalReferenceService journalReferenceService,
      AccountingDtoMapperService dtoMapperService) {
    this.companyContextService = companyContextService;
    this.invoiceRepository = invoiceRepository;
    this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    this.journalEntryRepository = journalEntryRepository;
    this.accountingLookupService = accountingLookupService;
    this.journalEntryService = journalEntryService;
    this.journalReplayService = journalReplayService;
    this.settlementReferenceService = settlementReferenceService;
    this.settlementOutcomeService = settlementOutcomeService;
    this.journalReferenceService = journalReferenceService;
    this.dtoMapperService = dtoMapperService;
  }

  @Transactional
  JournalEntryDto postCreditNote(CreditNoteRequest request) {
    Company company = companyContextService.requireCurrentCompany();
    Invoice invoice =
        invoiceRepository
            .lockByCompanyAndId(company, request.invoiceId())
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE, "Invoice not found"));
    JournalEntry source = invoice.getJournalEntry();
    if (source == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_REFERENCE,
          "Invoice " + invoice.getInvoiceNumber() + " has no posted journal to reverse");
    }
    String referenceNumber =
        StringUtils.hasText(request.referenceNumber()) ? request.referenceNumber().trim() : null;
    String idempotencyKey =
        settlementReferenceService.resolveReceiptIdempotencyKey(
            request.idempotencyKey(), referenceNumber, "credit note");
    String reference = StringUtils.hasText(referenceNumber) ? referenceNumber : idempotencyKey;
    LocalDate entryDate = request.entryDate() != null ? request.entryDate() : source.getEntryDate();
    JournalEntry existingEntry =
        journalReplayService.findExistingEntry(company, reference, idempotencyKey);
    if (existingEntry != null) {
      BigDecimal existingAmount = calculateEntryTotal(existingEntry);
      BigDecimal totalCredited = totalNoteAmount(company, source, "CREDIT_NOTE");
      settlementOutcomeService.applyCreditNoteToInvoice(
          invoice, existingAmount, totalCredited, existingEntry.getReferenceNumber(), entryDate);
      return dtoFrom(company, existingEntry);
    }
    BigDecimal totalAmount = MoneyUtils.zeroIfNull(invoice.getTotalAmount());
    BigDecimal creditedSoFar = totalNoteAmount(company, source, "CREDIT_NOTE");
    BigDecimal remaining = totalAmount.subtract(creditedSoFar).max(BigDecimal.ZERO);
    BigDecimal creditAmount = request.amount() != null ? request.amount() : remaining;
    JournalReplayService.IdempotencyReservation reservation =
        journalReplayService.reserveReferenceMapping(
            company, idempotencyKey, reference, "CREDIT_NOTE");
    if (!reservation.leader()) {
      JournalEntry awaited =
          journalReplayService.awaitJournalEntry(company, reference, idempotencyKey);
      if (awaited != null) {
        settlementOutcomeService.applyCreditNoteToInvoice(
            invoice,
            calculateEntryTotal(awaited),
            creditedSoFar,
            awaited.getReferenceNumber(),
            entryDate);
        return dtoFrom(company, awaited);
      }
    }
    String memo =
        StringUtils.hasText(request.memo())
            ? request.memo().trim()
            : "Credit note for invoice " + invoice.getInvoiceNumber();
    BigDecimal ratio = creditAmount.divide(totalAmount, 6, RoundingMode.HALF_UP);
    List<JournalEntryRequest.JournalLineRequest> lines =
        buildScaledReversalLines(source, ratio, "Credit note reversal - ");
    JournalEntryDto dto =
        journalEntryService.createJournalEntry(
            new JournalEntryRequest(
                reference,
                entryDate,
                memo,
                invoice.getDealer() != null ? invoice.getDealer().getId() : null,
                null,
                request.adminOverride(),
                lines,
                null,
                null,
                "CREDIT_NOTE",
                invoice.getInvoiceNumber(),
                null));
    JournalEntry saved = accountingLookupService.requireJournalEntry(company, dto.id());
    saved.setReversalOf(source);
    saved.setCorrectionType(JournalCorrectionType.REVERSAL);
    saved.setCorrectionReason("CREDIT_NOTE");
    saved.setSourceModule("CREDIT_NOTE");
    saved.setSourceReference(invoice.getInvoiceNumber());
    journalEntryRepository.save(saved);
    journalReplayService.linkReferenceMapping(company, idempotencyKey, saved, "CREDIT_NOTE");
    BigDecimal postedAmount = calculateEntryTotal(saved);
    settlementOutcomeService.applyCreditNoteToInvoice(
        invoice,
        postedAmount,
        creditedSoFar.add(postedAmount),
        saved.getReferenceNumber(),
        entryDate);
    return dto;
  }

  @Transactional
  JournalEntryDto postDebitNote(DebitNoteRequest request) {
    Company company = companyContextService.requireCurrentCompany();
    RawMaterialPurchase purchase =
        rawMaterialPurchaseRepository
            .lockByCompanyAndId(company, request.purchaseId())
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE, "Raw material purchase not found"));
    JournalEntry source = purchase.getJournalEntry();
    if (source == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_REFERENCE,
          "Purchase " + purchase.getInvoiceNumber() + " has no posted journal to reverse");
    }
    String referenceNumber =
        StringUtils.hasText(request.referenceNumber()) ? request.referenceNumber().trim() : null;
    String resolvedReference =
        StringUtils.hasText(referenceNumber)
            ? referenceNumber
            : (StringUtils.hasText(request.idempotencyKey())
                ? request.idempotencyKey().trim()
                : journalReferenceService.resolveJournalReference(company, null));
    String idempotencyKey =
        settlementReferenceService.resolveReceiptIdempotencyKey(
            request.idempotencyKey(), resolvedReference, "debit note");
    String reference = StringUtils.hasText(referenceNumber) ? referenceNumber : resolvedReference;
    LocalDate entryDate = request.entryDate() != null ? request.entryDate() : source.getEntryDate();
    JournalEntry existingEntry =
        journalReplayService.findExistingEntry(company, reference, idempotencyKey);
    if (existingEntry != null) {
      return dtoFrom(company, existingEntry);
    }
    BigDecimal totalAmount = MoneyUtils.zeroIfNull(purchase.getTotalAmount());
    BigDecimal debitedSoFar = totalNoteAmount(company, source, "DEBIT_NOTE");
    BigDecimal remaining = totalAmount.subtract(debitedSoFar).max(BigDecimal.ZERO);
    BigDecimal debitAmount = request.amount() != null ? request.amount() : remaining;
    JournalReplayService.IdempotencyReservation reservation =
        journalReplayService.reserveReferenceMapping(
            company, idempotencyKey, reference, "DEBIT_NOTE");
    if (!reservation.leader()) {
      JournalEntry awaited =
          journalReplayService.awaitJournalEntry(company, reference, idempotencyKey);
      if (awaited != null) {
        return dtoFrom(company, awaited);
      }
    }
    String memo =
        StringUtils.hasText(request.memo())
            ? request.memo().trim()
            : "Debit note for purchase " + purchase.getInvoiceNumber();
    BigDecimal ratio = debitAmount.divide(totalAmount, 6, RoundingMode.HALF_UP);
    List<JournalEntryRequest.JournalLineRequest> lines =
        buildScaledReversalLines(source, ratio, "Debit note reversal - ");
    JournalEntryDto dto =
        journalEntryService.createJournalEntry(
            new JournalEntryRequest(
                reference,
                entryDate,
                memo,
                null,
                purchase.getSupplier() != null ? purchase.getSupplier().getId() : null,
                request.adminOverride(),
                lines,
                null,
                null,
                "DEBIT_NOTE",
                purchase.getInvoiceNumber(),
                null));
    JournalEntry saved = accountingLookupService.requireJournalEntry(company, dto.id());
    saved.setReversalOf(source);
    saved.setCorrectionType(JournalCorrectionType.REVERSAL);
    saved.setCorrectionReason("DEBIT_NOTE");
    saved.setSourceModule("DEBIT_NOTE");
    saved.setSourceReference(purchase.getInvoiceNumber());
    journalEntryRepository.save(saved);
    journalReplayService.linkReferenceMapping(company, idempotencyKey, saved, "DEBIT_NOTE");
    settlementOutcomeService.applyDebitNoteToPurchase(
        purchase, calculateEntryTotal(saved), debitedSoFar.add(calculateEntryTotal(saved)));
    return dto;
  }

  private BigDecimal totalNoteAmount(
      Company company, JournalEntry source, String correctionReason) {
    if (source == null) {
      return BigDecimal.ZERO;
    }
    return journalEntryRepository
        .findByCompanyAndReversalOfAndCorrectionReasonIgnoreCase(company, source, correctionReason)
        .stream()
        .map(this::calculateEntryTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateEntryTotal(JournalEntry entry) {
    if (entry == null || entry.getLines() == null) {
      return BigDecimal.ZERO;
    }
    return entry.getLines().stream()
        .map(JournalLine::getDebit)
        .filter(Objects::nonNull)
        .max(Comparator.naturalOrder())
        .orElse(BigDecimal.ZERO);
  }

  private List<JournalEntryRequest.JournalLineRequest> buildScaledReversalLines(
      JournalEntry source, BigDecimal ratio, String descriptionPrefix) {
    String resolvedPrefix = StringUtils.hasText(descriptionPrefix) ? descriptionPrefix : "";
    return source.getLines().stream()
        .map(
            line -> {
              BigDecimal scaledDebit =
                  MoneyUtils.zeroIfNull(line.getDebit())
                      .multiply(ratio)
                      .setScale(2, RoundingMode.HALF_UP);
              BigDecimal scaledCredit =
                  MoneyUtils.zeroIfNull(line.getCredit())
                      .multiply(ratio)
                      .setScale(2, RoundingMode.HALF_UP);
              return new JournalEntryRequest.JournalLineRequest(
                  line.getAccount().getId(),
                  resolvedPrefix + line.getDescription(),
                  scaledCredit,
                  scaledDebit);
            })
        .toList();
  }

  private JournalEntryDto dtoFrom(Company company, JournalEntry entry) {
    JournalEntry resolved =
        entry.getId() != null
            ? accountingLookupService.requireJournalEntry(company, entry.getId())
            : entry;
    return dtoMapperService.toJournalEntryDto(resolved);
  }
}
