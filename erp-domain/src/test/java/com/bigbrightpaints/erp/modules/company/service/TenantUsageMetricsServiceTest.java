package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;

@ExtendWith(MockitoExtension.class)
class TenantUsageMetricsServiceTest {

  @Mock private CompanyRepository companyRepository;

  @Mock private SystemSettingsRepository systemSettingsRepository;

  @Mock private TenantUsageRollupService tenantUsageRollupService;

  @Test
  void recordApiCall_concurrentTraffic_buffersAndFlushesOneBoundedWrite() throws Exception {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));

    int calls = 120;
    ExecutorService executor = Executors.newFixedThreadPool(12);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();
    try {
      for (int i = 0; i < calls; i++) {
        futures.add(
            executor.submit(
                () -> {
                  try {
                    startLatch.await(5, TimeUnit.SECONDS);
                  } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                  }
                  service.recordApiCall("ACME");
                }));
      }

      startLatch.countDown();
      for (Future<?> future : futures) {
        future.get(5, TimeUnit.SECONDS);
      }
    } finally {
      executor.shutdownNow();
      executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    verifyNoInteractions(systemSettingsRepository);
    verifyNoInteractions(tenantUsageRollupService);

    service.flushPendingMetrics();

    verify(systemSettingsRepository)
        .incrementLongSettingBy("tenant.usage.api-call-count.42", calls);
    verify(tenantUsageRollupService).recordApiCalls(company, calls);
    verify(systemSettingsRepository)
        .save(
            argThat(
                setting ->
                    setting != null
                        && "tenant.usage.last-activity-at.42".equals(setting.getKey())));
    verify(systemSettingsRepository, never())
        .save(
            argThat(
                setting ->
                    setting != null && "tenant.usage.api-call-count.42".equals(setting.getKey())));
  }

  @Test
  void recordApiCall_unknownCompany_skipsPersistence() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(companyRepository, systemSettingsRepository);
    when(companyRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

    service.recordApiCall("NOPE");
    service.flushPendingMetrics();

    verifyNoInteractions(systemSettingsRepository);
  }

  @Test
  void recordApiCall_blankCompanyCodeDoesNotCreateFlushWork() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);

    service.recordApiCall("   ");
    service.flushPendingMetrics();

    verifyNoInteractions(companyRepository, systemSettingsRepository, tenantUsageRollupService);
  }

  @Test
  void flushPendingMetrics_skipsCompanyWithoutPersistedId() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company transientCompany = company(null, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(transientCompany));

    service.recordApiCall(" acme ");
    service.flushPendingMetrics();

    verifyNoInteractions(systemSettingsRepository, tenantUsageRollupService);
  }

  @Test
  void flushPendingMetrics_twoArgumentConstructorPersistsOnlyCanonicalSettings() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(companyRepository, systemSettingsRepository);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));

    service.recordApiCall("ACME");
    service.flushPendingMetrics();
    clearInvocations(systemSettingsRepository, companyRepository);
    service.flushPendingMetrics();

    verify(systemSettingsRepository, never())
        .incrementLongSettingBy("tenant.usage.api-call-count.42", 0L);
    verifyNoInteractions(companyRepository);
  }

  @Test
  void flushPendingMetrics_restoresDrainedUsageWhenPersistenceFails() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));
    doThrow(new RuntimeException("settings-write-down"))
        .doNothing()
        .when(systemSettingsRepository)
        .incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);

    service.recordApiCall("ACME");

    assertThatThrownBy(service::flushPendingMetrics).hasMessageContaining("settings-write-down");
    service.flushPendingMetrics();

    verify(systemSettingsRepository, times(2))
        .incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);
    verify(tenantUsageRollupService).recordApiCalls(company, 1L);
  }

  @Test
  void pendingUsage_restoreIgnoresNullDrain() throws Exception {
    Class<?> pendingUsageClass =
        Class.forName(TenantUsageMetricsService.class.getName() + "$PendingTenantUsage");
    Constructor<?> constructor = pendingUsageClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object pendingUsage = constructor.newInstance();
    Method restore =
        pendingUsageClass.getDeclaredMethod(
            "restore", Class.forName(pendingUsageClass.getName() + "$DrainedUsage"));
    restore.setAccessible(true);

    restore.invoke(pendingUsage, new Object[] {null});

    verifyNoInteractions(companyRepository, systemSettingsRepository, tenantUsageRollupService);
  }

  @Test
  void flushPendingMetrics_drainedUsageWithoutActivitySkipsLastActivityPersistence()
      throws Exception {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));
    Object pendingUsage = newPendingUsage();
    invokePendingUsageRecord(pendingUsage, null);
    @SuppressWarnings("unchecked")
    ConcurrentMap<String, Object> pending =
        (ConcurrentMap<String, Object>)
            ReflectionTestUtils.getField(service, "pendingUsageByCompanyCode");
    pending.put("ACME", pendingUsage);

    service.flushPendingMetrics();

    verify(systemSettingsRepository).incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);
    verify(tenantUsageRollupService).recordApiCalls(company, 1L);
    verify(systemSettingsRepository, never()).save(any());
  }

  @Test
  void flushPendingMetrics_privateGuardIgnoresBlankCompanyAndNullBucket() throws Exception {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Object pendingUsage = newPendingUsage();
    Method flush =
        TenantUsageMetricsService.class.getDeclaredMethod(
            "flushPendingMetrics", String.class, pendingUsage.getClass());
    flush.setAccessible(true);

    flush.invoke(service, "   ", pendingUsage);
    flush.invoke(service, "ACME", (Object) null);

    verifyNoInteractions(companyRepository, systemSettingsRepository, tenantUsageRollupService);
  }

  @Test
  void pendingUsage_restoreCoversZeroCountsAndOlderActivityTimestamps() throws Exception {
    Object pendingUsage = newPendingUsage();
    Class<?> drainedUsageClass = Class.forName(pendingUsage.getClass().getName() + "$DrainedUsage");
    Constructor<?> drainedConstructor =
        drainedUsageClass.getDeclaredConstructor(long.class, Instant.class);
    drainedConstructor.setAccessible(true);
    Method restore = pendingUsage.getClass().getDeclaredMethod("restore", drainedUsageClass);
    restore.setAccessible(true);

    restore.invoke(pendingUsage, drainedConstructor.newInstance(0L, null));
    restore.invoke(
        pendingUsage, drainedConstructor.newInstance(1L, Instant.parse("2026-01-01T00:00:10Z")));
    restore.invoke(
        pendingUsage, drainedConstructor.newInstance(1L, Instant.parse("2026-01-01T00:00:05Z")));

    verifyNoInteractions(companyRepository, systemSettingsRepository, tenantUsageRollupService);
  }

  private Object newPendingUsage() throws Exception {
    Class<?> pendingUsageClass =
        Class.forName(TenantUsageMetricsService.class.getName() + "$PendingTenantUsage");
    Constructor<?> constructor = pendingUsageClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    return constructor.newInstance();
  }

  private void invokePendingUsageRecord(Object pendingUsage, Instant occurredAt) throws Exception {
    Method record = pendingUsage.getClass().getDeclaredMethod("record", Instant.class);
    record.setAccessible(true);
    record.invoke(pendingUsage, occurredAt);
  }

  private Company company(Long id, String code) {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", id);
    ReflectionTestUtils.setField(company, "publicId", UUID.randomUUID());
    company.setName("Company " + code);
    company.setCode(code);
    company.setTimezone("UTC");
    return company;
  }
}
