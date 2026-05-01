package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

import com.bigbrightpaints.erp.core.config.SystemSetting;
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
  void flushPendingMetrics_removesIdleTenantBucketsAfterSuccessfulFlush() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));

    service.recordApiCall("ACME");
    service.flushPendingMetrics();

    assertThat(pendingUsageSize(service)).isZero();
  }

  @Test
  void flushPendingMetrics_keepsTenantBucketWhenNewUsageArrivesDuringFlush() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    AtomicBoolean concurrentCallRecorded = new AtomicBoolean();
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));
    doAnswer(
            invocation -> {
              if (concurrentCallRecorded.compareAndSet(false, true)) {
                service.recordApiCall("ACME");
              }
              return null;
            })
        .when(systemSettingsRepository)
        .incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);

    service.recordApiCall("ACME");
    service.flushPendingMetrics();

    assertThat(pendingUsageSize(service)).isEqualTo(1);
    service.flushPendingMetrics();

    verify(systemSettingsRepository, times(2))
        .incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);
    assertThat(pendingUsageSize(service)).isZero();
  }

  @Test
  void flushPendingMetricsBeforeShutdown_persistsBufferedUsage() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));

    service.recordApiCall("ACME");
    service.recordApiCall("ACME");
    service.flushPendingMetricsBeforeShutdown();

    verify(systemSettingsRepository).incrementLongSettingBy("tenant.usage.api-call-count.42", 2L);
    verify(tenantUsageRollupService).recordApiCalls(company, 2L);
  }

  @Test
  void flushPendingMetricsBeforeShutdown_doesNotThrowWhenFinalFlushCannotReachStorage() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    when(companyRepository.findByCodeIgnoreCase("ACME"))
        .thenThrow(new RuntimeException("storage-down"));

    service.recordApiCall("ACME");
    service.flushPendingMetricsBeforeShutdown();

    verify(companyRepository).findByCodeIgnoreCase("ACME");
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
  void flushPendingMetrics_rollsBackBucketWhenRollupWriteFailsThenRetriesOnce() {
    TransactionalUsageProbe probe = new TransactionalUsageProbe();
    probe.failAfterRollupOnce("rollup-down");
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository,
            systemSettingsRepository,
            tenantUsageRollupService,
            probe.transactionOperations());
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));
    doAnswer(
            invocation -> {
              probe.stageApiCalls(invocation.getArgument(1, Long.class));
              return null;
            })
        .when(systemSettingsRepository)
        .incrementLongSettingBy(anyString(), anyLong());
    doAnswer(
            invocation -> {
              probe.stageRollupApiCalls(invocation.getArgument(1, Long.class));
              return null;
            })
        .when(tenantUsageRollupService)
        .recordApiCalls(any(), anyLong());
    doAnswer(
            invocation -> {
              probe.stageLastActivity(invocation.getArgument(0, SystemSetting.class));
              return invocation.getArgument(0);
            })
        .when(systemSettingsRepository)
        .save(any(SystemSetting.class));

    service.recordApiCall("ACME");

    assertThatThrownBy(service::flushPendingMetrics).hasMessageContaining("rollup-down");
    assertThat(probe.apiCalls()).isZero();
    assertThat(probe.rollupApiCalls()).isZero();

    service.flushPendingMetrics();

    assertThat(probe.apiCalls()).isEqualTo(1L);
    assertThat(probe.rollupApiCalls()).isEqualTo(1L);
    assertThat(probe.lastActivityAt()).isNotNull();
  }

  @Test
  void flushPendingMetrics_rollsBackBucketWhenLastActivityWriteFailsThenRetriesOnce() {
    TransactionalUsageProbe probe = new TransactionalUsageProbe();
    probe.failAfterLastActivityOnce("last-activity-down");
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository,
            systemSettingsRepository,
            tenantUsageRollupService,
            probe.transactionOperations());
    Company company = company(42L, "ACME");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(company));
    doAnswer(
            invocation -> {
              probe.stageApiCalls(invocation.getArgument(1, Long.class));
              return null;
            })
        .when(systemSettingsRepository)
        .incrementLongSettingBy(anyString(), anyLong());
    doAnswer(
            invocation -> {
              probe.stageRollupApiCalls(invocation.getArgument(1, Long.class));
              return null;
            })
        .when(tenantUsageRollupService)
        .recordApiCalls(any(), anyLong());
    doAnswer(
            invocation -> {
              probe.stageLastActivity(invocation.getArgument(0, SystemSetting.class));
              return invocation.getArgument(0);
            })
        .when(systemSettingsRepository)
        .save(any(SystemSetting.class));

    service.recordApiCall("ACME");

    assertThatThrownBy(service::flushPendingMetrics).hasMessageContaining("last-activity-down");
    assertThat(probe.apiCalls()).isZero();
    assertThat(probe.rollupApiCalls()).isZero();
    assertThat(probe.lastActivityAt()).isNull();

    service.flushPendingMetrics();

    assertThat(probe.apiCalls()).isEqualTo(1L);
    assertThat(probe.rollupApiCalls()).isEqualTo(1L);
    assertThat(probe.lastActivityAt()).isNotNull();
  }

  @Test
  void flushPendingMetrics_keepsOtherTenantFlushesWhenOneTenantFails() {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(
            companyRepository, systemSettingsRepository, tenantUsageRollupService);
    Company acme = company(42L, "ACME");
    Company bravo = company(43L, "BRAVO");
    when(companyRepository.findByCodeIgnoreCase("ACME")).thenReturn(Optional.of(acme));
    when(companyRepository.findByCodeIgnoreCase("BRAVO")).thenReturn(Optional.of(bravo));
    doThrow(new RuntimeException("bravo-write-down"))
        .doNothing()
        .when(systemSettingsRepository)
        .incrementLongSettingBy("tenant.usage.api-call-count.43", 1L);

    service.recordApiCall("ACME");
    service.recordApiCall("BRAVO");

    assertThatThrownBy(service::flushPendingMetrics).hasMessageContaining("bravo-write-down");
    service.flushPendingMetrics();

    verify(systemSettingsRepository).incrementLongSettingBy("tenant.usage.api-call-count.42", 1L);
    verify(systemSettingsRepository, times(2))
        .incrementLongSettingBy("tenant.usage.api-call-count.43", 1L);
    verify(tenantUsageRollupService).recordApiCalls(acme, 1L);
    verify(tenantUsageRollupService).recordApiCalls(bravo, 1L);
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

  private int pendingUsageSize(TenantUsageMetricsService service) {
    @SuppressWarnings("unchecked")
    ConcurrentMap<String, Object> pending =
        (ConcurrentMap<String, Object>)
            ReflectionTestUtils.getField(service, "pendingUsageByCompanyCode");
    return pending == null ? 0 : pending.size();
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

  private static final class TransactionalUsageProbe {
    private final ThreadLocal<List<Runnable>> stagedWrites = new ThreadLocal<>();
    private long apiCalls;
    private long rollupApiCalls;
    private String lastActivityAt;
    private String failAfterRollupMessage;
    private String failAfterLastActivityMessage;

    private TransactionOperations transactionOperations() {
      return new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
          stagedWrites.set(new ArrayList<>());
          try {
            T result = action.doInTransaction(new SimpleTransactionStatus());
            stagedWrites.get().forEach(Runnable::run);
            return result;
          } finally {
            stagedWrites.remove();
          }
        }
      };
    }

    private void failAfterRollupOnce(String message) {
      this.failAfterRollupMessage = message;
    }

    private void failAfterLastActivityOnce(String message) {
      this.failAfterLastActivityMessage = message;
    }

    private void stageApiCalls(long count) {
      stage(() -> apiCalls += count);
    }

    private void stageRollupApiCalls(long count) {
      stage(() -> rollupApiCalls += count);
      if (failAfterRollupMessage != null) {
        String message = failAfterRollupMessage;
        failAfterRollupMessage = null;
        throw new RuntimeException(message);
      }
    }

    private void stageLastActivity(SystemSetting setting) {
      stage(() -> lastActivityAt = setting.getValue());
      if (failAfterLastActivityMessage != null) {
        String message = failAfterLastActivityMessage;
        failAfterLastActivityMessage = null;
        throw new RuntimeException(message);
      }
    }

    private void stage(Runnable write) {
      List<Runnable> transactionWrites = stagedWrites.get();
      if (transactionWrites == null) {
        throw new IllegalStateException("usage write happened outside tenant flush transaction");
      }
      transactionWrites.add(write);
    }

    private long apiCalls() {
      return apiCalls;
    }

    private long rollupApiCalls() {
      return rollupApiCalls;
    }

    private String lastActivityAt() {
      return lastActivityAt;
    }
  }
}
