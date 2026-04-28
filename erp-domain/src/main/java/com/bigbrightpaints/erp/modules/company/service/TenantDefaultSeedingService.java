package com.bigbrightpaints.erp.modules.company.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantDefaultSeedRun;
import com.bigbrightpaints.erp.modules.company.domain.TenantDefaultSeedRunRepository;
import com.bigbrightpaints.erp.modules.company.dto.TenantSeedStatusDto;
import com.bigbrightpaints.erp.modules.rbac.domain.SystemRole;

@Service
public class TenantDefaultSeedingService {

  private static final Set<String> CORE_MAPPING_KEYS =
      Set.of(
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

  private static final List<String> SEED_CATEGORIES =
      List.of(
          "COA",
          "GST",
          "ACCOUNTING_MAPPINGS",
          "SETTINGS",
          "NUMBERING",
          "PAYMENT_MODES",
          "PREFIXES",
          "ROLES",
          "PERMISSION_TEMPLATES");

  private final CompanyRepository companyRepository;
  private final AccountRepository accountRepository;
  private final TenantDefaultSeedRunRepository seedRunRepository;
  private final AccountingPeriodService accountingPeriodService;
  private final AuditService auditService;

  public TenantDefaultSeedingService(
      CompanyRepository companyRepository,
      AccountRepository accountRepository,
      TenantDefaultSeedRunRepository seedRunRepository,
      AccountingPeriodService accountingPeriodService,
      AuditService auditService) {
    this.companyRepository = companyRepository;
    this.accountRepository = accountRepository;
    this.seedRunRepository = seedRunRepository;
    this.accountingPeriodService = accountingPeriodService;
    this.auditService = auditService;
  }

  @Transactional
  public TenantSeedStatusDto seedDefaults(Company company) {
    return seedDefaultsInternal(requireManagedCompany(company));
  }

  @Transactional
  public SeedAttempt seedDefaultsFailClosed(Company company) {
    Company lockedCompany = requireManagedCompany(company);
    Set<Long> accountIdsBefore =
        accountRepository.findByCompanyOrderByCodeAsc(lockedCompany).stream()
            .map(Account::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    try {
      TenantSeedStatusDto status = seedDefaultsInternal(lockedCompany);
      return SeedAttempt.ready(status);
    } catch (RuntimeException ex) {
      cleanupFailedSeedArtifacts(lockedCompany, accountIdsBefore);
      markSeedFailed(lockedCompany);
      companyRepository.saveAndFlush(lockedCompany);
      recordSeedRuns(lockedCompany, false, "PENDING_REPAIR", CompanyTime.now(lockedCompany));
      TenantSeedStatusDto status = buildStatus(lockedCompany, "FAILED");
      Long auditEventId =
          logAudit(
              lockedCompany,
              "tenant-default-seed-failed",
              Map.of(
                  "seedFailureCode",
                  "SEED_DEFAULTS_FAILED",
                  "ready",
                  "false",
                  "repairRequired",
                  "true"));
      return SeedAttempt.failed(
          withRepairOutcome(
              status, "FAILED;errorCode=SEED_DEFAULTS_FAILED;auditEventId=" + auditEventId),
          auditEventId);
    }
  }

  @Transactional(readOnly = true)
  public TenantSeedStatusDto getSeedStatus(Long companyId) {
    return buildStatus(requireCompany(companyId), null);
  }

  @Transactional
  public TenantSeedStatusDto repairSeedStatus(Long companyId) {
    Company company = requireCompany(companyId);
    TenantSeedStatusDto status = seedDefaults(company);
    Long auditEventId =
        logAudit(
            company,
            "tenant-default-seed-repair",
            Map.of(
                "repairOutcome", status.repairOutcome(), "ready", String.valueOf(status.ready())));
    return withRepairOutcome(status, status.repairOutcome() + ";auditEventId=" + auditEventId);
  }

  private TenantSeedStatusDto seedDefaultsInternal(Company lockedCompany) {
    int beforeAccountCount = accountRepository.findByCompanyOrderByCodeAsc(lockedCompany).size();
    boolean hadSeedRuns = seedRunRepository.existsByCompany_Id(lockedCompany.getId());
    ensureAccountsAndMappings(lockedCompany);
    accountingPeriodService.ensurePeriod(lockedCompany, CompanyTime.today(lockedCompany));
    TenantSeedStatusDto statusBeforeRunRecord = buildStatus(lockedCompany, null);
    if (statusBeforeRunRecord.ready()) {
      clearSeedFailedMarker(lockedCompany);
    }
    companyRepository.saveAndFlush(lockedCompany);
    int afterAccountCount = statusBeforeRunRecord.chartOfAccounts().accountCount();
    String operation = seedOperation(hadSeedRuns, afterAccountCount > beforeAccountCount);
    recordSeedRuns(
        lockedCompany,
        statusBeforeRunRecord.ready(),
        statusBeforeRunRecord.ready() ? operation : "PENDING_REPAIR",
        CompanyTime.now(lockedCompany));
    TenantSeedStatusDto status = buildStatus(lockedCompany, null);
    return withRepairOutcome(status, afterAccountCount > beforeAccountCount ? "REPAIRED" : "NOOP");
  }

  private void cleanupFailedSeedArtifacts(Company company, Set<Long> accountIdsBefore) {
    List<Account> createdAccounts =
        accountRepository.findByCompanyOrderByCodeAsc(company).stream()
            .filter(account -> account.getId() != null)
            .filter(account -> !accountIdsBefore.contains(account.getId()))
            .sorted(
                Comparator.comparing(
                        (Account account) -> account.getParent() == null ? 0 : 1,
                        Comparator.naturalOrder())
                    .reversed())
            .toList();
    if (!createdAccounts.isEmpty()) {
      accountRepository.deleteAll(createdAccounts);
    }
    company.setDefaultInventoryAccountId(null);
    company.setDefaultCogsAccountId(null);
    company.setDefaultRevenueAccountId(null);
    company.setDefaultDiscountAccountId(null);
    company.setDefaultTaxAccountId(null);
    company.setGstInputTaxAccountId(null);
    company.setGstOutputTaxAccountId(null);
    company.setGstPayableAccountId(null);
    company.setPayrollCashAccount(null);
    company.setPayrollExpenseAccount(null);
  }

  private void markSeedFailed(Company company) {
    company.setLifecycleReason("SEED_FAILED");
    company.setActivationStatus("NOT_SENT");
    company.setActivationSentAt(null);
    company.setActivationExpiresAt(null);
    company.setOnboardingCredentialsEmailedAt(null);
  }

  private void clearSeedFailedMarker(Company company) {
    if (isSeedFailedReason(company.getLifecycleReason())) {
      company.setLifecycleReason(null);
    }
  }

  private boolean isSeedFailedReason(String lifecycleReason) {
    if (!StringUtils.hasText(lifecycleReason)) {
      return false;
    }
    String normalized =
        lifecycleReason
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("_+", "_");
    return Set.of("SEED_FAILED", "SETUP_FAILED", "SEEDING_FAILED").contains(normalized);
  }

  @Transactional
  public TenantSeedStatusDto rejectCoreMappingDelete(Long companyId, String mappingKey) {
    Company company = requireCompany(companyId);
    String normalized = normalizeMappingKey(mappingKey);
    if (!CORE_MAPPING_KEYS.contains(normalized)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Unknown default accounting mapping: " + mappingKey);
    }
    Long auditEventId =
        logAudit(
            company,
            "tenant-default-mapping-delete-blocked",
            Map.of("mappingKey", normalized, "outcome", "LOCKED"));
    throw lockedMapping(normalized, auditEventId);
  }

  @Transactional
  public TenantSeedStatusDto rejectCoreMappingRemap(
      Long companyId, String mappingKey, Long requestedAccountId) {
    Company company = requireCompany(companyId);
    String normalized = normalizeMappingKey(mappingKey);
    if (!CORE_MAPPING_KEYS.contains(normalized)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Unknown default accounting mapping: " + mappingKey);
    }
    Account account =
        accountRepository
            .findById(requestedAccountId)
            .orElseThrow(
                () ->
                    com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                        "accountId must reference a tenant-owned account"));
    if (account.getCompany() == null
        || account.getCompany().getId() == null
        || !account.getCompany().getId().equals(company.getId())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "accountId must reference a tenant-owned account");
    }
    Long auditEventId =
        logAudit(
            company,
            "tenant-default-mapping-remap-blocked",
            Map.of(
                "mappingKey",
                normalized,
                "requestedAccountId",
                String.valueOf(requestedAccountId),
                "outcome",
                "LOCKED"));
    throw lockedMapping(normalized, auditEventId);
  }

