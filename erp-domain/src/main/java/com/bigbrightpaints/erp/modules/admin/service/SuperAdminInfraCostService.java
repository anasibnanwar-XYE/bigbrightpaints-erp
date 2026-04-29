package com.bigbrightpaints.erp.modules.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.admin.domain.SuperAdminInfraCostSnapshot;
import com.bigbrightpaints.erp.modules.admin.domain.SuperAdminInfraCostSnapshotCorrection;
import com.bigbrightpaints.erp.modules.admin.domain.SuperAdminInfraCostSnapshotCorrectionRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SuperAdminInfraCostSnapshotRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminInfraCostDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageRollupService;

@Service
public class SuperAdminInfraCostService {

  private static final Set<String> ALLOWED_COMPONENTS =
      Set.of("APP_SERVER", "DATABASE", "STORAGE", "EMAIL", "BACKUP", "MONITORING");
  private static final Set<String> FORBIDDEN_TEXT_MARKERS =
      Set.of(
          "password",
          "token",
          "secret",
          "bearer",
          "dsn",
          "dd_api_key",
          "sentry_auth_token",
          "invoice",
          "ledger",
          "inventory",
          "salary",
          "payroll",
          "vendor",
          "customer",
          "gst return",
          "file content",
          "request body",
          "response body");

  private final SuperAdminInfraCostSnapshotRepository snapshotRepository;
  private final SuperAdminInfraCostSnapshotCorrectionRepository correctionRepository;
  private final TenantUsageRollupService tenantUsageRollupService;
  private final AuditService auditService;

  public SuperAdminInfraCostService(
      SuperAdminInfraCostSnapshotRepository snapshotRepository,
      SuperAdminInfraCostSnapshotCorrectionRepository correctionRepository,
      TenantUsageRollupService tenantUsageRollupService,
      AuditService auditService) {
    this.snapshotRepository = snapshotRepository;
    this.correctionRepository = correctionRepository;
    this.tenantUsageRollupService = tenantUsageRollupService;
    this.auditService = auditService;
  }

  @Transactional
  public SuperAdminInfraCostDto.Dashboard dashboard(String currency) {
    String normalizedCurrency = normalizeCurrency(defaultCurrency(currency));
    List<SuperAdminInfraCostSnapshot> latest = latestComponentSnapshots(normalizedCurrency);
    long totalCost = latest.stream().mapToLong(this::amount).sum();
    SuperAdminUsageDtos.PlatformUsage platformUsage = tenantUsageRollupService.getPlatformUsage();
    List<SuperAdminInfraCostDto.UsageAggregate> aggregateUsage =
        platformUsage.totals().stream().map(this::toAggregate).toList();
    List<TenantWeight> weights = tenantWeights(platformUsage.tenants());
    long totalWeight = weights.stream().mapToLong(TenantWeight::weight).sum();
    int tenantCount = weights.size();
    List<SuperAdminInfraCostDto.TenantCostScore> tenantScores =
        weights.stream()
            .map(weight -> toScore(weight, totalWeight, tenantCount, totalCost))
            .sorted(
                Comparator.comparingLong(
                        SuperAdminInfraCostDto.TenantCostScore::costScoreBasisPoints)
                    .reversed()
                    .thenComparing(SuperAdminInfraCostDto.TenantCostScore::tenantCode))
            .toList();
    return new SuperAdminInfraCostDto.Dashboard(
        CompanyTime.now(),
        normalizedCurrency,
        totalCost,
        latest.stream().map(this::toResponse).toList(),
        aggregateUsage,
        tenantScores,
        privacy());
  }

