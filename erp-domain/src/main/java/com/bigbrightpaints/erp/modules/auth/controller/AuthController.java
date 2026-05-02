package com.bigbrightpaints.erp.modules.auth.controller;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.AuthService;
import com.bigbrightpaints.erp.modules.auth.service.AuthSessionService;
import com.bigbrightpaints.erp.modules.auth.service.IamCanonicalStorageService;
import com.bigbrightpaints.erp.modules.auth.service.PasswordResetService;
import com.bigbrightpaints.erp.modules.auth.service.PasswordService;
import com.bigbrightpaints.erp.modules.auth.web.AuthResponse;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;
import com.bigbrightpaints.erp.modules.auth.web.ForgotPasswordRequest;
import com.bigbrightpaints.erp.modules.auth.web.LoginRequest;
import com.bigbrightpaints.erp.modules.auth.web.MeResponse;
import com.bigbrightpaints.erp.modules.auth.web.RefreshTokenRequest;
import com.bigbrightpaints.erp.modules.auth.web.ResetPasswordRequest;
import com.bigbrightpaints.erp.modules.company.dto.ActivationCompleteRequest;
import com.bigbrightpaints.erp.modules.company.dto.ActivationCompleteResponse;
import com.bigbrightpaints.erp.modules.company.dto.ActivationVerifyResponse;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;
  private final PasswordService passwordService;
  private final PasswordResetService passwordResetService;
  private final SuperAdminTenantControlPlaneService tenantControlPlaneService;
  private final AuditService auditService;
  private final UserAccountRepository userAccountRepository;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final AuthSessionService authSessionService;

  public AuthController(
      AuthService authService,
      PasswordService passwordService,
      PasswordResetService passwordResetService,
      SuperAdminTenantControlPlaneService tenantControlPlaneService,
      AuditService auditService,
      UserAccountRepository userAccountRepository,
      IamCanonicalStorageService iamCanonicalStorageService,
      AuthSessionService authSessionService) {
    this.authService = authService;
    this.passwordService = passwordService;
    this.passwordResetService = passwordResetService;
    this.tenantControlPlaneService = tenantControlPlaneService;
    this.auditService = auditService;
    this.userAccountRepository = userAccountRepository;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.authSessionService = authSessionService;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        authService.login(request, authSessionService.metadataFrom(httpRequest)));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
    return ResponseEntity.ok(
        authService.refresh(request, authSessionService.metadataFrom(httpRequest)));
  }

  @PostMapping("/logout")
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> logout(
      @RequestBody(required = false) LogoutRequest request,
      Authentication authentication,
      HttpServletRequest httpRequest) {
    if (httpRequest != null && httpRequest.getParameterMap().containsKey("refreshToken")) {
      return ResponseEntity.badRequest().build();
    }
    authService.logout(
        request == null ? null : request.refreshToken(), accessToken(authentication));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<MeResponse>> me(
      @AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (!StringUtils.hasText(companyCode)) {
      companyCode = principal.getUser().getAuthScopeCode();
    }
    List<String> roles =
        principal.getUser().getRoles().stream().map(role -> role.getName()).sorted().toList();
    List<String> permissions =
        principal.getUser().getRoles().stream()
            .flatMap(role -> role.getPermissions().stream())
            .map(permission -> permission.getCode())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    MeResponse payload =
        new MeResponse(
            principal.getUsername(),
            principal.getUser().getDisplayName(),
            companyCode,
            authService.scopeType(companyCode),
            principal.getUser().isMfaEnabled(),
            principal.getUser().isMustChangePassword(),
            roles,
            permissions);
    return ResponseEntity.ok(ApiResponse.success(payload));
  }

  @PatchMapping("/me/profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<SelfProfileResponse>> updateProfile(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody(required = false) SelfProfileRequest request) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    UserAccount user = principal.getUser();
    if (request != null) {
      String beforePreferredName = user.getPreferredName();
      String beforeProfilePictureUrl = user.getProfilePictureUrl();
      user.setPreferredName(trimToNull(request.preferredName()));
      user.setProfilePictureUrl(trimToNull(request.profilePictureUrl()));
      userAccountRepository.save(user);
      iamCanonicalStorageService.syncUser(user);
      auditSelfAccountChange(
          AuditEvent.DATA_UPDATE,
          user,
          "self_profile_update",
          changedFields(
              Map.of(
                  "preferredName",
                  new FieldChange(beforePreferredName, user.getPreferredName()),
                  "profilePictureUrl",
                  new FieldChange(beforeProfilePictureUrl, user.getProfilePictureUrl()))));
    }
    return ResponseEntity.ok(ApiResponse.success(toProfileResponse(user)));
  }

  @PatchMapping("/me/contact")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<SelfContactResponse>> updateContact(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody(required = false) SelfContactRequest request) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    UserAccount user = principal.getUser();
    if (request != null) {
      String beforeSecondaryEmail = user.getSecondaryEmail();
      String beforePhoneSecondary = user.getPhoneSecondary();
      user.setSecondaryEmail(normalizeNullableEmail(request.secondaryEmail()));
      user.setPhoneSecondary(trimToNull(request.phoneSecondary()));
      userAccountRepository.save(user);
      iamCanonicalStorageService.syncUser(user);
      auditSelfAccountChange(
          AuditEvent.DATA_UPDATE,
          user,
          "self_contact_update",
          changedFields(
              Map.of(
                  "secondaryEmail",
                  new FieldChange(beforeSecondaryEmail, user.getSecondaryEmail()),
                  "phoneSecondary",
                  new FieldChange(beforePhoneSecondary, user.getPhoneSecondary()))));
    }
    return ResponseEntity.ok(ApiResponse.success(toContactResponse(user)));
  }

  @GetMapping("/me/security")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<SelfSecuritySummaryResponse>> securitySummary(
      @AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    UserAccount user = principal.getUser();
    return ResponseEntity.ok(
        ApiResponse.success(
            new SelfSecuritySummaryResponse(
                user.isMfaEnabled(),
                user.isMustChangePassword(),
                user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()),
                authSessionService.countActiveSessions(user))));
  }

  @GetMapping("/me/security-events")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> securityEvents(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) Integer limit) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    int requestedSize = boundedSecurityEventSize(size, limit);
    return ResponseEntity.ok(
        ApiResponse.success(
            iamCanonicalStorageService.listSelfSecurityEvents(
                principal.getUser(), type, page, requestedSize)));
  }

  private int boundedSecurityEventSize(Integer size, Integer limit) {
    int requestedSize = size != null ? size : (limit != null ? limit : 50);
    return Math.max(1, Math.min(requestedSize, 100));
  }

  @GetMapping("/sessions")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> sessions(
      @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    return ResponseEntity.ok(
        ApiResponse.success(
            authSessionService.listActiveSessions(
                principal.getUser(),
                authSessionService.currentSessionIdFromClaims(claims(request)))));
  }

  @DeleteMapping("/sessions/{sessionId}")
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> revokeOtherSession(
      @AuthenticationPrincipal UserPrincipal principal, @PathVariable String sessionId) {
    if (principal == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    boolean revoked =
        authSessionService.revokeSession(principal.getUser(), sessionId, "self_revoke");
    if (revoked) {
      Map<String, String> metadata = new java.util.LinkedHashMap<>();
      metadata.put("operation", "self_session_revoke");
      metadata.put("reason", "self_revoke");
      metadata.put("sessionId", sessionId);
      metadata.put("targetUserId", String.valueOf(principal.getUser().getId()));
      iamCanonicalStorageService.recordSecurityEvent(
          "SESSION_REVOKED",
          "SUCCESS",
          metadata,
          principal.getUser().getPublicId().toString(),
          principal.getUsername(),
          principal.getUser().getCompany() == null
              ? null
              : principal.getUser().getCompany().getId(),
          principal.getUser().getAuthScopeCode());
    }
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/sessions/current")
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> revokeCurrentSession(Authentication authentication) {
    authService.logout(null, accessToken(authentication));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/sessions")
  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<Void> revokeAllSessions(Authentication authentication) {
    authService.revokeAllSessionsForAccessToken(accessToken(authentication), "self_revoke_all");
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password/change")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<String>> changePassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody ChangePasswordRequest request) {
    if (principal == null) {
      return ResponseEntity.status(401).body(ApiResponse.failure("Unauthenticated"));
    }
    passwordService.changePassword(principal.getUser(), request);
    String companyCode =
        StringUtils.hasText(CompanyContextHolder.getCompanyCode())
            ? CompanyContextHolder.getCompanyCode()
            : principal.getUser().getAuthScopeCode();
    Map<String, String> metadata = new java.util.LinkedHashMap<>();
    metadata.put("operation", "password_change");
    if (StringUtils.hasText(companyCode)) {
      metadata.put("companyCode", companyCode);
    }
    metadata.put("outcome", "password_updated");
    auditService.logAuthSuccess(
        AuditEvent.PASSWORD_CHANGED, principal.getUsername(), companyCode, metadata);
    return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "OK"));
  }

  @PostMapping("/password/forgot")
  public ResponseEntity<ApiResponse<String>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {
    passwordResetService.requestReset(request.email(), request.companyCode());
    return ResponseEntity.ok(
        ApiResponse.success("If the email exists, a reset link has been sent", "OK"));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<ApiResponse<String>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {
    passwordResetService.resetPassword(
        request.token(), request.newPassword(), request.confirmPassword());
    return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", "OK"));
  }

  @GetMapping("/activation/verify")
  public ResponseEntity<ApiResponse<ActivationVerifyResponse>> verifyActivation(
      @RequestParam("token") String token) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Activation token verified", tenantControlPlaneService.verifyActivation(token)));
  }

  @PostMapping("/activation/complete")
  public ResponseEntity<ApiResponse<ActivationCompleteResponse>> completeActivation(
      @Valid @RequestBody ActivationCompleteRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Activation completed", tenantControlPlaneService.completeActivation(request)));
  }

  private String accessToken(Authentication authentication) {
    if (authentication == null) {
      return null;
    }
    Object credentials = authentication.getCredentials();
    if (credentials instanceof String token && !token.isBlank()) {
      return token;
    }
    return null;
  }

  private Claims claims(HttpServletRequest request) {
    if (request == null) {
      return null;
    }
    Object claims = request.getAttribute("jwtClaims");
    return claims instanceof Claims jwtClaims ? jwtClaims : null;
  }

  private SelfProfileResponse toProfileResponse(UserAccount user) {
    return new SelfProfileResponse(user.getPreferredName(), user.getProfilePictureUrl());
  }

  private SelfContactResponse toContactResponse(UserAccount user) {
    return new SelfContactResponse(
        user.getSecondaryEmail(), user.getPhoneSecondary(), false, false);
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String normalizeNullableEmail(String value) {
    String trimmed = trimToNull(value);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private void auditSelfAccountChange(
      AuditEvent event, UserAccount user, String operation, List<String> changedFields) {
    if (changedFields.isEmpty()) {
      return;
    }
    String companyCode =
        StringUtils.hasText(CompanyContextHolder.getCompanyCode())
            ? CompanyContextHolder.getCompanyCode()
            : user.getAuthScopeCode();
    Map<String, String> metadata = new java.util.LinkedHashMap<>();
    metadata.put("operation", operation);
    metadata.put("outcome", "updated");
    metadata.put("changedFields", String.join(",", changedFields));
    if (user.getPublicId() != null) {
      metadata.put("actorPublicId", user.getPublicId().toString());
    }
    if (StringUtils.hasText(companyCode)) {
      metadata.put("companyCode", companyCode);
    }
    auditService.logAuthSuccess(event, user.getEmail(), companyCode, metadata);
  }

  private List<String> changedFields(Map<String, FieldChange> fields) {
    return fields.entrySet().stream()
        .filter(
            entry -> !java.util.Objects.equals(entry.getValue().before(), entry.getValue().after()))
        .map(Map.Entry::getKey)
        .sorted()
        .toList();
  }

  public record LogoutRequest(String refreshToken) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record SelfProfileRequest(
      @Size(max = 120) String preferredName,
      @Size(max = 512)
          @Pattern(regexp = "^$|https?://[^\\s]+", message = "must be a valid http(s) URL")
          String profilePictureUrl) {}

  public record SelfProfileResponse(String preferredName, String profilePictureUrl) {}

  @JsonIgnoreProperties(ignoreUnknown = false)
  public record SelfContactRequest(
      @Size(max = 255) @Email String secondaryEmail,
      @Size(max = 64)
          @Pattern(regexp = "^$|[+0-9][0-9 .()\\-]{5,63}", message = "must be a valid phone number")
          String phoneSecondary) {}

  public record SelfContactResponse(
      String secondaryEmail,
      String phoneSecondary,
      boolean secondaryEmailVerified,
      boolean phoneSecondaryVerified) {}

  public record SelfSecuritySummaryResponse(
      boolean mfaEnabled, boolean mustChangePassword, boolean locked, int activeSessionCount) {}

  private record FieldChange(String before, String after) {}
}