  private ApplicationException lockedMapping(String mappingKey, Long auditEventId) {
    return new ApplicationException(
            ErrorCode.BUSINESS_INVALID_STATE,
            "Core default accounting mapping "
                + mappingKey
                + " is locked; auditEventId="
                + auditEventId)
        .withDetail("mappingKey", mappingKey)
        .withDetail("locked", true)
        .withDetail("auditEventId", auditEventId);
  }

  @Transactional(readOnly = true)
  public void requireReadiness(Company company) {
    TenantSeedStatusDto status = buildStatus(requireManagedCompany(company), null);
    if (!status.ready()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Tenant default seeding is not ready: " + status.readinessStatus());
    }
  }

  private Company requireManagedCompany(Company company) {
    if (company == null || company.getId() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Company is required for default seeding");
    }
    return requireCompany(company.getId());
  }

  private Company requireCompany(Long companyId) {
    return companyRepository
        .findById(companyId)
        .orElseThrow(
            () ->
                com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Tenant not found"));
  }

  private void ensureAccountsAndMappings(Company company) {
    List<Account> accounts = accountRepository.findByCompanyOrderByCodeAsc(company);
    Map<String, Account> accountsByCode = accountsByCode(accounts);
    for (AccountBlueprint blueprint : templateBlueprints(company.getOnboardingCoaTemplateCode())) {
      Account account = accountsByCode.get(blueprint.code());
      if (account == null) {
        account = new Account();
        account.setCompany(company);
        account.setCode(blueprint.code());
        account.setName(blueprint.name());
        account.setType(blueprint.type());
        account.setBalance(BigDecimal.ZERO);
        if (StringUtils.hasText(blueprint.parentCode())) {
          Account parent = accountsByCode.get(blueprint.parentCode());
          if (parent == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                "Template account parent not found for code " + blueprint.code());
          }
          account.setParent(parent);
        }
        account = accountRepository.save(account);
        accountsByCode.put(account.getCode().toUpperCase(Locale.ROOT), account);
      }
    }
    applyDefaultMappings(company, accountsByCode);
  }