  @Transactional(readOnly = true)
  public List<SuperAdminInfraCostDto.SnapshotResponse> listSnapshots(
      String currency, boolean includeArchived) {
    String normalizedCurrency = StringUtils.hasText(currency) ? normalizeCurrency(currency) : null;
    return snapshotRepository.findSnapshots(includeArchived, normalizedCurrency).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public SuperAdminInfraCostDto.SnapshotResponse createSnapshot(
      SuperAdminInfraCostDto.SnapshotRequest request) {
    SnapshotValues values = validateSnapshot(request);
    SuperAdminInfraCostSnapshot snapshot = new SuperAdminInfraCostSnapshot();
    apply(snapshot, values);
    snapshot.setStatus("ACTIVE");
    snapshot.setEnteredBy(currentActor());
    SuperAdminInfraCostSnapshot saved = snapshotRepository.saveAndFlush(snapshot);
    Long auditEventId =
        auditRequired(
            "infra-cost-snapshot-created",
            Map.of(
                "snapshotId",
                String.valueOf(saved.getId()),
                "component",
                saved.getComponent(),
                "amountMinorUnits",
                String.valueOf(saved.getAmountMinorUnits()),
                "currency",
                saved.getCurrency(),
                "periodStartAt",
                saved.getPeriodStartAt().toString(),
                "periodEndAt",
                saved.getPeriodEndAt().toString()));
    saved.setAuditEventId(auditEventId);
    return toResponse(snapshotRepository.saveAndFlush(saved));
  }

  @Transactional
  public SuperAdminInfraCostDto.SnapshotResponse correctSnapshot(
      Long snapshotId, SuperAdminInfraCostDto.SnapshotRequest request) {
    SuperAdminInfraCostSnapshot snapshot = requireSnapshot(snapshotId);
    if ("ARCHIVED".equals(snapshot.getStatus())) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Archived cost snapshots cannot be corrected");
    }
    SnapshotValues values = validateSnapshot(request);
    long previousAmount = amount(snapshot);
    String previousCurrency = snapshot.getCurrency();
    Long auditEventId =
        auditRequired(
            "infra-cost-snapshot-corrected",
            Map.of(
                "snapshotId",
                String.valueOf(snapshot.getId()),
                "component",
                values.component(),
                "previousAmountMinorUnits",
                String.valueOf(previousAmount),
                "newAmountMinorUnits",
                String.valueOf(values.amountMinorUnits()),
                "previousCurrency",
                previousCurrency,
                "newCurrency",
                values.currency()));
    SuperAdminInfraCostSnapshotCorrection correction = new SuperAdminInfraCostSnapshotCorrection();
    correction.setSnapshot(snapshot);
    correction.setPreviousAmountMinorUnits(previousAmount);
    correction.setNewAmountMinorUnits(values.amountMinorUnits());
    correction.setPreviousCurrency(previousCurrency);
    correction.setNewCurrency(values.currency());
    correction.setReason(values.reason());
    correction.setCorrectedBy(currentActor());
    correction.setAuditEventId(auditEventId);
    correctionRepository.save(correction);
    apply(snapshot, values);
    snapshot.setCorrectionCount(correctionCount(snapshot) + 1);
    snapshot.setAuditEventId(auditEventId);
    return toResponse(snapshotRepository.saveAndFlush(snapshot));
  }

  @Transactional(readOnly = true)
  public List<SuperAdminInfraCostDto.CorrectionResponse> corrections(Long snapshotId) {
    requireSnapshotExists(snapshotId);
    return correctionRepository.findBySnapshotIdOrderByCorrectedAtDescIdDesc(snapshotId).stream()
        .map(this::toCorrectionResponse)
        .toList();
  }

  @Transactional
  public SuperAdminInfraCostDto.SnapshotResponse archiveSnapshot(
      Long snapshotId, SuperAdminInfraCostDto.ArchiveRequest request) {
    SuperAdminInfraCostSnapshot snapshot = requireSnapshot(snapshotId);
    String reason = validateText(request == null ? null : request.reason(), "reason", 300);
    if ("ARCHIVED".equals(snapshot.getStatus())) {
      return toResponse(snapshot);
    }
    Long auditEventId =
        auditRequired(
            "infra-cost-snapshot-archived",
            Map.of(
                "snapshotId",
                String.valueOf(snapshot.getId()),
                "component",
                snapshot.getComponent(),
                "reasonText",
                reason));
    snapshot.setStatus("ARCHIVED");
    snapshot.setArchivedAt(CompanyTime.now());
    snapshot.setAuditEventId(auditEventId);
    return toResponse(snapshotRepository.saveAndFlush(snapshot));
  }

  private List<SuperAdminInfraCostSnapshot> latestComponentSnapshots(String currency) {
    Map<String, SuperAdminInfraCostSnapshot> latestByComponent = new LinkedHashMap<>();
    for (SuperAdminInfraCostSnapshot snapshot : snapshotRepository.findSnapshots(false, currency)) {
      latestByComponent.putIfAbsent(snapshot.getComponent(), snapshot);
    }
    return ALLOWED_COMPONENTS.stream()
        .map(latestByComponent::get)
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private SuperAdminInfraCostSnapshot requireSnapshot(Long snapshotId) {
    if (snapshotId == null) {
      throw invalidInput("snapshotId is required");
    }
    return snapshotRepository
        .lockById(snapshotId)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Cost snapshot not found"));
  }

  private void requireSnapshotExists(Long snapshotId) {
    if (snapshotId == null) {
      throw invalidInput("snapshotId is required");
    }
    if (!snapshotRepository.existsById(snapshotId)) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Cost snapshot not found");
    }
  }

