package com.bigbrightpaints.erp.modules.accounting.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMapping;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;

@Service
class ManualJournalService {

  private final AccountingCoreSupport accountingCoreSupport;
  private final JournalPostingService journalPostingService;

  ManualJournalService(
      AccountingCoreSupport accountingCoreSupport, JournalPostingService journalPostingService) {
    this.accountingCoreSupport = accountingCoreSupport;
    this.journalPostingService = journalPostingService;
  }

  JournalEntryDto createManualJournalEntry(JournalEntryRequest request, String idempotencyKey) {
    if (request == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Journal entry request is required");
    }
    var company = accountingCoreSupport.companyContextService.requireCurrentCompany();
    String rawKey = StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null;
    String key =
        StringUtils.hasText(rawKey)
            ? accountingCoreSupport.normalizeIdempotencyMappingKey(rawKey)
            : null;
    if (StringUtils.hasText(rawKey)) {
      Optional<JournalEntry> existingByReference =
          accountingCoreSupport.journalEntryRepository.findByCompanyAndReferenceNumber(
              company, rawKey);
      if (existingByReference.isPresent()) {
        return accountingCoreSupport.toDto(existingByReference.get());
      }
      Optional<JournalEntry> existingByResolver =
          accountingCoreSupport.journalReferenceResolver.findExistingEntry(company, rawKey);
      if (existingByResolver.isPresent()) {
        return accountingCoreSupport.toDto(existingByResolver.get());
      }
      int reserved =
          accountingCoreSupport.journalReferenceMappingRepository.reserveManualReference(
              company.getId(),
              key,
              accountingCoreSupport.reservedManualReference(key),
              "JOURNAL_ENTRY",
              CompanyTime.now(company));
      if (reserved == 0) {
        JournalEntry already = accountingCoreSupport.awaitJournalEntry(company, rawKey, key);
        if (already != null) {
          return accountingCoreSupport.toDto(already);
        }
        throw new ApplicationException(
                ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                "Manual journal idempotency key already reserved but entry not found")
            .withDetail("referenceNumber", rawKey);
      }
    }
    JournalEntryDto created;
    try {
      created =
          journalPostingService.createJournalEntry(
              new JournalEntryRequest(
                  null,
                  request.entryDate(),
                  request.memo(),
                  request.dealerId(),
                  request.supplierId(),
                  request.adminOverride(),
                  request.lines(),
                  request.currency(),
                  request.fxRate(),
                  request.sourceModule(),
                  request.sourceReference(),
                  StringUtils.hasText(request.journalType())
                      ? request.journalType()
                      : JournalEntryType.MANUAL.name(),
                  request.attachmentReferences()));
    } catch (RuntimeException ex) {
      if (!StringUtils.hasText(rawKey)
          || !accountingCoreSupport.isRetryableManualConcurrencyFailure(ex)) {
        throw ex;
      }
      JournalEntry already = accountingCoreSupport.awaitJournalEntry(company, rawKey, key);
      if (already != null) {
        return accountingCoreSupport.toDto(already);
      }
      throw ex;
    }
    if (StringUtils.hasText(key)
        && created != null
        && StringUtils.hasText(created.referenceNumber())) {
      JournalReferenceMapping mapping =
          accountingCoreSupport
              .findLatestLegacyReferenceMapping(company, key)
              .orElseThrow(
                  () ->
                      new ApplicationException(
                              ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                              "Manual journal idempotency reservation missing")
                          .withDetail("referenceNumber", rawKey));
      mapping.setCanonicalReference(created.referenceNumber());
      mapping.setEntityId(created.id());
      accountingCoreSupport.journalReferenceMappingRepository.save(mapping);
    }
    return created;
  }
}
