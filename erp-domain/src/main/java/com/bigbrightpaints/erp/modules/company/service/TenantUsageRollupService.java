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
import java.util.Set;

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
  private static final long MAX_ACTION_UNITS = Long.MAX_VALUE / 2L;
  private static final int WARNING_THRESHOLD_PERCENT = 80;
  private static final String DEFAULT_TIMEZONE = "UTC";
  private static final Set<String> COUNTED_EMAIL_CATEGORIES = Set.of("BUSINESS");
  private static final Set<String> EXEMPT_EMAIL_CATEGORIES =
      Set.of(
          "ACTIVATION",
          "PASSWORD_RESET",
          "LOCKOUT_SECURITY",
          "SECURITY",
          "SUSPENSION_WARNING",
          "BILLING_NOTICE",
          "SUPPORT_SYSTEM");

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
  public void recordApiCalls(Company company, long count) {
    if (count <= 0L) {
      return;
    }
    incrementCounter(company, UsageDimension.API_CALLS, count, 0L);
  }

  @Transactional(readOnly = true)
  public MonthlyApiUsage getCurrentMonthlyApiUsage(Company company) {
    if (company == null || company.getId() == null) {
      throw ValidationUtils.invalidInput("Company is required");
    }
    PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
    long used =
        rollupRepository
            .findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
                company.getId(),
                UsageDimension.API_CALLS.name(),
                monthly.periodType().name(),
                monthly.startAt())
            .map(rollup -> usedValue(UsageDimension.API_CALLS, rollup))
            .orElse(0L);
    return new MonthlyApiUsage(used, monthly.startAt(), monthly.endAt());
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
  public void recordStorageWrite(Company company, long bytes) {
    if (bytes <= 0L) {
      return;
    }
    incrementCounter(company, UsageDimension.STORAGE, 0L, bytes);
  }

  @Transactional
  public void recordStorageDelete(Company company, long bytes) {
    if (bytes <= 0L) {
      return;
    }
    incrementCounter(company, UsageDimension.STORAGE, 0L, -bytes);
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
        List.of(),
        history);
  }

  @Transactional
  public SuperAdminUsageDtos.TenantQuotaPolicy getTenantQuotaPolicy(
      Long companyId,
      Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> effectiveLimits) {
    Company company = requireCompany(companyId);
    closeElapsedWindows(company);
    refreshCurrentWindows(company);
    Map<UsageDimension, Long> limits = resolveLimits(company, effectiveLimits);
    PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
    List<TenantUsageRollup> rollups =
        rollupRepository.findByCompany_IdAndPeriodTypeAndPeriodStartAtOrderByDimensionAsc(
            company.getId(), monthly.periodType().name(), monthly.startAt());
    List<SuperAdminUsageDtos.QuotaDimensionPolicy> dimensions =
        UsageDimension.ordered().stream()
            .map(
                dimension -> {
                  long used = currentUsed(dimension, rollups);
                  long limit = limits.getOrDefault(dimension, 0L);
                  return quotaPolicy(dimension, company, used, limit);
                })
            .toList();
    return new SuperAdminUsageDtos.TenantQuotaPolicy(
        company.getId(),
        company.getCode(),
        timezone(company),
        toPeriod(PeriodType.MONTHLY, monthly, false, null),
        dimensions,
        emailCategoryPolicies());
  }

  @Transactional
  public SuperAdminUsageDtos.QuotaActionResult enforceQuotaAction(
      Long companyId,
      SuperAdminUsageDtos.QuotaActionRequest request,
      Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> effectiveLimits) {
    if (request == null) {
      throw ValidationUtils.invalidInput("Quota action payload is required");
    }
    Company company = requireCompany(companyId);
    UsageDimension dimension = normalizeDimension(request.dimension());
    String emailCategory =
        dimension == UsageDimension.EMAILS ? normalizeEmailCategory(request.emailCategory()) : null;
    boolean countedEmail =
        dimension != UsageDimension.EMAILS || COUNTED_EMAIL_CATEGORIES.contains(emailCategory);
    closeElapsedWindows(company);
    refreshCurrentWindows(company);
    Map<UsageDimension, Long> limits = resolveLimits(company, effectiveLimits);
    PeriodWindow monthly = periodWindow(company, PeriodType.MONTHLY);
    List<TenantUsageRollup> rollups =
        rollupRepository.findByCompany_IdAndPeriodTypeAndPeriodStartAtOrderByDimensionAsc(
            company.getId(), monthly.periodType().name(), monthly.startAt());
    long usedBefore = currentUsed(dimension, rollups);
    long requestedUnits = requestedUnits(dimension, request);
    long effectiveIncrement = countedEmail ? requestedUnits : 0L;
    long usedAfter = saturatedAdd(usedBefore, effectiveIncrement);
    long limit = limits.getOrDefault(dimension, 0L);
    SuperAdminUsageDtos.QuotaDimensionPolicy beforePolicy =
        quotaPolicy(dimension, company, usedBefore, limit);
    QuotaDecision decision =
        decide(
            dimension,
            usedAfter,
            limit,
            company.isQuotaSoftLimitEnabled(),
            company.isQuotaHardLimitEnabled());
    if (!countedEmail) {
      decision =
          new QuotaDecision(
              "ALLOWED_EXEMPT",
              true,
              "EMAIL_CATEGORY_EXEMPT",
              "Email category is exempt from tenant business-email quota");
    }
    boolean usageRecorded = false;
    if (decision.accepted() && effectiveIncrement > 0L && !request.dryRun()) {
      recordAcceptedCounter(company, dimension, requestedUnits, request.bytes());
      usageRecorded = dimension.isCounter() || dimension == UsageDimension.STORAGE;
    }
    SuperAdminUsageDtos.QuotaDimensionPolicy afterPolicy =
        quotaPolicy(dimension, company, usedAfter, limit);
    return new SuperAdminUsageDtos.QuotaActionResult(
        company.getId(),
        company.getCode(),
        dimension.name(),
        emailCategory,
        decision.decision(),
        decision.accepted(),
        usageRecorded,
        requestedUnits,
        usedBefore,
        usedAfter,
        limit,
        beforePolicy.state(),
        afterPolicy.state(),
        decision.reasonCode(),
        decision.message(),
        afterPolicy.safeReadsAllowed(),
        afterPolicy.existingResourcesPreserved(),
        dimension == UsageDimension.EMAILS,
        dimension == UsageDimension.EMAILS);
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
        countDelta,
        bytesDelta);
  }

  private void refreshCurrentWindows(Company company) {
    for (PeriodType periodType : PeriodType.values()) {
      PeriodWindow period = periodWindow(company, periodType);
      upsertSnapshot(company, UsageDimension.USERS, period, activeUserCount(company.getId()), 0L);
      upsertSnapshot(
          company, UsageDimension.STORAGE, period, 0L, currentStorageBytes(company, period));
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
    rollupRepository.upsertSnapshot(
        company.getId(),
        company.getCode(),
        dimension.name(),
        period.periodType().name(),
        period.startAt(),
        period.endAt(),
        period.timezone(),
        usageCount,
        usageBytes);
  }

  private void ensureCounter(Company company, UsageDimension dimension, PeriodWindow period) {
    rollupRepository.ensureCounter(
        company.getId(),
        company.getCode(),
        dimension.name(),
        period.periodType().name(),
        period.startAt(),
        period.endAt(),
        period.timezone());
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

  private SuperAdminUsageDtos.QuotaDimensionPolicy quotaPolicy(
      UsageDimension dimension, Company company, long used, long limit) {
    long graceAllowance = graceAllowance(dimension, limit);
    long graceStartAt = limit <= 0L || graceAllowance == 0L ? 0L : saturatedAdd(limit, 1L);
    long graceEndAt =
        limit <= 0L || graceAllowance == 0L ? 0L : saturatedAdd(limit, graceAllowance);
    long hardBlockAt = hardBlockAt(dimension, limit);
    return new SuperAdminUsageDtos.QuotaDimensionPolicy(
        dimension.name(),
        dimension.label(),
        dimension.unit(),
        used,
        limit,
        percentage(used, limit),
        policyState(dimension, used, limit),
        WARNING_THRESHOLD_PERCENT,
        graceStartAt,
        graceEndAt,
        hardBlockAt,
        true,
        true,
        company != null && company.isQuotaSoftLimitEnabled(),
        company == null || company.isQuotaHardLimitEnabled(),
        loweredLimitBehavior(dimension, used, limit));
  }

  private List<SuperAdminUsageDtos.EmailQuotaCategoryPolicy> emailCategoryPolicies() {
    List<SuperAdminUsageDtos.EmailQuotaCategoryPolicy> policies = new ArrayList<>();
    policies.add(
        new SuperAdminUsageDtos.EmailQuotaCategoryPolicy("BUSINESS", true, true, null, true, true));
    EXEMPT_EMAIL_CATEGORIES.stream()
        .sorted()
        .forEach(
            category ->
                policies.add(
                    new SuperAdminUsageDtos.EmailQuotaCategoryPolicy(
                        category,
                        false,
                        false,
                        "REQUIRED_ONBOARDING_SECURITY_OR_PLATFORM_NOTICE",
                        true,
                        true)));
    return policies;
  }

  private long currentUsed(UsageDimension dimension, List<TenantUsageRollup> rollups) {
    TenantUsageRollup rollup =
        rollups.stream()
            .filter(candidate -> dimension.name().equals(candidate.getDimension()))
            .findFirst()
            .orElse(null);
    return usedValue(dimension, rollup);
  }

  private QuotaDecision decide(
      UsageDimension dimension,
      long projectedUsed,
      long limit,
      boolean softLimit,
      boolean hardLimit) {
    if (limit <= 0L || !hardLimit) {
      return new QuotaDecision("ALLOWED", true, "QUOTA_ALLOWED", "Quota allows this action");
    }
    long hardBlockAt = hardBlockAt(dimension, limit);
    if (projectedUsed >= hardBlockAt) {
      return new QuotaDecision(
          "BLOCKED",
          false,
          "TENANT_" + dimension.name() + "_QUOTA_EXHAUSTED",
          dimension.label() + " quota exhausted");
    }
    if (projectedUsed > limit) {
      return new QuotaDecision(
          softLimit ? "GRACE" : "ALLOWED_GRACE",
          true,
          "TENANT_" + dimension.name() + "_QUOTA_GRACE",
          dimension.label() + " quota is in grace");
    }
    if (percentage(projectedUsed, limit) >= WARNING_THRESHOLD_PERCENT) {
      return new QuotaDecision(
          "WARNING",
          true,
          "TENANT_" + dimension.name() + "_QUOTA_WARNING",
          dimension.label() + " quota is near the limit");
    }
    return new QuotaDecision("ALLOWED", true, "QUOTA_ALLOWED", "Quota allows this action");
  }

  private String policyState(UsageDimension dimension, long used, long limit) {
    if (limit <= 0L) {
      return "OK";
    }
    if (used >= hardBlockAt(dimension, limit)) {
      return "BLOCKED";
    }
    if (used > limit) {
      return "GRACE";
    }
    if (percentage(used, limit) >= WARNING_THRESHOLD_PERCENT) {
      return "WARNING";
    }
    return "OK";
  }

  private long graceAllowance(UsageDimension dimension, long limit) {
    if (limit <= 0L || !dimension.hasGrace()) {
      return 0L;
    }
    return Math.max(1L, (limit / 10L) + (limit % 10L == 0L ? 0L : 1L));
  }

  private long hardBlockAt(UsageDimension dimension, long limit) {
    if (limit <= 0L) {
      return 0L;
    }
    return saturatedAdd(saturatedAdd(limit, graceAllowance(dimension, limit)), 1L);
  }

  private String loweredLimitBehavior(UsageDimension dimension, long used, long limit) {
    if (limit <= 0L || used <= limit) {
      return "NORMAL";
    }
    if (used >= hardBlockAt(dimension, limit)) {
      return "LOWERED_LIMIT_BLOCKS_NEW_WRITES_SAFE_READS_ALLOWED";
    }
    if (dimension.hasGrace()) {
      return "LOWERED_LIMIT_ENTERS_GRACE_UNTIL_GRACE_EXHAUSTED";
    }
    return "LOWERED_LIMIT_BLOCKS_NEW_WRITES_SAFE_READS_ALLOWED";
  }

  private UsageDimension normalizeDimension(String dimension) {
    if (!StringUtils.hasText(dimension)) {
      throw ValidationUtils.invalidInput("dimension is required");
    }
    try {
      return UsageDimension.valueOf(dimension.trim().toUpperCase(Locale.ROOT));
    } catch (RuntimeException ex) {
      throw ValidationUtils.invalidInput("Unsupported quota dimension: " + dimension);
    }
  }

  private String normalizeEmailCategory(String category) {
    if (!StringUtils.hasText(category)) {
      return "BUSINESS";
    }
    String normalized = category.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    if (COUNTED_EMAIL_CATEGORIES.contains(normalized)
        || EXEMPT_EMAIL_CATEGORIES.contains(normalized)) {
      return normalized;
    }
    throw ValidationUtils.invalidInput("Unsupported email quota category: " + category);
  }

  private long requestedUnits(
      UsageDimension dimension, SuperAdminUsageDtos.QuotaActionRequest request) {
    long units = request.units() == null ? 1L : request.units();
    if (dimension == UsageDimension.STORAGE && request.bytes() != null) {
      units = request.bytes();
    }
    if (units <= 0L) {
      throw ValidationUtils.invalidInput("Quota action units must be positive");
    }
    if (units > MAX_ACTION_UNITS) {
      throw ValidationUtils.invalidInput("Quota action units exceed the safe projection bound");
    }
    if (request.bytes() != null && request.bytes() > MAX_ACTION_UNITS) {
      throw ValidationUtils.invalidInput("Quota action bytes exceed the safe projection bound");
    }
    return units;
  }

  private void recordAcceptedCounter(
      Company company, UsageDimension dimension, long requestedUnits, Long requestedBytes) {
    if (!dimension.isCounter() && dimension != UsageDimension.STORAGE) {
      return;
    }
    long bytesDelta =
        dimension == UsageDimension.STORAGE
            ? Math.max(requestedBytes == null ? requestedUnits : requestedBytes, 0L)
            : 0L;
    incrementCounter(company, dimension, requestedUnits, bytesDelta);
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

  private long currentStorageBytes(Company company, PeriodWindow period) {
    if (company == null || company.getId() == null || period == null) {
      return 0L;
    }
    return rollupRepository
        .findByCompany_IdAndDimensionAndPeriodTypeAndPeriodStartAt(
            company.getId(),
            UsageDimension.STORAGE.name(),
            period.periodType().name(),
            period.startAt())
        .map(TenantUsageRollup::getUsageBytes)
        .orElse(0L);
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
    if (used > Long.MAX_VALUE / PERCENT_SCALE) {
      return PERCENT_SCALE;
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

  private long saturatedAdd(long left, long right) {
    if (right > 0L && left > Long.MAX_VALUE - right) {
      return Long.MAX_VALUE;
    }
    if (right < 0L && left < Long.MIN_VALUE - right) {
      return Long.MIN_VALUE;
    }
    return left + right;
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

    private boolean isCounter() {
      return "COUNTER".equals(accountingMode);
    }

    private boolean hasGrace() {
      return this == API_CALLS || this == PDF_EXPORTS || this == EMAILS || this == JOBS;
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

  private record QuotaDecision(
      String decision, boolean accepted, String reasonCode, String message) {}

  public record MonthlyApiUsage(long used, Instant periodStartAt, Instant periodEndAt) {}
}
