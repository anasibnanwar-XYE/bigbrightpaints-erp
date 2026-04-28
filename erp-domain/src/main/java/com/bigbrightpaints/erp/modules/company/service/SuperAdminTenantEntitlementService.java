package com.bigbrightpaints.erp.modules.company.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSetting;
import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyModule;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.EntitlementFeature;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplate;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplateRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementOverrideRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantPlanAssignmentRequest;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SuperAdminTenantEntitlementService {

  private static final String ARCHIVED = "ARCHIVED";
  private static final String CUSTOM = "CUSTOM";
  private static final String POLICY_SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE =
      "SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE";
  private static final String SOURCE_PLAN_DEFAULT = "PLAN_DEFAULT";
  private static final String SOURCE_TENANT_OVERRIDE = "TENANT_OVERRIDE";
  private static final List<String> LIMIT_KEYS =
      List.of(
          "maxActiveUsers",
          "maxApiRequests",
          "maxStorageBytes",
          "maxPdfExports",
          "maxEmails",
          "maxJobs",
          "burstRequestsPerMinute",
          "maxConcurrentRequests");

  private final CompanyRepository companyRepository;
  private final SuperAdminPlanTemplateRepository planTemplateRepository;
  private final SystemSettingsRepository systemSettingsRepository;
  private final TenantRuntimeEnforcementService tenantRuntimeEnforcementService;
  private final AuditService auditService;

  public SuperAdminTenantEntitlementService(
      CompanyRepository companyRepository,
      SuperAdminPlanTemplateRepository planTemplateRepository,
      SystemSettingsRepository systemSettingsRepository,
      TenantRuntimeEnforcementService tenantRuntimeEnforcementService,
      AuditService auditService) {
    this.companyRepository = companyRepository;
    this.planTemplateRepository = planTemplateRepository;
    this.systemSettingsRepository = systemSettingsRepository;
    this.tenantRuntimeEnforcementService = tenantRuntimeEnforcementService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public SuperAdminTenantEntitlementsDto getEffectiveEntitlements(Long companyId) {
    Company company = requireCompany(companyId);
    return buildEntitlements(company, null, false, null);
  }

  @Transactional
  public SuperAdminTenantEntitlementsDto assignPlan(
      Long companyId, SuperAdminTenantPlanAssignmentRequest request) {
    if (request == null) {
      throw invalidInput("Tenant plan assignment payload is required");
    }
    Company company = requireCompany(companyId);
    String planId =
        request.customPlan() != null ? CUSTOM : normalizeToken(request.planId(), "planId");
    PlanView plan = resolvePlanForAssignment(planId, request.customPlan());
    String oldPlanId = company.getCommercialPlanId();
    String oldSupportTier = company.getCommercialSupportTier();
    Map<String, String> oldBillingSnapshot = billingSnapshot(company.getId());
    if (request.customPlan() != null) {
      persistCustomPlan(company.getId(), plan);
    } else {
      clearCustomPlan(company.getId());
    }
    company.setCommercialPlanId(plan.planId());
    company.setCommercialSupportTier(plan.supportTier());
    company.setCommercialTrialEndsAt(resolveTrialEndsAt(plan.trialDurationDays()));
    applyEffectiveEntitlementsToTenant(company, plan);
    companyRepository.saveAndFlush(company);
    boolean repriceApplied =
        refreshBillingSnapshotIfNeeded(
            company.getId(),
            plan,
            oldBillingSnapshot,
            Boolean.TRUE.equals(request.repriceSubscription()));
    Long auditEventId =
        audit(
            company,
            "tenant-plan-assigned",
            Map.of(
                "oldPlanId",
                nullSafe(oldPlanId),
                "newPlanId",
                plan.planId(),
                "oldSupportTier",
                nullSafe(oldSupportTier),
                "newSupportTier",
                plan.supportTier(),
                "customPlan",
                Boolean.toString(plan.custom()),
                "repriceApplied",
                Boolean.toString(repriceApplied),
                "reasonDetail",
                safeReason(request.reason())));
    return buildEntitlements(company, auditEventId, repriceApplied, plan);
  }

  @Transactional
  public SuperAdminTenantEntitlementsDto putOverrides(
      Long companyId, SuperAdminTenantEntitlementOverrideRequest request) {
    if (request == null
        || ((request.limits() == null || request.limits().isEmpty())
            && (request.features() == null || request.features().isEmpty()))) {
      throw invalidInput("Entitlement override payload is required");
    }
    Company company = requireCompany(companyId);
    Instant now = CompanyTime.now(company);
    if (request.limits() != null) {
      request.limits().forEach((key, value) -> putLimitOverride(company.getId(), key, value, now));
    }
    if (request.features() != null) {
      request
          .features()
          .forEach((key, value) -> putFeatureOverride(company.getId(), key, value, now));
    }
    PlanView plan = resolvePlan(company);
    applyEffectiveEntitlementsToTenant(company, plan);
    companyRepository.saveAndFlush(company);
    Long auditEventId =
        audit(
            company,
            "tenant-entitlement-overrides-upserted",
            Map.of(
                "limitOverrideCount",
                String.valueOf(request.limits() == null ? 0 : request.limits().size()),
                "featureOverrideCount",
                String.valueOf(request.features() == null ? 0 : request.features().size()),
                "reasonDetail",
                safeReason(request.reason())));
    return buildEntitlements(company, auditEventId, false, plan);
  }

  @Transactional
  public SuperAdminTenantEntitlementsDto removeOverride(Long companyId, String key, String reason) {
    Company company = requireCompany(companyId);
    String normalizedKey = normalizeEntitlementKey(key);
    boolean removed = false;
    if (LIMIT_KEYS.contains(normalizedKey)) {
      removed |= deleteSetting(limitOverrideKey(company.getId(), normalizedKey));
      removed |= deleteSetting(limitOverrideUpdatedKey(company.getId(), normalizedKey));
    } else {
      String featureKey = normalizeFeatureKey(normalizedKey);
      EntitlementFeature feature = EntitlementFeature.require(featureKey, this::invalidInput);
      if (!feature.mutable()) {
        throw invalidInput("Feature override " + feature.key() + " is not mutable");
      }
      removed |= deleteSetting(featureOverrideKey(company.getId(), featureKey));
      removed |= deleteSetting(featureOverrideUpdatedKey(company.getId(), featureKey));
    }
    if (!removed) {
      throw invalidInput("No entitlement override exists for " + normalizedKey);
    }
    PlanView plan = resolvePlan(company);
    applyEffectiveEntitlementsToTenant(company, plan);
    companyRepository.saveAndFlush(company);
    Long auditEventId =
        audit(
            company,
            "tenant-entitlement-override-removed",
            Map.of("overrideKey", normalizedKey, "reasonDetail", safeReason(reason)));
    return buildEntitlements(company, auditEventId, false, plan);
  }

  @Transactional(readOnly = true)
  public SuperAdminTenantEntitlementsDto.PlanSummary planSummaryFor(Company company) {
    if (company == null) {
      return new SuperAdminTenantEntitlementsDto.PlanSummary(
          "TRIAL", "Trial", 1, false, "STANDARD", null);
    }
    PlanView plan = resolvePlan(company);
    return new SuperAdminTenantEntitlementsDto.PlanSummary(
        plan.planId(),
        plan.displayName(),
        plan.version(),
        plan.custom(),
        plan.supportTier(),
        plan.effectiveFrom());
  }

  @Transactional(readOnly = true)
  public boolean isFeatureEnabled(Company company, CompanyModule module) {
    if (module == null || module.isCore()) {
      return true;
    }
    if (company == null) {
      return CompanyModule.defaultEnabledGatableModuleNames().contains(module.name());
    }
    SuperAdminTenantEntitlementsDto entitlements = buildEntitlements(company, null, false, null);
    SuperAdminTenantEntitlementsDto.FeatureEntitlement feature =
        entitlements.features().get(EntitlementFeature.keyForModule(module));
    return feature != null
        ? feature.effectiveValue()
        : company.getEnabledModules().contains(module.name());
  }

  private SuperAdminTenantEntitlementsDto buildEntitlements(
      Company company, Long auditEventId, boolean repriceApplied, PlanView requestedPlan) {
    PlanView plan = requestedPlan == null ? resolvePlan(company) : requestedPlan;
    Instant now = CompanyTime.now(company);
    Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> limits = new LinkedHashMap<>();
    for (String key : LIMIT_KEYS) {
      long defaultValue = plan.limits().getOrDefault(key, 0L);
      Long override = readLong(limitOverrideKey(company.getId(), key)).orElse(null);
      Instant updatedAt =
          override == null
              ? plan.updatedAt()
              : readInstant(limitOverrideUpdatedKey(company.getId(), key)).orElse(now);
      limits.put(
          key,
          new SuperAdminTenantEntitlementsDto.LimitEntitlement(
              key,
              defaultValue,
              override,
              override == null ? defaultValue : override,
              override == null ? SOURCE_PLAN_DEFAULT : SOURCE_TENANT_OVERRIDE,
              updatedAt));
    }
    Map<String, SuperAdminTenantEntitlementsDto.FeatureEntitlement> features =
        effectiveFeatureMap(company, plan, now);
    boolean cacheInvalidated = auditEventId != null || requestedPlan != null;
    return new SuperAdminTenantEntitlementsDto(
        company.getId(),
        company.getCode(),
        new SuperAdminTenantEntitlementsDto.PlanSummary(
            plan.planId(),
            plan.displayName(),
            plan.version(),
            plan.custom(),
            plan.supportTier(),
            plan.effectiveFrom()),
        limits,
        features,
        new SuperAdminTenantEntitlementsDto.CacheMetadata(
            cacheInvalidated,
            cacheInvalidated ? now : null,
            true,
            "TENANT_ENTITLEMENTS_" + company.getId()),
        billingDto(company.getId(), plan, repriceApplied),
        auditEventId);
  }

  private void applyEffectiveEntitlementsToTenant(Company company, PlanView plan) {
    SuperAdminTenantEntitlementsDto entitlements = buildEntitlements(company, null, false, plan);
    company.setQuotaMaxActiveUsers(entitlements.limits().get("maxActiveUsers").effectiveValue());
    company.setQuotaMaxApiRequests(entitlements.limits().get("maxApiRequests").effectiveValue());
    company.setQuotaMaxStorageBytes(entitlements.limits().get("maxStorageBytes").effectiveValue());
    company.setQuotaMaxConcurrentRequests(
        entitlements.limits().get("maxConcurrentRequests").effectiveValue());
    company.setEnabledModules(enabledGatableModules(entitlements.features()));
    tenantRuntimeEnforcementService.updatePolicy(
        company.getCode(),
        null,
        "TENANT_ENTITLEMENTS_UPDATE",
        safeInteger(entitlements.limits().get("maxConcurrentRequests").effectiveValue()),
        safeInteger(entitlements.limits().get("burstRequestsPerMinute").effectiveValue()),
        safeInteger(entitlements.limits().get("maxActiveUsers").effectiveValue()),
        currentActor());
    tenantRuntimeEnforcementService.invalidatePolicyCache(company.getCode());
  }

  private Set<String> enabledGatableModules(
      Map<String, SuperAdminTenantEntitlementsDto.FeatureEntitlement> features) {
    LinkedHashSet<String> modules = new LinkedHashSet<>();
    for (CompanyModule module : CompanyModule.values()) {
      if (!module.isGatable()) {
        continue;
      }
      SuperAdminTenantEntitlementsDto.FeatureEntitlement feature =
          features.get(EntitlementFeature.keyForModule(module));
      if (feature != null && feature.effectiveValue()) {
        modules.add(module.name());
      }
    }
    return modules;
  }

  private Map<String, SuperAdminTenantEntitlementsDto.FeatureEntitlement> effectiveFeatureMap(
      Company company, PlanView plan, Instant now) {
    Map<String, SuperAdminTenantEntitlementsDto.FeatureEntitlement> result = new LinkedHashMap<>();
    for (EntitlementFeature feature : EntitlementFeature.values()) {
      String featureKey = feature.key();
      boolean planDefault =
          feature.alwaysOn()
              || (feature == EntitlementFeature.CUSTOM_PLAN && plan.custom())
              || Boolean.TRUE.equals(plan.features().get(featureKey));
      Boolean override =
          feature.mutable()
              ? readBoolean(featureOverrideKey(company.getId(), featureKey)).orElse(null)
              : null;
      Instant updatedAt =
          override == null
              ? plan.updatedAt()
              : readInstant(featureOverrideUpdatedKey(company.getId(), featureKey)).orElse(now);
      result.put(
          featureKey,
          new SuperAdminTenantEntitlementsDto.FeatureEntitlement(
              featureKey,
              planDefault,
              override,
              override == null ? planDefault : override,
              override == null ? SOURCE_PLAN_DEFAULT : SOURCE_TENANT_OVERRIDE,
              updatedAt));
    }
    return result;
  }

  private PlanView resolvePlanForAssignment(
      String planId, SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan) {
    if (customPlan != null) {
      return customPlanView(customPlan);
    }
    return templatePlanView(planId);
  }

  private PlanView resolvePlan(Company company) {
    String planId =
        StringUtils.hasText(company.getCommercialPlanId())
            ? company.getCommercialPlanId().trim().toUpperCase(Locale.ROOT)
            : "TRIAL";
    if (CUSTOM.equals(planId) && hasSetting(customPlanKey(company.getId(), "displayName"))) {
      return storedCustomPlanView(company.getId());
    }
    return templatePlanView(planId);
  }

  private PlanView templatePlanView(String stableId) {
    String normalizedStableId = normalizeToken(stableId, "planId");
    Instant now = CompanyTime.now();
    SuperAdminPlanTemplate template =
        planTemplateRepository
            .findByStableIdIgnoreCaseOrderByTemplateVersionDesc(normalizedStableId)
            .stream()
            .filter(candidate -> !ARCHIVED.equals(candidate.getStatus()))
            .filter(candidate -> !candidate.getEffectiveFrom().isAfter(now))
            .findFirst()
            .orElseGet(
                () ->
                    planTemplateRepository
                        .findTopByStableIdIgnoreCaseAndStatusNotOrderByTemplateVersionDesc(
                            normalizedStableId, ARCHIVED)
                        .orElseThrow(() -> new EntityNotFoundException("Plan template not found")));
    return new PlanView(
        template.getStableId(),
        template.getDisplayName(),
        template.getTemplateVersion(),
        false,
        template.getCadence(),
        template.getPriceMinorUnits(),
        template.getCurrency(),
        template.getTrialDurationDays(),
        template.getSupportTier(),
        normalizeFeatureFlags(template.getFeatureFlags()),
        limitsFromTemplate(template),
        template.getEffectiveFrom(),
        template.getUpdatedAt());
  }

  private PlanView customPlanView(SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan) {
    if (customPlan.defaultLimits() == null) {
      throw invalidInput("customPlan.defaultLimits is required");
    }
    return new PlanView(
        CUSTOM,
        requireText(customPlan.displayName(), "customPlan.displayName"),
        1,
        true,
        requireAllowed(
            customPlan.cadence(), "customPlan.cadence", Set.of("MONTHLY", "ANNUAL", "CUSTOM")),
        requireNonNegative(customPlan.priceMinorUnits(), "customPlan.priceMinorUnits"),
        requireCurrency(customPlan.currency()),
        requireNonNegative(customPlan.trialDurationDays(), "customPlan.trialDurationDays"),
        requireAllowed(
            customPlan.supportTier(),
            "customPlan.supportTier",
            Set.of("STANDARD", "PRIORITY", "DEDICATED")),
        normalizeFeatureFlags(customPlan.featureFlags()),
        limitsFromRequest(customPlan.defaultLimits()),
        CompanyTime.now(),
        CompanyTime.now());
  }

  private PlanView storedCustomPlanView(Long companyId) {
    Instant updatedAt =
        readInstant(customPlanKey(companyId, "updatedAt")).orElse(CompanyTime.now());
    Map<String, Boolean> features = new LinkedHashMap<>();
    for (EntitlementFeature feature : EntitlementFeature.values()) {
      readBoolean(customFeatureKey(companyId, feature.key()))
          .ifPresent(value -> features.put(feature.key(), value));
    }
    Map<String, Long> limits = new LinkedHashMap<>();
    for (String key : LIMIT_KEYS) {
      limits.put(key, readLong(customLimitKey(companyId, key)).orElse(0L));
    }
    return new PlanView(
        CUSTOM,
        readSetting(customPlanKey(companyId, "displayName")).orElse("Custom"),
        1,
        true,
        readSetting(customPlanKey(companyId, "cadence")).orElse("CUSTOM"),
        readLong(customPlanKey(companyId, "priceMinorUnits")).orElse(0L),
        readSetting(customPlanKey(companyId, "currency")).orElse("INR"),
        readInteger(customPlanKey(companyId, "trialDurationDays")).orElse(0),
        readSetting(customPlanKey(companyId, "supportTier")).orElse("DEDICATED"),
        features,
        limits,
        readInstant(customPlanKey(companyId, "effectiveFrom")).orElse(updatedAt),
        updatedAt);
  }

  private void persistCustomPlan(Long companyId, PlanView plan) {
    putSetting(customPlanKey(companyId, "displayName"), plan.displayName());
    putSetting(customPlanKey(companyId, "cadence"), plan.cadence());
    putSetting(customPlanKey(companyId, "priceMinorUnits"), Long.toString(plan.priceMinorUnits()));
    putSetting(customPlanKey(companyId, "currency"), plan.currency());
    putSetting(
        customPlanKey(companyId, "trialDurationDays"), Integer.toString(plan.trialDurationDays()));
    putSetting(customPlanKey(companyId, "supportTier"), plan.supportTier());
    putSetting(customPlanKey(companyId, "effectiveFrom"), plan.effectiveFrom().toString());
    putSetting(customPlanKey(companyId, "updatedAt"), plan.updatedAt().toString());
    EntitlementFeature.canonicalKeys()
        .forEach(key -> deleteSetting(customFeatureKey(companyId, key)));
    LIMIT_KEYS.forEach(key -> deleteSetting(customLimitKey(companyId, key)));
    plan.features()
        .forEach(
            (key, value) -> putSetting(customFeatureKey(companyId, key), Boolean.toString(value)));
    plan.limits()
        .forEach((key, value) -> putSetting(customLimitKey(companyId, key), Long.toString(value)));
  }

  private void clearCustomPlan(Long companyId) {
    List.of(
            "displayName",
            "cadence",
            "priceMinorUnits",
            "currency",
            "trialDurationDays",
            "supportTier",
            "effectiveFrom",
            "updatedAt")
        .forEach(key -> deleteSetting(customPlanKey(companyId, key)));
    EntitlementFeature.canonicalKeys()
        .forEach(key -> deleteSetting(customFeatureKey(companyId, key)));
    LIMIT_KEYS.forEach(key -> deleteSetting(customLimitKey(companyId, key)));
  }

  private boolean refreshBillingSnapshotIfNeeded(
      Long companyId, PlanView plan, Map<String, String> oldSnapshot, boolean explicitReprice) {
    boolean missingSnapshot = !StringUtils.hasText(oldSnapshot.get("capturedAt"));
    if (!missingSnapshot && !explicitReprice) {
      return false;
    }
    putSetting(billingSnapshotKey(companyId, "planId"), plan.planId());
    putSetting(billingSnapshotKey(companyId, "displayName"), plan.displayName());
    putSetting(billingSnapshotKey(companyId, "cadence"), plan.cadence());
    putSetting(
        billingSnapshotKey(companyId, "priceMinorUnits"), Long.toString(plan.priceMinorUnits()));
    putSetting(billingSnapshotKey(companyId, "currency"), plan.currency());
    putSetting(billingSnapshotKey(companyId, "capturedAt"), CompanyTime.now().toString());
    return true;
  }

  private SuperAdminTenantEntitlementsDto.BillingSnapshot billingDto(
      Long companyId, PlanView plan, boolean repriceApplied) {
    Map<String, String> snapshot = billingSnapshot(companyId);
    return new SuperAdminTenantEntitlementsDto.BillingSnapshot(
        snapshot.getOrDefault("planId", plan.planId()),
        snapshot.getOrDefault("displayName", plan.displayName()),
        snapshot.getOrDefault("cadence", plan.cadence()),
        parseLong(snapshot.get("priceMinorUnits")).orElse(plan.priceMinorUnits()),
        snapshot.getOrDefault("currency", plan.currency()),
        parseInstant(snapshot.get("capturedAt")).orElse(null),
        repriceApplied,
        POLICY_SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE);
  }

  private Map<String, String> billingSnapshot(Long companyId) {
    Map<String, String> snapshot = new LinkedHashMap<>();
    List.of("planId", "displayName", "cadence", "priceMinorUnits", "currency", "capturedAt")
        .forEach(
            key ->
                readSetting(billingSnapshotKey(companyId, key))
                    .ifPresent(value -> snapshot.put(key, value)));
    return snapshot;
  }

  private Map<String, Long> limitsFromTemplate(SuperAdminPlanTemplate template) {
    Map<String, Long> limits = new LinkedHashMap<>();
    limits.put("maxActiveUsers", template.getMaxActiveUsers());
    limits.put("maxApiRequests", template.getMaxApiRequests());
    limits.put("maxStorageBytes", template.getMaxStorageBytes());
    limits.put("maxPdfExports", template.getMaxPdfExports());
    limits.put("maxEmails", template.getMaxEmails());
    limits.put("maxJobs", template.getMaxJobs());
    limits.put("burstRequestsPerMinute", template.getBurstRequestsPerMinute());
    limits.put("maxConcurrentRequests", template.getMaxConcurrentRequests());
    return limits;
  }

  private Map<String, Long> limitsFromRequest(
      com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementLimitsRequest limits) {
    Map<String, Long> result = new LinkedHashMap<>();
    result.put(
        "maxActiveUsers",
        requireNonNegative(limits.maxActiveUsers(), "customPlan.defaultLimits.maxActiveUsers"));
    result.put(
        "maxApiRequests",
        requireNonNegative(limits.maxApiRequests(), "customPlan.defaultLimits.maxApiRequests"));
    result.put(
        "maxStorageBytes",
        requireNonNegative(limits.maxStorageBytes(), "customPlan.defaultLimits.maxStorageBytes"));
    result.put(
        "maxPdfExports",
        requireNonNegative(limits.maxPdfExports(), "customPlan.defaultLimits.maxPdfExports"));
    result.put(
        "maxEmails", requireNonNegative(limits.maxEmails(), "customPlan.defaultLimits.maxEmails"));
    result.put("maxJobs", requireNonNegative(limits.maxJobs(), "customPlan.defaultLimits.maxJobs"));
    result.put(
        "burstRequestsPerMinute",
        requireNonNegative(
            limits.burstRequestsPerMinute(), "customPlan.defaultLimits.burstRequestsPerMinute"));
    result.put(
        "maxConcurrentRequests",
        requireNonNegative(
            limits.maxConcurrentRequests(), "customPlan.defaultLimits.maxConcurrentRequests"));
    return result;
  }

  private void putLimitOverride(Long companyId, String key, Long value, Instant now) {
    String normalizedKey = normalizeEntitlementKey(key);
    if (!LIMIT_KEYS.contains(normalizedKey)) {
      throw invalidInput("Unsupported limit override: " + key);
    }
    putSetting(
        limitOverrideKey(companyId, normalizedKey),
        Long.toString(requireNonNegative(value, normalizedKey)));
    putSetting(limitOverrideUpdatedKey(companyId, normalizedKey), now.toString());
  }

  private void putFeatureOverride(Long companyId, String key, Boolean value, Instant now) {
    if (value == null) {
      throw invalidInput("Feature override value is required for " + key);
    }
    String featureKey = normalizeFeatureKey(key);
    EntitlementFeature feature = EntitlementFeature.require(featureKey, this::invalidInput);
    if (!feature.mutable()) {
      throw invalidInput("Feature override " + feature.key() + " is not mutable");
    }
    putSetting(featureOverrideKey(companyId, featureKey), Boolean.toString(value));
    putSetting(featureOverrideUpdatedKey(companyId, featureKey), now.toString());
  }

  private Map<String, Boolean> normalizeFeatureFlags(Map<String, Boolean> featureFlags) {
    if (featureFlags == null || featureFlags.isEmpty()) {
      throw invalidInput("featureFlags must include at least one feature");
    }
    Map<String, Boolean> normalized = new LinkedHashMap<>();
    featureFlags.forEach(
        (key, value) -> {
          if (value == null) {
            throw invalidInput("featureFlags." + key + " is required");
          }
          EntitlementFeature feature = EntitlementFeature.require(key, this::invalidInput);
          if (!feature.mutable() && !value) {
            throw invalidInput(
                "featureFlags." + key + " cannot disable always-on feature " + feature.key());
          }
          normalized.put(feature.key(), value);
        });
    return normalized;
  }

  private String normalizeFeatureKey(String value) {
    normalizeToken(value, "feature");
    return EntitlementFeature.normalizeKey(value, this::invalidInput);
  }

  private String normalizeEntitlementKey(String value) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput("override key is required");
    }
    String normalized = value.trim();
    if (normalized.startsWith("limits.")) {
      return normalized.substring("limits.".length());
    }
    if (normalized.startsWith("features.")) {
      return normalized.substring("features.".length()).toUpperCase(Locale.ROOT);
    }
    return normalized;
  }

  private Company requireCompany(Long companyId) {
    return companyRepository
        .findById(companyId)
        .orElseThrow(() -> new EntityNotFoundException("Company not found"));
  }

  private Instant resolveTrialEndsAt(int trialDurationDays) {
    if (trialDurationDays <= 0) {
      return null;
    }
    return CompanyTime.now().plusSeconds(trialDurationDays * 86_400L);
  }

  private Long audit(Company company, String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new LinkedHashMap<>();
    auditMetadata.put("actor", currentActor());
    auditMetadata.put("reason", reason);
    auditMetadata.put("targetType", "TENANT_ENTITLEMENTS");
    auditMetadata.putAll(metadata);
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), company.getCode(), auditMetadata);
    return auditLog == null ? null : auditLog.getId();
  }

  private void putSetting(String key, String value) {
    systemSettingsRepository.save(new SystemSetting(key, value));
  }

  private boolean deleteSetting(String key) {
    if (!systemSettingsRepository.existsById(key)) {
      return false;
    }
    systemSettingsRepository.deleteById(key);
    return true;
  }

  private boolean hasSetting(String key) {
    return systemSettingsRepository.existsById(key);
  }

  private Optional<String> readSetting(String key) {
    return systemSettingsRepository
        .findById(key)
        .map(SystemSetting::getValue)
        .filter(StringUtils::hasText);
  }

  private Optional<Long> readLong(String key) {
    return readSetting(key).flatMap(this::parseLong);
  }

  private Optional<Integer> readInteger(String key) {
    return readSetting(key).flatMap(value -> parseLong(value).map(Long::intValue));
  }

  private Optional<Boolean> readBoolean(String key) {
    return readSetting(key).map(Boolean::parseBoolean);
  }

  private Optional<Instant> readInstant(String key) {
    return readSetting(key).flatMap(this::parseInstant);
  }

  private Optional<Long> parseLong(String value) {
    try {
      return StringUtils.hasText(value)
          ? Optional.of(Long.parseLong(value.trim()))
          : Optional.empty();
    } catch (NumberFormatException ex) {
      return Optional.empty();
    }
  }

  private Optional<Instant> parseInstant(String value) {
    try {
      return StringUtils.hasText(value)
          ? Optional.of(Instant.parse(value.trim()))
          : Optional.empty();
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  private String limitOverrideKey(Long companyId, String key) {
    return "ten.ent.ovr." + companyId + "." + key;
  }

  private String limitOverrideUpdatedKey(Long companyId, String key) {
    return "ten.ent.ovt." + companyId + "." + key;
  }

  private String featureOverrideKey(Long companyId, String key) {
    return "ten.ent.fo." + companyId + "." + key;
  }

  private String featureOverrideUpdatedKey(Long companyId, String key) {
    return "ten.ent.fot." + companyId + "." + key;
  }

  private String customPlanKey(Long companyId, String key) {
    return "ten.ent.cp." + companyId + "." + key;
  }

  private String customFeatureKey(Long companyId, String key) {
    return "ten.ent.cf." + companyId + "." + key;
  }

  private String customLimitKey(Long companyId, String key) {
    return "ten.ent.cl." + companyId + "." + key;
  }

  private String billingSnapshotKey(Long companyId, String key) {
    return "ten.bill.snap." + companyId + "." + key;
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
    String normalized = normalizeToken(value, "customPlan.currency");
    if (!normalized.matches("[A-Z]{3}")) {
      throw invalidInput("customPlan.currency must be an ISO 4217 code");
    }
    return normalized;
  }

  private long requireNonNegative(Long value, String fieldName) {
    if (value == null || value < 0L) {
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

  private int safeInteger(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    if (value < Integer.MIN_VALUE) {
      return Integer.MIN_VALUE;
    }
    return (int) value;
  }

  private String safeReason(String reason) {
    return StringUtils.hasText(reason) ? reason.trim() : "not-provided";
  }

  private String nullSafe(String value) {
    return StringUtils.hasText(value) ? value : "unset";
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return "anonymous";
    }
    return authentication.getName().trim();
  }

  private com.bigbrightpaints.erp.core.exception.ApplicationException invalidInput(String message) {
    return com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(message);
  }

  private record PlanView(
      String planId,
      String displayName,
      int version,
      boolean custom,
      String cadence,
      long priceMinorUnits,
      String currency,
      int trialDurationDays,
      String supportTier,
      Map<String, Boolean> features,
      Map<String, Long> limits,
      Instant effectiveFrom,
      Instant updatedAt) {}
}
