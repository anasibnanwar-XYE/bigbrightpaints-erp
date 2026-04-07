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

  private static final BigDecimal NOTE_AMOUNT_TOLERANCE = new BigDecimal("0.01");

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
    Long receivableAccountId =
        invoice.getDealer() != null && invoice.getDealer().getReceivableAccount() != null
            ? invoice.getDealer().getReceivableAccount().getId()
            : null;
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
    BigDecimal sourceAmount = calculateControlAccountAmount(source, receivableAccountId);
    BigDecimal creditedSoFar =
        totalNoteAmount(company, source, "CREDIT_NOTE", receivableAccountId);
    BigDecimal remainingBySource = sourceAmount.subtract(creditedSoFar).max(BigDecimal.ZERO);
    BigDecimal currentOutstanding = MoneyUtils.zeroIfNull(invoice.getOutstandingAmount());
    BigDecimal allowedAmount = remainingBySource.min(currentOutstanding).max(BigDecimal.ZERO);
    BigDecimal creditAmount = request.amount() != null ? request.amount() : allowedAmount;
    if (existingEntry != null) {
      BigDecimal existingAmount = calculateEntryTotal(existingEntry);
      existingAmount = calculateControlAccountAmount(existingEntry, receivableAccountId);
      ensureIdempotentAmountMatch(
          existingAmount, request.amount(), idempotencyKey, "credit note", reference);
      BigDecimal totalCredited =
          totalNoteAmount(company, source, "CREDIT_NOTE", receivableAccountId);
      settlementOutcomeService.applyCreditNoteToInvoice(
          invoice, existingAmount, totalCredited, existingEntry.getReferenceNumber(), entryDate);
      return dtoFrom(company, existingEntry);
    }
    validateNoteAmount(
        creditAmount,
        allowedAmount,
        request.amount() != null,
        "Credit note has no remaining outstanding amount to apply",
        "Credit note exceeds remaining invoice outstanding amount",
        "invoiceId",
        invoice.getId(),
        currentOutstanding);
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
    BigDecimal ratio = creditAmount.divide(sourceAmount, 6, RoundingMode.HALF_UP);
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
    BigDecimal postedAmount = calculateControlAccountAmount(saved, receivableAccountId);
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
    Long payableAccountId =
        purchase.getSupplier() != null && purchase.getSupplier().getPayableAccount() != null
            ? purchase.getSupplier().getPayableAccount().getId()
            : null;
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
    BigDecimal sourceAmount = calculateControlAccountAmount(source, payableAccountId);
    BigDecimal debitedSoFar =
        totalNoteAmount(company, source, "DEBIT_NOTE", payableAccountId);
    BigDecimal remainingBySource = sourceAmount.subtract(debitedSoFar).max(BigDecimal.ZERO);
    BigDecimal currentOutstanding = MoneyUtils.zeroIfNull(purchase.getOutstandingAmount());
    BigDecimal allowedAmount = remainingBySource.min(currentOutstanding).max(BigDecimal.ZERO);
    BigDecimal debitAmount = request.amount() != null ? request.amount() : allowedAmount;
    if (existingEntry != null) {
      ensureIdempotentAmountMatch(
          calculateControlAccountAmount(existingEntry, payableAccountId),
          request.amount(),
          idempotencyKey,
          "debit note",
          reference);
      return dtoFrom(company, existingEntry);
    }
    validateNoteAmount(
        debitAmount,
        allowedAmount,
        request.amount() != null,
        "Debit note has no remaining outstanding amount to apply",
        "Debit note exceeds remaining purchase outstanding amount",
        "purchaseId",
        purchase.getId(),
        currentOutstanding);
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
    BigDecimal ratio = debitAmount.divide(sourceAmount, 6, RoundingMode.HALF_UP);
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
    BigDecimal postedAmount = calculateControlAccountAmount(saved, payableAccountId);
    settlementOutcomeService.applyDebitNoteToPurchase(
        purchase, postedAmount, debitedSoFar.add(postedAmount));
    return dto;
  }

  private void validateNoteAmount(
      BigDecimal requestedAmount,
      BigDecimal allowedAmount,
      boolean explicitAmountProvided,
      String emptyMessage,
      String overflowMessage,
      String entityKey,
      Long entityId,
      BigDecimal currentOutstanding) {
    BigDecimal resolvedAmount = MoneyUtils.zeroIfNull(requestedAmount);
    if (resolvedAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, emptyMessage)
          .withDetail(entityKey, entityId)
          .withDetail("outstandingAmount", MoneyUtils.zeroIfNull(currentOutstanding));
    }
    if (explicitAmountProvided
        && resolvedAmount.subtract(MoneyUtils.zeroIfNull(allowedAmount))
                .compareTo(NOTE_AMOUNT_TOLERANCE)
            > 0) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, overflowMessage)
          .withDetail(entityKey, entityId)
          .withDetail("requested", resolvedAmount)
          .withDetail("outstandingAmount", MoneyUtils.zeroIfNull(currentOutstanding));
    }
  }

  private void ensureIdempotentAmountMatch(
      BigDecimal existingAmount,
      BigDecimal requestedAmount,
      String idempotencyKey,
      String label,
      String reference) {
    if (requestedAmount == null) {
      return;
    }
    BigDecimal existing = MoneyUtils.zeroIfNull(existingAmount);
    BigDecimal requested = MoneyUtils.zeroIfNull(requestedAmount);
    if (requested.subtract(existing).abs().compareTo(NOTE_AMOUNT_TOLERANCE) <= 0) {
      return;
    }
    throw new ApplicationException(
            ErrorCode.CONCURRENCY_CONFLICT,
            "Idempotency key already used for another " + label + " amount")
        .withDetail("idempotencyKey", idempotencyKey)
        .withDetail("referenceNumber", reference)
        .withDetail("existingAmount", existing)
        .withDetail("requestedAmount", requested);
  }

  private BigDecimal totalNoteAmount(
      Company company, JournalEntry source, String correctionReason, Long controlAccountId) {
    if (source == null) {
      return BigDecimal.ZERO;
    }
    return journalEntryRepository
        .findByCompanyAndReversalOfAndCorrectionReasonIgnoreCase(company, source, correctionReason)
        .stream()
        .map(entry -> calculateControlAccountAmount(entry, controlAccountId))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateControlAccountAmount(JournalEntry entry, Long controlAccountId) {
    if (entry == null || entry.getLines() == null || entry.getLines().isEmpty()) {
      return BigDecimal.ZERO;
    }
    if (controlAccountId == null) {
      return calculateEntryTotal(entry);
    }
    BigDecimal net =
        entry.getLines().stream()
            .filter(line -> line.getAccount() != null && controlAccountId.equals(line.getAccount().getId()))
            .map(
                line ->
                    MoneyUtils.zeroIfNull(line.getDebit())
                        .subtract(MoneyUtils.zeroIfNull(line.getCredit())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .abs();
    if (net.compareTo(BigDecimal.ZERO) > 0) {
      return net;
    }
    return calculateEntryTotal(entry);
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
