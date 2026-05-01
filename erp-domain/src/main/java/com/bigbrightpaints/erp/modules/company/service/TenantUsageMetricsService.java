package com.bigbrightpaints.erp.modules.company.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.config.SystemSetting;
import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;

@Service
public class TenantUsageMetricsService {

  private static final String API_CALL_COUNT_PREFIX = "tenant.usage.api-call-count.";
  private static final String LAST_ACTIVITY_AT_PREFIX = "tenant.usage.last-activity-at.";

  private final ConcurrentMap<String, PendingTenantUsage> pendingUsageByCompanyCode =
      new ConcurrentHashMap<>();
  private final CompanyRepository companyRepository;
  private final SystemSettingsRepository systemSettingsRepository;
  private final TenantUsageRollupService tenantUsageRollupService;

  public TenantUsageMetricsService(
      CompanyRepository companyRepository, SystemSettingsRepository systemSettingsRepository) {
    this(companyRepository, systemSettingsRepository, null);
  }

  @Autowired
  public TenantUsageMetricsService(
      CompanyRepository companyRepository,
      SystemSettingsRepository systemSettingsRepository,
      TenantUsageRollupService tenantUsageRollupService) {
    this.companyRepository = companyRepository;
    this.systemSettingsRepository = systemSettingsRepository;
    this.tenantUsageRollupService = tenantUsageRollupService;
  }

  public void recordApiCall(String companyCode) {
    String normalizedCompanyCode = normalizeCompanyCode(companyCode);
    if (!StringUtils.hasText(normalizedCompanyCode)) {
      return;
    }
    pendingUsageByCompanyCode
        .computeIfAbsent(normalizedCompanyCode, ignored -> new PendingTenantUsage())
        .record(CompanyTime.now());
  }

  @Scheduled(fixedDelayString = "${erp.tenant.usage.flush-ms:30000}")
  @Transactional
  public void flushPendingMetrics() {
    for (Map.Entry<String, PendingTenantUsage> pendingEntry :
        pendingUsageByCompanyCode.entrySet()) {
      flushPendingMetrics(pendingEntry.getKey(), pendingEntry.getValue());
    }
  }

  private void flushPendingMetrics(String companyCode, PendingTenantUsage pendingUsage) {
    if (!StringUtils.hasText(companyCode) || pendingUsage == null) {
      return;
    }
    PendingTenantUsage.DrainedUsage drainedUsage = pendingUsage.drain();
    if (drainedUsage.apiCalls() <= 0L) {
      return;
    }
    try {
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
    } catch (RuntimeException ex) {
      pendingUsage.restore(drainedUsage);
      throw ex;
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

  private long parseLong(String raw, long fallback) {
    if (!StringUtils.hasText(raw)) {
      return fallback;
    }
    try {
      return Long.parseLong(ValidationUtils.requireNotBlank(raw, "setting value"));
    } catch (RuntimeException ex) {
      return fallback;
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
