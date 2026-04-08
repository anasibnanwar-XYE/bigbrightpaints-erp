package com.bigbrightpaints.erp.modules.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.MfaService;
import com.bigbrightpaints.erp.modules.auth.service.MfaService.MfaEnrollment;
import com.bigbrightpaints.erp.modules.auth.web.MfaActivateRequest;
import com.bigbrightpaints.erp.modules.auth.web.MfaDisableRequest;
import com.bigbrightpaints.erp.modules.auth.web.MfaSetupResponse;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth/mfa")
@PreAuthorize("isAuthenticated()")
public class MfaController {

  private final MfaService mfaService;
  private final AuditService auditService;

  public MfaController(MfaService mfaService, AuditService auditService) {
    this.mfaService = mfaService;
    this.auditService = auditService;
  }

  @PostMapping("/setup")
  public ResponseEntity<ApiResponse<MfaSetupResponse>> setup(
      @AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    MfaEnrollment enrollment = mfaService.beginEnrollment(principal.getUser());
    auditService.logAuthSuccess(
        AuditEvent.MFA_ENROLLED,
        principal.getUsername(),
        resolveCompanyCode(principal),
        auditMetadata("mfa_enrollment_started"));
    MfaSetupResponse payload =
        new MfaSetupResponse(enrollment.secret(), enrollment.qrUri(), enrollment.recoveryCodes());
    return ResponseEntity.ok(ApiResponse.success("MFA enrollment started", payload));
  }

  @PostMapping("/activate")
  public ResponseEntity<ApiResponse<Map<String, Object>>> activate(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody MfaActivateRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    mfaService.activate(principal.getUser(), request.code());
    auditService.logAuthSuccess(
        AuditEvent.MFA_ACTIVATED,
        principal.getUsername(),
        resolveCompanyCode(principal),
        auditMetadata("mfa_enabled"));
    return ResponseEntity.ok(ApiResponse.success("MFA enabled", Map.of("enabled", true)));
  }

  @PostMapping("/disable")
  public ResponseEntity<ApiResponse<Map<String, Object>>> disable(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody MfaDisableRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    mfaService.disable(principal.getUser(), request.code(), request.recoveryCode());
    auditService.logAuthSuccess(
        AuditEvent.MFA_DISABLED,
        principal.getUsername(),
        resolveCompanyCode(principal),
        auditMetadata("mfa_disabled"));
    return ResponseEntity.ok(ApiResponse.success("MFA disabled", Map.of("enabled", false)));
  }

  private String resolveCompanyCode(UserPrincipal principal) {
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (org.springframework.util.StringUtils.hasText(companyCode)) {
      return companyCode;
    }
    if (principal == null || principal.getUser() == null) {
      return null;
    }
    return principal.getUser().getAuthScopeCode();
  }

  private Map<String, String> auditMetadata(String outcome) {
    Map<String, String> metadata = new java.util.LinkedHashMap<>();
    metadata.put("operation", "mfa_profile_change");
    if (outcome != null) {
      metadata.put("outcome", outcome);
    }
    return metadata;
  }
}
