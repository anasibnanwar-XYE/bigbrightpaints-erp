package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
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
    when(auditLogRepository.estimateAuditStorageBytesByCompanyId(7L)).thenReturn(2048L);

    SuperAdminUsageDtos.TenantUsage usage = service.getTenantUsage(7L, entitlementLimits());

    assertThat(usage.companyCode()).isEqualTo("ACME");
    assertThat(usage.currentDailyPeriod().periodId()).isEqualTo("2026-04-01");
    assertThat(usage.currentDailyPeriod().startAt()).isEqualTo("2026-03-31T18:30:00Z");
    assertThat(usage.currentDailyPeriod().timezone()).isEqualTo("Asia/Kolkata");
    assertThat(usage.currentMonthlyPeriod().periodId()).isEqualTo("2026-04");
    assertThat(usage.dimensions())
        .extracting(SuperAdminUsageDtos.DimensionUsage::dimension)
        .containsExactly("USERS", "STORAGE", "API_CALLS", "PDF_EXPORTS", "EMAILS", "JOBS");
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
              assertThat(dimension.used()).isEqualTo(2048L);
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
