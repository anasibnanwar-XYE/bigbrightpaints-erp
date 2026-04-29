package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface TenantUsageRollupRepository extends JpaRepository<TenantUsageRollup, Long> {

  Optional<TenantUsageRollup> findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
      Long companyId, String dimension, String periodType, Instant periodStartAt);

  List<TenantUsageRollup>
      findByCompany_IdAndClosedFalseAndPeriodEndAtLessThanEqualOrderByPeriodEndAtAsc(
          Long companyId, Instant now);

  List<TenantUsageRollup>
      findTop100ByCompany_IdAndPeriodTypeAndClosedTrueOrderByPeriodStartAtDescDimensionAsc(
          Long companyId, String periodType);

  List<TenantUsageRollup> findByCompany_IdAndPeriodTypeAndPeriodStartAtOrderByDimensionAsc(
      Long companyId, String periodType, Instant periodStartAt);

  @Modifying
  @Transactional
  @Query(
      value =
          """
          INSERT INTO tenant_usage_rollups (
              company_id,
              company_code,
              dimension,
              period_type,
              period_start_at,
              period_end_at,
              tenant_timezone,
              usage_count,
              usage_bytes,
              source,
              closed,
              created_at,
              updated_at
          ) VALUES (
              :companyId,
              :companyCode,
              :dimension,
              :periodType,
              :periodStartAt,
              :periodEndAt,
              :tenantTimezone,
              :countDelta,
              :bytesDelta,
              'COUNTER',
              FALSE,
              now(),
              now()
          )
          ON CONFLICT (company_id, dimension, period_type, period_start_at)
          DO UPDATE SET
              company_code = EXCLUDED.company_code,
              period_end_at = EXCLUDED.period_end_at,
              tenant_timezone = EXCLUDED.tenant_timezone,
              usage_count = tenant_usage_rollups.usage_count + EXCLUDED.usage_count,
              usage_bytes = tenant_usage_rollups.usage_bytes + EXCLUDED.usage_bytes,
              source = 'COUNTER',
              updated_at = now()
          """,
      nativeQuery = true)
  void incrementCounter(
      @Param("companyId") Long companyId,
      @Param("companyCode") String companyCode,
      @Param("dimension") String dimension,
      @Param("periodType") String periodType,
      @Param("periodStartAt") Instant periodStartAt,
      @Param("periodEndAt") Instant periodEndAt,
      @Param("tenantTimezone") String tenantTimezone,
      @Param("countDelta") long countDelta,
      @Param("bytesDelta") long bytesDelta);
}
