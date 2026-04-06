package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.ManualJournalRequest;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;

@Service
class ManualJournalFacadeOperations {

  private final AccountingService accountingService;
  private final CompanyContextService companyContextService;
  private final CompanyClock companyClock;

  ManualJournalFacadeOperations(
      AccountingService accountingService,
      CompanyContextService companyContextService,
      CompanyClock companyClock) {
    this.accountingService = accountingService;
    this.companyContextService = companyContextService;
    this.companyClock = companyClock;
  }

  JournalEntryDto createManualJournal(ManualJournalRequest request) {
    if (request == null) {
      throw validationMissingField("Manual journal request is required");
    }
    if (request.lines() == null || request.lines().isEmpty()) {
      throw validationInvalidInput("Manual journal requires at least one line");
    }

    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    List<JournalCreationRequest.LineRequest> lines = new ArrayList<>();
    for (ManualJournalRequest.LineRequest line : request.lines()) {
      if (line == null || line.accountId() == null) {
        throw validationMissingField("Account is required for manual journal lines");
      }
      if (line.entryType() == null) {
        throw validationMissingField("Entry type is required for manual journal lines");
      }
      BigDecimal amount = ValidationUtils.requirePositive(line.amount(), "amount");
      String lineNarration =
          StringUtils.hasText(line.narration())
              ? line.narration().trim()
              : (StringUtils.hasText(request.narration())
                  ? request.narration().trim()
                  : "Manual journal line");
      BigDecimal debit =
          line.entryType() == ManualJournalRequest.EntryType.DEBIT ? amount : BigDecimal.ZERO;
      BigDecimal credit =
          line.entryType() == ManualJournalRequest.EntryType.CREDIT ? amount : BigDecimal.ZERO;
      totalDebit = totalDebit.add(debit);
      totalCredit = totalCredit.add(credit);
      lines.add(
          new JournalCreationRequest.LineRequest(line.accountId(), debit, credit, lineNarration));
    }

    if (totalDebit.subtract(totalCredit).abs().compareTo(BigDecimal.ZERO) > 0) {
      throw validationInvalidInput("Manual journal entry must balance")
          .withDetail("totalDebit", totalDebit)
          .withDetail("totalCredit", totalCredit);
    }

    LocalDate entryDate = resolveManualEntryDate(request.entryDate());
    if (!StringUtils.hasText(request.narration())) {
      throw validationMissingField("Manual journal reason is required");
    }
    String narration = request.narration().trim();
    String idempotencyKey =
        StringUtils.hasText(request.idempotencyKey()) ? request.idempotencyKey().trim() : null;
    String sourceReference =
        StringUtils.hasText(idempotencyKey)
            ? idempotencyKey
            : generatedManualSourceReference(entryDate);

    return accountingService.createStandardJournal(
        new JournalCreationRequest(
            totalDebit,
            firstDebitAccountFromCreationLines(lines),
            firstCreditAccountFromCreationLines(lines),
            narration,
            "MANUAL",
            sourceReference,
            null,
            lines,
            entryDate,
            null,
            null,
            Boolean.TRUE.equals(request.adminOverride()),
            request.attachmentReferences()));
  }

  JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey) {
    if (request == null) {
      throw validationMissingField("Journal entry request is required");
    }

    if (!StringUtils.hasText(request.memo())) {
      throw validationMissingField("Manual journal reason is required");
    }
    String resolvedIdempotencyKey =
        StringUtils.hasText(idempotencyKey)
            ? idempotencyKey.trim()
            : (StringUtils.hasText(request.referenceNumber())
                ? request.referenceNumber().trim()
                : null);
    JournalEntryRequest sanitizedRequest =
        new JournalEntryRequest(
            null,
            resolveManualEntryDate(request.entryDate()),
            request.memo().trim(),
            request.dealerId(),
            request.supplierId(),
            Boolean.TRUE.equals(request.adminOverride()),
            request.lines(),
            request.currency(),
            request.fxRate(),
            null,
            null,
            null,
            request.attachmentReferences());
    return accountingService.createManualJournalEntry(sanitizedRequest, resolvedIdempotencyKey);
  }

  private Long firstDebitAccountFromCreationLines(List<JournalCreationRequest.LineRequest> lines) {
    return lines.stream()
        .filter(line -> line.debit() != null && line.debit().compareTo(BigDecimal.ZERO) > 0)
        .map(JournalCreationRequest.LineRequest::accountId)
        .findFirst()
        .orElseThrow(
            () ->
                validationInvalidInput(
                    "Journal lines must include at least one debit and one credit"));
  }

  private Long firstCreditAccountFromCreationLines(List<JournalCreationRequest.LineRequest> lines) {
    return lines.stream()
        .filter(line -> line.credit() != null && line.credit().compareTo(BigDecimal.ZERO) > 0)
        .map(JournalCreationRequest.LineRequest::accountId)
        .findFirst()
        .orElseThrow(
            () ->
                validationInvalidInput(
                    "Journal lines must include at least one debit and one credit"));
  }

  private LocalDate resolveManualEntryDate(LocalDate requestedEntryDate) {
    if (requestedEntryDate != null) {
      return requestedEntryDate;
    }
    return companyClock.today(companyContextService.requireCurrentCompany());
  }

  private String generatedManualSourceReference(LocalDate entryDate) {
    LocalDate resolvedEntryDate = entryDate != null ? entryDate : resolveManualEntryDate(null);
    String nonce = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return AccountingFacade.MANUAL_REFERENCE_PREFIX + resolvedEntryDate + "-" + nonce;
  }

  private ApplicationException validationMissingField(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, message);
  }

  private ApplicationException validationInvalidInput(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, message);
  }
}