  private void applyDefaultMappings(Company company, Map<String, Account> accountsByCode) {
    boolean nonGstMode = isNonGstMode(company);
    setIfPresent(
        company::setDefaultInventoryAccountId, accountsByCode, "FINISHED-GOODS-INVENTORY", "INV");
    setIfPresent(company::setDefaultCogsAccountId, accountsByCode, "FG-COGS", "COGS");
    setIfPresent(company::setDefaultRevenueAccountId, accountsByCode, "SALES-REV", "REV");
    setIfPresent(company::setDefaultDiscountAccountId, accountsByCode, "DISC", "SALES-RETURNS");
    Account taxOutput = firstPresent(accountsByCode, "GST-OUT", "TAX-PAYABLE");
    Account taxInput = firstPresent(accountsByCode, "GST-IN", "TDS-RECEIVABLE");
    Account taxPayable = firstPresent(accountsByCode, "GST-PAY", "TDS-PAYABLE", "TAX-PAYABLE");
    if (taxOutput != null) {
      company.setDefaultTaxAccountId(taxOutput.getId());
      company.setGstOutputTaxAccountId(nonGstMode ? null : taxOutput.getId());
    }
    company.setGstInputTaxAccountId(nonGstMode || taxInput == null ? null : taxInput.getId());
    company.setGstPayableAccountId(nonGstMode || taxPayable == null ? null : taxPayable.getId());
    Account cash = firstPresent(accountsByCode, "CASH", "BANK-CURRENT");
    if (cash != null) {
      company.setPayrollCashAccount(cash);
    }
    Account payrollExpense = firstPresent(accountsByCode, "SALARY-EXPENSE", "OPEX");
    if (payrollExpense != null) {
      company.setPayrollExpenseAccount(payrollExpense);
    }
  }

