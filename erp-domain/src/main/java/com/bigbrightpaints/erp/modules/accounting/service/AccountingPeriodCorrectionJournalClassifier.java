package com.bigbrightpaints.erp.modules.accounting.service;

import java.util.List;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;

final class AccountingPeriodCorrectionJournalClassifier {

  long countCorrectionLinkageGaps(List<JournalEntry> periodEntries) {
    if (periodEntries == null || periodEntries.isEmpty()) {
      return 0;
    }
    return periodEntries.stream()
        .filter(this::isCorrectionJournal)
        .filter(this::isMissingCorrectionLinkage)
        .count();
  }

  boolean isCorrectionJournal(JournalEntry entry) {
    if (entry == null) {
      return false;
    }
    if (entry.getCorrectionType() != null || entry.getReversalOf() != null) {
      return true;
    }
    String reference = entry.getReferenceNumber();
    if (!StringUtils.hasText(reference)) {
      return false;
    }
    String normalized = reference.trim().toUpperCase();
    return normalized.startsWith("CRN-")
        || normalized.startsWith("CN-")
        || normalized.startsWith("DN-")
        || normalized.startsWith("PRN-");
  }

  boolean isMissingCorrectionLinkage(JournalEntry entry) {
    if (isLegacyReturnJournalWithoutModernCorrectionMetadata(entry)) {
      return false;
    }
    return entry.getCorrectionType() == null
        || !StringUtils.hasText(entry.getCorrectionReason())
        || !StringUtils.hasText(entry.getSourceModule())
        || !StringUtils.hasText(entry.getSourceReference());
  }

  private boolean isLegacyReturnJournalWithoutModernCorrectionMetadata(JournalEntry entry) {
    if (entry == null
        || entry.getCorrectionType() != null
        || StringUtils.hasText(entry.getCorrectionReason())
        || StringUtils.hasText(entry.getSourceModule())
        || StringUtils.hasText(entry.getSourceReference())) {
      return false;
    }
    String reference = entry.getReferenceNumber();
    if (!StringUtils.hasText(reference)) {
      return false;
    }
    String normalizedReference = reference.trim().toUpperCase();
    if (normalizedReference.startsWith("CRN-")) {
      return entry.getDealer() != null;
    }
    if (normalizedReference.startsWith("PRN-")) {
      return entry.getSupplier() != null;
    }
    return false;
  }
}
