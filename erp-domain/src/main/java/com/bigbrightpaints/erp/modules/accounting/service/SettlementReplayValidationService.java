package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.IntegrationFailureMetadataSchema;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationRequest;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;

@Service
class SettlementReplayValidationService {

  void validateDealerReceiptIdempotency(
      String idempotencyKey,
      Dealer dealer,
      Account cashAccount,
      Account receivableAccount,
      BigDecimal amount,
      String memo,
      JournalEntry entry,
      List<PartnerSettlementAllocation> existingAllocations,
      List<SettlementAllocationRequest> allocations) {
    validateSettlementIdempotencyKey(
        idempotencyKey, PartnerType.DEALER, dealer.getId(), existingAllocations, allocations);
    List<JournalEntryRequest.JournalLineRequest> expectedLines =
        List.of(
            new JournalEntryRequest.JournalLineRequest(
                cashAccount.getId(), memo, amount, BigDecimal.ZERO),
            new JournalEntryRequest.JournalLineRequest(
                receivableAccount.getId(), memo, BigDecimal.ZERO, amount));
    validateReceiptJournalLines(idempotencyKey, dealer, memo, entry, expectedLines);
  }

  void validateSplitReceiptIdempotency(
      String idempotencyKey,
      Dealer dealer,
      String memo,
      JournalEntry entry,
      List<JournalEntryRequest.JournalLineRequest> expectedLines) {
    validateReceiptJournalLines(idempotencyKey, dealer, memo, entry, expectedLines);
  }

  void validateSupplierPaymentIdempotency(
      String idempotencyKey,
      Supplier supplier,
      Account cashAccount,
      Account payableAccount,
      BigDecimal amount,
      String memo,
      JournalEntry entry,
      List<PartnerSettlementAllocation> existingAllocations,
      List<SettlementAllocationRequest> allocations) {
    Supplier resolvedSupplier = ValidationUtils.requireNotNull(supplier, "supplier");
    validateSettlementIdempotencyKey(
        idempotencyKey,
        PartnerType.SUPPLIER,
        resolvedSupplier.getId(),
        existingAllocations,
        allocations);
    List<JournalEntryRequest.JournalLineRequest> expectedLines =
        List.of(
            new JournalEntryRequest.JournalLineRequest(
                payableAccount.getId(), memo, amount, BigDecimal.ZERO),
            new JournalEntryRequest.JournalLineRequest(
                cashAccount.getId(), memo, BigDecimal.ZERO, amount));
    validatePartnerJournalReplay(
        idempotencyKey,
        PartnerType.SUPPLIER,
        supplier != null ? supplier.getId() : null,
        memo,
        entry,
        expectedLines,
        "Idempotency key already used for a different supplier payment payload");
  }

  void validatePartnerSettlementJournalLines(
      String idempotencyKey,
      PartnerType partnerType,
      Long partnerId,
      LocalDate requestedEffectiveSettlementDate,
      String memo,
      JournalEntry entry,
      List<JournalEntryRequest.JournalLineRequest> expectedLines) {
    validatePartnerSettlementReplayDate(
        idempotencyKey, partnerType, partnerId, requestedEffectiveSettlementDate, entry);
    validatePartnerJournalReplay(
        idempotencyKey,
        partnerType,
        partnerId,
        memo,
        entry,
        expectedLines,
        "Idempotency key already used for a different settlement payload");
  }

  void validateSettlementIdempotencyKey(
      String idempotencyKey,
      PartnerType partnerType,
      Long partnerId,
      List<PartnerSettlementAllocation> existing,
      List<SettlementAllocationRequest> allocations) {
    List<PartnerSettlementAllocation> existingAllocations = existing == null ? List.of() : existing;
    List<SettlementAllocationRequest> requestedAllocations =
        allocations == null ? List.of() : allocations;
    boolean partnerMismatch =
        existingAllocations.stream()
            .anyMatch(row -> isSettlementAllocationPartnerMismatch(row, partnerType, partnerId));
    if (partnerMismatch) {
      throw replayConflictWithPartnerContext(
          partnerMismatchMessage(partnerType), idempotencyKey, partnerType, partnerId);
    }

    Map<String, Integer> existingSignatures =
        allocationSignatureCountsFromRows(existingAllocations);
    Map<String, Integer> requestSignatures =
        allocationSignatureCountsFromRequests(requestedAllocations);
    if (!existingSignatures.equals(requestSignatures)) {
      ApplicationException exception =
          replayConflictWithPartnerContext(
              "Idempotency key already used for a different settlement payload",
              idempotencyKey,
              partnerType,
              partnerId);
      exception.withDetail("existingAllocationCount", existing != null ? existing.size() : 0);
      exception.withDetail("requestAllocationCount", allocations != null ? allocations.size() : 0);
      exception.withDetail(
          "existingAllocationSignatureDigest", allocationSignatureDigest(existingSignatures));
      exception.withDetail(
          "requestAllocationSignatureDigest", allocationSignatureDigest(requestSignatures));
      throw exception;
    }
  }

