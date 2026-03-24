package com.bigbrightpaints.erp.modules.company.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  @Test
  void recordApiCall_concurrentTraffic_usesAtomicIncrementPerCall() throws Exception {
    TenantUsageMetricsService service =
        new TenantUsageMetricsService(companyRepository, systemSettingsRepository);
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

    verify(systemSettingsRepository, times(calls))
        .incrementLongSetting("tenant.usage.api-call-count.42");
    verify(systemSettingsRepository, times(calls))
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

    verifyNoInteractions(systemSettingsRepository);
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