  private void setIfPresent(
      java.util.function.Consumer<Long> setter,
      Map<String, Account> accountsByCode,
      String... codes) {
    Account account = firstPresent(accountsByCode, codes);
    if (account != null) {
      setter.accept(account.getId());
    }
  }

  private TenantSeedStatusDto buildStatus(Company company, String repairOutcome) {
    List<Account> accounts = accountRepository.findByCompanyOrderByCodeAsc(company);
    Map<String, Account> accountsByCode = accountsByCode(accounts);
    List<TenantSeedStatusDto.AccountingMapping> mappings = mappingDtos(company, accountsByCode);
    boolean duplicateCodes =
        accounts.stream()
            .collect(
                Collectors.groupingBy(
                    account -> normalizeCode(account.getCode()), Collectors.counting()))
            .values()
            .stream()
            .anyMatch(count -> count > 1);
    boolean requiredClassesPresent =
        Set.of(
                AccountType.ASSET,
                AccountType.LIABILITY,
                AccountType.EQUITY,
                AccountType.REVENUE,
                AccountType.COGS,
                AccountType.EXPENSE)
            .stream()
            .allMatch(
                type -> accounts.stream().anyMatch(account -> type.equals(account.getType())));
    boolean mappingsReady =
        mappings.stream()
            .filter(mapping -> !"DEFAULT_TAX".equals(mapping.key()) || !isNonGstMode(company))
            .allMatch(
                mapping ->
                    mapping.accountId() != null && mapping.tenantOwned() && mapping.active());
    boolean gstReady =
        isNonGstMode(company)
            ? company.getGstInputTaxAccountId() == null
                && company.getGstOutputTaxAccountId() == null
                && company.getGstPayableAccountId() == null
            : company.getGstInputTaxAccountId() != null
                && company.getGstOutputTaxAccountId() != null
                && company.getGstPayableAccountId() != null;
    boolean ready =
        accounts.size() >= 50
            && !duplicateCodes
            && requiredClassesPresent
            && mappingsReady
            && gstReady;
    return new TenantSeedStatusDto(
        company.getId(),
        company.getCode(),
        resolveTemplateCode(company.getOnboardingCoaTemplateCode()),
        ready,
        ready ? "READY" : "REPAIR_REQUIRED",
        repairOutcome,
        seedRuns(company),
        new TenantSeedStatusDto.ChartOfAccounts(
            accounts.size(),
            List.of("ASSET", "LIABILITY", "EQUITY", "REVENUE", "COGS", "EXPENSE"),
            List.of(
                "CASH",
                "BANK-CURRENT",
                "AR",
                "AP",
                "SALES-REV",
                "FG-COGS",
                "GST-IN",
                "GST-OUT",
                "GST-PAY"),
            duplicateCodes),
        new TenantSeedStatusDto.GstDefaults(
            !isNonGstMode(company),
            company.getGstInputTaxAccountId(),
            company.getGstOutputTaxAccountId(),
            company.getGstPayableAccountId(),
            gstReady ? (isNonGstMode(company) ? "DISABLED" : "ENABLED") : "REPAIR_REQUIRED"),
        mappings,
        new TenantSeedStatusDto.DefaultSettings(
            company.getBaseCurrency(), company.getTimezone(), "APRIL_1", "ACCRUAL"),
        List.of(
            new TenantSeedStatusDto.NumberingDefault("SALES_ORDER", "SO-000001", "FINANCIAL_YEAR"),
            new TenantSeedStatusDto.NumberingDefault("INVOICE", "INV-000001", "FINANCIAL_YEAR"),
            new TenantSeedStatusDto.NumberingDefault(
                "PURCHASE_ORDER", "PO-000001", "FINANCIAL_YEAR"),
            new TenantSeedStatusDto.NumberingDefault("RECEIPT", "RCP-000001", "FINANCIAL_YEAR")),
        List.of("CASH", "BANK_TRANSFER", "UPI", "CREDIT"),
        List.of(
            new TenantSeedStatusDto.DocumentPrefix("SALES_ORDER", "SO-"),
            new TenantSeedStatusDto.DocumentPrefix("INVOICE", "INV-"),
            new TenantSeedStatusDto.DocumentPrefix("CREDIT_NOTE", "CN-"),
            new TenantSeedStatusDto.DocumentPrefix("PURCHASE_ORDER", "PO-"),
            new TenantSeedStatusDto.DocumentPrefix("RECEIPT", "RCP-")),
        roleTemplates());
  }

