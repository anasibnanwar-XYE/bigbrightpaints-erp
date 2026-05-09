package com.bigbrightpaints.erp.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryStatus;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingPeriodDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingPeriodReopenRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PeriodCloseRequestActionRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import com.bigbrightpaints.erp.test.support.TestDateUtils;

@Tag("critical")
@Tag("concurrency")
@Tag("reconciliation")
class PeriodCloseAtomicityTest extends AbstractIntegrationTest {

  private static final int PERIOD_CLOSE_LOCK_CLASS = 4242;
  private static final int PERIOD_CLOSE_LOCK_KEY = 20260509;
  private static final String PERIOD_CLOSE_BLOCKER_TRIGGER = "test_period_close_blocker";
  private static final String PERIOD_CLOSE_BLOCKER_FUNCTION = "test_period_close_blocker";

  @Autowired private AccountingPeriodService accountingPeriodService;
  @Autowired private AccountingService accountingService;
  @Autowired private AccountRepository accountRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private CompanyRepository companyRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private DataSource dataSource;

  @AfterEach
  void clearCompanyContext() {
    CompanyContextHolder.clear();
    SecurityContextHolder.clearContext();
  }

  @Test
  void closePeriod_blocksConcurrentPosting() throws Exception {
    String companyCode = "RISK-CLOSE-" + System.nanoTime();
    Company company = dataSeeder.ensureCompany(companyCode, companyCode + " Ltd");
    CompanyContextHolder.setCompanyCode(companyCode);
    try {
      LocalDate today = TestDateUtils.safeDate(company);
      AccountingPeriod period = accountingPeriodService.ensurePeriod(company, today);
      Account cash = ensureAccount(company, "CASH-LOCK", "Cash", AccountType.ASSET);
      Account revenue = ensureAccount(company, "REV-LOCK", "Revenue", AccountType.REVENUE);

      postJournal(
          today.minusDays(1),
          List.of(
              line(cash.getId(), new BigDecimal("100.00"), BigDecimal.ZERO),
              line(revenue.getId(), BigDecimal.ZERO, new BigDecimal("100.00"))));

      installPeriodCloseBlocker(period.getId());
      try (Connection lockConnection = dataSource.getConnection()) {
        acquirePeriodCloseBlocker(lockConnection);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
          CompletableFuture<AccountingPeriodDto> closeFuture =
              CompletableFuture.supplyAsync(
                  () -> {
                    CompanyContextHolder.setCompanyCode(companyCode);
                    try {
                      return forceClosePeriod(
                          period.getId(), "risk close request", "risk close approval");
                    } finally {
                      CompanyContextHolder.clear();
                      SecurityContextHolder.clearContext();
                    }
                  },
                  executor);

          awaitPeriodCloseBlocked(Duration.ofSeconds(5));

          CompletableFuture<?> postFuture =
              CompletableFuture.supplyAsync(
                  () -> {
                    CompanyContextHolder.setCompanyCode(companyCode);
                    try {
                      return postJournal(
                          today,
                          List.of(
                              line(cash.getId(), new BigDecimal("10.00"), BigDecimal.ZERO),
                              line(revenue.getId(), BigDecimal.ZERO, new BigDecimal("10.00"))));
                    } finally {
                      CompanyContextHolder.clear();
                    }
                  },
                  executor);

          Thread.sleep(200);
          assertThat(postFuture.isDone())
              .as("Posting should block while close holds lock")
              .isFalse();

          releasePeriodCloseBlocker(lockConnection);
          AccountingPeriodDto closed = closeFuture.get(10, TimeUnit.SECONDS);
          assertThat(closed.status()).isEqualTo("CLOSED");

          assertThatThrownBy(() -> postFuture.get(5, TimeUnit.SECONDS))
              .hasCauseInstanceOf(ApplicationException.class)
              .hasMessageContaining("locked/closed");
        } finally {
          executor.shutdownNow();
        }
      } finally {
        dropPeriodCloseBlocker();
      }
    } finally {
      CompanyContextHolder.clear();
    }
  }

  @Test
  void requestCloseAndReopen_areIdempotent() {
    String companyCode = "RISK-CLOSE-IDEMP-" + System.nanoTime();
    Company company = dataSeeder.ensureCompany(companyCode, companyCode + " Ltd");
    CompanyContextHolder.setCompanyCode(companyCode);
    try {
      LocalDate today = TestDateUtils.safeDate(company);
      AccountingPeriod period = accountingPeriodService.ensurePeriod(company, today);
      Account cash = ensureAccount(company, "CASH-IDEMP", "Cash", AccountType.ASSET);
      Account revenue = ensureAccount(company, "REV-IDEMP", "Revenue", AccountType.REVENUE);
      Account expense = ensureAccount(company, "EXP-IDEMP", "Expense", AccountType.EXPENSE);

      postJournal(
          today.minusDays(2),
          List.of(
              line(cash.getId(), new BigDecimal("200.00"), BigDecimal.ZERO),
              line(revenue.getId(), BigDecimal.ZERO, new BigDecimal("200.00"))));
      postJournal(
          today.minusDays(1),
          List.of(
              line(expense.getId(), new BigDecimal("50.00"), BigDecimal.ZERO),
              line(cash.getId(), BigDecimal.ZERO, new BigDecimal("50.00"))));

      authenticate("maker.user", "ROLE_ACCOUNTING");
      var firstRequest =
          accountingPeriodService.requestPeriodClose(
              period.getId(), new PeriodCloseRequestActionRequest("risk close request", true));
      var secondRequest =
          accountingPeriodService.requestPeriodClose(
              period.getId(),
              new PeriodCloseRequestActionRequest("risk close request retry", true));

      assertThat(secondRequest.id()).isEqualTo(firstRequest.id());

      authenticate("checker.user", "ROLE_ADMIN");
      AccountingPeriodDto closed =
          accountingPeriodService.approvePeriodClose(
              period.getId(), new PeriodCloseRequestActionRequest("risk close approval", true));

      assertThat(closed.status()).isEqualTo("CLOSED");
      assertThat(countClosingJournals(company, period)).isEqualTo(1);
      JournalEntry closing =
          journalEntryRepository
              .findByCompanyAndReferenceNumber(company, closingReference(period))
              .orElseThrow();
      assertThat(closing.getSourceReference()).isEqualTo(closingReference(period));
      assertThat(closing.getSourceModule()).isEqualTo("ACCOUNTING_PERIOD");

      authenticate("super.admin", "ROLE_SUPER_ADMIN");
      AccountingPeriodDto reopened =
          accountingPeriodService.reopenPeriod(
              period.getId(), new AccountingPeriodReopenRequest("risk reopen"));
      assertThat(reopened.status()).isEqualTo("OPEN");
      assertThat(reopened.closingJournalEntryId()).isNull();

      authenticate("super.admin", "ROLE_SUPER_ADMIN");
      AccountingPeriodDto reopenedAgain =
          accountingPeriodService.reopenPeriod(
              period.getId(), new AccountingPeriodReopenRequest("risk reopen retry"));
      assertThat(reopenedAgain.status()).isEqualTo("OPEN");
      assertThat(reopenedAgain.closingJournalEntryId()).isNull();
      closing =
          journalEntryRepository
              .findByCompanyAndReferenceNumber(company, closingReference(period))
              .orElseThrow();
      assertThat(closing.getStatus()).isEqualTo(JournalEntryStatus.REVERSED);
      Integer reversalCount =
          jdbcTemplate.queryForObject(
              "select count(*) from journal_entries where reversal_of_id = ?",
              Integer.class,
              closing.getId());
      assertThat(reversalCount).isNotNull().isEqualTo(1);
    } finally {
      CompanyContextHolder.clear();
    }
  }

  private Long postJournal(
      LocalDate entryDate, List<JournalEntryRequest.JournalLineRequest> lines) {
    JournalEntryRequest request =
        new JournalEntryRequest(
            "TEST-" + System.nanoTime(), entryDate, "risk seed", null, null, Boolean.FALSE, lines);
    return accountingService.createJournalEntry(request).id();
  }

  private JournalEntryRequest.JournalLineRequest line(
      Long accountId, BigDecimal debit, BigDecimal credit) {
    return new JournalEntryRequest.JournalLineRequest(accountId, "line", debit, credit);
  }

  private Account ensureAccount(Company company, String code, String name, AccountType type) {
    return accountRepository
        .findByCompanyAndCodeIgnoreCase(company, code)
        .orElseGet(
            () -> {
              Account account = new Account();
              account.setCompany(company);
              account.setCode(code);
              account.setName(name);
              account.setType(type);
              return accountRepository.save(account);
            });
  }

  private String closingReference(AccountingPeriod period) {
    return "PERIOD-CLOSE-" + period.getYear() + String.format("%02d", period.getMonth());
  }

  private int countClosingJournals(Company company, AccountingPeriod period) {
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from journal_entries where company_id = ? and reference_number = ?",
            Integer.class,
            company.getId(),
            closingReference(period));
    return count != null ? count : 0;
  }

  private AccountingPeriodDto forceClosePeriod(
      Long periodId, String requestNote, String approvalNote) {
    authenticate("maker.user", "ROLE_ACCOUNTING");
    accountingPeriodService.requestPeriodClose(
        periodId, new PeriodCloseRequestActionRequest(requestNote, true));
    authenticate("checker.user", "ROLE_ADMIN");
    return accountingPeriodService.approvePeriodClose(
        periodId, new PeriodCloseRequestActionRequest(approvalNote, true));
  }

  private void authenticate(String username, String... roles) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList()));
  }

  private void installPeriodCloseBlocker(Long periodId) {
    dropPeriodCloseBlocker();
    jdbcTemplate.execute(
        """
        create function test_period_close_blocker() returns trigger as $$
        begin
          if NEW.id = %d
              and OLD.status is distinct from NEW.status
              and NEW.status = 'CLOSED' then
            perform pg_advisory_lock(%d, %d);
            perform pg_advisory_unlock(%d, %d);
          end if;
          return NEW;
        end;
        $$ language plpgsql
        """
            .formatted(
                periodId,
                PERIOD_CLOSE_LOCK_CLASS,
                PERIOD_CLOSE_LOCK_KEY,
                PERIOD_CLOSE_LOCK_CLASS,
                PERIOD_CLOSE_LOCK_KEY));
    jdbcTemplate.execute(
        "create trigger "
            + PERIOD_CLOSE_BLOCKER_TRIGGER
            + " before update of status on accounting_periods for each row execute function "
            + PERIOD_CLOSE_BLOCKER_FUNCTION
            + "()");
  }

  private void dropPeriodCloseBlocker() {
    jdbcTemplate.execute(
        "drop trigger if exists " + PERIOD_CLOSE_BLOCKER_TRIGGER + " on accounting_periods");
    jdbcTemplate.execute("drop function if exists " + PERIOD_CLOSE_BLOCKER_FUNCTION + "()");
  }

  private void acquirePeriodCloseBlocker(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "select pg_advisory_lock("
              + PERIOD_CLOSE_LOCK_CLASS
              + ", "
              + PERIOD_CLOSE_LOCK_KEY
              + ")");
    }
  }

  private void releasePeriodCloseBlocker(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "select pg_advisory_unlock("
              + PERIOD_CLOSE_LOCK_CLASS
              + ", "
              + PERIOD_CLOSE_LOCK_KEY
              + ")");
    }
  }

  private void awaitPeriodCloseBlocked(Duration timeout) throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      Integer waiters =
          jdbcTemplate.queryForObject(
              """
              select count(*)
              from pg_locks
              where locktype = 'advisory'
                and classid = ?
                and objid = ?
                and not granted
              """,
              Integer.class,
              PERIOD_CLOSE_LOCK_CLASS,
              PERIOD_CLOSE_LOCK_KEY);
      if (waiters != null && waiters > 0) {
        return;
      }
      Thread.sleep(50);
    }
    throw new AssertionError("period close did not reach the database blocker in time");
  }
}
