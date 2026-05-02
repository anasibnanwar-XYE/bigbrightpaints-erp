package com.bigbrightpaints.erp.modules.admin.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSettingsService;
import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPlatformSettingsDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPlatformSettingsUpdateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SystemSettingsDto;
import com.bigbrightpaints.erp.modules.admin.dto.SystemSettingsUpdateRequest;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin/settings")
public class AdminSettingsController {

  private static final String AUDIT_NOT_REQUESTED = "<not_requested>";
  private static final String AUDIT_REDACTED = "<redacted>";

  private final SystemSettingsService systemSettingsService;
  private final AuditService auditService;

  @Autowired
  public AdminSettingsController(
      SystemSettingsService systemSettingsService, AuditService auditService) {
    this.systemSettingsService = systemSettingsService;
    this.auditService = auditService;
  }

  @GetMapping
  @PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
  public ApiResponse<SuperAdminPlatformSettingsDto> getSettings() {
    return ApiResponse.success(
        "Settings fetched", toSuperAdminSettings(systemSettingsService.snapshot()));
  }

  @PutMapping
  @PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
  public ApiResponse<SuperAdminPlatformSettingsDto> updateSettings(
      @Valid @RequestBody SuperAdminPlatformSettingsUpdateRequest request) {
    SystemSettingsDto before = systemSettingsService.snapshot();
    SystemSettingsDto dto = systemSettingsService.update(toFlatRequest(request));
    recordSettingsUpdateAudit(before, request, dto);
    return ApiResponse.success("Settings updated", toSuperAdminSettings(dto));
  }

  private void recordSettingsUpdateAudit(
      SystemSettingsDto before,
      SuperAdminPlatformSettingsUpdateRequest request,
      SystemSettingsDto after) {
    if (auditService == null) {
      return;
    }
    Map<String, String> metadata = new java.util.LinkedHashMap<>();
    metadata.put("action", "admin_settings_update");
    if (before != null) {
      metadata.put("beforeAutoApprovalEnabled", Boolean.toString(before.autoApprovalEnabled()));
      metadata.put("beforePeriodLockEnforced", Boolean.toString(before.periodLockEnforced()));
      metadata.put(
          "beforeExportApprovalRequired", Boolean.toString(before.exportApprovalRequired()));
      metadata.put("beforePlatformAuthCode", AUDIT_REDACTED);
    }
    metadata.put(
        "requestedAutoApprovalEnabled",
        auditRequestedBoolean(
            request == null || request.workflow() == null
                ? null
                : request.workflow().autoApprovalEnabled()));
    metadata.put(
        "requestedPeriodLockEnforced",
        auditRequestedBoolean(
            request == null || request.workflow() == null
                ? null
                : request.workflow().periodLockEnforced()));
    metadata.put(
        "requestedExportApprovalRequired",
        auditRequestedBoolean(
            request == null || request.workflow() == null
                ? null
                : request.workflow().exportApprovalRequired()));
    metadata.put("requestedPlatformAuthCode", auditRequestedPlatformAuthCode());
    if (after != null) {
      metadata.put("afterAutoApprovalEnabled", Boolean.toString(after.autoApprovalEnabled()));
      metadata.put("afterPeriodLockEnforced", Boolean.toString(after.periodLockEnforced()));
      metadata.put("afterExportApprovalRequired", Boolean.toString(after.exportApprovalRequired()));
      metadata.put("afterPlatformAuthCode", AUDIT_REDACTED);
    }
    String actor = SecurityActorResolver.resolveActorWithSystemProcessFallback();
    if (actor != null && !actor.isBlank()) {
      metadata.put("actor", actor);
    }
    auditService.logAuthSuccess(AuditEvent.CONFIGURATION_CHANGED, actor, null, metadata);
  }

  private String auditRequestedBoolean(Boolean value) {
    return value == null ? AUDIT_NOT_REQUESTED : value.toString();
  }

  private String auditRequestedPlatformAuthCode() {
    return AUDIT_REDACTED;
  }

  private SuperAdminPlatformSettingsDto toSuperAdminSettings(SystemSettingsDto settings) {
    return new SuperAdminPlatformSettingsDto(
        new SuperAdminPlatformSettingsDto.Access(
            settings.allowedOrigins(),
            new SuperAdminPlatformSettingsDto.AuthCode(
                settings.platformAuthCode() != null && !settings.platformAuthCode().isBlank(),
                AUDIT_REDACTED,
                "Changing the platform auth code updates platform-scoped login and rejects old"
                    + " platform tokens; tenant login scopes are unaffected.")),
        new SuperAdminPlatformSettingsDto.Mail(
            settings.mailEnabled(),
            settings.mailFromAddress(),
            settings.mailBaseUrl(),
            settings.sendCredentials(),
            settings.sendPasswordReset()),
        new SuperAdminPlatformSettingsDto.Workflow(
            settings.autoApprovalEnabled(),
            settings.periodLockEnforced(),
            settings.exportApprovalRequired()),
        new SuperAdminPlatformSettingsDto.Security(
            "secret values are never returned; configured values use <redacted>",
            "accepted settings mutations emit CONFIGURATION_CHANGED audit evidence"));
  }

  private SystemSettingsUpdateRequest toFlatRequest(
      SuperAdminPlatformSettingsUpdateRequest request) {
    if (request == null) {
      return new SystemSettingsUpdateRequest(
          null, null, null, null, null, null, null, null, null, null);
    }
    SuperAdminPlatformSettingsUpdateRequest.AccessUpdate access = request.access();
    SuperAdminPlatformSettingsUpdateRequest.MailUpdate mail = request.mail();
    SuperAdminPlatformSettingsUpdateRequest.WorkflowUpdate workflow = request.workflow();
    return new SystemSettingsUpdateRequest(
        access == null ? null : access.allowedOrigins(),
        workflow == null ? null : workflow.autoApprovalEnabled(),
        workflow == null ? null : workflow.periodLockEnforced(),
        workflow == null ? null : workflow.exportApprovalRequired(),
        access == null ? null : access.platformAuthCode(),
        mail == null ? null : mail.enabled(),
        mail == null ? null : mail.fromAddress(),
        mail == null ? null : mail.baseUrl(),
        mail == null ? null : mail.sendCredentials(),
        mail == null ? null : mail.sendPasswordReset());
  }
}
