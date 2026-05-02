package com.bigbrightpaints.erp.modules.company.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigbrightpaints.erp.modules.company.dto.CompanyEnabledModulesDto;
import com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateDto;
import com.bigbrightpaints.erp.modules.company.dto.CompanySuperAdminDashboardDto;
import com.bigbrightpaints.erp.modules.company.dto.CompanySupportWarningDto;
import com.bigbrightpaints.erp.modules.company.dto.MainAdminSummaryDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminAddClientCreateRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminAddClientCreateResponse;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminAddClientOptionsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantAdminEmailChangeConfirmationDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantAdminEmailChangeRequestDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantDetailDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementOverrideRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantForceLogoutDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantLimitsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantPlanAssignmentRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantSummaryDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantSupportContextDto;
import com.bigbrightpaints.erp.modules.company.service.CompanyService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminBillingService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantEntitlementService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminUsageService;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageRollupService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

  @Mock private CompanyService companyService;
  @Mock private SuperAdminTenantControlPlaneService controlPlaneService;
  @Mock private SuperAdminTenantEntitlementService entitlementService;
  @Mock private TenantUsageRollupService tenantUsageRollupService;
  @Mock private SuperAdminUsageService superAdminUsageService;
  @Mock private SuperAdminBillingService billingService;

  private SuperAdminController controller;

  @BeforeEach
  void setUp() {
    controller =
        new SuperAdminController(
            companyService,
            controlPlaneService,
            entitlementService,
            tenantUsageRollupService,
            superAdminUsageService,
            billingService);
  }

  @Test
  void routes_delegateToServicesAcrossCanonicalSuperadminSurface() {
    when(companyService.getSuperAdminDashboard())
        .thenReturn(
            new CompanySuperAdminDashboardDto(
                1,
                1,
                0,
                0,
                2,
                10,
                200,
                400,
                1,
                4,
                List.of(
                    new CompanySuperAdminDashboardDto.TenantOverview(
                        7L, "ACME", "Acme", "KA", "ACTIVE", null, 2, 10, 200, 400, 1, 4, 40, 2000,
                        1, 250, true, false, 2000, 5000, 2500))));
    when(controlPlaneService.listTenants("ACTIVE", "acme", 0, 20, "companyCode,asc", false))
        .thenReturn(
            PageResponse.of(
                List.of(
                    new SuperAdminTenantSummaryDto(
                        7L,
                        "ACME",
                        "Acme",
                        "UTC",
                        "ACTIVE",
                        null,
                        12,
                        120,
                        400,
                        2000,
                        2048,
                        4096,
                        3,
                        8,
                        Set.of("ACCOUNTING"),
                        new MainAdminSummaryDto(91L, "admin@acme.com", "Main Admin", true, true),
                        Instant.parse("2026-03-26T11:00:00Z"))),
                1,
                0,
                20));
    SuperAdminAddClientOptionsDto options =
        new SuperAdminAddClientOptionsDto(
            new SuperAdminAddClientOptionsDto.Section("company", "Company", List.of()),
            new SuperAdminAddClientOptionsDto.Section("owner", "Owner", List.of()),
            new SuperAdminAddClientOptionsDto.Section("commercial", "Commercial", List.of()),
            new SuperAdminAddClientOptionsDto.Section("quotas", "Quotas", List.of()),
            new SuperAdminAddClientOptionsDto.Section("modules", "Modules", List.of()),
            new SuperAdminAddClientOptionsDto.Section("support", "Support", List.of()),
            List.of(),
            new SuperAdminAddClientOptionsDto.SeedPolicy("v1", true, List.of(), "rule"));
    SuperAdminAddClientCreateRequest createRequest =
        new SuperAdminAddClientCreateRequest(
            new SuperAdminAddClientCreateRequest.Company(
                "Acme", "ACME2", "UTC", "KA", "INR", null, "SME"),
            new SuperAdminAddClientCreateRequest.Owner("owner@acme.com", "Owner", null),
            new SuperAdminAddClientCreateRequest.Commercial("TRIAL", "MANUAL", 14, "STANDARD"),
            new SuperAdminAddClientCreateRequest.Quotas(10L, 100L, 1000L, 4L, false, true),
            new SuperAdminAddClientCreateRequest.Modules(Set.of("ACCOUNTING")),
            new SuperAdminAddClientCreateRequest.Support(null, Set.of("M4")),
            SuperAdminAddClientCreateRequest.CreateMode.DRAFT);
    SuperAdminAddClientCreateResponse createResponse =
        new SuperAdminAddClientCreateResponse(
            44L,
            "ACME2",
            "Acme",
            "DRAFT",
            new SuperAdminAddClientCreateResponse.Owner(
                92L, "owner@acme.com", "Owner", "PENDING_ACTIVATION"),
            "TRIAL",
            "MANUAL",
            null,
            "STANDARD",
            new SuperAdminAddClientCreateResponse.Quotas(10, 100, 1000, 4, false, true),
            Set.of("ACCOUNTING"),
            new SuperAdminAddClientCreateResponse.Activation(
                "NOT_SENT", null, null, null, "NOT_SENT", List.of("rawToken")),
            options.seedPolicy(),
            501L);
    when(controlPlaneService.getAddClientOptions()).thenReturn(options);
    when(controlPlaneService.createAddClient(createRequest)).thenReturn(createResponse);
    SuperAdminTenantDetailDto detail =
        new SuperAdminTenantDetailDto(
            7L,
            "ACME",
            "Acme",
            "UTC",
            "KA",
            "ACTIVE",
            null,
            Set.of("ACCOUNTING"),
            new SuperAdminTenantDetailDto.Onboarding(
                "SME", "admin@acme.com", 91L, true, Instant.parse("2026-03-26T09:30:00Z")),
            new MainAdminSummaryDto(91L, "admin@acme.com", "Main Admin", true, true),
            new SuperAdminTenantDetailDto.Limits(10, 20, 30, 4, 5, true, false),
            new SuperAdminTenantDetailDto.Usage(
                2, 40, 1, 250, 200, 1, Instant.parse("2026-03-26T11:00:00Z")),
            new SuperAdminTenantDetailDto.SupportContext("note", Set.of("URGENT")),
            List.of(
                new SuperAdminTenantDetailDto.SupportTimelineEvent(
                    "WARNING",
                    "FINANCE",
                    "Check payment",
                    "ops@bbp.com",
                    Instant.parse("2026-03-26T08:00:00Z"))),
            new SuperAdminTenantDetailDto.AvailableActions(
                true, true, true, true, true, true, true, true));
    when(controlPlaneService.getTenantDetail(7L)).thenReturn(detail);
    when(controlPlaneService.updateLifecycleState(
            7L,
            new com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateRequest(
                "ACTIVE", "ok")))
        .thenReturn(new CompanyLifecycleStateDto(7L, "ACME", "SUSPENDED", "ACTIVE", "ok"));
    when(controlPlaneService.updateLimits(7L, 10L, 20L, 30L, 4L, 5L, true, false))
        .thenReturn(new SuperAdminTenantLimitsDto(7L, "ACME", 10, 20, 30, 4, 5, true, false));
    when(controlPlaneService.updateModules(7L, Set.of("ACCOUNTING", "SALES")))
        .thenReturn(new CompanyEnabledModulesDto(7L, "ACME", Set.of("ACCOUNTING", "SALES")));
    SuperAdminTenantEntitlementsDto entitlements = entitlements();
    SuperAdminTenantPlanAssignmentRequest planRequest =
        new SuperAdminTenantPlanAssignmentRequest("GROWTH", null, false, "upgrade");
    SuperAdminTenantEntitlementOverrideRequest overrideRequest =
        new SuperAdminTenantEntitlementOverrideRequest(
            Map.of("maxActiveUsers", 25L), Map.of("PORTAL", false), "contract");
    SuperAdminController.TenantEntitlementOverrideRemoveRequest removeRequest =
        new SuperAdminController.TenantEntitlementOverrideRemoveRequest("restore");
    when(entitlementService.getEffectiveEntitlements(7L)).thenReturn(entitlements);
    when(entitlementService.assignPlan(7L, planRequest)).thenReturn(entitlements);
    when(entitlementService.putOverrides(7L, overrideRequest)).thenReturn(entitlements);
    when(entitlementService.removeOverride(7L, "PORTAL", "restore")).thenReturn(entitlements);
    when(tenantUsageRollupService.getPlatformUsage())
        .thenReturn(
            new com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos.PlatformUsage(
                Instant.parse("2026-03-26T12:00:00Z"), null, List.of(), List.of()));
    when(superAdminUsageService.getTenantUsage(7L, entitlements.limits()))
        .thenReturn(
            new com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos.TenantUsage(
                7L, "ACME", "Acme", "UTC", null, null, List.of(), List.of(), List.of()));
    when(tenantUsageRollupService.getTenantUsageHistory(7L, "DAILY"))
        .thenReturn(
            new com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos.TenantUsageHistory(
                7L, "ACME", "UTC", "DAILY", List.of()));
    when(controlPlaneService.issueSupportWarning(7L, "OPS", "Check", "SUSPENDED", 24))
        .thenReturn(
            new CompanySupportWarningDto(
                7L,
                "ACME",
                "55",
                "OPS",
                "Check",
                "SUSPENDED",
                24,
                "super-admin@bbp.com",
                Instant.parse("2026-03-26T12:30:00Z")));
    when(controlPlaneService.updateSupportContext(7L, "note", Set.of("OPS")))
        .thenReturn(new SuperAdminTenantSupportContextDto(7L, "ACME", "note", Set.of("OPS")));
    when(controlPlaneService.forceLogoutAllUsers(7L, "security"))
        .thenReturn(
            new SuperAdminTenantForceLogoutDto(
                7L,
                "ACME",
                3,
                "security",
                "super-admin@bbp.com",
                Instant.parse("2026-03-26T13:30:00Z")));
    when(controlPlaneService.replaceMainAdmin(7L, 91L))
        .thenReturn(new MainAdminSummaryDto(91L, "admin@acme.com", "Main Admin", true, true));
    when(controlPlaneService.requestAdminEmailChange(7L, 91L, "new-admin@acme.com"))
        .thenReturn(
            new SuperAdminTenantAdminEmailChangeRequestDto(
                301L,
                7L,
                "ACME",
                91L,
                "admin@acme.com",
                "new-admin@acme.com",
                Instant.parse("2026-03-26T13:40:00Z"),
                Instant.parse("2026-03-27T13:40:00Z")));
    when(controlPlaneService.confirmAdminEmailChange(7L, 91L, 301L, "verify-123"))
        .thenReturn(
            new SuperAdminTenantAdminEmailChangeConfirmationDto(
                301L,
                7L,
                "ACME",
                91L,
                "new-admin@acme.com",
                Instant.parse("2026-03-26T14:00:00Z"),
                Instant.parse("2026-03-26T14:00:00Z")));

    assertSuccess(controller.dashboard().getBody(), "Superadmin dashboard fetched");
    assertSuccess(
        controller.listTenants("ACTIVE", "acme", 0, 20, "companyCode,asc", false).getBody(),
        "Superadmin tenant list fetched");
    assertSuccess(controller.addClientOptions().getBody(), "Add Client options fetched");
    assertThat(controller.createTenant(createRequest).getStatusCode().value()).isEqualTo(201);
    assertThat(controller.createTenant(createRequest).getBody().data().status()).isEqualTo("DRAFT");
    assertThat(controller.getTenantDetail(7L).getBody().data()).isEqualTo(detail);
    assertSuccess(
        controller
            .updateLifecycleState(
                7L,
                new com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateRequest(
                    "ACTIVE", "ok"))
            .getBody(),
        "Tenant lifecycle state updated");
    assertSuccess(
        controller
            .updateTenantLimits(
                7L,
                new SuperAdminController.TenantLimitsUpdateRequest(
                    10L, 20L, 30L, 4L, 5L, true, false))
            .getBody(),
        "Tenant limits updated");
    assertSuccess(
        controller
            .updateTenantModules(
                7L,
                new SuperAdminController.TenantModulesUpdateRequest(Set.of("ACCOUNTING", "SALES")))
            .getBody(),
        "Tenant modules updated");
    assertSuccess(
        controller.getTenantEntitlements(7L).getBody(), "Tenant effective entitlements fetched");
    assertSuccess(controller.getPlatformUsage().getBody(), "Platform usage fetched");
    assertSuccess(controller.getTenantUsage(7L).getBody(), "Tenant usage fetched");
    assertSuccess(
        controller.getTenantUsageHistory(7L, "DAILY").getBody(), "Tenant usage history fetched");
    assertSuccess(controller.assignTenantPlan(7L, planRequest).getBody(), "Tenant plan assigned");
    assertSuccess(
        controller.upsertTenantOverrides(7L, overrideRequest).getBody(),
        "Tenant entitlement overrides updated");
    assertSuccess(
        controller.removeTenantOverride(7L, "PORTAL", removeRequest).getBody(),
        "Tenant entitlement override removed");
    assertSuccess(
        controller
            .issueSupportWarning(
                7L,
                new SuperAdminController.TenantSupportWarningRequest(
                    "OPS", "Check", "SUSPENDED", 24))
            .getBody(),
        "Tenant warning issued");
    assertSuccess(
        controller
            .updateSupportContext(
                7L,
                new SuperAdminController.TenantSupportContextUpdateRequest("note", Set.of("OPS")))
            .getBody(),
        "Tenant support context updated");
    assertSuccess(
        controller
            .forceLogout(7L, new SuperAdminController.TenantForceLogoutRequest("security"))
            .getBody(),
        "Tenant sessions revoked");
    assertSuccess(
        controller
            .replaceMainAdmin(7L, new SuperAdminController.TenantMainAdminUpdateRequest(91L))
            .getBody(),
        "Tenant main admin replaced");
    assertSuccess(
        controller
            .requestAdminEmailChange(
                7L,
                91L,
                new SuperAdminController.TenantAdminEmailChangeRequest("new-admin@acme.com"))
            .getBody(),
        "Tenant admin email change requested");
    assertSuccess(
        controller
            .confirmAdminEmailChange(
                7L,
                91L,
                new SuperAdminController.TenantAdminEmailChangeConfirmRequest(301L, "verify-123"))
            .getBody(),
        "Tenant admin email change confirmed");
  }

  private void assertSuccess(ApiResponse<?> response, String message) {
    assertThat(response.success()).isTrue();
    assertThat(response.message()).isEqualTo(message);
    assertThat(response.data()).isNotNull();
  }

  private SuperAdminTenantEntitlementsDto entitlements() {
    Instant now = Instant.parse("2026-03-26T12:00:00Z");
    return new SuperAdminTenantEntitlementsDto(
        7L,
        "ACME",
        new SuperAdminTenantEntitlementsDto.PlanSummary(
            "GROWTH", "Growth", 1, false, "PRIORITY", now),
        Map.of(
            "maxActiveUsers",
            new SuperAdminTenantEntitlementsDto.LimitEntitlement(
                "maxActiveUsers", 50, 25L, 25, "TENANT_OVERRIDE", now)),
        Map.of(
            "PORTAL",
            new SuperAdminTenantEntitlementsDto.FeatureEntitlement(
                "PORTAL", true, false, false, "TENANT_OVERRIDE", now)),
        new SuperAdminTenantEntitlementsDto.CacheMetadata(true, now, true, "TENANT_ENTITLEMENTS_7"),
        new SuperAdminTenantEntitlementsDto.BillingSnapshot(
            "STARTER",
            "Starter",
            "MONTHLY",
            499900,
            "INR",
            now,
            false,
            "SNAPSHOT_UNCHANGED_UNTIL_EXPLICIT_REPRICE"),
        701L);
  }
}
