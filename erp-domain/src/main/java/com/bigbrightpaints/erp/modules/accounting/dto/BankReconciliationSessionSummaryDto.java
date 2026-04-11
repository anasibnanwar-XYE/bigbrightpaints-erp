package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record BankReconciliationSessionSummaryDto(
    Long sessionId,
    String referenceNumber,
    Long bankAccountId,
    String bankAccountCode,
    String bankAccountName,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate statementDate,
    BigDecimal statementEndingBalance,
    String status,
    String createdBy,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant completedAt,
    BankReconciliationSummaryDto summary,
    int clearedItemCount) {}
