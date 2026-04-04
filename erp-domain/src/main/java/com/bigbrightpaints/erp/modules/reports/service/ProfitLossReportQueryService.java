package com.bigbrightpaints.erp.modules.reports.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.bigbrightpaints.erp.modules.reports.dto.ReportSource;

@Service
@Transactional(readOnly = true)
public class ProfitLossReportQueryService {

  private final ReportQuerySupport reportQuerySupport;
  private final AccountingPeriodRepository accountingPeriodRepository;
  private final AccountingPeriodSnapshotRepository snapshotRepository;
  private final AccountingPeriodTrialBalanceLineRepository snapshotLineRepository;
  private final JournalLineRepository journalLineRepository;

  public ProfitLossReportQueryService(
      ReportQuerySupport reportQuerySupport,
      AccountingPeriodRepository accountingPeriodRepository,
      AccountingPeriodSnapshotRepository snapshotRepository,
      AccountingPeriodTrialBalanceLineRepository snapshotLineRepository,
      JournalLineRepository journalLineRepository) {
    this.reportQuerySupport = reportQuerySupport;
    this.accountingPeriodRepository = accountingPeriodRepository;
    this.snapshotRepository = snapshotRepository;
    this.snapshotLineRepository = snapshotLineRepository;
    this.journalLineRepository = journalLineRepository;
  }

  public ProfitLossDto generate(FinancialReportQueryRequest request) {
    ReportQuerySupport.FinancialQueryWindow primaryWindow =
        reportQuerySupport.resolveWindow(request);
    ProfitLossSnapshot primary = summarize(primaryWindow);

    ProfitLossDto.Comparative comparative = null;
    ReportQuerySupport.FinancialComparisonWindow comparison =
        reportQuerySupport.resolveComparison(request);
    if (comparison != null) {
      ReportQuerySupport.FinancialQueryWindow comparativeWindow = comparison.window();
      ProfitLossSnapshot comparativeSnapshot = summarize(comparativeWindow);
      comparative =
          new ProfitLossDto.Comparative(
              comparativeSnapshot.revenue(),
              comparativeSnapshot.costOfGoodsSold(),
              comparativeSnapshot.grossProfit(),
              comparativeSnapshot.operatingExpenses(),
              comparativeSnapshot.expenseCategories(),
              comparativeSnapshot.netIncome(),
              reportQuerySupport.metadata(comparativeWindow));
    }

    return new ProfitLossDto(
        primary.revenue(),
        primary.costOfGoodsSold(),
        primary.grossProfit(),
        primary.operatingExpenses(),
        primary.expenseCategories(),
        primary.netIncome(),
        reportQuerySupport.metadata(primaryWindow),
        comparative);
  }

  private ProfitLossSnapshot summarize(ReportQuerySupport.FinancialQueryWindow window) {
    Map<AccountType, BigDecimal> naturalBalances =
        usesClosedSnapshot(window) ? fromClosedSnapshot(window) : fromJournalSummary(window);

    BigDecimal revenue =
        safe(naturalBalances.get(AccountType.REVENUE))
            .add(safe(naturalBalances.get(AccountType.OTHER_INCOME)));
    BigDecimal cogs = safe(naturalBalances.get(AccountType.COGS));
    BigDecimal grossProfit = revenue.subtract(cogs);

    BigDecimal operatingExpenses =
        safe(naturalBalances.get(AccountType.EXPENSE))
            .add(safe(naturalBalances.get(AccountType.OTHER_EXPENSE)));
    List<ProfitLossDto.ExpenseCategory> expenseCategories = new ArrayList<>();
    expenseCategories.add(
        new ProfitLossDto.ExpenseCategory(
            "OPERATING", safe(naturalBalances.get(AccountType.EXPENSE))));
    expenseCategories.add(
        new ProfitLossDto.ExpenseCategory(
            "OTHER", safe(naturalBalances.get(AccountType.OTHER_EXPENSE))));

    BigDecimal netIncome = grossProfit.subtract(operatingExpenses);
    return new ProfitLossSnapshot(
        revenue, cogs, grossProfit, operatingExpenses, expenseCategories, netIncome);
  }

  private boolean usesClosedSnapshot(ReportQuerySupport.FinancialQueryWindow window) {
    if (window.source() != ReportSource.SNAPSHOT
        || window.snapshot() == null
        || window.period() == null) {
      return false;
    }
    return window.startDate().equals(window.period().getStartDate())
        && window.endDate().equals(window.period().getEndDate());
  }

