package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record AccountingTransactionAuditListItemDto(
    Long journalEntryId,
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
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    Long reversalOfId,
    Long reversalEntryId,
    String correctionType,
    String consistencyStatus,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant postedAt) {}
