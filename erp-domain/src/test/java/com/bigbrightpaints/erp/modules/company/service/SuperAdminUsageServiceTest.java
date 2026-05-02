package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;

@ExtendWith(MockitoExtension.class)
class SuperAdminUsageServiceTest {

  @Mock private TenantUsageRollupService tenantUsageRollupService;
  @Mock private TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService;

  @Test
  void tenantUsageAddsRuntimeOperationalDimensionsFromAdmissionSnapshot() {
    SuperAdminUsageService service =
        new SuperAdminUsageService(tenantUsageRollupService, tenantRuntimeRequestAdmissionService);
    SuperAdminUsageDtos.Period monthly =
        new SuperAdminUsageDtos.Period(
            "MONTHLY",
            "2026-04",
            Instant.parse("2026-04-01T00:00:00Z"),
            Instant.parse("2026-05-01T00:00:00Z"),
            "UTC",
            false,
            null);
    SuperAdminUsageDtos.DimensionUsage durableApiCalls =
        new SuperAdminUsageDtos.DimensionUsage(
            "API_CALLS", "API calls", "COUNTER", "COUNT", 17L, 100L, 17L, "OK", monthly);
    when(tenantUsageRollupService.getTenantUsage(7L, Map.of()))
        .thenReturn(
            new SuperAdminUsageDtos.TenantUsage(
                7L,
                "ACME",
                "Acme",
                "UTC",
                null,
                monthly,
                List.of(durableApiCalls),
                List.of(),
                List.of()));
    when(tenantRuntimeRequestAdmissionService.snapshot("ACME"))
        .thenReturn(
            new TenantRuntimeEnforcementService.TenantRuntimeSnapshot(
                "ACME",
                TenantRuntimeEnforcementService.TenantRuntimeState.ACTIVE,
                "POLICY_ACTIVE",
                "audit-1",
                Instant.parse("2026-04-01T00:00:00Z"),
                3,
                10,
                5,
                new TenantRuntimeEnforcementService.TenantRuntimeMetrics(
                    22L,
                    4L,
                    1L,
                    2,
                    8,
                    3,
                    4L,
                    Instant.parse("2026-04-01T00:12:00Z"),
                    Instant.parse("2026-04-01T00:13:00Z"),
                    Instant.parse("2026-04-01T00:13:00Z"),
                    Instant.parse("2026-04-01T00:12:34Z"))));

    SuperAdminUsageDtos.TenantUsage usage = service.getTenantUsage(7L, Map.of());

    assertThat(usage.dimensions()).containsExactly(durableApiCalls);
    assertThat(usage.operationalDimensions())
        .extracting(SuperAdminUsageDtos.DimensionUsage::dimension)
        .containsExactly(
            "CURRENT_WINDOW_API_REQUESTS",
            "CURRENT_WINDOW_REJECTED_REQUESTS",
            "IN_FLIGHT_CONCURRENT_REQUESTS");
    assertThat(usage.operationalDimensions())
        .filteredOn(dimension -> "CURRENT_WINDOW_API_REQUESTS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.used()).isEqualTo(8L);
              assertThat(dimension.limit()).isEqualTo(10L);
              assertThat(dimension.accountingMode()).isEqualTo("RUNTIME_WINDOW");
              assertThat(dimension.period().periodType()).isEqualTo("MINUTE");
              assertThat(dimension.period().startAt()).isEqualTo("2026-04-01T00:12:00Z");
              assertThat(dimension.period().endAt()).isEqualTo("2026-04-01T00:13:00Z");
            });
    assertThat(usage.operationalDimensions())
        .filteredOn(dimension -> "CURRENT_WINDOW_REJECTED_REQUESTS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.used()).isEqualTo(3L);
              assertThat(dimension.limit()).isEqualTo(10L);
              assertThat(dimension.accountingMode()).isEqualTo("RUNTIME_WINDOW");
            });
    assertThat(usage.operationalDimensions())
        .filteredOn(dimension -> "IN_FLIGHT_CONCURRENT_REQUESTS".equals(dimension.dimension()))
        .singleElement()
        .satisfies(
            dimension -> {
              assertThat(dimension.used()).isEqualTo(2L);
              assertThat(dimension.limit()).isEqualTo(3L);
              assertThat(dimension.accountingMode()).isEqualTo("RUNTIME_SNAPSHOT");
              assertThat(dimension.period().periodType()).isEqualTo("POINT_IN_TIME");
              assertThat(dimension.period().startAt()).isEqualTo("2026-04-01T00:12:34Z");
            });
    assertThat(usage.toString().toLowerCase())
        .doesNotContain("invoice", "ledger", "inventory", "salary", "vendor", "customer");
  }
}
