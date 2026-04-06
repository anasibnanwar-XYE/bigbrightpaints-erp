package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.AccrualRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.BadDebtWriteOffRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.CreditNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DebitNoteRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;

@Service
public class CreditDebitNoteService {
  private final NotePostingService notePostingService;
  private final AccrualBadDebtPostingService accrualBadDebtPostingService;

  public CreditDebitNoteService(
      NotePostingService notePostingService,
      AccrualBadDebtPostingService accrualBadDebtPostingService) {
    this.notePostingService = notePostingService;
    this.accrualBadDebtPostingService = accrualBadDebtPostingService;
  }

  public JournalEntryDto postCreditNote(CreditNoteRequest request) {
    return notePostingService.postCreditNote(normalizeCreditNoteRequest(request));
  }

  public JournalEntryDto postDebitNote(DebitNoteRequest request) {
    return notePostingService.postDebitNote(normalizeDebitNoteRequest(request));
  }

  public JournalEntryDto postAccrual(AccrualRequest request) {
    return accrualBadDebtPostingService.postAccrual(normalizeAccrualRequest(request));
  }

  public JournalEntryDto writeOffBadDebt(BadDebtWriteOffRequest request) {
    return accrualBadDebtPostingService.writeOffBadDebt(normalizeBadDebtRequest(request));
  }

  private CreditNoteRequest normalizeCreditNoteRequest(CreditNoteRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.invoiceId(), "invoiceId");
    return new CreditNoteRequest(
        request.invoiceId(),
        request.amount() == null
            ? null
            : ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private DebitNoteRequest normalizeDebitNoteRequest(DebitNoteRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.purchaseId(), "purchaseId");
    return new DebitNoteRequest(
        request.purchaseId(),
        request.amount() == null
            ? null
            : ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private AccrualRequest normalizeAccrualRequest(AccrualRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.debitAccountId(), "debitAccountId");
    ValidationUtils.requireNotNull(request.creditAccountId(), "creditAccountId");
    return new AccrualRequest(
        request.debitAccountId(),
        request.creditAccountId(),
        ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        request.autoReverseDate(),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private BadDebtWriteOffRequest normalizeBadDebtRequest(BadDebtWriteOffRequest request) {
    ValidationUtils.requireNotNull(request, "request");
    ValidationUtils.requireNotNull(request.invoiceId(), "invoiceId");
    ValidationUtils.requireNotNull(request.expenseAccountId(), "expenseAccountId");
    return new BadDebtWriteOffRequest(
        request.invoiceId(),
        request.expenseAccountId(),
        ValidationUtils.requirePositive(request.amount(), "amount").abs(),
        request.entryDate(),
        normalizeText(request.referenceNumber()),
        normalizeText(request.memo()),
        normalizeText(request.idempotencyKey()),
        Boolean.TRUE.equals(request.adminOverride()));
  }

  private String normalizeText(String value) {
    String normalized = IdempotencyUtils.normalizeToken(value);
    return normalized.isBlank() ? null : normalized;
  }
}
