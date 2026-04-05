package com.bigbrightpaints.erp.modules.accounting.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

public record JournalEntryDto(
    Long id,
    UUID publicId,
    String referenceNumber,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate entryDate,
    String memo,
    String status,
    Long dealerId,
    String dealerName,
    Long supplierId,
    String supplierName,
    Long accountingPeriodId,
    String accountingPeriodLabel,
    String accountingPeriodStatus,
    Long reversalOfEntryId,
    Long reversalEntryId,
    String correctionType,
    String correctionReason,
    String voidReason,
    List<JournalLineDto> lines,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant updatedAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant postedAt,
    String createdBy,
    String postedBy,
    String lastModifiedBy) {}
