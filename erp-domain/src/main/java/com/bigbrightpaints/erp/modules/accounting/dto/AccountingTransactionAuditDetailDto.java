package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto;

public record AccountingTransactionAuditDetailDto(
    Long journalEntryId,
    UUID journalPublicId,
    String referenceNumber,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String status,
    String module,
    String transactionType,
    String memo,
    Long dealerId,
    String dealerName,
    Long supplierId,
    String supplierName,
    Long accountingPeriodId,
    String accountingPeriodLabel,
    String accountingPeriodStatus,
    Long reversalOfId,
    Long reversalEntryId,
    String correctionType,
    String correctionReason,
    String voidReason,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    String consistencyStatus,
    List<String> consistencyNotes,
    List<JournalLineDto> lines,
    List<LinkedDocument> linkedDocuments,
    List<SettlementAllocation> settlementAllocations,
    List<EventTrailItem> eventTrail,
    LinkedBusinessReferenceDto drivingDocument,
    List<LinkedBusinessReferenceDto> linkedReferenceChain,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant postedAt,
    String createdBy,
    String postedBy,
    String lastModifiedBy) {
  public record LinkedDocument(
      String documentType,
      Long documentId,
      String documentNumber,
      String status,
      BigDecimal totalAmount,
      BigDecimal outstandingAmount) {}

  public record SettlementAllocation(
      Long allocationId,
      String partnerType,
      Long dealerId,
      Long supplierId,
      Long invoiceId,
      Long purchaseId,
      BigDecimal allocationAmount,
      BigDecimal discountAmount,
      BigDecimal writeOffAmount,
      BigDecimal fxDifferenceAmount,
      String applicationType,
      String memo,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate settlementDate) {}

  public record EventTrailItem(
      Long eventId,
      String eventType,
      String aggregateType,
      UUID aggregateId,
      Long sequenceNumber,
      @JsonFormat(shape = JsonFormat.Shape.STRING) Instant eventTimestamp,
      @JsonFormat(pattern = "yyyy-MM-dd") LocalDate effectiveDate,
      Long accountId,
      String accountCode,
      BigDecimal debitAmount,
      BigDecimal creditAmount,
      BigDecimal balanceBefore,
      BigDecimal balanceAfter,
      String description,
      String userId,
      UUID correlationId) {}
}
