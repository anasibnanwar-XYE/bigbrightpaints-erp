package com.bigbrightpaints.erp.modules.company.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.config.SystemSetting;
import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;

import jakarta.annotation.PreDestroy;

@Service
public class TenantUsageMetricsService {

  private static final Logger log = LoggerFactory.getLogger(TenantUsageMetricsService.class);

  private static final String API_CALL_COUNT_PREFIX = "tenant.usage.api-call-count.";
  private static final String LAST_ACTIVITY_AT_PREFIX = "tenant.usage.last-activity-at.";
  private static final TransactionOperations DIRECT_TRANSACTION =
      new TransactionOperations() {
        @Override
        public <T> T execute(TransactionCallback<T> action) {
          return action.doInTransaction(new SimpleTransactionStatus());
        }
      };

  private final ConcurrentMap<String, PendingTenantUsage> pendingUsageByCompanyCode =
      new ConcurrentHashMap<>();
  private final CompanyRepository companyRepository;
  private final SystemSettingsRepository systemSettingsRepository;
  private final TenantUsageRollupService tenantUsageRollupService;
  private final TransactionOperations tenantFlushTransaction;

  TenantUsageMetricsService(
      CompanyRepository companyRepository, SystemSettingsRepository systemSettingsRepository) {
    this(companyRepository, systemSettingsRepository, null, DIRECT_TRANSACTION);
  }

  TenantUsageMetricsService(
      CompanyRepository companyRepository,
      SystemSettingsRepository systemSettingsRepository,
      TenantUsageRollupService tenantUsageRollupService) {
    this(companyRepository, systemSettingsRepository, tenantUsageRollupService, DIRECT_TRANSACTION);
  }

  @Autowired
  public TenantUsageMetricsService(
      CompanyRepository companyRepository,
      SystemSettingsRepository systemSettingsRepository,
      TenantUsageRollupService tenantUsageRollupService,
      PlatformTransactionManager transactionManager) {
    this(
        companyRepository,
        systemSettingsRepository,
        tenantUsageRollupService,
        tenantFlushTransaction(transactionManager));
  }

  TenantUsageMetricsService(
      CompanyRepository companyRepository,
      SystemSettingsRepository systemSettingsRepository,
      TenantUsageRollupService tenantUsageRollupService,
      TransactionOperations tenantFlushTransaction) {
    this.companyRepository = companyRepository;
    this.systemSettingsRepository = systemSettingsRepository;
    this.tenantUsageRollupService = tenantUsageRollupService;
    this.tenantFlushTransaction = tenantFlushTransaction;
  }

  public void recordApiCall(String companyCode) {
    String normalizedCompanyCode = normalizeCompanyCode(companyCode);
    if (!StringUtils.hasText(normalizedCompanyCode)) {
      return;
    }
    Instant occurredAt = CompanyTime.now();
    pendingUsageByCompanyCode.compute(
        normalizedCompanyCode,
        (ignored, pendingUsage) -> {
          PendingTenantUsage usage = pendingUsage == null ? new PendingTenantUsage() : pendingUsage;
          usage.record(occurredAt);
          return usage;
        });
  }

  @Scheduled(fixedDelayString = "${erp.tenant.usage.flush-ms:30000}")
  public void flushPendingMetrics() {
    RuntimeException firstFailure = null;
    for (Map.Entry<String, PendingTenantUsage> pendingEntry :
        pendingUsageByCompanyCode.entrySet()) {
      try {
        flushPendingMetrics(pendingEntry.getKey(), pendingEntry.getValue());
      } catch (RuntimeException ex) {
        if (firstFailure == null) {
          firstFailure = ex;
        }
      }
    }
    if (firstFailure != null) {
      throw firstFailure;
    }
  }

  @PreDestroy
  public void flushPendingMetricsBeforeShutdown() {
    try {
      flushPendingMetrics();
    } catch (RuntimeException ex) {
      log.warn(
          "Unable to flush pending tenant usage metrics during shutdown: {}: {}",
          ex.getClass().getSimpleName(),
          ex.getMessage());
    }
  }

