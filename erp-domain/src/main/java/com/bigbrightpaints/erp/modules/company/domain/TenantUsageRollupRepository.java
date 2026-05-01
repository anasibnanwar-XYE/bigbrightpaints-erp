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
    GREATEST(:countDelta, 0),
    GREATEST(:bytesDelta, 0),
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
    usage_count = CASE
        WHEN :countDelta < 0 THEN GREATEST(0, tenant_usage_rollups.usage_count + :countDelta)
        WHEN tenant_usage_rollups.usage_count > 9223372036854775807 - :countDelta
            THEN 9223372036854775807
        ELSE tenant_usage_rollups.usage_count + :countDelta
    END,
    usage_bytes = CASE
        WHEN :bytesDelta < 0 THEN GREATEST(0, tenant_usage_rollups.usage_bytes + :bytesDelta)
        WHEN tenant_usage_rollups.usage_bytes > 9223372036854775807 - :bytesDelta
            THEN 9223372036854775807
        ELSE tenant_usage_rollups.usage_bytes + :bytesDelta
    END,
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

  @Modifying(flushAutomatically = true, clearAutomatically = true)
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
    closed_at,
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
    GREATEST(:usageCount, 0),
    GREATEST(:usageBytes, 0),
    'SNAPSHOT',
    FALSE,
    NULL,
    now(),
    now()
)
ON CONFLICT (company_id, dimension, period_type, period_start_at)
DO UPDATE SET
    company_code = EXCLUDED.company_code,
    period_end_at = EXCLUDED.period_end_at,
    tenant_timezone = EXCLUDED.tenant_timezone,
    usage_count = GREATEST(:usageCount, 0),
    usage_bytes = GREATEST(:usageBytes, 0),
    source = 'SNAPSHOT',
    closed = FALSE,
    closed_at = NULL,
    updated_at = now()
""",
      nativeQuery = true)
  void upsertSnapshot(
      @Param("companyId") Long companyId,
      @Param("companyCode") String companyCode,
      @Param("dimension") String dimension,
      @Param("periodType") String periodType,
      @Param("periodStartAt") Instant periodStartAt,
      @Param("periodEndAt") Instant periodEndAt,
      @Param("tenantTimezone") String tenantTimezone,
      @Param("usageCount") long usageCount,
      @Param("usageBytes") long usageBytes);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
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
    0,
    0,
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
    source = 'COUNTER',
    closed = FALSE,
    closed_at = NULL,
    updated_at = now()
WHERE tenant_usage_rollups.company_code IS DISTINCT FROM EXCLUDED.company_code
   OR tenant_usage_rollups.period_end_at IS DISTINCT FROM EXCLUDED.period_end_at
   OR tenant_usage_rollups.tenant_timezone IS DISTINCT FROM EXCLUDED.tenant_timezone
   OR tenant_usage_rollups.source IS DISTINCT FROM 'COUNTER'
   OR tenant_usage_rollups.closed IS DISTINCT FROM FALSE
   OR tenant_usage_rollups.closed_at IS NOT NULL
""",
      nativeQuery = true)
  void ensureCounter(
      @Param("companyId") Long companyId,
      @Param("companyCode") String companyCode,
      @Param("dimension") String dimension,
      @Param("periodType") String periodType,
      @Param("periodStartAt") Instant periodStartAt,
      @Param("periodEndAt") Instant periodEndAt,
      @Param("tenantTimezone") String tenantTimezone);
}
