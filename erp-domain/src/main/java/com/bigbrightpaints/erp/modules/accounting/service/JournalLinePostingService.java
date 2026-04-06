package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;

@Service
class JournalLinePostingService {

  private static final BigDecimal FX_ROUNDING_TOLERANCE = new BigDecimal("0.05");

  private final JournalReferenceService journalReferenceService;

  JournalLinePostingService(JournalReferenceService journalReferenceService) {
    this.journalReferenceService = journalReferenceService;
  }

  JournalLine buildPostedLine(
      JournalEntry entry,
      JournalEntryRequest.JournalLineRequest lineRequest,
      Map<Long, Account> lockedAccounts,
      BigDecimal fxRate) {
    if (lineRequest.accountId() == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Account is required for every journal line");
    }
    Account account = lockedAccounts.get(lineRequest.accountId());
    if (account == null) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_REFERENCE, "Account not found");
    }
    BigDecimal debitInput = lineRequest.debit() == null ? BigDecimal.ZERO : lineRequest.debit();
    BigDecimal creditInput = lineRequest.credit() == null ? BigDecimal.ZERO : lineRequest.credit();
    if (debitInput.compareTo(BigDecimal.ZERO) < 0 || creditInput.compareTo(BigDecimal.ZERO) < 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Debit/Credit cannot be negative");
    }
    if (debitInput.compareTo(BigDecimal.ZERO) > 0 && creditInput.compareTo(BigDecimal.ZERO) > 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Debit and credit cannot both be non-zero on the same line");
    }
    JournalLine line = new JournalLine();
    line.setJournalEntry(entry);
    line.setAccount(account);
    line.setDescription(lineRequest.description());
    line.setDebit(journalReferenceService.toBaseCurrency(debitInput, fxRate));
    line.setCredit(journalReferenceService.toBaseCurrency(creditInput, fxRate));
    entry.addLine(line);
    return line;
  }

  void absorbRoundingDelta(
      BigDecimal roundingDelta,
      List<JournalLine> postedLines,
      Map<Account, BigDecimal> accountDeltas) {
    if (roundingDelta.abs().compareTo(FX_ROUNDING_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Journal entry must balance")
          .withDetail("delta", roundingDelta);
    }
    if (roundingDelta.signum() > 0) {
      JournalLine target =
          postedLines.stream()
              .filter(line -> line.getCredit().compareTo(BigDecimal.ZERO) > 0)
              .max(Comparator.comparing(JournalLine::getCredit))
              .orElse(null);
      if (target != null) {
        target.setCredit(target.getCredit().add(roundingDelta));
        accountDeltas.merge(target.getAccount(), roundingDelta.negate(), BigDecimal::add);
      }
      return;
    }
    BigDecimal adjust = roundingDelta.abs();
    JournalLine target =
        postedLines.stream()
            .filter(line -> line.getDebit().compareTo(BigDecimal.ZERO) > 0)
            .max(Comparator.comparing(JournalLine::getDebit))
            .orElse(null);
    if (target != null) {
      target.setDebit(target.getDebit().add(adjust));
      accountDeltas.merge(target.getAccount(), adjust, BigDecimal::add);
    }
  }
}
