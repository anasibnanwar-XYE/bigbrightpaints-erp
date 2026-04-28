package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
import com.bigbrightpaints.erp.modules.company.domain.TenantDefaultSeedRun;
import com.bigbrightpaints.erp.modules.company.domain.TenantDefaultSeedRunRepository;
import com.bigbrightpaints.erp.modules.company.dto.TenantSeedStatusDto;

@ExtendWith(MockitoExtension.class)
class TenantDefaultSeedingServiceTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private AccountRepository accountRepository;
  @Mock private TenantDefaultSeedRunRepository seedRunRepository;
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
        .containsExactly("ROLE_ACCOUNTING", "ROLE_FACTORY", "ROLE_SALES", "ROLE_DEALER");
    assertThat(first.roleTemplates())
        .extracting(TenantSeedStatusDto.RoleTemplate::key)
        .doesNotContain(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "TENANT_OWNER", "TENANT_ADMIN", "TENANT_STAFF");
    assertThat(first.roleTemplates())
        .allSatisfy(
            roleTemplate -> {
              assertThat(roleTemplate.displayName()).isNotBlank();
              assertThat(roleTemplate.permissions()).isNotEmpty();
            });
    assertThat(first.seedRuns())
        .hasSize(9)
        .allSatisfy(
            run -> {
              assertThat(run.runId()).isNotBlank();
              assertThat(run.status()).isEqualTo("COMPLETE");
              assertThat(run.operation()).isEqualTo("SEEDED");
              assertThat(run.completedAt()).isNotNull();
            });

    assertThat(accounts).hasSize(accountCountAfterFirstRun);
    assertThat(second.repairOutcome()).isEqualTo("NOOP");
    assertThat(second.ready()).isTrue();
    assertThat(second.seedRuns())
        .hasSize(9)
        .allSatisfy(run -> assertThat(run.operation()).isEqualTo("NOOP"));
  }

  @Test
  void getSeedStatus_readsPersistedRunsWithoutRefreshingRunIdsOrCompletedAt() {
    Company company = company(78L, "M7DURABLE", BigDecimal.valueOf(18), "SME");
    List<Account> accounts = new ArrayList<>();
    TenantDefaultSeedingService service = service(company, accounts);
    TenantSeedStatusDto seeded = service.seedDefaults(company);

    TenantSeedStatusDto firstRead = service.getSeedStatus(company.getId());
    TenantSeedStatusDto secondRead = service.getSeedStatus(company.getId());

    assertThat(firstRead.seedRuns()).hasSize(9);
    assertThat(firstRead.seedRuns())
        .extracting(TenantSeedStatusDto.SeedRun::runId)
        .containsExactlyElementsOf(
            seeded.seedRuns().stream().map(TenantSeedStatusDto.SeedRun::runId).toList());
    assertThat(secondRead.seedRuns())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyElementsOf(firstRead.seedRuns());
    verify(seedRunRepository, times(1)).saveAll(any(Iterable.class));
  }

  @Test
  void repairSeedStatus_recordsRepairedThenNoopWithoutDuplicatingRunRowsOrAccounts() {
    Company company = company(79L, "M7REPAIR", BigDecimal.valueOf(18), "SME");
    List<Account> accounts = new ArrayList<>();
    TenantDefaultSeedingService service = service(company, accounts);
    TenantSeedStatusDto seeded = service.seedDefaults(company);
    List<String> originalRunIds =
        seeded.seedRuns().stream().map(TenantSeedStatusDto.SeedRun::runId).toList();
    int seededAccountCount = accounts.size();
    accounts.removeIf(account -> "CASH".equals(account.getCode()));

    TenantSeedStatusDto repaired = service.repairSeedStatus(company.getId());
    TenantSeedStatusDto noop = service.repairSeedStatus(company.getId());

    assertThat(accounts).hasSize(seededAccountCount);
    assertThat(repaired.repairOutcome()).contains("REPAIRED", "auditEventId=601");
    assertThat(repaired.seedRuns())
        .hasSize(9)
        .allSatisfy(run -> assertThat(run.operation()).isEqualTo("REPAIRED"));
    assertThat(repaired.seedRuns())
        .extracting(TenantSeedStatusDto.SeedRun::runId)
        .containsExactlyElementsOf(originalRunIds);
    assertThat(noop.repairOutcome()).contains("NOOP", "auditEventId=601");
    assertThat(noop.seedRuns())
        .hasSize(9)
        .allSatisfy(run -> assertThat(run.operation()).isEqualTo("NOOP"));
    assertThat(noop.seedRuns())
        .extracting(TenantSeedStatusDto.SeedRun::runId)
        .containsExactlyElementsOf(originalRunIds);
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

  @Test
  void seedDefaultsFailClosedMarksTenantRecoverableAuditsAndRemovesPartialAccounts() {
    Company company = company(91L, "M7FAIL", BigDecimal.valueOf(18), "SME");
    List<Account> accounts = new ArrayList<>();
    AtomicLong accountIds = new AtomicLong(1L);
    AtomicReference<Account> partialAccount = new AtomicReference<>();
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);
    when(accountRepository.findByCompanyOrderByCodeAsc(company)).thenAnswer(invocation -> accounts);
    List<TenantDefaultSeedRun> seedRuns = new ArrayList<>();
    stubSeedRuns(company, seedRuns);
    when(accountRepository.save(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account account = invocation.getArgument(0);
              if (account.getId() == null) {
                ReflectionTestUtils.setField(account, "id", accountIds.getAndIncrement());
              }
              accounts.add(account);
              if (partialAccount.get() == null) {
                partialAccount.set(account);
                return account;
              }
              throw new IllegalStateException("forced seed failure");
            });
    doAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Iterable<Account> removed = invocation.getArgument(0);
              removed.forEach(accounts::remove);
              return null;
            })
        .when(accountRepository)
        .deleteAll(any(Iterable.class));
    AuditLog auditLog = new AuditLog();
    auditLog.setId(701L);
    when(auditService.logAuthSuccessRequired(
            eq(AuditEvent.CONFIGURATION_CHANGED), any(), eq("M7FAIL"), any()))
        .thenReturn(auditLog);
    TenantDefaultSeedingService service =
        new TenantDefaultSeedingService(
            companyRepository,
            accountRepository,
            seedRunRepository,
            accountingPeriodService,
            auditService);

    TenantDefaultSeedingService.SeedAttempt attempt = service.seedDefaultsFailClosed(company);

    assertThat(attempt.ready()).isFalse();
    assertThat(attempt.auditEventId()).isEqualTo(701L);
    assertThat(company.getLifecycleReason()).isEqualTo("SEED_FAILED");
    assertThat(company.getActivationStatus()).isEqualTo("NOT_SENT");
    assertThat(company.getActivationSentAt()).isNull();
    assertThat(company.getActivationExpiresAt()).isNull();
    assertThat(accounts).isEmpty();
    assertThat(attempt.status().readinessStatus()).isEqualTo("REPAIR_REQUIRED");
    assertThat(attempt.status().seedRuns())
        .hasSize(9)
        .allSatisfy(
            run -> {
              assertThat(run.status()).isEqualTo("REPAIR_REQUIRED");
              assertThat(run.operation()).isEqualTo("PENDING_REPAIR");
            });
    assertThat(attempt.status().repairOutcome())
        .contains("FAILED", "auditEventId=701", "errorCode=SEED_DEFAULTS_FAILED");
    verify(accountRepository).deleteAll(any(Iterable.class));
  }

  private TenantDefaultSeedingService service(Company company, List<Account> accounts) {
    AtomicLong accountIds = new AtomicLong(1L);
    List<TenantDefaultSeedRun> seedRuns = new ArrayList<>();
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);
    when(accountRepository.findByCompanyOrderByCodeAsc(company)).thenAnswer(invocation -> accounts);
    stubSeedRuns(company, seedRuns);
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
    AuditLog auditLog = new AuditLog();
    auditLog.setId(601L);
    org.mockito.Mockito.lenient()
        .when(
            auditService.logAuthSuccessRequired(
                eq(AuditEvent.CONFIGURATION_CHANGED), any(), eq(company.getCode()), any()))
        .thenReturn(auditLog);
    return new TenantDefaultSeedingService(
        companyRepository,
        accountRepository,
        seedRunRepository,
        accountingPeriodService,
        auditService);
  }

  private void stubSeedRuns(Company company, List<TenantDefaultSeedRun> seedRuns) {
    when(seedRunRepository.findByCompany_IdOrderByCategoryAsc(company.getId()))
        .thenAnswer(
            invocation ->
                seedRuns.stream()
                    .sorted(Comparator.comparing(TenantDefaultSeedRun::getCategory))
                    .toList());
    when(seedRunRepository.existsByCompany_Id(company.getId()))
        .thenAnswer(invocation -> !seedRuns.isEmpty());
    when(seedRunRepository.saveAll(any(Iterable.class)))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Iterable<TenantDefaultSeedRun> saved = invocation.getArgument(0);
              List<TenantDefaultSeedRun> result = new ArrayList<>();
              for (TenantDefaultSeedRun run : saved) {
                if (!seedRuns.contains(run)) {
                  seedRuns.add(run);
                }
                result.add(run);
              }
              return result;
            });
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