  private void validateReceiptJournalLines(
      String idempotencyKey,
      Dealer dealer,
      String memo,
      JournalEntry entry,
      List<JournalEntryRequest.JournalLineRequest> expectedLines) {
    validatePartnerJournalReplay(
        idempotencyKey,
        PartnerType.DEALER,
        dealer != null ? dealer.getId() : null,
        memo,
        entry,
        expectedLines,
        "Idempotency key already used for a different receipt payload");
  }

  private void validatePartnerSettlementReplayDate(
      String idempotencyKey,
      PartnerType partnerType,
      Long partnerId,
      LocalDate requestedEffectiveSettlementDate,
      JournalEntry entry) {
    if (entry == null || requestedEffectiveSettlementDate == null) {
      return;
    }
    LocalDate persistedSettlementDate = entry.getEntryDate();
    if (Objects.equals(persistedSettlementDate, requestedEffectiveSettlementDate)) {
      return;
    }
    throw replayConflictWithPartnerContext(
            "Idempotency key already used with a different settlement date",
            idempotencyKey,
            partnerType,
            partnerId)
        .withDetail("existingSettlementDate", persistedSettlementDate)
        .withDetail("requestedSettlementDate", requestedEffectiveSettlementDate);
  }

  private void validatePartnerJournalReplay(
      String idempotencyKey,
      PartnerType partnerType,
      Long partnerId,
      String memo,
      JournalEntry entry,
      List<JournalEntryRequest.JournalLineRequest> expectedLines,
      String payloadMismatchMessage) {
    if (entry == null) {
      throw replayConflictWithPartnerContext(
          "Idempotency key already used but journal entry is missing",
          idempotencyKey,
          partnerType,
          partnerId);
    }
    if (isJournalEntryPartnerMismatch(entry, partnerType, partnerId)) {
      throw replayConflictWithPartnerContext(
          partnerMismatchMessage(partnerType), idempotencyKey, partnerType, partnerId);
    }
    if (StringUtils.hasText(memo) && !Objects.equals(entry.getMemo(), memo)) {
      throw replayConflictWithPartnerContext(
          "Idempotency key already used with a different memo",
          idempotencyKey,
          partnerType,
          partnerId);
    }
    Map<JournalLineSignature, Integer> existingLines = lineSignatureCounts(entry.getLines());
    Map<JournalLineSignature, Integer> expected = lineSignatureCountsFromRequests(expectedLines);
    if (!existingLines.equals(expected)) {
      throw replayConflictWithPartnerContext(
          payloadMismatchMessage, idempotencyKey, partnerType, partnerId);
    }
  }

