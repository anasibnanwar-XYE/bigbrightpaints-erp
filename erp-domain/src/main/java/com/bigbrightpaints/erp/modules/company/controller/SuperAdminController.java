package com.bigbrightpaints.erp.modules.company.controller;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.bigbrightpaints.erp.modules.company.dto.*;
import com.bigbrightpaints.erp.modules.company.service.CompanyService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminBillingService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantEntitlementService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminUsageService;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageRollupService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/superadmin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

  private final CompanyService companyService;
  private final SuperAdminTenantControlPlaneService controlPlaneService;
  private final SuperAdminTenantEntitlementService entitlementService;
  private final TenantUsageRollupService tenantUsageRollupService;
  private final SuperAdminUsageService superAdminUsageService;
  private final SuperAdminBillingService billingService;

  @Autowired
  public SuperAdminController(
      CompanyService companyService,
      SuperAdminTenantControlPlaneService controlPlaneService,
      SuperAdminTenantEntitlementService entitlementService,
      TenantUsageRollupService tenantUsageRollupService,
      SuperAdminUsageService superAdminUsageService,
      SuperAdminBillingService billingService) {
    this.companyService = companyService;
    this.controlPlaneService = controlPlaneService;
    this.entitlementService = entitlementService;
    this.tenantUsageRollupService = tenantUsageRollupService;
    this.superAdminUsageService = superAdminUsageService;
    this.billingService = billingService;
  }

  public SuperAdminController(
      CompanyService companyService,
      SuperAdminTenantControlPlaneService controlPlaneService,
      SuperAdminTenantEntitlementService entitlementService,
      TenantUsageRollupService tenantUsageRollupService,
      SuperAdminUsageService superAdminUsageService) {
    this(
        companyService,
        controlPlaneService,
        entitlementService,
        tenantUsageRollupService,
        superAdminUsageService,
        null);
  }

  @GetMapping("/dashboard")
  public ResponseEntity<ApiResponse<CompanySuperAdminDashboardDto>> dashboard() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Superadmin dashboard fetched", companyService.getSuperAdminDashboard()));
  }

  @GetMapping("/tenants")
  public ResponseEntity<ApiResponse<PageResponse<SuperAdminTenantSummaryDto>>> listTenants(
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "q", required = false) String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "sort", defaultValue = "companyCode,asc") String sort,
      @RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Superadmin tenant list fetched",
            controlPlaneService.listTenants(status, query, page, size, sort, includeArchived)));
  }

  public ResponseEntity<ApiResponse<PageResponse<SuperAdminTenantSummaryDto>>> listTenants(
      String status, String query, int page, int size, String sort) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Superadmin tenant list fetched",
            controlPlaneService.listTenants(status, query, page, size, sort)));
  }

  @GetMapping("/tenants/new")
  public ResponseEntity<ApiResponse<SuperAdminAddClientOptionsDto>> addClientOptions() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Add Client options fetched", controlPlaneService.getAddClientOptions()));
  }

  @PostMapping("/tenants")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ApiResponse<SuperAdminAddClientCreateResponse>> createTenant(
      @Valid @RequestBody SuperAdminAddClientCreateRequest request) {
    SuperAdminAddClientCreateResponse response = controlPlaneService.createAddClient(request);
    return ResponseEntity.created(URI.create("/api/v1/superadmin/tenants/" + response.tenantId()))
        .body(ApiResponse.success("Add Client created", response));
  }

  @GetMapping("/tenants/{id}")
  public ResponseEntity<ApiResponse<SuperAdminTenantDetailDto>> getTenantDetail(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Superadmin tenant detail fetched", controlPlaneService.getTenantDetail(tenantId)));
  }

  @PostMapping("/tenants/{id}/billing/subscription")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.SubscriptionResponse>>
      createBillingSubscription(
          @PathVariable("id") Long tenantId,
          @Valid @RequestBody SuperAdminBillingDtos.SubscriptionRequest request) {
    SuperAdminBillingDtos.SubscriptionResponse response =
        billingService.createSubscription(tenantId, request);
    return ResponseEntity.created(
            URI.create("/api/v1/superadmin/tenants/" + tenantId + "/billing/subscription"))
        .body(ApiResponse.success("Tenant billing subscription created", response));
  }

  @GetMapping("/tenants/{id}/billing/subscription")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.SubscriptionResponse>>
      getBillingSubscription(@PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant billing subscription fetched", billingService.getSubscription(tenantId)));
  }

  @PostMapping("/tenants/{id}/billing/invoices")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.LedgerEntryResponse>> createManualInvoice(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.LedgerEntryRequest request) {
    SuperAdminBillingService.LedgerMutationResult result =
        billingService.createInvoice(tenantId, request);
    return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(ApiResponse.success("Tenant manual invoice recorded", result.response()));
  }

  @PostMapping("/tenants/{id}/billing/payments")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.LedgerEntryResponse>> createManualPayment(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.LedgerEntryRequest request) {
    SuperAdminBillingService.LedgerMutationResult result =
        billingService.createPayment(tenantId, request);
    return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(ApiResponse.success("Tenant manual payment recorded", result.response()));
  }

  @PostMapping("/tenants/{id}/billing/adjustments")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.LedgerEntryResponse>>
      createManualAdjustment(
          @PathVariable("id") Long tenantId,
          @Valid @RequestBody SuperAdminBillingDtos.AdjustmentRequest request) {
    SuperAdminBillingService.LedgerMutationResult result =
        billingService.createAdjustment(tenantId, request);
    return ResponseEntity.status(result.replay() ? HttpStatus.OK : HttpStatus.CREATED)
        .body(ApiResponse.success("Tenant manual adjustment recorded", result.response()));
  }

  @GetMapping("/tenants/{id}/billing/ledger")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.LedgerResponse>> getBillingLedger(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success("Tenant billing ledger fetched", billingService.getLedger(tenantId)));
  }

  @GetMapping("/tenants/{id}/commercial-state")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>>
      getCommercialState(@PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant commercial state fetched", billingService.getCommercialState(tenantId)));
  }

  @PostMapping("/tenants/{id}/suspension/grace")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> startGrace(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant billing grace started", billingService.startGrace(tenantId, request)));
  }

  @PostMapping("/tenants/{id}/suspension/read-only")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> suspendReadOnly(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant read-only suspension applied",
            billingService.suspendReadOnly(tenantId, request)));
  }

  @PostMapping("/tenants/{id}/suspension/blocked")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> suspendBlocked(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant blocked suspension applied", billingService.suspendBlocked(tenantId, request)));
  }

  @PostMapping("/tenants/{id}/resume")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> resumeTenant(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Tenant resumed", billingService.resume(tenantId, request)));
  }

  @PostMapping("/tenants/{id}/cancel")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> cancelTenant(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Tenant canceled", billingService.cancel(tenantId, request)));
  }

  @PostMapping("/tenants/{id}/archive")
  public ResponseEntity<ApiResponse<SuperAdminBillingDtos.CommercialStateResponse>> archiveTenant(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Tenant archived", billingService.archive(tenantId, request)));
  }

  @GetMapping("/billing/metrics")
  public ResponseEntity<ApiResponse<Map<String, SuperAdminBillingDtos.CurrencyMetrics>>>
      getBillingMetrics() {
    return ResponseEntity.ok(
        ApiResponse.success("Billing metrics fetched", billingService.getBillingMetrics()));
  }

  @GetMapping("/usage")
  public ResponseEntity<ApiResponse<SuperAdminUsageDtos.PlatformUsage>> getPlatformUsage() {
    return ResponseEntity.ok(
        ApiResponse.success("Platform usage fetched", tenantUsageRollupService.getPlatformUsage()));
  }

  @GetMapping("/tenants/{id}/usage")
  public ResponseEntity<ApiResponse<SuperAdminUsageDtos.TenantUsage>> getTenantUsage(
      @PathVariable("id") Long tenantId) {
    SuperAdminTenantEntitlementsDto entitlements =
        entitlementService.getEffectiveEntitlements(tenantId);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant usage fetched",
            superAdminUsageService.getTenantUsage(tenantId, entitlements.limits())));
  }

  @GetMapping("/tenants/{id}/usage/history")
  public ResponseEntity<ApiResponse<SuperAdminUsageDtos.TenantUsageHistory>> getTenantUsageHistory(
      @PathVariable("id") Long tenantId,
      @RequestParam(value = "periodType", required = false) String periodType) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant usage history fetched",
            tenantUsageRollupService.getTenantUsageHistory(tenantId, periodType)));
  }

  @GetMapping("/tenants/{id}/quota-policy")
  public ResponseEntity<ApiResponse<SuperAdminUsageDtos.TenantQuotaPolicy>> getTenantQuotaPolicy(
      @PathVariable("id") Long tenantId) {
    SuperAdminTenantEntitlementsDto entitlements =
        entitlementService.getEffectiveEntitlements(tenantId);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant quota policy fetched",
            tenantUsageRollupService.getTenantQuotaPolicy(tenantId, entitlements.limits())));
  }

  @PostMapping("/tenants/{id}/quota-check")
  public ResponseEntity<ApiResponse<SuperAdminUsageDtos.QuotaActionResult>> checkTenantQuotaAction(
      @PathVariable("id") Long tenantId,
      @RequestBody SuperAdminUsageDtos.QuotaActionRequest request) {
    SuperAdminTenantEntitlementsDto entitlements =
        entitlementService.getEffectiveEntitlements(tenantId);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant quota action evaluated",
            tenantUsageRollupService.enforceQuotaAction(tenantId, request, entitlements.limits())));
  }

  @GetMapping("/tenants/{id}/seed-status")
  public ResponseEntity<ApiResponse<TenantSeedStatusDto>> getSeedStatus(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant default seed status fetched", controlPlaneService.getSeedStatus(tenantId)));
  }

  @PostMapping("/tenants/{id}/seed-status/repair")
  public ResponseEntity<ApiResponse<TenantSeedStatusDto>> repairSeedStatus(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant default seed repair completed",
            controlPlaneService.repairSeedStatus(tenantId)));
  }

  @DeleteMapping("/tenants/{id}/accounting-mappings/{mappingKey}")
  public ResponseEntity<ApiResponse<TenantSeedStatusDto>> deleteAccountingMapping(
      @PathVariable("id") Long tenantId, @PathVariable("mappingKey") String mappingKey) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant default accounting mapping deleted",
            controlPlaneService.rejectCoreMappingDelete(tenantId, mappingKey)));
  }

  @PutMapping("/tenants/{id}/accounting-mappings/{mappingKey}")
  public ResponseEntity<ApiResponse<TenantSeedStatusDto>> updateAccountingMapping(
      @PathVariable("id") Long tenantId,
      @PathVariable("mappingKey") String mappingKey,
      @Valid @RequestBody TenantSeedMappingUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant default accounting mapping updated",
            controlPlaneService.rejectCoreMappingRemap(tenantId, mappingKey, request.accountId())));
  }

  @PostMapping("/tenants/{id}/activation/send")
  public ResponseEntity<ApiResponse<SuperAdminActivationActionResponse>> sendActivation(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant activation sent", controlPlaneService.sendActivation(tenantId)));
  }

  @PostMapping("/tenants/{id}/activation/resend")
  public ResponseEntity<ApiResponse<SuperAdminActivationActionResponse>> resendActivation(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant activation resent", controlPlaneService.resendActivation(tenantId)));
  }

  @PostMapping("/tenants/{id}/activation/copy")
  public ResponseEntity<ApiResponse<SuperAdminActivationCopyResponse>> copyActivation(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant activation link copied", controlPlaneService.copyActivationLink(tenantId)));
  }

  @PostMapping("/tenants/{id}/activation/expire")
  public ResponseEntity<ApiResponse<SuperAdminActivationActionResponse>> expireActivation(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant activation expired", controlPlaneService.expireActivation(tenantId)));
  }

  @PutMapping("/tenants/{id}/lifecycle")
  public ResponseEntity<ApiResponse<CompanyLifecycleStateDto>> updateLifecycleState(
      @PathVariable("id") Long tenantId, @Valid @RequestBody CompanyLifecycleStateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant lifecycle state updated",
            controlPlaneService.updateLifecycleState(tenantId, request)));
  }

  @PutMapping("/tenants/{id}/limits")
  public ResponseEntity<ApiResponse<SuperAdminTenantLimitsDto>> updateTenantLimits(
      @PathVariable("id") Long tenantId, @Valid @RequestBody TenantLimitsUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant limits updated",
            controlPlaneService.updateLimits(
                tenantId,
                request.quotaMaxActiveUsers(),
                request.quotaMaxApiRequests(),
                request.quotaMaxStorageBytes(),
                request.quotaMaxConcurrentRequests(),
                request.burstRequestsPerMinute(),
                request.quotaSoftLimitEnabled(),
                request.quotaHardLimitEnabled())));
  }

  @PutMapping("/tenants/{id}/modules")
  public ResponseEntity<ApiResponse<CompanyEnabledModulesDto>> updateTenantModules(
      @PathVariable("id") Long tenantId, @Valid @RequestBody TenantModulesUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant modules updated",
            controlPlaneService.updateModules(tenantId, request.enabledModules())));
  }

  @GetMapping("/tenants/{id}/entitlements")
  public ResponseEntity<ApiResponse<SuperAdminTenantEntitlementsDto>> getTenantEntitlements(
      @PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant effective entitlements fetched",
            entitlementService.getEffectiveEntitlements(tenantId)));
  }

  @PutMapping("/tenants/{id}/plan")
  public ResponseEntity<ApiResponse<SuperAdminTenantEntitlementsDto>> assignTenantPlan(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminTenantPlanAssignmentRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant plan assigned", entitlementService.assignPlan(tenantId, request)));
  }

  @PutMapping("/tenants/{id}/entitlements/overrides")
  public ResponseEntity<ApiResponse<SuperAdminTenantEntitlementsDto>> upsertTenantOverrides(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody SuperAdminTenantEntitlementOverrideRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant entitlement overrides updated",
            entitlementService.putOverrides(tenantId, request)));
  }

  @DeleteMapping("/tenants/{id}/entitlements/overrides/{key}")
  public ResponseEntity<ApiResponse<SuperAdminTenantEntitlementsDto>> removeTenantOverride(
      @PathVariable("id") Long tenantId,
      @PathVariable("key") String key,
      @RequestBody(required = false) TenantEntitlementOverrideRemoveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant entitlement override removed",
            entitlementService.removeOverride(
                tenantId, key, request == null ? null : request.reason())));
  }

  @PostMapping("/tenants/{id}/support/warnings")
  public ResponseEntity<ApiResponse<CompanySupportWarningDto>> issueSupportWarning(
      @PathVariable("id") Long tenantId, @Valid @RequestBody TenantSupportWarningRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant warning issued",
            controlPlaneService.issueSupportWarning(
                tenantId,
                request.warningCategory(),
                request.message(),
                request.requestedLifecycleState(),
                request.gracePeriodHours())));
  }

  @Hidden
  @PostMapping("/tenants/{id}/support/admin-password-reset")
  public ResponseEntity<ApiResponse<Map<String, Object>>> retiredTenantAdminPasswordReset(
      @PathVariable("id") Long tenantId,
      @RequestBody(required = false) Object ignored,
      HttpServletRequest request) {
    return retiredRoute(
        "retired-superadmin-admin-password-reset",
        "Tenant admin credential reset is retired; use the V1 activation/password recovery flow",
        request,
        "/api/v1/superadmin/tenants/" + tenantId + "/support/admin-password-reset");
  }

  @PutMapping("/tenants/{id}/support/context")
  public ResponseEntity<ApiResponse<SuperAdminTenantSupportContextDto>> updateSupportContext(
      @PathVariable("id") Long tenantId,
      @Valid @RequestBody TenantSupportContextUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant support context updated",
            controlPlaneService.updateSupportContext(
                tenantId, request.supportNotes(), request.supportTags())));
  }

  @GetMapping("/tenants/{id}/review-intelligence")
  public ResponseEntity<ApiResponse<SuperAdminTenantReviewIntelligenceToggleDto>>
      getReviewIntelligenceToggle(@PathVariable("id") Long tenantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant review intelligence toggle fetched",
            controlPlaneService.getReviewIntelligenceToggle(tenantId)));
  }

  @PutMapping("/tenants/{id}/review-intelligence")
  public ResponseEntity<ApiResponse<SuperAdminTenantReviewIntelligenceToggleDto>>
      updateReviewIntelligenceToggle(
          @PathVariable("id") Long tenantId,
          @Valid @RequestBody TenantReviewIntelligenceToggleRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant review intelligence toggle updated",
            controlPlaneService.updateReviewIntelligenceToggle(tenantId, request.enabled())));
  }

  @PostMapping("/tenants/{id}/force-logout")
  public ResponseEntity<ApiResponse<SuperAdminTenantForceLogoutDto>> forceLogout(
      @PathVariable("id") Long tenantId,
      @RequestBody(required = false) TenantForceLogoutRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant sessions revoked",
            controlPlaneService.forceLogoutAllUsers(
                tenantId, request == null ? null : request.reason())));
  }

  @PutMapping("/tenants/{id}/admins/main")
  public ResponseEntity<ApiResponse<MainAdminSummaryDto>> replaceMainAdmin(
      @PathVariable("id") Long tenantId, @Valid @RequestBody TenantMainAdminUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant main admin replaced",
            controlPlaneService.replaceMainAdmin(tenantId, request.adminUserId())));
  }

  @PostMapping("/tenants/{id}/admins/{adminId}/email-change/request")
  public ResponseEntity<ApiResponse<SuperAdminTenantAdminEmailChangeRequestDto>>
      requestAdminEmailChange(
          @PathVariable("id") Long tenantId,
          @PathVariable("adminId") Long adminId,
          @Valid @RequestBody TenantAdminEmailChangeRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant admin email change requested",
            controlPlaneService.requestAdminEmailChange(tenantId, adminId, request.newEmail())));
  }

  @PostMapping("/tenants/{id}/admins/{adminId}/email-change/confirm")
  public ResponseEntity<ApiResponse<SuperAdminTenantAdminEmailChangeConfirmationDto>>
      confirmAdminEmailChange(
          @PathVariable("id") Long tenantId,
          @PathVariable("adminId") Long adminId,
          @Valid @RequestBody TenantAdminEmailChangeConfirmRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tenant admin email change confirmed",
            controlPlaneService.confirmAdminEmailChange(
                tenantId, adminId, request.requestId(), request.verificationToken())));
  }

  public record TenantModulesUpdateRequest(
      @NotNull Set<@NotBlank @Size(max = 64) String> enabledModules) {}

  public record TenantEntitlementOverrideRemoveRequest(
      @Size(max = 300, message = "reason must be at most 300 characters") String reason) {}

  public record TenantLimitsUpdateRequest(
      @Min(value = 0, message = "quotaMaxActiveUsers must be greater than or equal to 0")
          Long quotaMaxActiveUsers,
      @Min(value = 0, message = "quotaMaxApiRequests must be greater than or equal to 0")
          Long quotaMaxApiRequests,
      @Min(value = 0, message = "quotaMaxStorageBytes must be greater than or equal to 0")
          Long quotaMaxStorageBytes,
      @Min(value = 0, message = "quotaMaxConcurrentRequests must be greater than or equal to 0")
          Long quotaMaxConcurrentRequests,
      @Min(value = 0, message = "burstRequestsPerMinute must be greater than or equal to 0")
          Long burstRequestsPerMinute,
      Boolean quotaSoftLimitEnabled,
      Boolean quotaHardLimitEnabled) {}

  public record TenantSupportWarningRequest(
      @Size(max = 100, message = "warningCategory must be at most 100 characters")
          String warningCategory,
      @NotBlank @Size(max = 500, message = "message must be at most 500 characters") String message,
      @Size(max = 32, message = "requestedLifecycleState must be at most 32 characters")
          String requestedLifecycleState,
      @Min(value = 1, message = "gracePeriodHours must be at least 1") Integer gracePeriodHours) {}

  public record TenantAdminPasswordResetRequest(
      @Email @NotBlank String adminEmail,
      @Size(max = 300, message = "reason must be at most 300 characters") String reason) {}

  public record TenantSupportContextUpdateRequest(
      @Size(max = 4000, message = "supportNotes must be at most 4000 characters")
          String supportNotes,
      Set<@NotBlank @Size(max = 64) String> supportTags) {}

  public record TenantReviewIntelligenceToggleRequest(@NotNull Boolean enabled) {}

  public record TenantForceLogoutRequest(
      @Size(max = 300, message = "reason must be at most 300 characters") String reason) {}

  public record TenantMainAdminUpdateRequest(@NotNull Long adminUserId) {}

  public record TenantAdminEmailChangeRequest(@Email @NotBlank String newEmail) {}

  public record TenantAdminEmailChangeConfirmRequest(
      @NotNull Long requestId, @NotBlank @Size(max = 255) String verificationToken) {}

  private ResponseEntity<ApiResponse<Map<String, Object>>> retiredRoute(
      String code, String message, HttpServletRequest request, String fallbackPath) {
    return SuperAdminRetiredRouteErrors.gone(code, message, request, fallbackPath);
  }
}