  private void flushPendingMetrics(String companyCode, PendingTenantUsage pendingUsage) {
    if (!StringUtils.hasText(companyCode) || pendingUsage == null) {
      return;
    }
    PendingTenantUsage.DrainedUsage drainedUsage = pendingUsage.drain();
    if (drainedUsage.apiCalls() <= 0L) {
      removeIdleBucket(companyCode, pendingUsage);
      return;
    }
    try {
      tenantFlushTransaction.execute(
          status -> {
            persistDrainedUsage(companyCode, drainedUsage);
            return null;
          });
      removeIdleBucket(companyCode, pendingUsage);
    } catch (RuntimeException ex) {
      pendingUsage.restore(drainedUsage);
      throw ex;
    }
  }

  private void removeIdleBucket(String companyCode, PendingTenantUsage pendingUsage) {
    pendingUsageByCompanyCode.computeIfPresent(
        companyCode,
        (ignored, currentUsage) ->
            currentUsage == pendingUsage && currentUsage.isEmpty() ? null : currentUsage);
  }

  private void persistDrainedUsage(
      String companyCode, PendingTenantUsage.DrainedUsage drainedUsage) {
    Optional<Company> companyOptional = companyRepository.findByCodeIgnoreCase(companyCode);
    if (companyOptional.isEmpty()) {
      return;
    }
    Company company = companyOptional.get();
    Long companyId = company.getId();
    if (companyId == null) {
      return;
    }
    systemSettingsRepository.incrementLongSettingBy(
        apiCallCountKey(companyId), drainedUsage.apiCalls());
    if (tenantUsageRollupService != null) {
      tenantUsageRollupService.recordApiCalls(company, drainedUsage.apiCalls());
    }
    if (drainedUsage.lastActivityAt() != null) {
      persistSetting(lastActivityAtKey(companyId), drainedUsage.lastActivityAt().toString());
    }
  }

  public long getApiCallCount(Long companyId) {
    if (companyId == null) {
      return 0L;
    }
    return parseLong(readSetting(apiCallCountKey(companyId)), 0L);
  }

  public Instant getLastActivityAt(Long companyId) {
    if (companyId == null) {
      return null;
    }
    String raw = readSetting(lastActivityAtKey(companyId));
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    try {
      return Instant.parse(raw.trim());
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private String readSetting(String key) {
    return systemSettingsRepository.findById(key).map(SystemSetting::getValue).orElse(null);
  }

  private void persistSetting(String key, String value) {
    systemSettingsRepository.save(new SystemSetting(key, value));
  }

  private long parseLong(String raw, long defaultValue) {
    if (!StringUtils.hasText(raw)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(ValidationUtils.requireNotBlank(raw, "setting value"));
    } catch (RuntimeException ex) {
      return defaultValue;
    }
  }

  private String apiCallCountKey(Long companyId) {
    return API_CALL_COUNT_PREFIX + companyId;
  }

  private String lastActivityAtKey(Long companyId) {
    return LAST_ACTIVITY_AT_PREFIX + companyId;
  }

  private String normalizeCompanyCode(String companyCode) {
    return StringUtils.hasText(companyCode)
        ? companyCode.trim().toUpperCase(java.util.Locale.ROOT)
        : null;
  }

  private static TransactionOperations tenantFlushTransaction(
      PlatformTransactionManager transactionManager) {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transactionTemplate;
  }

  private static final class PendingTenantUsage {
    private final AtomicLong apiCalls = new AtomicLong();
    private final AtomicReference<Instant> lastActivityAt = new AtomicReference<>();

    private void record(Instant occurredAt) {
      apiCalls.incrementAndGet();
      if (occurredAt != null) {
        lastActivityAt.accumulateAndGet(
            occurredAt,
            (current, next) -> current == null || next.isAfter(current) ? next : current);
      }
    }

    private DrainedUsage drain() {
      return new DrainedUsage(apiCalls.getAndSet(0L), lastActivityAt.getAndSet(null));
    }

    private boolean isEmpty() {
      return apiCalls.get() <= 0L && lastActivityAt.get() == null;
    }

    private void restore(DrainedUsage drainedUsage) {
      if (drainedUsage == null) {
        return;
      }
      if (drainedUsage.apiCalls() > 0L) {
        apiCalls.addAndGet(drainedUsage.apiCalls());
      }
      if (drainedUsage.lastActivityAt() != null) {
        lastActivityAt.accumulateAndGet(
            drainedUsage.lastActivityAt(),
            (current, restored) ->
                current == null || restored.isAfter(current) ? restored : current);
      }
    }

    private record DrainedUsage(long apiCalls, Instant lastActivityAt) {}
  }
}
