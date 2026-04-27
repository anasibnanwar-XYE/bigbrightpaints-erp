package com.bigbrightpaints.erp.modules.reports.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.reports.dto.ReportSource;

final class ReportFixtures {

  private ReportFixtures() {}

  static ReportQuerySupport.FinancialQueryWindow window(
      LocalDate startDate, LocalDate endDate, LocalDate asOfDate) {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", 7000L);
    company.setCode("RPT");
    company.setTimezone("UTC");
    return new ReportQuerySupport.FinancialQueryWindow(
        company,
        startDate,
        endDate,
        asOfDate,
        null,
        null,
        ReportSource.LIVE,
        new ReportQuerySupport.ExportOptions(true, true, null));
  }

  static Account account(Long id, String code, String name, AccountType type) {
    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", id);
    account.setCode(code);
    account.setName(name);
    account.setType(type);
    return account;
  }

  static Account account(Long id, String code, String name, AccountType type, String balance) {
    Account account = account(id, code, name, type);
    account.setBalance(new BigDecimal(balance));
    return account;
  }

  static Object[] row(Long accountId, String debit, String credit) {
    return new Object[] {accountId, new BigDecimal(debit), new BigDecimal(credit)};
  }

  static Object[] row(AccountType type, String debit, String credit) {
    return new Object[] {type, new BigDecimal(debit), new BigDecimal(credit)};
  }
}
