package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.TenantSeedStatusDto;

@ExtendWith(MockitoExtension.class)
class TenantDefaultSeedingServiceTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private AccountingPeriodService accountingPeriodService;
  @Mock private AuditService auditService;

  @Test
  void seedDefaults_createsTenantOwnedCoaMappingsSettingsAndTrackingIdempotently() {
    Company company = company(77L, "M7GST", BigDecimal.valueOf(18), "MANUFACTURING");
    List<Account> accounts = new ArrayList<>();
    TenantDefaultSeedingService service = service(company, accounts);

    TenantSeedStatusDto first = service.seedDefaults(company);
    int accountCountAfterFirstRun = accounts.size();
    TenantSeedStatusDto second = service.seedDefaults(company);

    assertThat(first.ready()).isTrue();
    assertThat(first.templateCode()).isEqualTo("MANUFACTURING");
    assertThat(first.chartOfAccounts().accountCount()).isGreaterThanOrEqualTo(50);
    assertThat(first.chartOfAccounts().duplicateAccountCodesPresent()).isFalse();
    assertThat(first.chartOfAccounts().requiredClasses())
        .containsExactly("ASSET", "LIABILITY", "EQUITY", "REVENUE", "COGS", "EXPENSE");
    assertThat(first.gstDefaults().gstEnabled()).isTrue();
    assertThat(first.gstDefaults().inputAccountId()).isNotNull();
    assertThat(first.gstDefaults().outputAccountId()).isNotNull();
    assertThat(first.gstDefaults().payableAccountId()).isNotNull();
    assertThat(first.accountingMappings())
        .extracting(TenantSeedStatusDto.AccountingMapping::key)
        .contains(
            "DEFAULT_BANK",
            "DEFAULT_CASH",
            "DEFAULT_REVENUE",
            "DEFAULT_PURCHASE",
            "DEFAULT_AR",
            "DEFAULT_AP",
            "DEFAULT_TAX",
            "DEFAULT_ROUNDING",
            "DEFAULT_DISCOUNT",
            "DEFAULT_FREIGHT");
    assertThat(first.accountingMappings())
        .allSatisfy(
            mapping -> {
              assertThat(mapping.tenantOwned()).isTrue();
              assertThat(mapping.active()).isTrue();
              assertThat(mapping.locked()).isTrue();
            });
    assertThat(first.numbering())
        .extracting(TenantSeedStatusDto.NumberingDefault::documentType)
        .contains("INVOICE", "SALES_ORDER", "PURCHASE_ORDER", "RECEIPT");
    assertThat(first.paymentModes()).contains("CASH", "BANK_TRANSFER", "UPI", "CREDIT");
    assertThat(first.documentPrefixes())
        .extracting(TenantSeedStatusDto.DocumentPrefix::prefix)
        .contains("INV-", "SO-", "PO-", "RCP-");
    assertThat(first.roleTemplates())
        .extracting(TenantSeedStatusDto.RoleTemplate::key)
        .containsExactly("TENANT_OWNER", "TENANT_ADMIN", "TENANT_STAFF");
    assertThat(first.seedRuns())
        .hasSize(9)
        .allSatisfy(run -> assertThat(run.status()).isEqualTo("COMPLETE"));

    assertThat(accounts).hasSize(accountCountAfterFirstRun);
    assertThat(second.repairOutcome()).isEqualTo("NOOP");
    assertThat(second.ready()).isTrue();
  }

  @Test
  void seedDefaults_keepsNonGstMappingsDisabledWithoutBlockingReadiness() {
    Company company = company(88L, "M7NONGST", BigDecimal.ZERO, "SME");
    TenantDefaultSeedingService service = service(company, new ArrayList<>());

    TenantSeedStatusDto status = service.seedDefaults(company);

    assertThat(status.ready()).isTrue();
    assertThat(status.gstDefaults().gstEnabled()).isFalse();
    assertThat(status.gstDefaults().mappingStatus()).isEqualTo("DISABLED");
    assertThat(status.gstDefaults().inputAccountId()).isNull();
    assertThat(status.gstDefaults().outputAccountId()).isNull();
    assertThat(status.gstDefaults().payableAccountId()).isNull();
    assertThat(company.getGstInputTaxAccountId()).isNull();
    assertThat(company.getGstOutputTaxAccountId()).isNull();
    assertThat(company.getGstPayableAccountId()).isNull();
    assertThat(company.getDefaultTaxAccountId()).isNotNull();
  }

  @Test
  void mappingChanges_rejectCrossTenantAccountsAndLockedCoreMappingsWithAudit() {
    Company company = company(77L, "M7LOCK", BigDecimal.valueOf(18), "SME");
    Company otherCompany = company(99L, "OTHER", BigDecimal.valueOf(18), "SME");
    List<Account> accounts = new ArrayList<>();
    TenantDefaultSeedingService service = service(company, accounts);
    service.seedDefaults(company);
    Account tenantAccount = accounts.get(0);
    Account otherAccount = account(900L, otherCompany, "OTHER-CASH", AccountType.ASSET);
    when(accountRepository.findById(tenantAccount.getId())).thenReturn(Optional.of(tenantAccount));
    when(accountRepository.findById(otherAccount.getId())).thenReturn(Optional.of(otherAccount));
    AuditLog auditLog = new AuditLog();
    auditLog.setId(601L);
    when(auditService.logAuthSuccessRequired(
            eq(AuditEvent.CONFIGURATION_CHANGED), any(), eq("M7LOCK"), any()))
        .thenReturn(auditLog);

    assertThatThrownBy(
            () -> service.rejectCoreMappingRemap(77L, "DEFAULT_REVENUE", otherAccount.getId()))
        .hasMessageContaining("tenant-owned account");
    assertThatThrownBy(
            () -> service.rejectCoreMappingRemap(77L, "DEFAULT_REVENUE", tenantAccount.getId()))
        .hasMessageContaining("locked")
        .hasMessageContaining("auditEventId=601");
    assertThatThrownBy(() -> service.rejectCoreMappingDelete(77L, "DEFAULT_REVENUE"))
        .hasMessageContaining("locked")
        .hasMessageContaining("auditEventId=601");
  }

  private TenantDefaultSeedingService service(Company company, List<Account> accounts) {
    AtomicLong accountIds = new AtomicLong(1L);
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);
    when(accountRepository.findByCompanyOrderByCodeAsc(company)).thenAnswer(invocation -> accounts);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account account = invocation.getArgument(0);
              if (account.getId() == null) {
                ReflectionTestUtils.setField(account, "id", accountIds.getAndIncrement());
              }
              accounts.add(account);
              return account;
            });
    AccountingPeriod period = new AccountingPeriod();
    ReflectionTestUtils.setField(period, "id", 501L);
    when(accountingPeriodService.ensurePeriod(eq(company), any(LocalDate.class)))
        .thenReturn(period);
    return new TenantDefaultSeedingService(
        companyRepository, accountRepository, accountingPeriodService, auditService);
  }

  private Company company(Long id, String code, BigDecimal gstRate, String template) {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", id);
    company.setCode(code);
    company.setName(code + " Company");
    company.setTimezone("UTC");
    company.setDefaultGstRate(gstRate);
    company.setOnboardingCoaTemplateCode(template);
    return company;
  }

  private Account account(Long id, Company company, String code, AccountType type) {
    Account account = new Account();
    ReflectionTestUtils.setField(account, "id", id);
    account.setCompany(company);
    account.setCode(code);
    account.setName(code + " Account");
    account.setType(type);
    return account;
  }
}
