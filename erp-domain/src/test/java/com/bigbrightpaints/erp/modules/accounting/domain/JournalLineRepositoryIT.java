package com.bigbrightpaints.erp.modules.accounting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class JournalLineRepositoryIT extends AbstractIntegrationTest {

  @Autowired private JournalLineRepository journalLineRepository;

  @Autowired private JournalEntryRepository journalEntryRepository;

  @Autowired private AccountRepository accountRepository;

  @Test
  void summarizeTotalsByCompanyAndJournalEntryIds_isTenantScoped() {
    String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    Company companyA = dataSeeder.ensureCompany("JLR-A-" + suffix, "Journal Repo A " + suffix);
    Company companyB = dataSeeder.ensureCompany("JLR-B-" + suffix, "Journal Repo B " + suffix);

    Account accountA = saveAccount(companyA, "CASH-A-" + suffix);
    Account accountB = saveAccount(companyB, "CASH-B-" + suffix);

    JournalEntry entryA = saveJournalEntry(companyA, "JE-A-" + suffix);
    JournalEntry entryB = saveJournalEntry(companyB, "JE-B-" + suffix);

    saveLine(entryA, accountA, new BigDecimal("100.00"), BigDecimal.ZERO);
    saveLine(entryB, accountB, new BigDecimal("250.00"), BigDecimal.ZERO);

    List<JournalLineRepository.JournalEntryLineTotals> totals =
        journalLineRepository.summarizeTotalsByCompanyAndJournalEntryIds(
            companyA, List.of(entryA.getId(), entryB.getId()));

    assertThat(totals).hasSize(1);
    JournalLineRepository.JournalEntryLineTotals only = totals.getFirst();
    assertThat(only.getJournalEntryId()).isEqualTo(entryA.getId());
    assertThat(only.getTotalDebit()).isEqualByComparingTo("100.00");
    assertThat(only.getTotalCredit()).isEqualByComparingTo("0.00");
  }

  private Account saveAccount(Company company, String code) {
    Account account = new Account();
    account.setCompany(company);
    account.setCode(code);
    account.setName(code + " Account");
    account.setType(AccountType.ASSET);
    return accountRepository.saveAndFlush(account);
  }

  private JournalEntry saveJournalEntry(Company company, String referenceNumber) {
    JournalEntry entry = new JournalEntry();
    entry.setCompany(company);
    entry.setReferenceNumber(referenceNumber);
    entry.setEntryDate(LocalDate.now());
    entry.setStatus("POSTED");
    entry.setMemo("tenant scope test");
    return journalEntryRepository.saveAndFlush(entry);
  }

  private void saveLine(JournalEntry entry, Account account, BigDecimal debit, BigDecimal credit) {
    JournalLine line = new JournalLine();
    line.setJournalEntry(entry);
    line.setAccount(account);
    line.setDebit(debit);
    line.setCredit(credit);
    journalLineRepository.saveAndFlush(line);
  }
}
