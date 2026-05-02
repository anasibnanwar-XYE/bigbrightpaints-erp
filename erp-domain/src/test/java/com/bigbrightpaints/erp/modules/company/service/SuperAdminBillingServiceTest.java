package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.modules.company.domain.Company;

class SuperAdminBillingServiceTest {

  @Test
  void safeRuntimeLimit_preservesZeroUnlimitedAndCapsOverflow() {
    SuperAdminBillingService service = new SuperAdminBillingService(null, null, null, null, null);

    assertThat(ReflectionTestUtils.<Integer>invokeMethod(service, "safeRuntimeLimit", 0L)).isZero();
    assertThat(ReflectionTestUtils.<Integer>invokeMethod(service, "safeRuntimeLimit", 7L))
        .isEqualTo(7);
    assertThat(
            ReflectionTestUtils.<Integer>invokeMethod(
                service, "safeRuntimeLimit", (long) Integer.MAX_VALUE + 1L))
        .isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  void currentBurstRequestsPerMinuteUsesPersistedRuntimePolicySnapshot() {
    TenantRuntimeEnforcementService runtimeService = mock(TenantRuntimeEnforcementService.class);
    SuperAdminBillingService service =
        new SuperAdminBillingService(null, null, null, null, runtimeService);
    Company company = new Company();
    company.setCode("ACME");
    when(runtimeService.snapshot("ACME")).thenReturn(runtimeSnapshot(25));

    assertThat(
            ReflectionTestUtils.<Integer>invokeMethod(
                service, "currentBurstRequestsPerMinute", company))
        .isEqualTo(25);
  }

  private TenantRuntimeEnforcementService.TenantRuntimeSnapshot runtimeSnapshot(
      int burstRequestsPerMinute) {
    Instant now = Instant.parse("2026-03-26T09:00:00Z");
    return new TenantRuntimeEnforcementService.TenantRuntimeSnapshot(
        "ACME",
        TenantRuntimeEnforcementService.TenantRuntimeState.ACTIVE,
        "POLICY_ACTIVE",
        "policy",
        now,
        8,
        burstRequestsPerMinute,
        10,
        new TenantRuntimeEnforcementService.TenantRuntimeMetrics(
            0, 0, 0, 0, 0, 0, 0, now, now.plusSeconds(60), now.plusSeconds(60), now));
  }
}