  private Map<AccountType, BigDecimal> fromClosedSnapshot(
      ReportQuerySupport.FinancialQueryWindow window) {
    Map<AccountType, BigDecimal> currentBalances = snapshotBalances(window.snapshot());
    Map<AccountType, BigDecimal> periodBalances = new EnumMap<>(AccountType.class);
    Map<AccountType, BigDecimal> priorBalances = resolvePriorBalances(window);
    for (AccountType type : PROFIT_LOSS_TYPES) {
      periodBalances.put(
          type, safe(currentBalances.get(type)).subtract(safe(priorBalances.get(type))));
    }
    return periodBalances;
  }

  private Map<AccountType, BigDecimal> resolvePriorBalances(
      ReportQuerySupport.FinancialQueryWindow window) {
    LocalDate previousMonth = window.period().getStartDate().minusDays(1);
    AccountingPeriod previousPeriod =
        accountingPeriodRepository
            .findByCompanyAndYearAndMonth(
                window.company(), previousMonth.getYear(), previousMonth.getMonthValue())
            .orElse(null);
    if (previousPeriod == null) {
      return fromJournalSummaryUpTo(window.company(), previousMonth);
    }
    AccountingPeriodSnapshot previousSnapshot =
        snapshotRepository.findByCompanyAndPeriod(window.company(), previousPeriod).orElse(null);
    if (previousSnapshot != null) {
      return snapshotBalances(previousSnapshot);
    }
    if (previousPeriod.getStatus() == AccountingPeriodStatus.CLOSED) {
      throw new ApplicationException(
              ErrorCode.BUSINESS_CONSTRAINT_VIOLATION,
              "Prior period snapshot is required for closed-period profit and loss reports")
          .withDetail("companyId", window.company() != null ? window.company().getId() : null)
          .withDetail("periodId", window.period() != null ? window.period().getId() : null)
          .withDetail("priorPeriodId", previousPeriod.getId());
    }
    return fromJournalSummaryUpTo(window.company(), previousMonth);
  }

  private Map<AccountType, BigDecimal> snapshotBalances(AccountingPeriodSnapshot snapshot) {
    Map<AccountType, BigDecimal> balances = new EnumMap<>(AccountType.class);
    List<AccountingPeriodTrialBalanceLine> lines =
        snapshotLineRepository.findBySnapshotOrderByAccountCodeAsc(snapshot);
    for (AccountingPeriodTrialBalanceLine line : lines) {
      if (line == null || !isProfitLossType(line.getAccountType())) {
        continue;
      }
      BigDecimal natural =
          toNatural(line.getAccountType(), safe(line.getDebit()), safe(line.getCredit()));
      balances.merge(line.getAccountType(), natural, BigDecimal::add);
    }
    return balances;
  }

  private Map<AccountType, BigDecimal> fromJournalSummary(
      ReportQuerySupport.FinancialQueryWindow window) {
    List<Object[]> summarized =
        journalLineRepository.summarizeByAccountType(
            window.company(), window.startDate(), window.endDate());
    return toNaturalBalances(summarized);
  }

  private Map<AccountType, BigDecimal> fromJournalSummaryUpTo(Company company, LocalDate endDate) {
    List<Object[]> summarized = journalLineRepository.summarizeByAccountTypeUpTo(company, endDate);
    return toNaturalBalances(summarized);
  }

  private Map<AccountType, BigDecimal> toNaturalBalances(List<Object[]> summarized) {
    Map<AccountType, BigDecimal> naturalBalances = new EnumMap<>(AccountType.class);
    for (Object[] row : summarized) {
      if (row == null || row.length < 3 || row[0] == null) {
        continue;
      }
      AccountType type = (AccountType) row[0];
      BigDecimal debit = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
      BigDecimal credit = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
      BigDecimal natural = toNatural(type, debit, credit);
      naturalBalances.merge(type, natural, BigDecimal::add);
    }
    return naturalBalances;
  }

  private boolean isProfitLossType(AccountType type) {
    return PROFIT_LOSS_TYPES.contains(type);
  }

  private BigDecimal toNatural(AccountType type, BigDecimal debit, BigDecimal credit) {
    if (type == null || type.isDebitNormalBalance()) {
      return safe(debit).subtract(safe(credit));
    }
    return safe(credit).subtract(safe(debit));
  }

  private BigDecimal safe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static final List<AccountType> PROFIT_LOSS_TYPES =
      List.of(
          AccountType.REVENUE,
          AccountType.OTHER_INCOME,
          AccountType.COGS,
          AccountType.EXPENSE,
          AccountType.OTHER_EXPENSE);

  private record ProfitLossSnapshot(
      BigDecimal revenue,
      BigDecimal costOfGoodsSold,
      BigDecimal grossProfit,
      BigDecimal operatingExpenses,
      List<ProfitLossDto.ExpenseCategory> expenseCategories,
      BigDecimal netIncome) {}
}
