package com.bigbrightpaints.erp.modules.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodSnapshot;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodSnapshotRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodTrialBalanceLine;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodTrialBalanceLineRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.reports.dto.ProfitLossDto;
import com.bigbrightpaints.erp.modules.reports.dto.ReportMetadata;
import com.bigbrightpaints.erp.modules.reports.dto.ReportSource;

@ExtendWith(MockitoExtension.class)
class ProfitLossReportQueryServiceTest {

  @Mock private ReportQuerySupport reportQuerySupport;
  @Mock private AccountingPeriodRepository accountingPeriodRepository;
  @Mock private AccountingPeriodSnapshotRepository snapshotRepository;
  @Mock private AccountingPeriodTrialBalanceLineRepository snapshotLineRepository;
  @Mock private JournalLineRepository journalLineRepository;

  @Test
  void generate_computesRevenueCogsAndCategorizedExpensesWithComparative() {
    ProfitLossReportQueryService service =
        new ProfitLossReportQueryService(
            reportQuerySupport,
            accountingPeriodRepository,
            snapshotRepository,
            snapshotLineRepository,
            journalLineRepository);

    ReportQuerySupport.FinancialQueryWindow primary =
        ReportFixtures.window(
            LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 3, 31));
    ReportQuerySupport.FinancialQueryWindow comparative =
        ReportFixtures.window(
            LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 2, 28));

    FinancialReportQueryRequest request =
        new FinancialReportQueryRequest(
            null,
            primary.startDate(),
            primary.endDate(),
            null,
            null,
            comparative.startDate(),
            comparative.endDate(),
            null,
            "PDF");

    when(reportQuerySupport.resolveWindow(request)).thenReturn(primary);
    when(reportQuerySupport.resolveComparison(request))
        .thenReturn(new ReportQuerySupport.FinancialComparisonWindow(comparative));
    when(reportQuerySupport.metadata(primary))
        .thenReturn(
            new ReportMetadata(
                primary.asOfDate(),
                primary.startDate(),
                primary.endDate(),
                primary.source(),
                null,
                null,
                null,
                true,
                true,
                "PDF"));
    when(reportQuerySupport.metadata(comparative))
        .thenReturn(
            new ReportMetadata(
                comparative.asOfDate(),
                comparative.startDate(),
                comparative.endDate(),
                comparative.source(),
                null,
                null,
                null,
                true,
                true,
                "PDF"));

    when(journalLineRepository.summarizeByAccountType(
            primary.company(), primary.startDate(), primary.endDate()))
        .thenReturn(
            List.of(
                row(AccountType.REVENUE, "0.00", "1000.00"),
                row(AccountType.OTHER_INCOME, "0.00", "50.00"),
                row(AccountType.COGS, "400.00", "0.00"),
                row(AccountType.EXPENSE, "200.00", "0.00"),
                row(AccountType.OTHER_EXPENSE, "40.00", "0.00")));
    when(journalLineRepository.summarizeByAccountType(
            comparative.company(), comparative.startDate(), comparative.endDate()))
        .thenReturn(
            List.of(
                row(AccountType.REVENUE, "0.00", "800.00"),
                row(AccountType.COGS, "350.00", "0.00"),
                row(AccountType.EXPENSE, "150.00", "0.00"),
                row(AccountType.OTHER_EXPENSE, "20.00", "0.00")));

    ProfitLossDto dto = service.generate(request);

    assertThat(dto.revenue()).isEqualByComparingTo("1050.00");
    assertThat(dto.costOfGoodsSold()).isEqualByComparingTo("400.00");
    assertThat(dto.grossProfit()).isEqualByComparingTo("650.00");
    assertThat(dto.operatingExpenses()).isEqualByComparingTo("240.00");
    assertThat(dto.netIncome()).isEqualByComparingTo("410.00");
    assertThat(dto.operatingExpenseCategories()).hasSize(2);
    assertThat(dto.operatingExpenseCategories().get(0).category()).isEqualTo("OPERATING");
    assertThat(dto.operatingExpenseCategories().get(0).amount()).isEqualByComparingTo("200.00");
    assertThat(dto.operatingExpenseCategories().get(1).category()).isEqualTo("OTHER");
    assertThat(dto.operatingExpenseCategories().get(1).amount()).isEqualByComparingTo("40.00");

    assertThat(dto.comparative()).isNotNull();
    assertThat(dto.comparative().revenue()).isEqualByComparingTo("800.00");
    assertThat(dto.comparative().costOfGoodsSold()).isEqualByComparingTo("350.00");
    assertThat(dto.comparative().grossProfit()).isEqualByComparingTo("450.00");
    assertThat(dto.comparative().operatingExpenses()).isEqualByComparingTo("170.00");
    assertThat(dto.comparative().netIncome()).isEqualByComparingTo("280.00");
  }

  @Test
  void generate_usesSnapshotDeltaForClosedPeriodRange() {
    ProfitLossReportQueryService service =
        new ProfitLossReportQueryService(
            reportQuerySupport,
            accountingPeriodRepository,
            snapshotRepository,
            snapshotLineRepository,
            journalLineRepository);

    Company company = new Company();

    AccountingPeriod previousPeriod = new AccountingPeriod();
    previousPeriod.setStartDate(LocalDate.of(2026, 2, 1));
    previousPeriod.setEndDate(LocalDate.of(2026, 2, 28));

    AccountingPeriod period = new AccountingPeriod();
    period.setStartDate(LocalDate.of(2026, 3, 1));
    period.setEndDate(LocalDate.of(2026, 3, 31));

    AccountingPeriodSnapshot previousSnapshot = new AccountingPeriodSnapshot();
    AccountingPeriodSnapshot snapshot = new AccountingPeriodSnapshot();

    ReportQuerySupport.FinancialQueryWindow primary =
        new ReportQuerySupport.FinancialQueryWindow(
            company,
            period.getStartDate(),
            period.getEndDate(),
            period.getEndDate(),
            period,
            snapshot,
            ReportSource.SNAPSHOT,
            new ReportQuerySupport.ExportOptions(true, true, null));
    FinancialReportQueryRequest request =
        new FinancialReportQueryRequest(
            null,
            period.getStartDate(),
            period.getEndDate(),
            null,
            null,
            null,
            null,
            null,
            null);

    when(reportQuerySupport.resolveWindow(request)).thenReturn(primary);
    when(reportQuerySupport.metadata(primary))
        .thenReturn(
            new ReportMetadata(
                primary.asOfDate(),
                primary.startDate(),
                primary.endDate(),
                primary.source(),
                null,
                null,
                99L,
                true,
                true,
                null));
    when(accountingPeriodRepository.findByCompanyAndYearAndMonth(company, 2026, 2))
        .thenReturn(Optional.of(previousPeriod));
    when(snapshotRepository.findByCompanyAndPeriod(company, previousPeriod))
        .thenReturn(Optional.of(previousSnapshot));
    when(snapshotLineRepository.findBySnapshotOrderByAccountCodeAsc(snapshot))
        .thenReturn(
            List.of(
                snapshotLine(AccountType.REVENUE, "0.00", "1500.00"),
                snapshotLine(AccountType.OTHER_INCOME, "0.00", "100.00"),
                snapshotLine(AccountType.COGS, "525.00", "0.00"),
                snapshotLine(AccountType.EXPENSE, "370.00", "0.00"),
                snapshotLine(AccountType.OTHER_EXPENSE, "35.00", "0.00")));
    when(snapshotLineRepository.findBySnapshotOrderByAccountCodeAsc(previousSnapshot))
        .thenReturn(
            List.of(
                snapshotLine(AccountType.REVENUE, "0.00", "900.00"),
                snapshotLine(AccountType.OTHER_INCOME, "0.00", "20.00"),
                snapshotLine(AccountType.COGS, "325.00", "0.00"),
                snapshotLine(AccountType.EXPENSE, "250.00", "0.00"),
                snapshotLine(AccountType.OTHER_EXPENSE, "10.00", "0.00")));

    ProfitLossDto dto = service.generate(request);

    assertThat(dto.revenue()).isEqualByComparingTo("680.00");
    assertThat(dto.costOfGoodsSold()).isEqualByComparingTo("200.00");
    assertThat(dto.grossProfit()).isEqualByComparingTo("480.00");
    assertThat(dto.operatingExpenses()).isEqualByComparingTo("145.00");
    assertThat(dto.netIncome()).isEqualByComparingTo("335.00");
    assertThat(dto.metadata().source()).isEqualTo(ReportSource.SNAPSHOT);
    assertThat(dto.metadata().snapshotId()).isEqualTo(99L);

    verify(journalLineRepository, never())
        .summarizeByAccountType(primary.company(), primary.startDate(), primary.endDate());
  }

  @Test
  void generate_failsWhenPriorPeriodSnapshotIsMissingForClosedPeriodRange() {
    ProfitLossReportQueryService service =
        new ProfitLossReportQueryService(
            reportQuerySupport,
            accountingPeriodRepository,
            snapshotRepository,
            snapshotLineRepository,
            journalLineRepository);

    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", 7L);

    AccountingPeriod previousPeriod = new AccountingPeriod();
    ReflectionTestUtils.setField(previousPeriod, "id", 88L);
    previousPeriod.setStartDate(LocalDate.of(2026, 2, 1));
    previousPeriod.setEndDate(LocalDate.of(2026, 2, 28));
    previousPeriod.setStatus(AccountingPeriodStatus.CLOSED);

    AccountingPeriod period = new AccountingPeriod();
    ReflectionTestUtils.setField(period, "id", 99L);
    period.setStartDate(LocalDate.of(2026, 3, 1));
    period.setEndDate(LocalDate.of(2026, 3, 31));

    AccountingPeriodSnapshot snapshot = new AccountingPeriodSnapshot();

    ReportQuerySupport.FinancialQueryWindow primary =
        new ReportQuerySupport.FinancialQueryWindow(
            company,
            period.getStartDate(),
            period.getEndDate(),
            period.getEndDate(),
            period,
            snapshot,
            ReportSource.SNAPSHOT,
            new ReportQuerySupport.ExportOptions(true, true, null));
    FinancialReportQueryRequest request =
        new FinancialReportQueryRequest(
            null,
            period.getStartDate(),
            period.getEndDate(),
            null,
            null,
            null,
            null,
            null,
            null);

    when(reportQuerySupport.resolveWindow(request)).thenReturn(primary);
    when(accountingPeriodRepository.findByCompanyAndYearAndMonth(company, 2026, 2))
        .thenReturn(Optional.of(previousPeriod));
    when(snapshotRepository.findByCompanyAndPeriod(company, previousPeriod))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.generate(request))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex -> {
              ApplicationException appEx = (ApplicationException) ex;
              assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONSTRAINT_VIOLATION);
              assertThat(appEx.getUserMessage())
                  .isEqualTo(
                      "Prior period snapshot is required for closed-period profit and loss reports");
              assertThat(appEx.getDetails()).containsEntry("periodId", 99L);
              assertThat(appEx.getDetails()).containsEntry("priorPeriodId", 88L);
            });
  }

  @Test
  void generate_fallsBackToJournalBaselineWhenPriorPeriodDoesNotExist() {
    ProfitLossReportQueryService service =
        new ProfitLossReportQueryService(
            reportQuerySupport,
            accountingPeriodRepository,
            snapshotRepository,
            snapshotLineRepository,
            journalLineRepository);

    Company company = new Company();

    AccountingPeriod period = new AccountingPeriod();
    period.setStartDate(LocalDate.of(2026, 3, 1));
    period.setEndDate(LocalDate.of(2026, 3, 31));

    AccountingPeriodSnapshot snapshot = new AccountingPeriodSnapshot();

    ReportQuerySupport.FinancialQueryWindow primary =
        new ReportQuerySupport.FinancialQueryWindow(
            company,
            period.getStartDate(),
            period.getEndDate(),
            period.getEndDate(),
            period,
            snapshot,
            ReportSource.SNAPSHOT,
            new ReportQuerySupport.ExportOptions(true, true, null));
    FinancialReportQueryRequest request =
        new FinancialReportQueryRequest(
            null,
            period.getStartDate(),
            period.getEndDate(),
            null,
            null,
            null,
            null,
            null,
            null);

    when(reportQuerySupport.resolveWindow(request)).thenReturn(primary);
    when(reportQuerySupport.metadata(primary))
        .thenReturn(
            new ReportMetadata(
                primary.asOfDate(),
                primary.startDate(),
                primary.endDate(),
                primary.source(),
                null,
                null,
                100L,
                true,
                true,
                null));
    when(accountingPeriodRepository.findByCompanyAndYearAndMonth(company, 2026, 2))
        .thenReturn(Optional.empty());
    when(snapshotLineRepository.findBySnapshotOrderByAccountCodeAsc(snapshot))
        .thenReturn(
            List.of(
                snapshotLine(AccountType.REVENUE, "0.00", "1500.00"),
                snapshotLine(AccountType.OTHER_INCOME, "0.00", "100.00"),
                snapshotLine(AccountType.COGS, "525.00", "0.00"),
                snapshotLine(AccountType.EXPENSE, "370.00", "0.00"),
                snapshotLine(AccountType.OTHER_EXPENSE, "35.00", "0.00")));
    when(journalLineRepository.summarizeByAccountTypeUpTo(company, LocalDate.of(2026, 2, 28)))
        .thenReturn(
            List.of(
                row(AccountType.REVENUE, "0.00", "900.00"),
                row(AccountType.OTHER_INCOME, "0.00", "20.00"),
                row(AccountType.COGS, "325.00", "0.00"),
                row(AccountType.EXPENSE, "250.00", "0.00"),
                row(AccountType.OTHER_EXPENSE, "10.00", "0.00")));

    ProfitLossDto dto = service.generate(request);

    assertThat(dto.revenue()).isEqualByComparingTo("680.00");
    assertThat(dto.costOfGoodsSold()).isEqualByComparingTo("200.00");
    assertThat(dto.grossProfit()).isEqualByComparingTo("480.00");
    assertThat(dto.operatingExpenses()).isEqualByComparingTo("145.00");
    assertThat(dto.netIncome()).isEqualByComparingTo("335.00");

    verify(journalLineRepository, never())
        .summarizeByAccountType(primary.company(), primary.startDate(), primary.endDate());
    verify(journalLineRepository)
        .summarizeByAccountTypeUpTo(company, LocalDate.of(2026, 2, 28));
  }

  @Test
  void generate_fallsBackToJournalBaselineWhenPriorPeriodIsNotClosedAndHasNoSnapshot() {
    ProfitLossReportQueryService service =
        new ProfitLossReportQueryService(
            reportQuerySupport,
            accountingPeriodRepository,
            snapshotRepository,
            snapshotLineRepository,
            journalLineRepository);

    Company company = new Company();

    AccountingPeriod previousPeriod = new AccountingPeriod();
    previousPeriod.setStartDate(LocalDate.of(2026, 2, 1));
    previousPeriod.setEndDate(LocalDate.of(2026, 2, 28));
    previousPeriod.setStatus(AccountingPeriodStatus.OPEN);

    AccountingPeriod period = new AccountingPeriod();
    period.setStartDate(LocalDate.of(2026, 3, 1));
    period.setEndDate(LocalDate.of(2026, 3, 31));

    AccountingPeriodSnapshot snapshot = new AccountingPeriodSnapshot();

    ReportQuerySupport.FinancialQueryWindow primary =
        new ReportQuerySupport.FinancialQueryWindow(
            company,
            period.getStartDate(),
            period.getEndDate(),
            period.getEndDate(),
            period,
            snapshot,
            ReportSource.SNAPSHOT,
            new ReportQuerySupport.ExportOptions(true, true, null));
    FinancialReportQueryRequest request =
        new FinancialReportQueryRequest(
            null,
            period.getStartDate(),
            period.getEndDate(),
            null,
            null,
            null,
            null,
            null,
            null);

    when(reportQuerySupport.resolveWindow(request)).thenReturn(primary);
    when(reportQuerySupport.metadata(primary))
        .thenReturn(
            new ReportMetadata(
                primary.asOfDate(),
                primary.startDate(),
                primary.endDate(),
                primary.source(),
                null,
                null,
                101L,
                true,
                true,
                null));
    when(accountingPeriodRepository.findByCompanyAndYearAndMonth(company, 2026, 2))
        .thenReturn(Optional.of(previousPeriod));
    when(snapshotRepository.findByCompanyAndPeriod(company, previousPeriod))
        .thenReturn(Optional.empty());
    when(snapshotLineRepository.findBySnapshotOrderByAccountCodeAsc(snapshot))
        .thenReturn(
            List.of(
                snapshotLine(AccountType.REVENUE, "0.00", "1500.00"),
                snapshotLine(AccountType.OTHER_INCOME, "0.00", "100.00"),
                snapshotLine(AccountType.COGS, "525.00", "0.00"),
                snapshotLine(AccountType.EXPENSE, "370.00", "0.00"),
                snapshotLine(AccountType.OTHER_EXPENSE, "35.00", "0.00")));
    when(journalLineRepository.summarizeByAccountTypeUpTo(company, LocalDate.of(2026, 2, 28)))
        .thenReturn(
            List.of(
                row(AccountType.REVENUE, "0.00", "900.00"),
                row(AccountType.OTHER_INCOME, "0.00", "20.00"),
                row(AccountType.COGS, "325.00", "0.00"),
                row(AccountType.EXPENSE, "250.00", "0.00"),
                row(AccountType.OTHER_EXPENSE, "10.00", "0.00")));

    ProfitLossDto dto = service.generate(request);

    assertThat(dto.revenue()).isEqualByComparingTo("680.00");
    assertThat(dto.costOfGoodsSold()).isEqualByComparingTo("200.00");
    assertThat(dto.grossProfit()).isEqualByComparingTo("480.00");
    assertThat(dto.operatingExpenses()).isEqualByComparingTo("145.00");
    assertThat(dto.netIncome()).isEqualByComparingTo("335.00");

    verify(journalLineRepository, never())
        .summarizeByAccountType(primary.company(), primary.startDate(), primary.endDate());
    verify(journalLineRepository)
        .summarizeByAccountTypeUpTo(company, LocalDate.of(2026, 2, 28));
  }

  private Object[] row(AccountType type, String debit, String credit) {
    return new Object[] {type, new BigDecimal(debit), new BigDecimal(credit)};
  }

  private AccountingPeriodTrialBalanceLine snapshotLine(
      AccountType type, String debit, String credit) {
    AccountingPeriodTrialBalanceLine line = new AccountingPeriodTrialBalanceLine();
    line.setAccountType(type);
    line.setDebit(new BigDecimal(debit));
    line.setCredit(new BigDecimal(credit));
    return line;
  }
}
