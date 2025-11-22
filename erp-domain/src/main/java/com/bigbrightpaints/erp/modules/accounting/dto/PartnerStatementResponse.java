package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PartnerStatementResponse(
        Long partnerId,
        String partnerName,
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        List<StatementTransactionDto> transactions) {}
