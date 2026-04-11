package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public record PartnerStatementResponse(
    Long partnerId,
    String partnerName,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    List<StatementTransactionDto> transactions) {}