  private TenantSeedStatusDto withRepairOutcome(TenantSeedStatusDto status, String repairOutcome) {
    return new TenantSeedStatusDto(
        status.companyId(),
        status.companyCode(),
        status.templateCode(),
        status.ready(),
        status.readinessStatus(),
        repairOutcome,
        status.seedRuns(),
        status.chartOfAccounts(),
        status.gstDefaults(),
        status.accountingMappings(),
        status.defaultSettings(),
        status.numbering(),
        status.paymentModes(),
        status.documentPrefixes(),
        status.roleTemplates());
  }

  private String seedOperation(boolean hadSeedRuns, boolean createdSeedData) {
    if (createdSeedData) {
      return hadSeedRuns ? "REPAIRED" : "SEEDED";
    }
    return hadSeedRuns ? "NOOP" : "SEEDED";
  }

  private void recordSeedRuns(
      Company company, boolean ready, String operation, Instant completedAt) {
    Map<String, TenantDefaultSeedRun> runsByCategory =
        seedRunRepository.findByCompany_IdOrderByCategoryAsc(company.getId()).stream()
            .collect(Collectors.toMap(TenantDefaultSeedRun::getCategory, Function.identity()));
    String status = ready ? "COMPLETE" : "REPAIR_REQUIRED";
    List<TenantDefaultSeedRun> runs = new ArrayList<>();
    for (String category : SEED_CATEGORIES) {
      TenantDefaultSeedRun run = runsByCategory.get(category);
      if (run == null) {
        run = TenantDefaultSeedRun.create(company, category, status, operation, true, completedAt);
      } else {
        run.record(status, operation, true, completedAt);
      }
      runs.add(run);
    }
    seedRunRepository.saveAll(runs);
  }

  private List<TenantSeedStatusDto.SeedRun> seedRuns(Company company) {
    Map<String, TenantDefaultSeedRun> runsByCategory =
        seedRunRepository.findByCompany_IdOrderByCategoryAsc(company.getId()).stream()
            .collect(Collectors.toMap(TenantDefaultSeedRun::getCategory, Function.identity()));
    return SEED_CATEGORIES.stream()
        .map(runsByCategory::get)
        .filter(Objects::nonNull)
        .map(
            run ->
                new TenantSeedStatusDto.SeedRun(
                    run.getRunId(),
                    run.getCategory(),
                    run.getStatus(),
                    run.getOperation(),
                    run.getCompletedAt(),
                    run.isRequired()))
        .toList();
  }

  private List<TenantSeedStatusDto.AccountingMapping> mappingDtos(
      Company company, Map<String, Account> accountsByCode) {
    Map<String, Long> mappingIds = new LinkedHashMap<>();
    mappingIds.put("DEFAULT_BANK", idOf(firstPresent(accountsByCode, "BANK-CURRENT")));
    mappingIds.put("DEFAULT_CASH", idOf(firstPresent(accountsByCode, "CASH")));
    mappingIds.put("DEFAULT_REVENUE", company.getDefaultRevenueAccountId());
    mappingIds.put("DEFAULT_PURCHASE", idOf(firstPresent(accountsByCode, "AP")));
    mappingIds.put("DEFAULT_AR", idOf(firstPresent(accountsByCode, "AR")));
    mappingIds.put("DEFAULT_AP", idOf(firstPresent(accountsByCode, "AP")));
    mappingIds.put("DEFAULT_TAX", company.getDefaultTaxAccountId());
    mappingIds.put("DEFAULT_ROUNDING", idOf(firstPresent(accountsByCode, "ROUND-OFF")));
    mappingIds.put("DEFAULT_DISCOUNT", company.getDefaultDiscountAccountId());
    mappingIds.put("DEFAULT_FREIGHT", idOf(firstPresent(accountsByCode, "FREIGHT-OUT")));
    Map<Long, Account> accountsById =
        accountsByCode.values().stream()
            .filter(account -> account.getId() != null)
            .collect(Collectors.toMap(Account::getId, Function.identity(), (left, right) -> left));
    return mappingIds.entrySet().stream()
        .map(
            entry ->
                mappingDto(
                    company, entry.getKey(), entry.getValue(), accountsById.get(entry.getValue())))
        .toList();
  }

