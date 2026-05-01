package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSetting;
import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyModule;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplate;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminPlanTemplateRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementLimitsRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementOverrideRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantPlanAssignmentRequest;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuperAdminTenantEntitlementServiceTest {

  @Mock private CompanyRepository companyRepository;
  @Mock private SuperAdminPlanTemplateRepository planTemplateRepository;
  @Mock private SystemSettingsRepository systemSettingsRepository;
  @Mock private TenantRuntimeEnforcementService tenantRuntimeEnforcementService;
  @Mock private TenantSupportControlPort tenantSupportControlPort;
  @Mock private AuditService auditService;

  private final Map<String, SystemSetting> settings = new HashMap<>();
  private SuperAdminTenantEntitlementService service;

  @BeforeEach
  void setUp() {
    service =
        new SuperAdminTenantEntitlementService(
            companyRepository,
            planTemplateRepository,
            systemSettingsRepository,
            tenantRuntimeEnforcementService,
            tenantSupportControlPort,
            auditService);
    when(systemSettingsRepository.save(any(SystemSetting.class)))
        .thenAnswer(
            invocation -> {
              SystemSetting setting = invocation.getArgument(0);
              settings.put(setting.getKey(), setting);
              return setting;
            });
    when(systemSettingsRepository.findById(any(String.class)))
        .thenAnswer(invocation -> Optional.ofNullable(settings.get(invocation.getArgument(0))));
    when(systemSettingsRepository.existsById(any(String.class)))
        .thenAnswer(invocation -> settings.containsKey(invocation.getArgument(0)));
    org.mockito.Mockito.doAnswer(
            invocation -> {
              settings.remove(invocation.getArgument(0));
              return null;
            })
        .when(systemSettingsRepository)
        .deleteById(any(String.class));
    AuditLog auditLog = new AuditLog();
    auditLog.setId(701L);
    when(auditService.logAuthSuccessRequired(any(), eq("super-admin@bbp.com"), any(), any()))
        .thenReturn(auditLog);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("super-admin@bbp.com", "n/a"));
  }

  @Test
  void assignPlanAppliesTemplateEntitlementsInvalidatesRuntimeAndSnapshotsBilling() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);
    when(planTemplateRepository.findByStableIdIgnoreCaseOrderByTemplateVersionDesc("GROWTH"))
        .thenReturn(java.util.List.of(plan("GROWTH", "Growth", 1_499_900L, "PRIORITY")));

    SuperAdminTenantEntitlementsDto response =
        service.assignPlan(
            7L, new SuperAdminTenantPlanAssignmentRequest("growth", null, false, "upgrade"));

    assertThat(response.plan().planId()).isEqualTo("GROWTH");
    assertThat(response.plan().displayName()).isEqualTo("Growth");
    assertThat(response.limits().get("maxActiveUsers").effectiveValue()).isEqualTo(50);
    assertThat(response.limits().get("maxActiveUsers").source()).isEqualTo("PLAN_DEFAULT");
    assertThat(company.getCommercialPlanId()).isEqualTo("GROWTH");
    assertThat(company.getCommercialSupportTier()).isEqualTo("PRIORITY");
    assertThat(company.getQuotaMaxActiveUsers()).isEqualTo(50);
    assertThat(company.getEnabledModules()).contains(CompanyModule.MANUFACTURING.name());
    assertThat(company.getEnabledModules()).doesNotContain(CompanyModule.PORTAL.name());
    assertThat(response.billing().priceMinorUnits()).isEqualTo(1_499_900L);
    assertThat(response.billing().repriceApplied()).isTrue();
    verify(tenantRuntimeEnforcementService)
        .updatePolicy(
            "ACME", null, "TENANT_ENTITLEMENTS_UPDATE", 25, 300, 50, "super-admin@bbp.com");
    verify(tenantRuntimeEnforcementService).invalidatePolicyCache("ACME");
    verify(tenantSupportControlPort)
        .recalculateActiveTenantTicketsForSupportTierChange(company, "STANDARD", "PRIORITY", 701L);
  }

  @Test
  void customPlanAndOverridesDriveFeatureAccessWithoutPlanNameChecks() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);

    SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan =
        new SuperAdminTenantPlanAssignmentRequest.CustomPlan(
            "Custom Access",
            "CUSTOM",
            42_000L,
            "INR",
            0,
            "DEDICATED",
            Map.of("PORTAL", true, "PRODUCTION", true),
            new SuperAdminTenantEntitlementLimitsRequest(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));
    service.assignPlan(
        7L, new SuperAdminTenantPlanAssignmentRequest(null, customPlan, true, "custom plan"));

    SuperAdminTenantEntitlementsDto overridden =
        service.putOverrides(
            7L,
            new SuperAdminTenantEntitlementOverrideRequest(
                Map.of("maxActiveUsers", 2L), Map.of("PORTAL", false), "disable portal"));

    assertThat(overridden.plan().custom()).isTrue();
    assertThat(overridden.limits().get("maxActiveUsers").effectiveValue()).isEqualTo(2);
    assertThat(overridden.limits().get("maxActiveUsers").source()).isEqualTo("TENANT_OVERRIDE");
    assertThat(overridden.features().get("PORTAL").effectiveValue()).isFalse();
    assertThat(overridden.features().get("PORTAL").source()).isEqualTo("TENANT_OVERRIDE");
    assertThat(service.isFeatureEnabled(company, CompanyModule.PORTAL)).isFalse();
    assertThat(service.isFeatureEnabled(company, CompanyModule.MANUFACTURING)).isTrue();

    SuperAdminTenantEntitlementsDto restored = service.removeOverride(7L, "PORTAL", "restore");

    assertThat(restored.features().get("PORTAL").effectiveValue()).isTrue();
    assertThat(restored.features().get("PORTAL").source()).isEqualTo("PLAN_DEFAULT");
  }

  @Test
  void customPlanAcceptedRegistryKeysPersistAndReadBackWithoutCoreMismatch() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);

    SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan =
        new SuperAdminTenantPlanAssignmentRequest.CustomPlan(
            "Registry Custom",
            "CUSTOM",
            42_000L,
            "INR",
            0,
            "DEDICATED",
            Map.of("PRODUCTION", false, "REPORTS", true, "ACCOUNTING", true, "CUSTOM_PLAN", true),
            new SuperAdminTenantEntitlementLimitsRequest(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

    service.assignPlan(
        7L, new SuperAdminTenantPlanAssignmentRequest(null, customPlan, true, "registry plan"));
    SuperAdminTenantEntitlementsDto readback = service.getEffectiveEntitlements(7L);

    assertThat(readback.features()).containsKeys("PRODUCTION", "REPORTS", "ACCOUNTING", "SALES");
    assertThat(readback.features().get("PRODUCTION").planDefault()).isFalse();
    assertThat(readback.features().get("REPORTS").planDefault()).isTrue();
    assertThat(readback.features().get("ACCOUNTING").effectiveValue()).isTrue();
    assertThat(readback.features().get("SALES").effectiveValue()).isTrue();
    assertThat(service.isFeatureEnabled(company, CompanyModule.ACCOUNTING)).isTrue();
    assertThat(service.isFeatureEnabled(company, CompanyModule.MANUFACTURING)).isFalse();
  }

  @Test
  void unsupportedCustomPlanAndOverrideKeysFailBeforeAuditOrCacheSideEffects() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));

    SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan =
        new SuperAdminTenantPlanAssignmentRequest.CustomPlan(
            "Bad Custom",
            "CUSTOM",
            42_000L,
            "INR",
            0,
            "DEDICATED",
            Map.of("UNKNOWN_FEATURE", true),
            new SuperAdminTenantEntitlementLimitsRequest(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

    assertThatThrownBy(
            () ->
                service.assignPlan(
                    7L,
                    new SuperAdminTenantPlanAssignmentRequest(
                        null, customPlan, true, "bad registry key")))
        .hasMessageContaining("Unsupported entitlement feature");

    assertThat(settings.keySet()).noneMatch(key -> key.startsWith("ten.ent.cf.7."));

    assertThatThrownBy(
            () ->
                service.putOverrides(
                    7L,
                    new SuperAdminTenantEntitlementOverrideRequest(
                        null, Map.of("UNKNOWN_FEATURE", true), "bad override")))
        .hasMessageContaining("Unsupported entitlement feature");

    assertThat(settings.keySet()).noneMatch(key -> key.startsWith("ten.ent.fo.7."));
    verify(tenantRuntimeEnforcementService, never()).invalidatePolicyCache(any());
  }

  @Test
  void coreAlwaysOnFeaturesCannotBeDisabledByCustomPlansOrOverrides() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));

    SuperAdminTenantPlanAssignmentRequest.CustomPlan customPlan =
        new SuperAdminTenantPlanAssignmentRequest.CustomPlan(
            "Core Disable",
            "CUSTOM",
            42_000L,
            "INR",
            0,
            "DEDICATED",
            Map.of("ACCOUNTING", false, "PORTAL", true),
            new SuperAdminTenantEntitlementLimitsRequest(3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L));

    assertThatThrownBy(
            () ->
                service.assignPlan(
                    7L,
                    new SuperAdminTenantPlanAssignmentRequest(
                        null, customPlan, true, "disable accounting")))
        .hasMessageContaining("cannot disable always-on feature ACCOUNTING");

    assertThatThrownBy(
            () ->
                service.putOverrides(
                    7L,
                    new SuperAdminTenantEntitlementOverrideRequest(
                        null, Map.of("ACCOUNTING", false), "disable accounting override")))
        .hasMessageContaining("Feature override ACCOUNTING is not mutable");

    assertThat(service.isFeatureEnabled(company, CompanyModule.ACCOUNTING)).isTrue();
    verify(tenantRuntimeEnforcementService, never()).invalidatePolicyCache(any());
  }

  @Test
  void planChangeLeavesSubscriptionSnapshotUntilExplicitReprice() {
    Company company = company();
    when(companyRepository.findById(7L)).thenReturn(Optional.of(company));
    when(companyRepository.saveAndFlush(company)).thenReturn(company);
    when(planTemplateRepository.findByStableIdIgnoreCaseOrderByTemplateVersionDesc("STARTER"))
        .thenReturn(java.util.List.of(plan("STARTER", "Starter", 499_900L, "STANDARD")));
    when(planTemplateRepository.findByStableIdIgnoreCaseOrderByTemplateVersionDesc("GROWTH"))
        .thenReturn(java.util.List.of(plan("GROWTH", "Growth", 1_499_900L, "PRIORITY")));

    service.assignPlan(
        7L, new SuperAdminTenantPlanAssignmentRequest("STARTER", null, false, "initial"));
    SuperAdminTenantEntitlementsDto unchangedSnapshot =
        service.assignPlan(
            7L, new SuperAdminTenantPlanAssignmentRequest("GROWTH", null, false, "entitlements"));

    assertThat(unchangedSnapshot.plan().planId()).isEqualTo("GROWTH");
    assertThat(unchangedSnapshot.limits().get("maxActiveUsers").effectiveValue()).isEqualTo(50);
    assertThat(unchangedSnapshot.billing().snapshotPlanId()).isEqualTo("STARTER");
    assertThat(unchangedSnapshot.billing().priceMinorUnits()).isEqualTo(499_900L);
    assertThat(unchangedSnapshot.billing().repriceApplied()).isFalse();

    SuperAdminTenantEntitlementsDto repriced =
        service.assignPlan(
            7L, new SuperAdminTenantPlanAssignmentRequest("GROWTH", null, true, "reprice"));

    assertThat(repriced.billing().snapshotPlanId()).isEqualTo("GROWTH");
    assertThat(repriced.billing().priceMinorUnits()).isEqualTo(1_499_900L);
    assertThat(repriced.billing().repriceApplied()).isTrue();
  }

  private Company company() {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", 7L);
    company.setCode("ACME");
    company.setName("Acme");
    company.setTimezone("UTC");
    return company;
  }

  private SuperAdminPlanTemplate plan(
      String stableId, String displayName, long priceMinorUnits, String supportTier) {
    SuperAdminPlanTemplate template = new SuperAdminPlanTemplate();
    template.setStableId(stableId);
    template.setDisplayName(displayName);
    template.setTemplateVersion(1);
    template.setStatus("ACTIVE");
    template.setEffectiveFrom(Instant.parse("2026-01-01T00:00:00Z"));
    template.setCadence("MONTHLY");
    template.setPriceMinorUnits(priceMinorUnits);
    template.setCurrency("INR");
    template.setTrialDurationDays(0);
    template.setSupportTier(supportTier);
    template.setFeatureFlags(
        Map.of("PRODUCTION", true, "PORTAL", false, "PURCHASING", true, "REPORTS", true));
    template.setMaxActiveUsers(50L);
    template.setMaxApiRequests(250_000L);
    template.setMaxStorageBytes(53_687_091_200L);
    template.setMaxPdfExports(10_000L);
    template.setMaxEmails(20_000L);
    template.setMaxJobs(5_000L);
    template.setBurstRequestsPerMinute(300L);
    template.setMaxConcurrentRequests(25L);
    return template;
  }
}
