package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantUsageRollup;
import com.bigbrightpaints.erp.modules.company.domain.TenantUsageRollupRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;

@ExtendWith(MockitoExtension.class)
class TenantUsageRollupServiceTest {

  @Mock private TenantUsageRollupRepository rollupRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private UserAccountRepository userAccountRepository;
  @Mock private AuditLogRepository auditLogRepository;

  private final Instant fixedNow = Instant.parse("2026-03-31T20:30:00Z");
  private final List<TenantUsageRollup> rollups = new ArrayList<>();
  private TenantUsageRollupService service;

  @BeforeEach
  void setUp() {
    wireRepositoryStore();
    service =
        new TenantUsageRollupService(
            rollupRepository,
            companyRepository,
            userAccountRepository,
            auditLogRepository,
            Clock.fixed(fixedNow, ZoneOffset.UTC));
  }

  @Test
  void tenantUsage_coversDurableDimensionsPeriodMetadataLimitsAndPrivacySafeHistory() {
    Company company = company(7L, "ACME", "Asia/Kolkata");
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(userAccountRepository.countByCompany_IdAndEnabledTrue(7L)).thenReturn(8L);

    SuperAdminUsageDtos.TenantUsage usage = service.getTenantUsage(7L, entitlementLimits());

    assertThat(usage.companyCode()).isEqualTo("ACME");
    assertThat(usage.currentDailyPeriod().periodId()).isEqualTo("2026-04-01");
    assertThat(usage.currentDailyPeriod().startAt()).isEqualTo("2026-03-31T18:30:00Z");
    assertThat(usage.currentDailyPeriod().timezone()).isEqualTo("Asia/Kolkata");
    assertThat(usage.currentMonthlyPeriod().periodId()).isEqualTo("2026-04");
    assertThat(usage.dimensions())
        .extracting(SuperAdminUsageDtos.DimensionUsage::dimension)
        .containsExactly("USERS", "STORAGE", "API_CALLS", "PDF_EXPORTS", "EMAILS", "JOBS");
    assertThat(usage.operationalDimensions()).isEmpty();
    assertThat(usage.dimensions())
        .filteredOn(dimension -> "USERS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.used()).isEqualTo(8L);
              assertThat(dimension.limit()).isEqualTo(10L);
              assertThat(dimension.accountingMode()).isEqualTo("SNAPSHOT");
              assertThat(dimension.unit()).isEqualTo("COUNT");
            });
    assertThat(usage.dimensions())
        .filteredOn(dimension -> "STORAGE".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.used()).isZero();
              assertThat(dimension.limit()).isEqualTo(4096L);
              assertThat(dimension.accountingMode()).isEqualTo("SNAPSHOT");
              assertThat(dimension.unit()).isEqualTo("BYTES");
            });
    assertThat(usage.dimensions())
        .filteredOn(dimension -> "PDF_EXPORTS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.limit()).isEqualTo(30L);
              assertThat(dimension.accountingMode()).isEqualTo("COUNTER");
            });
    assertThat(usage.toString().toLowerCase())
        .doesNotContain("invoice", "ledger", "inventory", "salary", "vendor", "customer");
  }

  @Test
  void recordApiCall_incrementsDailyAndMonthlyDurableCountersForRestartPersistence() {
    Company company = company(7L, "ACME", "Asia/Kolkata");

    service.recordApiCall(company);

    verify(rollupRepository)
        .incrementCounter(
            eq(7L),
            eq("ACME"),
            eq("API_CALLS"),
            eq("DAILY"),
            eq(Instant.parse("2026-03-31T18:30:00Z")),
            eq(Instant.parse("2026-04-01T18:30:00Z")),
            eq("Asia/Kolkata"),
            eq(1L),
            eq(0L));
    verify(rollupRepository)
        .incrementCounter(
            eq(7L),
            eq("ACME"),
            eq("API_CALLS"),
            eq("MONTHLY"),
            eq(Instant.parse("2026-03-31T18:30:00Z")),
            eq(Instant.parse("2026-04-30T18:30:00Z")),
            eq("Asia/Kolkata"),
            eq(1L),
            eq(0L));
  }

  @Test
  void currentMonthlyApiUsageReturnsDurableCounterAndResetBoundary() {
    Company company = company(7L, "ACME", "Asia/Kolkata");
    rollups.add(
        TenantUsageRollup.snapshot(
            company,
            "API_CALLS",
            "MONTHLY",
            Instant.parse("2026-03-31T18:30:00Z"),
            Instant.parse("2026-04-30T18:30:00Z"),
            "Asia/Kolkata",
            17L,
            0L));

    TenantUsageRollupService.MonthlyApiUsage usage = service.getCurrentMonthlyApiUsage(company);

    assertThat(usage.used()).isEqualTo(17L);
    assertThat(usage.periodStartAt()).isEqualTo("2026-03-31T18:30:00Z");
    assertThat(usage.periodEndAt()).isEqualTo("2026-04-30T18:30:00Z");
  }

  @Test
  void tenantUsageHistory_closesElapsedWindowsUsingTenantTimezoneBoundaries() {
    Company company = company(7L, "ACME", "Asia/Kolkata");
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    TenantUsageRollup yesterday =
        TenantUsageRollup.snapshot(
            company,
            "USERS",
            "DAILY",
            Instant.parse("2026-03-30T18:30:00Z"),
            Instant.parse("2026-03-31T18:30:00Z"),
            "Asia/Kolkata",
            6L,
            0L);
    rollups.add(yesterday);

    SuperAdminUsageDtos.TenantUsageHistory history = service.getTenantUsageHistory(7L, "DAILY");

    assertThat(yesterday.isClosed()).isTrue();
    assertThat(yesterday.getClosedAt()).isEqualTo(fixedNow);
    assertThat(history.windows())
        .singleElement()
        .satisfies(
            window -> {
              assertThat(window.period().periodId()).isEqualTo("2026-03-31");
              assertThat(window.period().closed()).isTrue();
              assertThat(window.used()).isEqualTo(6L);
            });
  }

  @Test
  void quotaPolicy_exposesWarningGraceBlockLoweredLimitAndEmailCategoryMatrix() {
    Company company = company(7L, "ACME", "UTC");
    company.setQuotaSoftLimitEnabled(true);
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(userAccountRepository.countByCompany_IdAndEnabledTrue(7L)).thenReturn(9L);
    rollups.add(monthlyRollup(company, "EMAILS", 12L, 0L));
    rollups.add(monthlyRollup(company, "JOBS", 11L, 0L));

    SuperAdminUsageDtos.TenantQuotaPolicy policy =
        service.getTenantQuotaPolicy(7L, quotaLimits(10L, 1000L, 10L, 10L, 10L, 10L));

    assertThat(policy.dimensions())
        .filteredOn(dimension -> "USERS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.state()).isEqualTo("WARNING");
              assertThat(dimension.warningThresholdPercent()).isEqualTo(80);
              assertThat(dimension.hardBlockAt()).isEqualTo(11L);
              assertThat(dimension.safeReadsAllowed()).isTrue();
              assertThat(dimension.existingResourcesPreserved()).isTrue();
            });
    assertThat(policy.dimensions())
        .filteredOn(dimension -> "EMAILS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.state()).isEqualTo("BLOCKED");
              assertThat(dimension.graceStartAt()).isEqualTo(11L);
              assertThat(dimension.graceEndAt()).isEqualTo(11L);
              assertThat(dimension.hardBlockAt()).isEqualTo(12L);
              assertThat(dimension.loweredLimitBehavior())
                  .isEqualTo("LOWERED_LIMIT_BLOCKS_NEW_WRITES_SAFE_READS_ALLOWED");
            });
    assertThat(policy.emailCategories())
        .filteredOn(category -> "BUSINESS".equals(category.category()))
        .singleElement()
        .satisfies(
            category -> {
              assertThat(category.counted()).isTrue();
              assertThat(category.blockedWhenTenantEmailQuotaExhausted()).isTrue();
              assertThat(category.mailhogEvidenceSafe()).isTrue();
              assertThat(category.tokenRedactionRequired()).isTrue();
            });
    assertThat(policy.emailCategories())
        .filteredOn(category -> "PASSWORD_RESET".equals(category.category()))
        .singleElement()
        .satisfies(
            category -> {
              assertThat(category.counted()).isFalse();
              assertThat(category.blockedWhenTenantEmailQuotaExhausted()).isFalse();
              assertThat(category.exemptReason())
                  .isEqualTo("REQUIRED_ONBOARDING_SECURITY_OR_PLATFORM_NOTICE");
            });
  }

  @Test
  void enforceQuotaAction_allowsGraceRecordsCounterAndBlocksAfterPolicyExhaustion() {
    Company company = company(7L, "ACME", "UTC");
    company.setQuotaSoftLimitEnabled(true);
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    rollups.add(monthlyRollup(company, "PDF_EXPORTS", 10L, 0L));

    SuperAdminUsageDtos.QuotaActionResult grace =
        service.enforceQuotaAction(
            7L,
            new SuperAdminUsageDtos.QuotaActionRequest("PDF_EXPORTS", 1L, null, null, false),
            quotaLimits(10L, 1000L, 100L, 10L, 10L, 10L));
    SuperAdminUsageDtos.QuotaActionResult blocked =
        service.enforceQuotaAction(
            7L,
            new SuperAdminUsageDtos.QuotaActionRequest("PDF_EXPORTS", 2L, null, null, false),
            quotaLimits(10L, 1000L, 100L, 10L, 10L, 10L));

    assertThat(grace.accepted()).isTrue();
    assertThat(grace.decision()).isEqualTo("GRACE");
    assertThat(grace.usedBefore()).isEqualTo(10L);
    assertThat(grace.usedAfter()).isEqualTo(11L);
    assertThat(grace.usageRecorded()).isTrue();
    assertThat(blocked.accepted()).isFalse();
    assertThat(blocked.decision()).isEqualTo("BLOCKED");
    assertThat(blocked.reasonCode()).isEqualTo("TENANT_PDF_EXPORTS_QUOTA_EXHAUSTED");
    verify(rollupRepository, times(2))
        .incrementCounter(
            eq(7L),
            eq("ACME"),
            eq("PDF_EXPORTS"),
            anyString(),
            any(Instant.class),
            any(Instant.class),
            eq("UTC"),
            eq(1L),
            eq(0L));
  }

  @Test
  void enforceQuotaAction_exemptsRequiredEmailCategoriesFromBusinessEmailExhaustion() {
    Company company = company(7L, "ACME", "UTC");
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    rollups.add(monthlyRollup(company, "EMAILS", 99L, 0L));

    SuperAdminUsageDtos.QuotaActionResult result =
        service.enforceQuotaAction(
            7L,
            new SuperAdminUsageDtos.QuotaActionRequest("EMAILS", 1L, null, "PASSWORD_RESET", false),
            quotaLimits(10L, 1000L, 100L, 10L, 10L, 10L));

    assertThat(result.accepted()).isTrue();
    assertThat(result.decision()).isEqualTo("ALLOWED_EXEMPT");
    assertThat(result.reasonCode()).isEqualTo("EMAIL_CATEGORY_EXEMPT");
    assertThat(result.usageRecorded()).isFalse();
    assertThat(result.usedAfter()).isEqualTo(99L);
    assertThat(result.mailhogEvidenceSafe()).isTrue();
    assertThat(result.tokenRedactionRequired()).isTrue();
    verify(rollupRepository, never())
        .incrementCounter(
            anyLong(),
            anyString(),
            eq("EMAILS"),
            anyString(),
            any(Instant.class),
            any(Instant.class),
            anyString(),
            anyLong(),
            anyLong());
  }

  @Test
  void storageWriteAndDeleteAdjustRealStorageBytesWithoutAuditLogEstimation() {
    Company company = company(7L, "ACME", "UTC");

    service.recordStorageWrite(company, 2048L);
    service.recordStorageDelete(company, 512L);

    verify(rollupRepository)
        .incrementCounter(
            eq(7L),
            eq("ACME"),
            eq("STORAGE"),
            eq("DAILY"),
            any(Instant.class),
            any(Instant.class),
            eq("UTC"),
            eq(0L),
            eq(2048L));
    verify(rollupRepository)
        .incrementCounter(
            eq(7L),
            eq("ACME"),
            eq("STORAGE"),
            eq("MONTHLY"),
            any(Instant.class),
            any(Instant.class),
            eq("UTC"),
            eq(0L),
            eq(-512L));
  }

  @Test
  void enforceQuotaActionSaturatesOverflowProjectionIntoBlockedDecision() {
    Company company = company(7L, "ACME", "UTC");
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    rollups.add(monthlyRollup(company, "PDF_EXPORTS", Long.MAX_VALUE - 5L, 0L));

    SuperAdminUsageDtos.QuotaActionResult result =
        service.enforceQuotaAction(
            7L,
            new SuperAdminUsageDtos.QuotaActionRequest("PDF_EXPORTS", 10L, null, null, true),
            quotaLimits(10L, 1000L, 100L, Long.MAX_VALUE - 100L, 10L, 10L));

    assertThat(result.accepted()).isFalse();
    assertThat(result.decision()).isEqualTo("BLOCKED");
    assertThat(result.usedAfter()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void enforceQuotaActionRejectsUnsafeRequestedUnitsBeforeProjection() {
    Company company = company(7L, "ACME", "UTC");
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));

    assertThatThrownBy(
            () ->
                service.enforceQuotaAction(
                    7L,
                    new SuperAdminUsageDtos.QuotaActionRequest(
                        "PDF_EXPORTS", Long.MAX_VALUE, null, null, true),
                    quotaLimits(10L, 1000L, 100L, 10L, 10L, 10L)))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("safe projection bound");
  }

  private void wireRepositoryStore() {
    lenient()
        .when(rollupRepository.save(any(TenantUsageRollup.class)))
        .thenAnswer(
            invocation -> {
              TenantUsageRollup rollup = invocation.getArgument(0);
              if (!rollups.contains(rollup)) {
                rollups.add(rollup);
              }
              return rollup;
            });
    lenient()
        .when(rollupRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .doAnswer(
            invocation -> {
              Long companyId = invocation.getArgument(0);
              String companyCode = invocation.getArgument(1);
              String dimension = invocation.getArgument(2);
              String periodType = invocation.getArgument(3);
              Instant periodStartAt = invocation.getArgument(4);
              Instant periodEndAt = invocation.getArgument(5);
              String tenantTimezone = invocation.getArgument(6);
              long usageCount = invocation.getArgument(7);
              long usageBytes = invocation.getArgument(8);
              upsertSnapshotInStore(
                  companyId,
                  companyCode,
                  dimension,
                  periodType,
                  periodStartAt,
                  periodEndAt,
                  tenantTimezone,
                  usageCount,
                  usageBytes);
              return null;
            })
        .when(rollupRepository)
        .upsertSnapshot(
            anyLong(),
            anyString(),
            anyString(),
            anyString(),
            any(Instant.class),
            any(Instant.class),
            anyString(),
            anyLong(),
            anyLong());
    lenient()
        .doAnswer(
            invocation -> {
              Long companyId = invocation.getArgument(0);
              String companyCode = invocation.getArgument(1);
              String dimension = invocation.getArgument(2);
              String periodType = invocation.getArgument(3);
              Instant periodStartAt = invocation.getArgument(4);
              Instant periodEndAt = invocation.getArgument(5);
              String tenantTimezone = invocation.getArgument(6);
              ensureCounterInStore(
                  companyId,
                  companyCode,
                  dimension,
                  periodType,
                  periodStartAt,
                  periodEndAt,
                  tenantTimezone);
              return null;
            })
        .when(rollupRepository)
        .ensureCounter(
            anyLong(),
            anyString(),
            anyString(),
            anyString(),
            any(Instant.class),
            any(Instant.class),
            anyString());
    lenient()
        .when(
            rollupRepository.findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
                anyLong(), anyString(), anyString(), any(Instant.class)))
        .thenAnswer(
            invocation ->
                rollups.stream()
                    .filter(
                        rollup ->
                            invocation.getArgument(0).equals(rollup.getCompany().getId())
                                && invocation.getArgument(1).equals(rollup.getDimension())
                                && invocation.getArgument(2).equals(rollup.getPeriodType())
                                && invocation.getArgument(3).equals(rollup.getPeriodStartAt()))
                    .findFirst());
    lenient()
        .when(
            rollupRepository
                .findByCompany_IdAndClosedFalseAndPeriodEndAtLessThanEqualOrderByPeriodEndAtAsc(
                    anyLong(), any(Instant.class)))
        .thenAnswer(
            invocation ->
                rollups.stream()
                    .filter(
                        rollup ->
                            invocation.getArgument(0).equals(rollup.getCompany().getId())
                                && !rollup.isClosed()
                                && !rollup.getPeriodEndAt().isAfter(invocation.getArgument(1)))
                    .toList());
    lenient()
        .when(
            rollupRepository.findByCompany_IdAndPeriodTypeAndPeriodStartAtOrderByDimensionAsc(
                anyLong(), anyString(), any(Instant.class)))
        .thenAnswer(
            invocation ->
                rollups.stream()
                    .filter(
                        rollup ->
                            invocation.getArgument(0).equals(rollup.getCompany().getId())
                                && invocation.getArgument(1).equals(rollup.getPeriodType())
                                && invocation.getArgument(2).equals(rollup.getPeriodStartAt()))
                    .sorted(java.util.Comparator.comparing(TenantUsageRollup::getDimension))
                    .toList());
    lenient()
        .when(
            rollupRepository
                .findTop100ByCompany_IdAndPeriodTypeAndClosedTrueOrderByPeriodStartAtDescDimensionAsc(
                    anyLong(), anyString()))
        .thenAnswer(
            invocation ->
                rollups.stream()
                    .filter(
                        rollup ->
                            invocation.getArgument(0).equals(rollup.getCompany().getId())
                                && invocation.getArgument(1).equals(rollup.getPeriodType())
                                && rollup.isClosed())
                    .sorted(
                        java.util.Comparator.comparing(TenantUsageRollup::getPeriodStartAt)
                            .reversed()
                            .thenComparing(TenantUsageRollup::getDimension))
                    .limit(100)
                    .toList());
  }

  private void upsertSnapshotInStore(
      Long companyId,
      String companyCode,
      String dimension,
      String periodType,
      Instant periodStartAt,
      Instant periodEndAt,
      String tenantTimezone,
      long usageCount,
      long usageBytes) {
    Optional<TenantUsageRollup> existing =
        findStoredRollup(companyId, dimension, periodType, periodStartAt);
    if (existing.isPresent()) {
      existing.get().updateSnapshot(usageCount, usageBytes, periodEndAt, tenantTimezone);
      return;
    }
    rollups.add(
        TenantUsageRollup.snapshot(
            company(companyId, companyCode, tenantTimezone),
            dimension,
            periodType,
            periodStartAt,
            periodEndAt,
            tenantTimezone,
            usageCount,
            usageBytes));
  }

  private void ensureCounterInStore(
      Long companyId,
      String companyCode,
      String dimension,
      String periodType,
      Instant periodStartAt,
      Instant periodEndAt,
      String tenantTimezone) {
    if (findStoredRollup(companyId, dimension, periodType, periodStartAt).isPresent()) {
      return;
    }
    rollups.add(
        TenantUsageRollup.counter(
            company(companyId, companyCode, tenantTimezone),
            dimension,
            periodType,
            periodStartAt,
            periodEndAt,
            tenantTimezone));
  }

  private Optional<TenantUsageRollup> findStoredRollup(
      Long companyId, String dimension, String periodType, Instant periodStartAt) {
    return rollups.stream()
        .filter(
            rollup ->
                companyId.equals(rollup.getCompany().getId())
                    && dimension.equals(rollup.getDimension())
                    && periodType.equals(rollup.getPeriodType())
                    && periodStartAt.equals(rollup.getPeriodStartAt()))
        .findFirst();
  }

  private Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> entitlementLimits() {
    Instant updatedAt = Instant.parse("2026-03-01T00:00:00Z");
    return Map.of(
        "maxActiveUsers", limit("maxActiveUsers", 10L, updatedAt),
        "maxStorageBytes", limit("maxStorageBytes", 4096L, updatedAt),
        "maxApiRequests", limit("maxApiRequests", 100L, updatedAt),
        "maxPdfExports", limit("maxPdfExports", 30L, updatedAt),
        "maxEmails", limit("maxEmails", 40L, updatedAt),
        "maxJobs", limit("maxJobs", 50L, updatedAt));
  }

  private SuperAdminTenantEntitlementsDto.LimitEntitlement limit(
      String key, long value, Instant updatedAt) {
    return new SuperAdminTenantEntitlementsDto.LimitEntitlement(
        key, value, null, value, "PLAN_DEFAULT", updatedAt);
  }

  private Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> quotaLimits(
      long maxActiveUsers,
      long maxStorageBytes,
      long maxApiRequests,
      long maxPdfExports,
      long maxEmails,
      long maxJobs) {
    Instant updatedAt = Instant.parse("2026-03-01T00:00:00Z");
    return Map.of(
        "maxActiveUsers", limit("maxActiveUsers", maxActiveUsers, updatedAt),
        "maxStorageBytes", limit("maxStorageBytes", maxStorageBytes, updatedAt),
        "maxApiRequests", limit("maxApiRequests", maxApiRequests, updatedAt),
        "maxPdfExports", limit("maxPdfExports", maxPdfExports, updatedAt),
        "maxEmails", limit("maxEmails", maxEmails, updatedAt),
        "maxJobs", limit("maxJobs", maxJobs, updatedAt));
  }

  private TenantUsageRollup monthlyRollup(
      Company company, String dimension, long usageCount, long usageBytes) {
    return TenantUsageRollup.snapshot(
        company,
        dimension,
        "MONTHLY",
        Instant.parse("2026-03-01T00:00:00Z"),
        Instant.parse("2026-04-01T00:00:00Z"),
        "UTC",
        usageCount,
        usageBytes);
  }

  private Company company(Long id, String code, String timezone) {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", id);
    ReflectionTestUtils.setField(company, "publicId", UUID.randomUUID());
    company.setName("Company " + code);
    company.setCode(code);
    company.setTimezone(timezone);
    company.setQuotaMaxActiveUsers(10L);
    company.setQuotaMaxApiRequests(100L);
    company.setQuotaMaxStorageBytes(4096L);
    return company;
  }
}
