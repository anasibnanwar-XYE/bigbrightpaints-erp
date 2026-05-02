package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record TenantSeedStatusDto(
    Long companyId,
    String companyCode,
    String templateCode,
    boolean ready,
    String readinessStatus,
    String repairOutcome,
    List<SeedRun> seedRuns,
    ChartOfAccounts chartOfAccounts,
    GstDefaults gstDefaults,
    List<AccountingMapping> accountingMappings,
    DefaultSettings defaultSettings,
    List<NumberingDefault> numbering,
    List<String> paymentModes,
    List<DocumentPrefix> documentPrefixes,
    List<RoleTemplate> roleTemplates) {

  public record SeedRun(
      String runId,
      String category,
      String status,
      String operation,
      Instant completedAt,
      boolean required) {}

  public record ChartOfAccounts(
      int accountCount,
      List<String> requiredClasses,
      List<String> requiredAccounts,
      boolean duplicateAccountCodesPresent) {}

  public record GstDefaults(
      boolean gstEnabled,
      Long inputAccountId,
      Long outputAccountId,
      Long payableAccountId,
      String mappingStatus) {}

  public record AccountingMapping(
      String key,
      Long accountId,
      String accountCode,
      String accountName,
      String accountType,
      boolean tenantOwned,
      boolean active,
      boolean locked) {}

  public record DefaultSettings(
      String baseCurrency, String timezone, String financialYearStart, String bookBasis) {}

  public record NumberingDefault(String documentType, String nextNumber, String resetPolicy) {}

  public record DocumentPrefix(String documentType, String prefix) {}

  public record RoleTemplate(String key, String displayName, List<String> permissions) {}
}
