package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;

@Service
class JournalDuplicateGuardService {

  void ensureDuplicateMatchesExisting(
      JournalEntry existing, JournalEntry candidate, List<JournalLine> candidateLines) {
    List<String> mismatches = new ArrayList<>();
    List<String> partnerMismatchTypes = new ArrayList<>();
    if (!Objects.equals(existing.getEntryDate(), candidate.getEntryDate())) {
      mismatches.add("entryDate");
    }
    if (!Objects.equals(
        existing.getDealer() != null ? existing.getDealer().getId() : null,
        candidate.getDealer() != null ? candidate.getDealer().getId() : null)) {
      mismatches.add(partnerFieldLabel(PartnerType.DEALER));
      partnerMismatchTypes.add(PartnerType.DEALER.name());
    }
    if (!Objects.equals(
        existing.getSupplier() != null ? existing.getSupplier().getId() : null,
        candidate.getSupplier() != null ? candidate.getSupplier().getId() : null)) {
      mismatches.add(partnerFieldLabel(PartnerType.SUPPLIER));
      partnerMismatchTypes.add(PartnerType.SUPPLIER.name());
    }
    if (!sameCurrency(existing.getCurrency(), candidate.getCurrency())) {
      mismatches.add("currency");
    }
    if (!sameFxRate(existing.getFxRate(), candidate.getFxRate())) {
      mismatches.add("fxRate");
    }
    if (StringUtils.hasText(candidate.getMemo())
        && !Objects.equals(existing.getMemo(), candidate.getMemo())) {
      mismatches.add("memo");
    }
    if (!lineSignatureCounts(existing.getLines()).equals(lineSignatureCounts(candidateLines))) {
      mismatches.add("lines");
    }
    if (!mismatches.isEmpty()) {
      ApplicationException exception =
          new ApplicationException(
                  ErrorCode.BUSINESS_DUPLICATE_ENTRY,
                  "Journal entry reference already exists with different details")
              .withDetail("reference", existing.getReferenceNumber())
              .withDetail("mismatches", mismatches);
      if (!partnerMismatchTypes.isEmpty()) {
        exception.withDetail("partnerMismatchTypes", partnerMismatchTypes);
      }
      throw exception;
    }
  }

  private Map<JournalLineSignature, Integer> lineSignatureCounts(List<JournalLine> lines) {
    Map<JournalLineSignature, Integer> counts = new HashMap<>();
    if (lines == null) {
      return counts;
    }
    for (JournalLine line : lines) {
      if (line.getAccount() == null || line.getAccount().getId() == null) {
        continue;
      }
      JournalLineSignature signature =
          new JournalLineSignature(
              line.getAccount().getId(),
              roundedAmount(line.getDebit()),
              roundedAmount(line.getCredit()));
      counts.merge(signature, 1, Integer::sum);
    }
    return counts;
  }

  private BigDecimal roundedAmount(BigDecimal amount) {
    return amount == null
        ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        : amount.setScale(2, RoundingMode.HALF_UP);
  }

  private boolean sameCurrency(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    return left != null && right != null && left.equalsIgnoreCase(right);
  }

  private boolean sameFxRate(BigDecimal left, BigDecimal right) {
    BigDecimal normalizedLeft = left == null ? BigDecimal.ONE : left;
    BigDecimal normalizedRight = right == null ? BigDecimal.ONE : right;
    return normalizedLeft.compareTo(normalizedRight) == 0;
  }

  private String partnerFieldLabel(PartnerType partnerType) {
    return partnerType == PartnerType.DEALER ? "dealerId" : "supplierId";
  }

  private record JournalLineSignature(Long accountId, BigDecimal debit, BigDecimal credit) {}
}
