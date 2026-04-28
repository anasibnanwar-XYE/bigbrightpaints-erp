package com.bigbrightpaints.erp.modules.company.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplate;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplateRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateArchiveRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateCreateRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateUpdateRequest;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SuperAdminPlanTemplateService {

  private static final String ARCHIVED = "ARCHIVED";
  private static final String ACTIVE = "ACTIVE";
  private static final String SCHEDULED = "SCHEDULED";
  private static final Set<String> CADENCES = Set.of("MONTHLY", "ANNUAL", "CUSTOM");
  private static final Set<String> SUPPORT_TIERS = Set.of("STANDARD", "PRIORITY", "DEDICATED");

  private final SuperAdminPlanTemplateRepository planTemplateRepository;
  private final CompanyRepository companyRepository;
  private final AuditService auditService;

  public SuperAdminPlanTemplateService(
      SuperAdminPlanTemplateRepository planTemplateRepository,
      CompanyRepository companyRepository,
      AuditService auditService) {
    this.planTemplateRepository = planTemplateRepository;
    this.companyRepository = companyRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<SuperAdminPlanTemplateDto> listPlans(boolean includeArchived) {
    List<SuperAdminPlanTemplate> templates =
        includeArchived
            ? planTemplateRepository.findAll().stream().sorted(planSort()).toList()
            : planTemplateRepository.findByStatusNotOrderByStableIdAscTemplateVersionDesc(ARCHIVED);
    return templates.stream().map(template -> toDto(template, null)).toList();
  }

  @Transactional(readOnly = true)
  public SuperAdminPlanTemplateDto getPlan(
      String stableId, Integer version, boolean includeArchived) {
    String normalizedStableId = normalizeStableId(stableId);
    SuperAdminPlanTemplate template =
        planTemplateRepository
            .findByStableIdIgnoreCaseOrderByTemplateVersionDesc(normalizedStableId)
            .stream()
            .filter(candidate -> version == null || version.equals(candidate.getTemplateVersion()))
            .filter(candidate -> includeArchived || !ARCHIVED.equals(candidate.getStatus()))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Plan template not found"));
    return toDto(template, null);
  }

  @Transactional
  public SuperAdminPlanTemplateDto createPlan(SuperAdminPlanTemplateCreateRequest request) {
    if (request == null) {
      throw invalidInput("Plan template payload is required");
    }
    String stableId = normalizeStableId(request.stableId());
    if (planTemplateRepository.existsByStableIdIgnoreCase(stableId)) {
      throw new ApplicationException(
              ErrorCode.BUSINESS_DUPLICATE_ENTRY, "Plan template already exists")
          .withDetail("field", "stableId");
    }
    SuperAdminPlanTemplate template = new SuperAdminPlanTemplate();
    apply(template, stableId, 1, request);
    SuperAdminPlanTemplate saved = planTemplateRepository.saveAndFlush(template);
    Long auditEventId =
        logRequired(
            "plan-template-created",
            Map.of(
                "stableId",
                saved.getStableId(),
                "newVersion",
                String.valueOf(saved.getTemplateVersion()),
                "newStatus",
                saved.getStatus(),
                "reasonDetail",
                safeReason(request.reason())));
    return toDto(saved, auditEventId);
  }

  @Transactional
  public SuperAdminPlanTemplateDto updatePlan(
      String stableId, SuperAdminPlanTemplateUpdateRequest request) {
    if (request == null) {
      throw invalidInput("Plan template payload is required");
    }
    String normalizedStableId = normalizeStableId(stableId);
    if (StringUtils.hasText(request.stableId())
        && !normalizedStableId.equals(normalizeStableId(request.stableId()))) {
      throw invalidInput("stableId must match the plan template path");
    }
    List<SuperAdminPlanTemplate> existing = lockExisting(normalizedStableId);
    SuperAdminPlanTemplate current =
        existing.stream()
            .filter(template -> !ARCHIVED.equals(template.getStatus()))
            .findFirst()
            .orElseThrow(() -> invalidState("Archived plan templates cannot be updated"));
    int oldVersion = current.getTemplateVersion();
    String oldStatus = current.getStatus();
    int nextVersion =
        existing.stream()
                .map(SuperAdminPlanTemplate::getTemplateVersion)
                .max(Integer::compareTo)
                .orElse(0)
            + 1;
    Instant now = CompanyTime.now();
    SuperAdminPlanTemplate replacement = new SuperAdminPlanTemplate();
    apply(replacement, normalizedStableId, nextVersion, request);
    if (ACTIVE.equals(replacement.getStatus())) {
      existing.stream()
          .filter(template -> !ARCHIVED.equals(template.getStatus()))
          .forEach(
              template -> {
                template.setStatus(ARCHIVED);
                template.setEffectiveUntil(now);
                template.setArchivedAt(now);
              });
    } else if (current.getEffectiveUntil() == null) {
      current.setEffectiveUntil(replacement.getEffectiveFrom());
    }
    planTemplateRepository.saveAll(existing);
    SuperAdminPlanTemplate saved = planTemplateRepository.saveAndFlush(replacement);
    Long auditEventId =
        logRequired(
            "plan-template-updated",
            Map.of(
                "stableId",
                saved.getStableId(),
                "oldVersion",
                String.valueOf(oldVersion),
                "newVersion",
                String.valueOf(saved.getTemplateVersion()),
                "oldStatus",
                oldStatus,
                "newStatus",
                saved.getStatus(),
                "assignedTenantCount",
                String.valueOf(assignedTenantCount(saved.getStableId())),
                "reasonDetail",
                safeReason(request.reason())));
    return toDto(saved, auditEventId);
  }

  @Transactional
  public SuperAdminPlanTemplateDto archivePlan(
      String stableId, SuperAdminPlanTemplateArchiveRequest request) {
    String normalizedStableId = normalizeStableId(stableId);
    List<SuperAdminPlanTemplate> existing = lockExisting(normalizedStableId);
    Instant now = CompanyTime.now();
    List<SuperAdminPlanTemplate> activeTemplates =
        existing.stream().filter(template -> !ARCHIVED.equals(template.getStatus())).toList();
    if (activeTemplates.isEmpty()) {
      throw invalidState("Plan template is already archived");
    }
    activeTemplates.forEach(
        template -> {
          template.setStatus(ARCHIVED);
          if (template.getEffectiveUntil() == null) {
            template.setEffectiveUntil(now);
          }
          template.setArchivedAt(now);
        });
    planTemplateRepository.saveAll(activeTemplates);
    SuperAdminPlanTemplate latest =
        existing.stream()
            .max(Comparator.comparing(SuperAdminPlanTemplate::getTemplateVersion))
            .orElseThrow();
    Long auditEventId =
        logRequired(
            "plan-template-archived",
            Map.of(
                "stableId",
                normalizedStableId,
                "archivedVersion",
                String.valueOf(latest.getTemplateVersion()),
                "assignedTenantCount",
                String.valueOf(assignedTenantCount(normalizedStableId)),
                "reasonDetail",
                safeReason(request == null ? null : request.reason())));
    return toDto(latest, auditEventId);
  }

  private List<SuperAdminPlanTemplate> lockExisting(String stableId) {
    List<SuperAdminPlanTemplate> existing = planTemplateRepository.lockAllByStableId(stableId);
    if (existing.isEmpty()) {
      throw new EntityNotFoundException("Plan template not found");
    }
    return existing;
  }

  private void apply(
      SuperAdminPlanTemplate template,
      String stableId,
      int version,
      SuperAdminPlanTemplateCreateRequest request) {
    populate(
        template,
        stableId,
        version,
        request.displayName(),
        request.cadence(),
        request.priceMinorUnits(),
        request.currency(),
        request.trialDurationDays(),
        request.supportTier(),
        request.featureFlags(),
        request.defaultLimits(),
        request.effectiveFrom());
  }

  private void apply(
      SuperAdminPlanTemplate template,
      String stableId,
      int version,
      SuperAdminPlanTemplateUpdateRequest request) {
    populate(
        template,
        stableId,
        version,
        request.displayName(),
        request.cadence(),
        request.priceMinorUnits(),
        request.currency(),
        request.trialDurationDays(),
        request.supportTier(),
        request.featureFlags(),
        request.defaultLimits(),
        request.effectiveFrom());
  }

  private void populate(
      SuperAdminPlanTemplate template,
      String stableId,
      int version,
      String displayName,
      String cadence,
      Long priceMinorUnits,
      String currency,
      Integer trialDurationDays,
      String supportTier,
      Map<String, Boolean> featureFlags,
      SuperAdminPlanTemplateDto.DefaultLimits limits,
      Instant effectiveFrom) {
    template.setStableId(stableId);
    template.setTemplateVersion(version);
    template.setDisplayName(requireText(displayName, "displayName"));
    template.setCadence(requireAllowed(cadence, "cadence", CADENCES));
    template.setPriceMinorUnits(requireNonNegative(priceMinorUnits, "priceMinorUnits"));
    template.setCurrency(requireCurrency(currency));
    template.setTrialDurationDays(requireNonNegative(trialDurationDays, "trialDurationDays"));
    template.setSupportTier(requireAllowed(supportTier, "supportTier", SUPPORT_TIERS));
    template.setFeatureFlags(normalizeFeatureFlags(featureFlags));
    template.setEffectiveFrom(effectiveFrom == null ? CompanyTime.now() : effectiveFrom);
    template.setStatus(template.getEffectiveFrom().isAfter(CompanyTime.now()) ? SCHEDULED : ACTIVE);
    applyLimits(template, limits);
  }

  private void applyLimits(
      SuperAdminPlanTemplate template, SuperAdminPlanTemplateDto.DefaultLimits limits) {
    if (limits == null) {
      throw invalidInput("defaultLimits is required");
    }
    template.setMaxActiveUsers(
        requireNonNegative(limits.maxActiveUsers(), "defaultLimits.maxActiveUsers"));
    template.setMaxApiRequests(
        requireNonNegative(limits.maxApiRequests(), "defaultLimits.maxApiRequests"));
    template.setMaxStorageBytes(
        requireNonNegative(limits.maxStorageBytes(), "defaultLimits.maxStorageBytes"));
    template.setMaxPdfExports(
        requireNonNegative(limits.maxPdfExports(), "defaultLimits.maxPdfExports"));
    template.setMaxEmails(requireNonNegative(limits.maxEmails(), "defaultLimits.maxEmails"));
    template.setMaxJobs(requireNonNegative(limits.maxJobs(), "defaultLimits.maxJobs"));
    template.setBurstRequestsPerMinute(
        requireNonNegative(
            limits.burstRequestsPerMinute(), "defaultLimits.burstRequestsPerMinute"));
    template.setMaxConcurrentRequests(
        requireNonNegative(limits.maxConcurrentRequests(), "defaultLimits.maxConcurrentRequests"));
  }

  private SuperAdminPlanTemplateDto toDto(SuperAdminPlanTemplate template, Long auditEventId) {
    int assignedTenantCount = assignedTenantCount(template.getStableId());
    Instant cacheInvalidatedAt =
        ACTIVE.equals(template.getStatus()) ? template.getUpdatedAt() : null;
    return new SuperAdminPlanTemplateDto(
        template.getId(),
        template.getStableId(),
        template.getDisplayName(),
        template.getStatus(),
        template.getTemplateVersion(),
        template.getEffectiveFrom(),
        template.getEffectiveUntil(),
        template.getCadence(),
        template.getPriceMinorUnits(),
        template.getCurrency(),
        template.getTrialDurationDays(),
        template.getSupportTier(),
        template.getFeatureFlags(),
        new SuperAdminPlanTemplateDto.DefaultLimits(
            template.getMaxActiveUsers(),
            template.getMaxApiRequests(),
            template.getMaxStorageBytes(),
            template.getMaxPdfExports(),
            template.getMaxEmails(),
            template.getMaxJobs(),
            template.getBurstRequestsPerMinute(),
            template.getMaxConcurrentRequests(),
            true),
        new SuperAdminPlanTemplateDto.AssignedTenants(
            assignedTenantCount,
            "LATEST_EFFECTIVE_VERSION",
            "PLAN_HISTORY_REMAINS_READABLE",
            "COUNT_ONLY_NO_PRIVATE_TENANT_ROWS"),
        new SuperAdminPlanTemplateDto.MutationPolicy(
            "NEW_VERSION_PER_UPDATE",
            "NULL_OR_PAST_EFFECTIVE_FROM_APPLIES_IMMEDIATELY_FUTURE_IS_SCHEDULED",
            "ASSIGNED_TENANTS_FOLLOW_LATEST_EFFECTIVE_VERSION",
            "INVALIDATE_ON_ACCEPTED_MUTATION_WITHOUT_RESTART",
            "SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE",
            cacheInvalidatedAt),
        auditEventId,
        template.getCreatedAt(),
        template.getUpdatedAt(),
        template.getArchivedAt());
  }

  private Long logRequired(String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new LinkedHashMap<>();
    auditMetadata.put("actor", currentActor());
    auditMetadata.put("reason", reason);
    auditMetadata.put("targetType", "PLAN_TEMPLATE");
    auditMetadata.putAll(metadata);
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), null, auditMetadata);
    return auditLog == null ? null : auditLog.getId();
  }

  private int assignedTenantCount(String stableId) {
    return companyRepository.countByCommercialPlanIdIgnoreCase(stableId);
  }

  private Comparator<SuperAdminPlanTemplate> planSort() {
    return Comparator.comparing(SuperAdminPlanTemplate::getStableId)
        .thenComparing(SuperAdminPlanTemplate::getTemplateVersion, Comparator.reverseOrder());
  }

  private Map<String, Boolean> normalizeFeatureFlags(Map<String, Boolean> featureFlags) {
    if (featureFlags == null || featureFlags.isEmpty()) {
      throw invalidInput("featureFlags must include at least one feature");
    }
    Map<String, Boolean> normalized = new TreeMap<>();
    featureFlags.forEach(
        (key, value) -> {
          String normalizedKey = normalizeToken(key, "featureFlags");
          if (value == null) {
            throw invalidInput("featureFlags." + normalizedKey + " is required");
          }
          normalized.put(normalizedKey, value);
        });
    return normalized;
  }

  private String normalizeStableId(String value) {
    String normalized = normalizeToken(value, "stableId");
    if (!normalized.matches("[A-Z0-9_-]{2,64}")) {
      throw invalidInput("stableId must use uppercase letters, numbers, underscore, or hyphen");
    }
    return normalized;
  }

  private String normalizeToken(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput(fieldName + " is required");
    }
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private String requireText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput(fieldName + " is required");
    }
    return value.trim();
  }

  private String requireAllowed(String value, String fieldName, Set<String> allowed) {
    String normalized = normalizeToken(value, fieldName);
    if (!allowed.contains(normalized)) {
      throw invalidInput(fieldName + " must be one of " + allowed);
    }
    return normalized;
  }

  private String requireCurrency(String value) {
    String normalized = normalizeToken(value, "currency");
    if (!normalized.matches("[A-Z]{3}")) {
      throw invalidInput("currency must be an ISO 4217 code");
    }
    return normalized;
  }

  private long requireNonNegative(Long value, String fieldName) {
    if (value == null || value < 0) {
      throw invalidInput(fieldName + " must be greater than or equal to 0");
    }
    return value;
  }

  private int requireNonNegative(Integer value, String fieldName) {
    if (value == null || value < 0) {
      throw invalidInput(fieldName + " must be greater than or equal to 0");
    }
    return value;
  }

  private ApplicationException invalidInput(String message) {
    return com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(message);
  }

  private ApplicationException invalidState(String message) {
    return com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(message);
  }

  private String safeReason(String reason) {
    return StringUtils.hasText(reason) ? reason.trim() : "not-provided";
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return "anonymous";
    }
    return authentication.getName().trim();
  }
}
