package com.bigbrightpaints.erp.modules.company.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantUsageRollup;
import com.bigbrightpaints.erp.modules.company.domain.TenantUsageRollupRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;

@Service
public class TenantUsageRollupService {

  private static final long PERCENT_SCALE = 100L;
  private static final String DEFAULT_TIMEZONE = "UTC";

  private final TenantUsageRollupRepository rollupRepository;
  private final CompanyRepository companyRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogRepository auditLogRepository;
  private final Clock clock;

  @Autowired
  public TenantUsageRollupService(
      TenantUsageRollupRepository rollupRepository,
      CompanyRepository companyRepository,
      UserAccountRepository userAccountRepository,
      AuditLogRepository auditLogRepository) {
    this(
        rollupRepository,
        companyRepository,
        userAccountRepository,
        auditLogRepository,
        Clock.systemUTC());
  }

  TenantUsageRollupService(
      TenantUsageRollupRepository rollupRepository,
      CompanyRepository companyRepository,
      UserAccountRepository userAccountRepository,
      AuditLogRepository auditLogRepository,
      Clock clock) {
    this.rollupRepository = rollupRepository;
    this.companyRepository = companyRepository;
    this.userAccountRepository = userAccountRepository;
    this.auditLogRepository = auditLogRepository;
    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  @Transactional
  public void recordApiCall(Company company) {
    incrementCounter(company, UsageDimension.API_CALLS, 1L, 0L);
  }

  @Transactional
  public void recordPdfExport(Company company) {
    incrementCounter(company, UsageDimension.PDF_EXPORTS, 1L, 0L);
  }

  @Transactional
  public void recordEmailSend(Company company) {
    incrementCounter(company, UsageDimension.EMAILS, 1L, 0L);
  }

  @Transactional
  public void recordJobSubmission(Company company) {
    incrementCounter(company, UsageDimension.JOBS, 1L, 0L);
  }

  @Transactional
  public SuperAdminUsageDtos.TenantUsage getTenantUsage(
      Long companyId,
      Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> effectiveLimits) {
    Company company = requireCompany(companyId);
    closeElapsedWindows(company);
    refreshCurrentWindows(company);
    Map<UsageDimension, Long> limits = resolveLimits(company, effectiveLimits);
    PeriodWindow daily = periodWindow(company, PeriodType.DAILY);
    PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
    List<SuperAdminUsageDtos.DimensionUsage> dimensions =
        currentDimensionUsages(company, monthly, limits);
    List<SuperAdminUsageDtos.RollupWindow> history =
        rollupRepository
            .findTop100ByCompany_IdAndPeriodTypeAndClosedTrueOrderByPeriodStartAtDescDimensionAsc(
                company.getId(), PeriodType.DAILY.name())
            .stream()
            .map(rollup -> toHistoryWindow(rollup, limits))
            .toList();
    return new SuperAdminUsageDtos.TenantUsage(
        company.getId(),
        company.getCode(),
        company.getName(),
        timezone(company),
        toPeriod(PeriodType.DAILY, daily, false, null),
        toPeriod(PeriodType.MONTHLY, monthly, false, null),
        dimensions,
        history);
  }

  @Transactional
  public SuperAdminUsageDtos.TenantUsageHistory getTenantUsageHistory(
      Long companyId, String periodType) {
    Company company = requireCompany(companyId);
    closeElapsedWindows(company);
    PeriodType resolvedPeriodType = normalizePeriodType(periodType);
    Map<UsageDimension, Long> limits = resolveLimits(company, null);
    List<SuperAdminUsageDtos.RollupWindow> windows =
        rollupRepository
            .findTop100ByCompany_IdAndPeriodTypeAndClosedTrueOrderByPeriodStartAtDescDimensionAsc(
                company.getId(), resolvedPeriodType.name())
            .stream()
            .map(rollup -> toHistoryWindow(rollup, limits))
            .toList();
    return new SuperAdminUsageDtos.TenantUsageHistory(
        company.getId(), company.getCode(), timezone(company), resolvedPeriodType.name(), windows);
  }

  @Transactional
  public SuperAdminUsageDtos.PlatformUsage getPlatformUsage() {
    List<Company> companies =
        companyRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Company::getCode, String.CASE_INSENSITIVE_ORDER))
            .toList();
    List<SuperAdminUsageDtos.TenantSummary> tenantSummaries = new ArrayList<>();
    EnumMap<UsageDimension, Long> aggregateUsed = zeroDimensionMap();
    EnumMap<UsageDimension, Long> aggregateLimits = zeroDimensionMap();
    EnumMap<UsageDimension, Long> aggregateTenants = zeroDimensionMap();
    for (Company company : companies) {
      closeElapsedWindows(company);
      refreshCurrentWindows(company);
      Map<UsageDimension, Long> limits = resolveLimits(company, null);
      PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
      List<SuperAdminUsageDtos.DimensionUsage> dimensions =
          currentDimensionUsages(company, monthly, limits);
      dimensions.forEach(
          dimension -> {
            UsageDimension usageDimension = UsageDimension.valueOf(dimension.dimension());
            aggregateUsed.merge(usageDimension, dimension.used(), Long::sum);
            aggregateLimits.merge(usageDimension, dimension.limit(), Long::sum);
            aggregateTenants.merge(usageDimension, 1L, Long::sum);
          });
      tenantSummaries.add(
          new SuperAdminUsageDtos.TenantSummary(
              company.getId(),
              company.getCode(),
              company.getName(),
              company.getLifecycleState() == null ? "ACTIVE" : company.getLifecycleState().name(),
              dimensions));
    }
    PeriodWindow platformMonthly = platformMonthlyPeriod();
    List<SuperAdminUsageDtos.DimensionAggregate> totals =
        UsageDimension.ordered().stream()
            .map(
                dimension ->
                    new SuperAdminUsageDtos.DimensionAggregate(
                        dimension.name(),
                        dimension.label(),
                        dimension.accountingMode(),
                        dimension.unit(),
                        aggregateUsed.get(dimension),
                        aggregateLimits.get(dimension),
                        aggregateTenants.get(dimension)))
            .toList();
    return new SuperAdminUsageDtos.PlatformUsage(
        Instant.now(clock),
        toPeriod(PeriodType.MONTHLY, platformMonthly, false, null),
        totals,
        tenantSummaries);
  }

  private void incrementCounter(
      Company company, UsageDimension dimension, long countDelta, long bytesDelta) {
    if (company == null || company.getId() == null || !StringUtils.hasText(company.getCode())) {
      return;
    }
    PeriodWindow daily = periodWindow(company, PeriodType.DAILY);
    PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
    increment(company, dimension, daily, countDelta, bytesDelta);
    increment(company, dimension, monthly, countDelta, bytesDelta);
  }

  private void increment(
      Company company,
      UsageDimension dimension,
      PeriodWindow period,
      long countDelta,
      long bytesDelta) {
    rollupRepository.incrementCounter(
        company.getId(),
        company.getCode(),
        dimension.name(),
        period.periodType().name(),
        period.startAt(),
        period.endAt(),
        period.timezone(),
        Math.max(countDelta, 0L),
        Math.max(bytesDelta, 0L));
  }

  private void refreshCurrentWindows(Company company) {
    for (PeriodType periodType : PeriodType.values()) {
      PeriodWindow period = periodWindow(company, periodType);
      upsertSnapshot(company, UsageDimension.USERS, period, activeUserCount(company.getId()), 0L);
      upsertSnapshot(
          company, UsageDimension.STORAGE, period, 0L, auditStorageBytes(company.getId()));
      for (UsageDimension dimension : UsageDimension.counterDimensions()) {
        ensureCounter(company, dimension, period);
      }
    }
  }

  private void upsertSnapshot(
      Company company,
      UsageDimension dimension,
      PeriodWindow period,
      long usageCount,
      long usageBytes) {
    Optional<TenantUsageRollup> existing =
        rollupRepository.findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
            company.getId(), dimension.name(), period.periodType().name(), period.startAt());
    TenantUsageRollup rollup =
        existing.orElseGet(
            () ->
                TenantUsageRollup.snapshot(
                    company,
                    dimension.name(),
                    period.periodType().name(),
                    period.startAt(),
                    period.endAt(),
                    period.timezone(),
                    usageCount,
                    usageBytes));
    if (existing.isPresent()) {
      rollup.updateSnapshot(usageCount, usageBytes, period.endAt(), period.timezone());
    }
    rollupRepository.save(rollup);
  }

  private void ensureCounter(Company company, UsageDimension dimension, PeriodWindow period) {
    Optional<TenantUsageRollup> existing =
        rollupRepository.findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
            company.getId(), dimension.name(), period.periodType().name(), period.startAt());
    if (existing.isEmpty()) {
      rollupRepository.save(
          TenantUsageRollup.counter(
              company,
              dimension.name(),
              period.periodType().name(),
              period.startAt(),
              period.endAt(),
              period.timezone()));
    }
  }

  private void closeElapsedWindows(Company company) {
    if (company == null || company.getId() == null) {
      return;
    }
    Instant now = Instant.now(clock);
    List<TenantUsageRollup> elapsed =
        rollupRepository
            .findByCompany_IdAndClosedFalseAndPeriodEndAtLessThanEqualOrderByPeriodEndAtAsc(
                company.getId(), now);
    elapsed.forEach(rollup -> rollup.close(now));
    if (!elapsed.isEmpty()) {
      rollupRepository.saveAll(elapsed);
    }
  }

  private List<SuperAdminUsageDtos.DimensionUsage> currentDimensionUsages(
      Company company, PeriodWindow period, Map<UsageDimension, Long> limits) {
    List<TenantUsageRollup> rollups =
        rollupRepository.findByCompany_IdAndPeriodTypeAndPeriodStartAtOrderByDimensionAsc(
            company.getId(), period.periodType().name(), period.startAt());
    return UsageDimension.ordered().stream()
        .map(
            dimension -> {
              TenantUsageRollup rollup =
                  rollups.stream()
                      .filter(candidate -> dimension.name().equals(candidate.getDimension()))
                      .findFirst()
                      .orElse(null);
              long used = usedValue(dimension, rollup);
              long limit = limits.getOrDefault(dimension, 0L);
              return new SuperAdminUsageDtos.DimensionUsage(
                  dimension.name(),
                  dimension.label(),
                  dimension.accountingMode(),
                  dimension.unit(),
                  used,
                  limit,
                  percentage(used, limit),
                  state(used, limit),
                  toPeriod(period.periodType(), period, false, null));
            })
        .toList();
  }

  private SuperAdminUsageDtos.RollupWindow toHistoryWindow(
      TenantUsageRollup rollup, Map<UsageDimension, Long> limits) {
    UsageDimension dimension = UsageDimension.valueOf(rollup.getDimension());
    PeriodType periodType = PeriodType.valueOf(rollup.getPeriodType());
    PeriodWindow period =
        new PeriodWindow(
            periodType,
            rollup.getPeriodStartAt(),
            rollup.getPeriodEndAt(),
            rollup.getTenantTimezone(),
            periodId(periodType, rollup.getPeriodStartAt(), rollup.getTenantTimezone()));
    return new SuperAdminUsageDtos.RollupWindow(
        dimension.name(),
        dimension.label(),
        dimension.accountingMode(),
        dimension.unit(),
        usedValue(dimension, rollup),
        limits.getOrDefault(dimension, 0L),
        toPeriod(periodType, period, rollup.isClosed(), rollup.getClosedAt()));
  }

  private SuperAdminUsageDtos.Period toPeriod(
      PeriodType periodType, PeriodWindow period, boolean closed, Instant closedAt) {
    return new SuperAdminUsageDtos.Period(
        periodType.name(),
        period.periodId(),
        period.startAt(),
        period.endAt(),
        period.timezone(),
        closed,
        closedAt);
  }

  private PeriodWindow periodWindow(Company company, PeriodType periodType) {
    String timezone = timezone(company);
    ZoneId zoneId = ZoneId.of(timezone);
    Instant now = Instant.now(clock);
    if (periodType == PeriodType.DAILY) {
      LocalDate day = LocalDate.ofInstant(now, zoneId);
      Instant start = day.atStartOfDay(zoneId).toInstant();
      Instant end = day.plusDays(1).atStartOfDay(zoneId).toInstant();
      return new PeriodWindow(periodType, start, end, timezone, day.toString());
    }
    YearMonth month = YearMonth.from(now.atZone(zoneId));
    Instant start = month.atDay(1).atStartOfDay(zoneId).toInstant();
    Instant end = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant();
    return new PeriodWindow(periodType, start, end, timezone, month.toString());
  }

  private PeriodWindow platformMonthlyPeriod() {
    YearMonth month = YearMonth.from(Instant.now(clock).atZone(ZoneId.of(DEFAULT_TIMEZONE)));
    Instant start = month.atDay(1).atStartOfDay(ZoneId.of(DEFAULT_TIMEZONE)).toInstant();
    Instant end =
        month.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of(DEFAULT_TIMEZONE)).toInstant();
    return new PeriodWindow(PeriodType.MONTHLY, start, end, DEFAULT_TIMEZONE, month.toString());
  }

  private String periodId(PeriodType periodType, Instant startAt, String timezone) {
    ZoneId zoneId = ZoneId.of(StringUtils.hasText(timezone) ? timezone : DEFAULT_TIMEZONE);
    if (periodType == PeriodType.DAILY) {
      return DateTimeFormatter.ISO_LOCAL_DATE.format(startAt.atZone(zoneId).toLocalDate());
    }
    return YearMonth.from(startAt.atZone(zoneId)).toString();
  }

  private Map<UsageDimension, Long> resolveLimits(
      Company company,
      Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> effectiveLimits) {
    EnumMap<UsageDimension, Long> limits = zeroDimensionMap();
    limits.put(UsageDimension.USERS, nonNegative(company.getQuotaMaxActiveUsers()));
    limits.put(UsageDimension.API_CALLS, nonNegative(company.getQuotaMaxApiRequests()));
    limits.put(UsageDimension.STORAGE, nonNegative(company.getQuotaMaxStorageBytes()));
    if (effectiveLimits != null) {
      putLimit(limits, UsageDimension.USERS, effectiveLimits.get("maxActiveUsers"));
      putLimit(limits, UsageDimension.API_CALLS, effectiveLimits.get("maxApiRequests"));
      putLimit(limits, UsageDimension.STORAGE, effectiveLimits.get("maxStorageBytes"));
      putLimit(limits, UsageDimension.PDF_EXPORTS, effectiveLimits.get("maxPdfExports"));
      putLimit(limits, UsageDimension.EMAILS, effectiveLimits.get("maxEmails"));
      putLimit(limits, UsageDimension.JOBS, effectiveLimits.get("maxJobs"));
    }
    return limits;
  }

  private void putLimit(
      EnumMap<UsageDimension, Long> limits,
      UsageDimension dimension,
      SuperAdminTenantEntitlementsDto.LimitEntitlement entitlement) {
    if (entitlement != null) {
      limits.put(dimension, nonNegative(entitlement.effectiveValue()));
    }
  }

  private EnumMap<UsageDimension, Long> zeroDimensionMap() {
    EnumMap<UsageDimension, Long> values = new EnumMap<>(UsageDimension.class);
    UsageDimension.ordered().forEach(dimension -> values.put(dimension, 0L));
    return values;
  }

  private Company requireCompany(Long companyId) {
    return companyRepository
        .findById(companyId)
        .orElseThrow(() -> ValidationUtils.invalidInput("Company not found"));
  }

  private PeriodType normalizePeriodType(String periodType) {
    if (!StringUtils.hasText(periodType)) {
      return PeriodType.DAILY;
    }
    try {
      return PeriodType.valueOf(periodType.trim().toUpperCase(Locale.ROOT));
    } catch (RuntimeException ex) {
      throw ValidationUtils.invalidInput("Unsupported usage periodType: " + periodType);
    }
  }

  private long activeUserCount(Long companyId) {
    return userAccountRepository == null
        ? 0L
        : userAccountRepository.countByCompany_IdAndEnabledTrue(companyId);
  }

  private long auditStorageBytes(Long companyId) {
    return auditLogRepository == null
        ? 0L
        : auditLogRepository.estimateAuditStorageBytesByCompanyId(companyId);
  }

  private long usedValue(UsageDimension dimension, TenantUsageRollup rollup) {
    if (rollup == null) {
      return 0L;
    }
    return "BYTES".equals(dimension.unit()) ? rollup.getUsageBytes() : rollup.getUsageCount();
  }

  private long percentage(long used, long limit) {
    if (used <= 0L || limit <= 0L) {
      return 0L;
    }
    return Math.min(PERCENT_SCALE, (used * PERCENT_SCALE) / limit);
  }

  private String state(long used, long limit) {
    if (limit <= 0L) {
      return "OK";
    }
    long percentage = percentage(used, limit);
    if (percentage >= 100L) {
      return "BLOCKED";
    }
    if (percentage >= 80L) {
      return "WARNING";
    }
    return "OK";
  }

  private long nonNegative(long value) {
    return Math.max(value, 0L);
  }

  private String timezone(Company company) {
    String timezone = company == null ? null : company.getTimezone();
    return StringUtils.hasText(timezone) ? timezone.trim() : DEFAULT_TIMEZONE;
  }

  private enum PeriodType {
    DAILY,
    MONTHLY
  }

  private enum UsageDimension {
    USERS("Users", "SNAPSHOT", "COUNT"),
    STORAGE("Storage", "SNAPSHOT", "BYTES"),
    API_CALLS("API calls", "COUNTER", "COUNT"),
    PDF_EXPORTS("PDF exports", "COUNTER", "COUNT"),
    EMAILS("Emails", "COUNTER", "COUNT"),
    JOBS("Jobs", "COUNTER", "COUNT");

    private final String label;
    private final String accountingMode;
    private final String unit;

    UsageDimension(String label, String accountingMode, String unit) {
      this.label = label;
      this.accountingMode = accountingMode;
      this.unit = unit;
    }

    private String label() {
      return label;
    }

    private String accountingMode() {
      return accountingMode;
    }

    private String unit() {
      return unit;
    }

    private static List<UsageDimension> ordered() {
      return List.of(USERS, STORAGE, API_CALLS, PDF_EXPORTS, EMAILS, JOBS);
    }

    private static List<UsageDimension> counterDimensions() {
      return List.of(API_CALLS, PDF_EXPORTS, EMAILS, JOBS);
    }
  }

  private record PeriodWindow(
      PeriodType periodType, Instant startAt, Instant endAt, String timezone, String periodId) {}
}
