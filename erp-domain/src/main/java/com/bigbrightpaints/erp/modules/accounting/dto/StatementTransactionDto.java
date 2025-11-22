package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StatementTransactionDto(LocalDate entryDate,
                                      String referenceNumber,
                                      String memo,
                                      BigDecimal debit,
                                      BigDecimal credit,
                                      BigDecimal runningBalance,
                                      Long journalEntryId) {}