  private ApplicationException replayConflictWithPartnerContext(
      String message, String idempotencyKey, PartnerType partnerType, Long partnerId) {
    String normalizedIdempotencyKey =
        StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : idempotencyKey;
    String partnerTypeDetail = partnerType != null ? partnerType.name() : "null";
    ApplicationException exception =
        new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT, message)
            .withDetail(
                IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, normalizedIdempotencyKey)
            .withDetail(IntegrationFailureMetadataSchema.KEY_PARTNER_TYPE, partnerTypeDetail);
    if (partnerId != null) {
      exception.withDetail(IntegrationFailureMetadataSchema.KEY_PARTNER_ID, partnerId);
    }
    return exception;
  }

  private boolean isJournalEntryPartnerMismatch(
      JournalEntry entry, PartnerType partnerType, Long partnerId) {
    if (partnerType == PartnerType.DEALER) {
      return entry.getDealer() == null || !Objects.equals(entry.getDealer().getId(), partnerId);
    }
    if (partnerType == PartnerType.SUPPLIER) {
      return entry.getSupplier() == null || !Objects.equals(entry.getSupplier().getId(), partnerId);
    }
    return true;
  }

  private boolean isSettlementAllocationPartnerMismatch(
      PartnerSettlementAllocation allocation, PartnerType partnerType, Long partnerId) {
    if (partnerType == PartnerType.DEALER) {
      return allocation.getDealer() == null
          || !Objects.equals(allocation.getDealer().getId(), partnerId);
    }
    if (partnerType == PartnerType.SUPPLIER) {
      return allocation.getSupplier() == null
          || !Objects.equals(allocation.getSupplier().getId(), partnerId);
    }
    return true;
  }

  private String partnerMismatchMessage(PartnerType partnerType) {
    return "Idempotency key already used for another " + partnerMismatchSubject(partnerType);
  }

  private String partnerMismatchSubject(PartnerType partnerType) {
    if (partnerType == PartnerType.DEALER) {
      return "dealer";
    }
    if (partnerType == PartnerType.SUPPLIER) {
      return "supplier";
    }
    return "partner type";
  }

  private Map<String, Integer> allocationSignatureCountsFromRows(
      List<PartnerSettlementAllocation> allocations) {
    Map<String, Integer> counts = new HashMap<>();
    if (allocations == null) {
      return counts;
    }
    for (PartnerSettlementAllocation allocation : allocations) {
      String signature =
          "%s|%s|%s|%s|%s|%s"
              .formatted(
                  allocation.getInvoice() != null ? allocation.getInvoice().getId() : "null",
                  allocation.getPurchase() != null ? allocation.getPurchase().getId() : "null",
                  normalizedAllocationAmount(allocation.getAllocationAmount()),
                  normalizedAllocationAmount(allocation.getDiscountAmount()),
                  normalizedAllocationAmount(allocation.getWriteOffAmount()),
                  normalizedAllocationAmount(allocation.getFxDifferenceAmount()));
      counts.merge(signature, 1, Integer::sum);
    }
    return counts;
  }

  private Map<String, Integer> allocationSignatureCountsFromRequests(
      List<SettlementAllocationRequest> allocations) {
    Map<String, Integer> counts = new HashMap<>();
    if (allocations == null) {
      return counts;
    }
    for (SettlementAllocationRequest allocation : allocations) {
      String signature =
          "%s|%s|%s|%s|%s|%s"
              .formatted(
                  allocation.invoiceId() != null ? allocation.invoiceId() : "null",
                  allocation.purchaseId() != null ? allocation.purchaseId() : "null",
                  normalizedAllocationAmount(
                      ValidationUtils.requirePositive(allocation.appliedAmount(), "appliedAmount")),
                  normalizedAllocationAmount(allocation.discountAmount()),
                  normalizedAllocationAmount(allocation.writeOffAmount()),
                  normalizedAllocationAmount(allocation.fxAdjustment()));
      counts.merge(signature, 1, Integer::sum);
    }
    return counts;
  }

  private String allocationSignatureDigest(Map<String, Integer> signatureCounts) {
    if (signatureCounts == null || signatureCounts.isEmpty()) {
      return "<none>";
    }
    return signatureCounts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> entry.getKey() + " x" + entry.getValue())
        .reduce((left, right) -> left + "; " + right)
        .orElse("<none>");
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

  private Map<JournalLineSignature, Integer> lineSignatureCountsFromRequests(
      List<JournalEntryRequest.JournalLineRequest> lines) {
    Map<JournalLineSignature, Integer> counts = new HashMap<>();
    if (lines == null) {
      return counts;
    }
    for (JournalEntryRequest.JournalLineRequest line : lines) {
      if (line.accountId() == null) {
        continue;
      }
      JournalLineSignature signature =
          new JournalLineSignature(
              line.accountId(), roundedAmount(line.debit()), roundedAmount(line.credit()));
      counts.merge(signature, 1, Integer::sum);
    }
    return counts;
  }

  private BigDecimal roundedAmount(BigDecimal amount) {
    return amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
  }

  private record JournalLineSignature(Long accountId, BigDecimal debit, BigDecimal credit) {}

  private String normalizedAllocationAmount(BigDecimal amount) {
    return MoneyUtils.zeroIfNull(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }
}
