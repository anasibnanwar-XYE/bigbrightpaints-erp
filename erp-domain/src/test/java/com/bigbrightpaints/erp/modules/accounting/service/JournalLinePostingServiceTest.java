package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;

@ExtendWith(MockitoExtension.class)
class JournalLinePostingServiceTest {

  @Mock private JournalReferenceService journalReferenceService;

  @BeforeEach
  void setUp() {
    lenient()
        .when(journalReferenceService.toBaseCurrency(any(BigDecimal.class), eq(BigDecimal.ONE)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void buildPostedLine_rejectsInactiveAccount() {
    JournalLinePostingService service =
        new JournalLinePostingService(journalReferenceService, "WARN");
    Account inactiveExpense = account(11L, "EXP-11", AccountType.EXPENSE, false);

    assertThatThrownBy(
            () ->
                service.buildPostedLine(
                    new JournalEntry(),
                    new JournalEntryRequest.JournalLineRequest(
                        11L, "line", new BigDecimal("10.00"), BigDecimal.ZERO),
                    Map.of(11L, inactiveExpense),
                    BigDecimal.ONE))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex -> {
              ApplicationException applicationException = (ApplicationException) ex;
              assertThat(applicationException.getErrorCode())
                  .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
              assertThat(applicationException.getUserMessage()).contains("inactive account");
            });
  }

  @Test
  void buildPostedLine_warnModeAllowsNormalBalanceDirectionConflict() {
    JournalLinePostingService service =
        new JournalLinePostingService(journalReferenceService, "WARN");
    Account expense = account(22L, "EXP-22", AccountType.EXPENSE, true);

    var line =
        service.buildPostedLine(
            new JournalEntry(),
            new JournalEntryRequest.JournalLineRequest(
                22L, "line", BigDecimal.ZERO, new BigDecimal("10.00")),
            Map.of(22L, expense),
            BigDecimal.ONE);

    assertThat(line.getAccount()).isEqualTo(expense);
    assertThat(line.getDebit()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(line.getCredit()).isEqualByComparingTo(new BigDecimal("10.00"));
  }

  @Test
  void buildPostedLine_rejectModeBlocksNormalBalanceDirectionConflict() {
    JournalLinePostingService service =
        new JournalLinePostingService(journalReferenceService, "REJECT");
    Account revenue = account(33L, "REV-33", AccountType.REVENUE, true);

    assertThatThrownBy(
            () ->
                service.buildPostedLine(
                    new JournalEntry(),
                    new JournalEntryRequest.JournalLineRequest(
                        33L, "line", new BigDecimal("10.00"), BigDecimal.ZERO),
                    Map.of(33L, revenue),
                    BigDecimal.ONE))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex -> {
              ApplicationException applicationException = (ApplicationException) ex;
              assertThat(applicationException.getErrorCode())
                  .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
              assertThat(applicationException.getUserMessage()).contains("normal balance");
            });
  }

  private Account account(Long id, String code, AccountType type, boolean active) {
    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", id);
    account.setCode(code);
    account.setType(type);
    account.setActive(active);
    account.setBalance(BigDecimal.ZERO);
    return account;
  }
}