  private TenantSeedStatusDto.AccountingMapping mappingDto(
      Company company, String key, Long accountId, Account account) {
    boolean tenantOwned =
        account != null
            && account.getCompany() != null
            && Objects.equals(account.getCompany().getId(), company.getId());
    return new TenantSeedStatusDto.AccountingMapping(
        key,
        accountId,
        account == null ? null : account.getCode(),
        account == null ? null : account.getName(),
        account == null || account.getType() == null ? null : account.getType().name(),
        tenantOwned,
        account != null && account.isActive(),
        true);
  }

  private List<TenantSeedStatusDto.RoleTemplate> roleTemplates() {
    return List.of(
        roleTemplate(SystemRole.ACCOUNTING),
        roleTemplate(SystemRole.FACTORY),
        roleTemplate(SystemRole.SALES),
        roleTemplate(SystemRole.DEALER));
  }

  private TenantSeedStatusDto.RoleTemplate roleTemplate(SystemRole role) {
    return new TenantSeedStatusDto.RoleTemplate(
        role.getRoleName(), role.getDescription(), role.getDefaultPermissions());
  }

  private Map<String, Account> accountsByCode(List<Account> accounts) {
    Map<String, Account> map = new HashMap<>();
    for (Account account : accounts) {
      if (StringUtils.hasText(account.getCode())) {
        map.putIfAbsent(account.getCode().trim().toUpperCase(Locale.ROOT), account);
      }
    }
    return map;
  }

  private Account firstPresent(Map<String, Account> accountsByCode, String... preferredCodes) {
    for (String code : preferredCodes) {
      Account account = accountsByCode.get(code);
      if (account != null) {
        return account;
      }
    }
    return null;
  }

  private Long idOf(Account account) {
    return account == null ? null : account.getId();
  }

  private boolean isNonGstMode(Company company) {
    return company.getDefaultGstRate() != null
        && company.getDefaultGstRate().compareTo(BigDecimal.ZERO) == 0;
  }

