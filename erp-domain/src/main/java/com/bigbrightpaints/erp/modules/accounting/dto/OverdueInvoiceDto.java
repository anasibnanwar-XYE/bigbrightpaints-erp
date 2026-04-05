package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record OverdueInvoiceDto(
    String invoiceNumber,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate issueDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dueDate,
    long daysOverdue,
    BigDecimal outstandingAmount) {}