  private SnapshotValues validateSnapshot(SuperAdminInfraCostDto.SnapshotRequest request) {
    if (request == null) {
      throw invalidInput("request is required");
    }
    String component = normalizeToken(request.component(), "component");
    if (!ALLOWED_COMPONENTS.contains(component)) {
      throw invalidInput("component must be one of " + ALLOWED_COMPONENTS);
    }
    Instant periodStartAt = requireInstant(request.periodStartAt(), "periodStartAt");
    Instant periodEndAt = requireInstant(request.periodEndAt(), "periodEndAt");
    if (!periodEndAt.isAfter(periodStartAt)) {
      throw invalidInput("periodEndAt must be after periodStartAt");
    }
    Long amount = request.amountMinorUnits();
    if (amount == null || amount < 0) {
      throw invalidInput("amountMinorUnits must be zero or greater");
    }
    return new SnapshotValues(
        component,
        periodStartAt,
        periodEndAt,
        amount,
        normalizeCurrency(request.currency()),
        validateText(request.source(), "source", 120),
        validateText(request.reason(), "reason", 300),
        optionalText(request.notes(), "notes", 300));
  }

  private void apply(SuperAdminInfraCostSnapshot snapshot, SnapshotValues values) {
    snapshot.setComponent(values.component());
    snapshot.setPeriodStartAt(values.periodStartAt());
    snapshot.setPeriodEndAt(values.periodEndAt());
    snapshot.setAmountMinorUnits(values.amountMinorUnits());
    snapshot.setCurrency(values.currency());
    snapshot.setSource(values.source());
    snapshot.setNotes(values.notes());
  }

  private List<TenantWeight> tenantWeights(List<SuperAdminUsageDtos.TenantSummary> tenants) {
    List<TenantWeight> weights = new ArrayList<>();
    for (SuperAdminUsageDtos.TenantSummary tenant : tenants) {
      long weight = 0L;
      List<SuperAdminInfraCostDto.UsageAggregate> basis = new ArrayList<>();
      for (SuperAdminUsageDtos.DimensionUsage dimension : tenant.dimensions()) {
        long units = safeUsageUnits(dimension);
        weight += units;
        basis.add(
            new SuperAdminInfraCostDto.UsageAggregate(
                dimension.dimension(), dimension.unit(), dimension.used(), 1L));
      }
      weights.add(
          new TenantWeight(
              tenant.companyId(), tenant.companyCode(), tenant.status(), weight, basis));
    }
    return weights;
  }

  private SuperAdminInfraCostDto.TenantCostScore toScore(
      TenantWeight weight, long totalWeight, int tenantCount, long totalCost) {
    long scoreBasisPoints;
    if (totalWeight <= 0 && tenantCount > 0) {
      scoreBasisPoints =
          BigDecimal.valueOf(10_000L)
              .divide(BigDecimal.valueOf(tenantCount), 0, RoundingMode.HALF_UP)
              .longValue();
    } else if (totalWeight <= 0) {
      scoreBasisPoints = 0L;
    } else {
      scoreBasisPoints =
          BigDecimal.valueOf(weight.weight())
              .multiply(BigDecimal.valueOf(10_000L))
              .divide(BigDecimal.valueOf(totalWeight), 0, RoundingMode.HALF_UP)
              .longValue();
    }
    long estimatedCost =
        BigDecimal.valueOf(totalCost)
            .multiply(BigDecimal.valueOf(scoreBasisPoints))
            .divide(BigDecimal.valueOf(10_000L), 0, RoundingMode.HALF_UP)
            .longValue();
    return new SuperAdminInfraCostDto.TenantCostScore(
        weight.tenantId(),
        weight.tenantCode(),
        weight.status(),
        weight.weight(),
        scoreBasisPoints,
        estimatedCost,
        weight.usageBasis());
  }

  private long safeUsageUnits(SuperAdminUsageDtos.DimensionUsage dimension) {
    long used = Math.max(0L, dimension.used());
    if ("STORAGE".equalsIgnoreCase(dimension.dimension())) {
      return used / (1024L * 1024L);
    }
    return used;
  }

  private SuperAdminInfraCostDto.UsageAggregate toAggregate(
      SuperAdminUsageDtos.DimensionAggregate aggregate) {
    return new SuperAdminInfraCostDto.UsageAggregate(
        aggregate.dimension(), aggregate.unit(), aggregate.used(), aggregate.tenantCount());
  }

