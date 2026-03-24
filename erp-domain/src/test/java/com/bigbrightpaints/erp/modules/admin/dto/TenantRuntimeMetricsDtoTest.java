package com.bigbrightpaints.erp.modules.admin.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TenantRuntimeMetricsDtoTest {

  @Test
  void accessorsExposeSnapshotValues() {
    Instant updatedAt = Instant.parse("2026-02-19T04:30:00Z");
    TenantRuntimeMetricsDto dto =
        new TenantRuntimeMetricsDto(
            "ACME",
            "HOLD",
            "Risk review",
            250,
            1200,
            40,
            25L,
            30L,
            110,
            4,
            2,
            "policy-ref-7",
            updatedAt);

    assertThat(dto.companyCode()).isEqualTo("ACME");
    assertThat(dto.holdState()).isEqualTo("HOLD");
    assertThat(dto.holdReason()).isEqualTo("Risk review");
    assertThat(dto.maxActiveUsers()).isEqualTo(250);
    assertThat(dto.maxRequestsPerMinute()).isEqualTo(1200);
    assertThat(dto.maxConcurrentRequests()).isEqualTo(40);
    assertThat(dto.enabledUsers()).isEqualTo(25L);
    assertThat(dto.totalUsers()).isEqualTo(30L);
    assertThat(dto.requestsThisMinute()).isEqualTo(110);
    assertThat(dto.blockedThisMinute()).isEqualTo(4);
    assertThat(dto.inFlightRequests()).isEqualTo(2);
    assertThat(dto.policyReference()).isEqualTo("policy-ref-7");
    assertThat(dto.policyUpdatedAt()).isEqualTo(updatedAt);
  }
}