  private String normalizeMappingKey(String mappingKey) {
    if (!StringUtils.hasText(mappingKey)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "mappingKey is required");
    }
    return mappingKey.trim().toUpperCase(Locale.ROOT).replace('-', '_');
  }

  private String normalizeCode(String code) {
    return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "";
  }

  private Long logAudit(Company company, String reason, Map<String, String> metadata) {
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED,
            SecurityActorResolver.resolveActorOrUnknown(),
            company.getCode(),
            metadata);
    return auditLog == null ? null : auditLog.getId();
  }

  private String resolveTemplateCode(String templateCode) {
    String normalized =
        StringUtils.hasText(templateCode) ? templateCode.trim().toUpperCase(Locale.ROOT) : "SME";
    if ("DISTRIBUTION".equals(normalized)) {
      return "DISTRIBUTION";
    }
    if ("MANUFACTURING".equals(normalized)) {
      return "MANUFACTURING";
    }
    if ("INDIAN_STANDARD".equals(normalized)) {
      return "DISTRIBUTION";
    }
    if ("GENERIC".equals(normalized)) {
      return "SME";
    }
    return "SME";
  }

  private List<AccountBlueprint> templateBlueprints(String templateCode) {
    String normalized = resolveTemplateCode(templateCode);
    List<AccountBlueprint> base = genericTemplateBlueprints();
    if ("DISTRIBUTION".equals(normalized)) {
      return mergeBlueprints(base, distributionTemplateExtensions());
    }
    if ("MANUFACTURING".equals(normalized)) {
      return mergeBlueprints(
          mergeBlueprints(base, distributionTemplateExtensions()),
          manufacturingTemplateExtensions());
    }
    return base;
  }

  private List<AccountBlueprint> mergeBlueprints(
      List<AccountBlueprint> base, List<AccountBlueprint> extensions) {
    LinkedHashMap<String, AccountBlueprint> merged = new LinkedHashMap<>();
    base.forEach(blueprint -> merged.put(blueprint.code(), blueprint));
    extensions.forEach(blueprint -> merged.put(blueprint.code(), blueprint));
    return new ArrayList<>(merged.values());
  }

  private List<AccountBlueprint> genericTemplateBlueprints() {
    List<AccountBlueprint> blueprints = new ArrayList<>();
    blueprints.add(new AccountBlueprint("AST", "Assets", AccountType.ASSET, null));
    blueprints.add(new AccountBlueprint("LIAB", "Liabilities", AccountType.LIABILITY, null));
    blueprints.add(new AccountBlueprint("EQ", "Equity", AccountType.EQUITY, null));
    blueprints.add(new AccountBlueprint("REV", "Revenue", AccountType.REVENUE, null));
    blueprints.add(new AccountBlueprint("COGS", "Cost of Goods Sold", AccountType.COGS, null));
    blueprints.add(new AccountBlueprint("EXP", "Expenses", AccountType.EXPENSE, null));
    blueprints.add(new AccountBlueprint("AST-CUR", "Current Assets", AccountType.ASSET, "AST"));
    blueprints.add(new AccountBlueprint("AST-FIX", "Fixed Assets", AccountType.ASSET, "AST"));
    blueprints.add(new AccountBlueprint("CASH", "Cash", AccountType.ASSET, "AST-CUR"));
    blueprints.add(new AccountBlueprint("BANK-CURRENT", "Bank", AccountType.ASSET, "AST-CUR"));
    blueprints.add(new AccountBlueprint("AR", "Accounts Receivable", AccountType.ASSET, "AST-CUR"));
    blueprints.add(new AccountBlueprint("INV", "Inventory", AccountType.ASSET, "AST-CUR"));
    blueprints.add(
        new AccountBlueprint(
            "RAW-MATERIAL-INVENTORY", "Raw Material Inventory", AccountType.ASSET, "INV"));
    blueprints.add(
        new AccountBlueprint(
            "FINISHED-GOODS-INVENTORY", "Finished Goods Inventory", AccountType.ASSET, "INV"));
    blueprints.add(new AccountBlueprint("GST-IN", "GST Input", AccountType.ASSET, "AST-CUR"));
    blueprints.add(
        new AccountBlueprint("TDS-RECEIVABLE", "TDS Receivable", AccountType.ASSET, "AST-CUR"));
    blueprints.add(new AccountBlueprint("FIX-EQUIP", "Equipment", AccountType.ASSET, "AST-FIX"));
    blueprints.add(new AccountBlueprint("FIX-VEH", "Vehicles", AccountType.ASSET, "AST-FIX"));
    blueprints.add(
        new AccountBlueprint("LIAB-CUR", "Current Liabilities", AccountType.LIABILITY, "LIAB"));
    blueprints.add(
        new AccountBlueprint("AP", "Accounts Payable", AccountType.LIABILITY, "LIAB-CUR"));
    blueprints.add(
        new AccountBlueprint("GST-OUT", "GST Output", AccountType.LIABILITY, "LIAB-CUR"));
    blueprints.add(
        new AccountBlueprint("GST-PAY", "GST Payable", AccountType.LIABILITY, "LIAB-CUR"));
    blueprints.add(
        new AccountBlueprint("TAX-PAYABLE", "Tax Payable", AccountType.LIABILITY, "LIAB-CUR"));
    blueprints.add(
        new AccountBlueprint("TDS-PAYABLE", "TDS Payable", AccountType.LIABILITY, "LIAB-CUR"));
    blueprints.add(new AccountBlueprint("OWN-EQ", "Owner's Equity", AccountType.EQUITY, "EQ"));
    blueprints.add(new AccountBlueprint("RET-EARN", "Retained Earnings", AccountType.EQUITY, "EQ"));
    blueprints.add(
        new AccountBlueprint("OPEN-BAL", "Opening Balance Equity", AccountType.EQUITY, "EQ"));
    blueprints.add(new AccountBlueprint("SALES-REV", "Sales Revenue", AccountType.REVENUE, "REV"));
    blueprints.add(
        new AccountBlueprint("SERVICE-REVENUE", "Service Revenue", AccountType.REVENUE, "REV"));
    blueprints.add(
        new AccountBlueprint("SALES-RETURNS", "Sales Returns", AccountType.REVENUE, "REV"));
    blueprints.add(
        new AccountBlueprint("FG-COGS", "Finished Goods COGS", AccountType.COGS, "COGS"));
    blueprints.add(
        new AccountBlueprint(
            "RM-CONSUMPTION", "Raw Material Consumption", AccountType.COGS, "COGS"));
    blueprints.add(
        new AccountBlueprint(
            "DIRECT-MATERIAL-CONSUMPTION",
            "Direct Material Consumption",
            AccountType.COGS,
            "COGS"));
    blueprints.add(new AccountBlueprint("OPEX", "Operating Expenses", AccountType.EXPENSE, "EXP"));
    blueprints.add(new AccountBlueprint("DISC", "Sales Discount", AccountType.EXPENSE, "OPEX"));
    blueprints.add(
        new AccountBlueprint("ROUND-OFF", "Rounding Difference", AccountType.EXPENSE, "OPEX"));
    blueprints.add(new AccountBlueprint("FREIGHT-OUT", "Freight Out", AccountType.EXPENSE, "OPEX"));
    blueprints.add(new AccountBlueprint("FREIGHT-IN", "Freight In", AccountType.EXPENSE, "OPEX"));
    blueprints.add(
        new AccountBlueprint("SALARY-EXPENSE", "Salary Expense", AccountType.EXPENSE, "OPEX"));
    blueprints.add(
        new AccountBlueprint("OFFICE-EXPENSE", "Office Expense", AccountType.EXPENSE, "OPEX"));
    for (int index = 1; index <= 21; index++) {
      blueprints.add(
          new AccountBlueprint(
              "GEN-" + index,
              "Generic Account " + index,
              index % 2 == 0 ? AccountType.EXPENSE : AccountType.ASSET,
              index % 2 == 0 ? "OPEX" : "AST-FIX"));
    }
    return blueprints.stream().sorted(Comparator.comparingInt(this::blueprintOrder)).toList();
  }

  private int blueprintOrder(AccountBlueprint blueprint) {
    return StringUtils.hasText(blueprint.parentCode()) ? 1 : 0;
  }

  private List<AccountBlueprint> distributionTemplateExtensions() {
    List<AccountBlueprint> blueprints = new ArrayList<>();
    for (int index = 1; index <= 12; index++) {
      blueprints.add(
          new AccountBlueprint(
              "DIST-" + index,
              "Distribution Account " + index,
              index % 2 == 0 ? AccountType.LIABILITY : AccountType.EXPENSE,
              index % 2 == 0 ? "LIAB" : "EXP"));
    }
    return blueprints;
  }

  private List<AccountBlueprint> manufacturingTemplateExtensions() {
    List<AccountBlueprint> blueprints = new ArrayList<>();
    for (int index = 1; index <= 16; index++) {
      blueprints.add(
          new AccountBlueprint(
              "MFG-" + index,
              "Manufacturing Account " + index,
              index % 2 == 0 ? AccountType.ASSET : AccountType.EXPENSE,
              index % 2 == 0 ? "AST" : "EXP"));
    }
    return blueprints;
  }

  private record AccountBlueprint(String code, String name, AccountType type, String parentCode) {}

  public record SeedAttempt(boolean ready, TenantSeedStatusDto status, Long auditEventId) {
    public static SeedAttempt ready(TenantSeedStatusDto status) {
      return new SeedAttempt(true, status, null);
    }

    public static SeedAttempt failed(TenantSeedStatusDto status, Long auditEventId) {
      return new SeedAttempt(false, status, auditEventId);
    }
  }
}
