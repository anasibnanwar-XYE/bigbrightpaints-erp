package com.bigbrightpaints.erp.modules.auth.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.bigbrightpaints.erp.modules.auth.web.MfaRecoveryCodesResponse;
import com.bigbrightpaints.erp.modules.auth.web.MfaSetupResponse;
import com.bigbrightpaints.erp.modules.auth.web.MfaStatusResponse;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
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
    try {
      MfaEnrollment enrollment = mfaService.beginEnrollment(principal.getUser());
      auditService.logAuthSuccess(
          AuditEvent.MFA_ENROLLED,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_enrollment_started"));
      MfaSetupResponse payload =
          new MfaSetupResponse(enrollment.secret(), enrollment.qrUri(), enrollment.recoveryCodes());
      return ResponseEntity.ok(ApiResponse.success("MFA enrollment started", payload));
    } catch (RuntimeException ex) {
      auditService.logAuthFailure(
          AuditEvent.MFA_FAILURE,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_enrollment_denied"));
      throw ex;
    }
  }

  @GetMapping
  public ResponseEntity<ApiResponse<MfaStatusResponse>> status(
      @AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    return ResponseEntity.ok(
        ApiResponse.success(new MfaStatusResponse(principal.getUser().isMfaEnabled())));
  }

  @PostMapping("/activate")
  public ResponseEntity<ApiResponse<MfaStatusResponse>> activate(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody MfaActivateRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    try {
      mfaService.activate(principal.getUser(), request.code());
      auditService.logAuthSuccess(
          AuditEvent.MFA_ACTIVATED,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_enabled"));
      return ResponseEntity.ok(ApiResponse.success("MFA enabled", new MfaStatusResponse(true)));
    } catch (RuntimeException ex) {
      auditService.logAuthFailure(
          AuditEvent.MFA_FAILURE,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_activation_failed"));
      throw ex;
    }
  }

  @PostMapping("/disable")
  public ResponseEntity<ApiResponse<MfaStatusResponse>> disable(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody MfaDisableRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    try {
      mfaService.disable(principal.getUser(), request.code(), request.recoveryCode());
      auditService.logAuthSuccess(
          AuditEvent.MFA_DISABLED,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_disabled"));
      return ResponseEntity.ok(ApiResponse.success("MFA disabled", new MfaStatusResponse(false)));
    } catch (RuntimeException ex) {
      auditService.logAuthFailure(
          AuditEvent.MFA_FAILURE,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_disable_failed"));
      throw ex;
    }
  }

  @PostMapping("/recovery-codes/regenerate")
  public ResponseEntity<ApiResponse<MfaRecoveryCodesResponse>> regenerateRecoveryCodes(
      @AuthenticationPrincipal UserPrincipal principal,
      HttpServletRequest servletRequest,
      @Valid @RequestBody MfaDisableRequest request) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(ApiResponse.failure("Unauthenticated"));
    }
    try {
      var recoveryCodes =
          mfaService.regenerateRecoveryCodes(
              principal.getUser(),
              request == null ? null : request.code(),
              request == null ? null : request.recoveryCode(),
              resolveTokenIssuedAt(servletRequest));
      auditService.logAuthSuccess(
          AuditEvent.MFA_RECOVERY_CODE_USED,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_recovery_codes_regenerated"));
      return ResponseEntity.ok(
          ApiResponse.success(
              "MFA recovery codes regenerated", new MfaRecoveryCodesResponse(true, recoveryCodes)));
    } catch (RuntimeException ex) {
      auditService.logAuthFailure(
          AuditEvent.MFA_FAILURE,
          principal.getUsername(),
          resolveCompanyCode(principal),
          auditMetadata("mfa_recovery_codes_regeneration_failed"));
      throw ex;
    }
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

  private Instant resolveTokenIssuedAt(HttpServletRequest request) {
    if (request == null || !(request.getAttribute("jwtClaims") instanceof Claims claims)) {
      return null;
    }
    Number issuedAtMillis = claims.get("iatMs", Number.class);
    if (issuedAtMillis != null) {
      return Instant.ofEpochMilli(issuedAtMillis.longValue());
    }
    return claims.getIssuedAt() != null ? claims.getIssuedAt().toInstant() : null;
  }
}