  private SuperAdminInfraCostDto.SnapshotResponse toResponse(SuperAdminInfraCostSnapshot snapshot) {
    return new SuperAdminInfraCostDto.SnapshotResponse(
        snapshot.getId(),
        snapshot.getComponent(),
        snapshot.getPeriodStartAt(),
        snapshot.getPeriodEndAt(),
        amount(snapshot),
        snapshot.getCurrency(),
        snapshot.getSource(),
        snapshot.getStatus(),
        snapshot.getEnteredBy(),
        snapshot.getCreatedAt(),
        snapshot.getUpdatedAt(),
        snapshot.getArchivedAt(),
        correctionCount(snapshot),
        snapshot.getAuditEventId());
  }

  private SuperAdminInfraCostDto.CorrectionResponse toCorrectionResponse(
      SuperAdminInfraCostSnapshotCorrection correction) {
    return new SuperAdminInfraCostDto.CorrectionResponse(
        correction.getId(),
        correction.getSnapshot().getId(),
        correction.getPreviousAmountMinorUnits(),
        correction.getNewAmountMinorUnits(),
        correction.getPreviousCurrency(),
        correction.getNewCurrency(),
        correction.getReason(),
        correction.getCorrectedBy(),
        correction.getCorrectedAt(),
        correction.getAuditEventId());
  }

  private Long auditRequired(String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new LinkedHashMap<>(metadata);
    auditMetadata.put("actor", currentActor());
    auditMetadata.put("reason", reason);
    auditMetadata.put("target", "infra-cost");
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), null, auditMetadata);
    if (auditLog == null || auditLog.getId() == null) {
      throw invalidState("Infra cost audit event was not persisted");
    }
    return auditLog.getId();
  }

  private SuperAdminInfraCostDto.CostPrivacy privacy() {
    return new SuperAdminInfraCostDto.CostPrivacy(
        true,
        List.of(
            "infraComponent",
            "period",
            "amountMinorUnits",
            "currency",
            "aggregateUsage",
            "tenantCostScore"),
        List.of(
            "tenantPrivateBusinessRecords",
            "tenantPrivateFinancialRows",
            "tenantPrivateOperationsRows",
            "tenantPrivatePeopleData",
            "tenantPrivatePartnerData",
            "tenantPrivateDocuments",
            "tenantPrivateTaxFilings",
            "requestBodies",
            "credentials"),
        "Infra cost scoring uses platform-entered costs and durable aggregate tenant usage only.");
  }

  private String defaultCurrency(String currency) {
    return StringUtils.hasText(currency) ? currency : "INR";
  }

  private String normalizeCurrency(String value) {
    String currency = normalizeToken(value, "currency");
    if (currency.length() != 3 || !currency.chars().allMatch(Character::isLetter)) {
      throw invalidInput("currency must be a three-letter ISO code");
    }
    return currency;
  }

  private String normalizeToken(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput(fieldName + " is required");
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private Instant requireInstant(Instant value, String fieldName) {
    if (value == null) {
      throw invalidInput(fieldName + " is required");
    }
    return value;
  }

  private String optionalText(String value, String fieldName, int maxLength) {
    return StringUtils.hasText(value) ? validateText(value, fieldName, maxLength) : null;
  }

  private String validateText(String value, String fieldName, int maxLength) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput(fieldName + " is required");
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw invalidInput(fieldName + " must be at most " + maxLength + " characters");
    }
    String lower = normalized.toLowerCase(Locale.ROOT);
    if (FORBIDDEN_TEXT_MARKERS.stream().anyMatch(lower::contains)) {
      throw invalidInput(fieldName + " must not contain secrets or tenant private business data");
    }
    return normalized;
  }

  private long amount(SuperAdminInfraCostSnapshot snapshot) {
    Long value = snapshot.getAmountMinorUnits();
    if (value == null) {
      throw invalidState("Infra cost snapshot amount is missing");
    }
    return value;
  }

  private int correctionCount(SuperAdminInfraCostSnapshot snapshot) {
    Integer value = snapshot.getCorrectionCount();
    if (value == null) {
      throw invalidState("Infra cost snapshot correction count is missing");
    }
    return value;
  }

  private ApplicationException invalidInput(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, message);
  }

  private ApplicationException invalidState(String message) {
    return new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE, message);
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "system"
        : authentication.getName();
  }

  private record SnapshotValues(
      String component,
      Instant periodStartAt,
      Instant periodEndAt,
      Long amountMinorUnits,
      String currency,
      String source,
      String reason,
      String notes) {}

  private record TenantWeight(
      Long tenantId,
      String tenantCode,
      String status,
      long weight,
      List<SuperAdminInfraCostDto.UsageAggregate> usageBasis) {}
}
