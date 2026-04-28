package com.bigbrightpaints.erp.modules.auth.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.security.AuthScopeService;
import com.bigbrightpaints.erp.core.security.JwtProperties;
import com.bigbrightpaints.erp.core.security.JwtTokenService;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.exception.InvalidMfaException;
import com.bigbrightpaints.erp.modules.auth.exception.MfaRequiredException;
import com.bigbrightpaints.erp.modules.auth.web.AuthResponse;
import com.bigbrightpaints.erp.modules.auth.web.LoginRequest;
import com.bigbrightpaints.erp.modules.auth.web.RefreshTokenRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.TenantRuntimeRequestAdmissionService;

import io.jsonwebtoken.Claims;

@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

  private final JwtTokenService tokenService;
  private final RefreshTokenService refreshTokenService;
  private final UserAccountRepository userAccountRepository;
  private final CompanyRepository companyRepository;
  private final JwtProperties properties;
  private final MfaService mfaService;
  private final TokenBlacklistService tokenBlacklistService;
  private final AuditService auditService;
  private final TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService;
  private final PasswordEncoder passwordEncoder;
  private final AuthScopeService authScopeService;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final AccountLockoutService accountLockoutService;
  private final AuthSessionService authSessionService;
  private final PasswordPolicy passwordPolicy;

  public AuthService(
      JwtTokenService tokenService,
      RefreshTokenService refreshTokenService,
      UserAccountRepository userAccountRepository,
      CompanyRepository companyRepository,
      JwtProperties properties,
      MfaService mfaService,
      TokenBlacklistService tokenBlacklistService,
      AuditService auditService,
      TenantRuntimeRequestAdmissionService tenantRuntimeRequestAdmissionService,
      PasswordEncoder passwordEncoder,
      AuthScopeService authScopeService,
      IamCanonicalStorageService iamCanonicalStorageService,
      AccountLockoutService accountLockoutService,
      AuthSessionService authSessionService,
      PasswordPolicy passwordPolicy) {
    this.tokenService = tokenService;
    this.refreshTokenService = refreshTokenService;
    this.userAccountRepository = userAccountRepository;
    this.companyRepository = companyRepository;
    this.properties = properties;
    this.mfaService = mfaService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.auditService = auditService;
    this.tenantRuntimeRequestAdmissionService = tenantRuntimeRequestAdmissionService;
    this.passwordEncoder = passwordEncoder;
    this.authScopeService = authScopeService;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.accountLockoutService = accountLockoutService;
    this.authSessionService = authSessionService;
    this.passwordPolicy = passwordPolicy;
  }

  public AuthResponse login(LoginRequest request) {
    return login(request, null);
  }

  public AuthResponse login(LoginRequest request, SessionDeviceMetadata deviceMetadata) {
    UserAccount user = null;
    boolean failedSecretValidation = false;
    try {
      String scopeCode = authScopeService.requireScopeCode(request.companyCode());
      user = requireScopedAccount(request.email(), scopeCode);
      ensureEnabledForLogin(user);
      accountLockoutService.enforceUnlocked(user);
      if (!passwordEncoder.matches(
          passwordPolicy.normalize(request.password()), user.getPasswordHash())) {
        failedSecretValidation = true;
        accountLockoutService.recordFailure(user);
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
            "Invalid credentials");
      }
      Company company = resolveCompanyForScope(user, scopeCode);
      if (company != null) {
        tenantRuntimeRequestAdmissionService.enforceAuthOperationAllowed(
            company.getCode(), user.getEmail(), "LOGIN");
      }
      boolean mfaChallengeActive = user.isMfaEnabled();
      mfaService.verifyDuringLogin(user, request.mfaCode(), request.recoveryCode());
      if (mfaChallengeActive) {
        Map<String, String> mfaMetadata = new HashMap<>();
        mfaMetadata.put("operation", "mfa_login_verification");
        mfaMetadata.put("companyCode", scopeCode);
        if (user.getPublicId() != null) {
          mfaMetadata.put("actorPublicId", user.getPublicId().toString());
        }
        auditService.logAuthSuccess(
            AuditEvent.MFA_SUCCESS, user.getEmail(), scopeCode, mfaMetadata);
      }
      accountLockoutService.resetFailures(user);
      Map<String, String> successMetadata = new HashMap<>();
      successMetadata.put("companyCode", scopeCode);
      if (user.getPublicId() != null) {
        successMetadata.put("actorPublicId", user.getPublicId().toString());
      }
      auditService.logAuthSuccess(
          AuditEvent.LOGIN_SUCCESS, user.getEmail(), scopeCode, successMetadata);
      Map<String, Object> claims = new HashMap<>();
      claims.put("name", user.getDisplayName());
      claims.put("email", user.getEmail());
      claims.put("mustChangePassword", user.isMustChangePassword());
      Instant issuedAt = Instant.now();
      RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
          refreshTokenService.issueSession(
              user.getPublicId(),
              scopeCode,
              issuedAt,
              issuedAt.plusSeconds(properties.getRefreshTokenTtlSeconds()),
              deviceMetadata,
              null);
      claims.put("sid", issuedRefreshToken.sessionPublicId().toString());
      String accessToken =
          tokenService.generateAccessToken(
              user.getPublicId().toString(), scopeCode, claims, issuedAt);
      return new AuthResponse(
          "Bearer",
          accessToken,
          issuedRefreshToken.refreshToken(),
          properties.getAccessTokenTtlSeconds(),
          scopeCode,
          scopeType(scopeCode),
          user.getDisplayName(),
          user.isMustChangePassword(),
          roleNames(user));
    } catch (RuntimeException ex) {
      if (user != null && isMfaFailure(ex) && !failedSecretValidation) {
        accountLockoutService.recordFailure(user);
        Map<String, String> mfaFailureMetadata = new HashMap<>();
        mfaFailureMetadata.put("operation", "mfa_login_verification");
        mfaFailureMetadata.put("reason", "invalid_or_missing_mfa_verifier");
        if (user.getPublicId() != null) {
          mfaFailureMetadata.put("actorPublicId", user.getPublicId().toString());
        }
        auditService.logAuthFailure(
            AuditEvent.MFA_FAILURE, user.getEmail(), user.getAuthScopeCode(), mfaFailureMetadata);
      }
      String reason = ex.getMessage();
      if (reason == null || reason.isBlank()) {
        reason = "Login failed";
      }
      Map<String, String> failureMetadata = new HashMap<>();
      failureMetadata.put("reason", reason);
      if (user != null && user.getPublicId() != null) {
        failureMetadata.put("actorPublicId", user.getPublicId().toString());
      }
      auditService.logAuthFailure(
          AuditEvent.LOGIN_FAILURE,
          normalizeAuditIdentifier(request.email()),
          normalizeAuditIdentifier(request.companyCode()),
          failureMetadata);
      throw ex;
    }
  }

  public AuthResponse refresh(RefreshTokenRequest request) {
    return refresh(request, null);
  }

  @Transactional(noRollbackFor = ApplicationException.class)
  public AuthResponse refresh(RefreshTokenRequest request, SessionDeviceMetadata deviceMetadata) {
    String requestedScopeCode = authScopeService.requireScopeCode(request.companyCode());
    RefreshTokenService.TokenRecord inspectedRecord =
        refreshTokenService
            .inspect(request.refreshToken(), requestedScopeCode)
            .orElseGet(
                () -> {
                  refreshTokenService.consume(request.refreshToken(), requestedScopeCode);
                  throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                      "Invalid refresh token");
                });
    String accountKey = inspectedRecord.userPublicId().toString();
    if (tokenBlacklistService.isUserTokenRevoked(accountKey, inspectedRecord.issuedAt())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Refresh token revoked");
    }
    UserAccount user =
        userAccountRepository
            .findByPublicId(inspectedRecord.userPublicId())
            .orElseThrow(
                () ->
                    com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                        "User not found"));
    ensureEnabledForAuthentication(user);
    accountLockoutService.enforceUnlocked(user);
    if (user.isMustChangePassword()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Refresh token revoked");
    }
    Company company = resolveCompanyForScope(user, requestedScopeCode);
    if (company != null) {
      tenantRuntimeRequestAdmissionService.enforceAuthOperationAllowed(
          company.getCode(), user.getEmail(), "REFRESH_TOKEN");
    }
    RefreshTokenService.TokenRecord record =
        refreshTokenService
            .consume(request.refreshToken(), requestedScopeCode)
            .orElseThrow(
                () ->
                    com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                        "Invalid refresh token"));
    Map<String, Object> claims = new HashMap<>();
    claims.put("name", user.getDisplayName());
    claims.put("email", user.getEmail());
    claims.put("mustChangePassword", user.isMustChangePassword());
    Instant issuedAt = Instant.now();
    RefreshTokenService.IssuedRefreshToken issuedRefreshToken =
        refreshTokenService.issueSession(
            user.getPublicId(),
            requestedScopeCode,
            issuedAt,
            issuedAt.plusSeconds(properties.getRefreshTokenTtlSeconds()),
            deviceMetadata,
            record.refreshTokenDigest());
    auditSessionEvent(
        AuditEvent.TOKEN_REFRESH,
        user,
        requestedScopeCode,
        Map.of(
            "operation",
            "refresh_rotation",
            "sessionId",
            issuedRefreshToken.sessionPublicId().toString()));
    claims.put("sid", issuedRefreshToken.sessionPublicId().toString());
    String accessToken =
        tokenService.generateAccessToken(
            user.getPublicId().toString(), requestedScopeCode, claims, issuedAt);
    return new AuthResponse(
        "Bearer",
        accessToken,
        issuedRefreshToken.refreshToken(),
        properties.getAccessTokenTtlSeconds(),
        requestedScopeCode,
        scopeType(requestedScopeCode),
        user.getDisplayName(),
        user.isMustChangePassword(),
        roleNames(user));
  }

  public String scopeType(String scopeCode) {
    return authScopeService.isPlatformScope(scopeCode) ? "PLATFORM" : "TENANT";
  }

  private Company resolveCompanyForScope(UserAccount user, String scopeCode) {
    if (user == null || !scopeMatches(user, scopeCode)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Invalid credentials");
    }
    if (authScopeService.isPlatformScope(scopeCode)) {
      if (!hasSuperAdminRole(user)) {
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
            "Invalid credentials");
      }
      return null;
    }
    Company company = user.getCompany();
    if (company == null || !user.belongsToCompanyCode(scopeCode)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Invalid credentials");
    }
    return company;
  }

  private boolean hasSuperAdminRole(UserAccount user) {
    return user.getRoles().stream()
        .anyMatch(role -> SUPER_ADMIN_ROLE.equalsIgnoreCase(role.getName()));
  }

  private java.util.List<String> roleNames(UserAccount user) {
    return user.getRoles().stream().map(role -> role.getName()).sorted().toList();
  }

  private boolean isMfaFailure(RuntimeException ex) {
    return ex instanceof InvalidMfaException || ex instanceof MfaRequiredException;
  }

  private String normalizeAuditIdentifier(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private void ensureEnabledForAuthentication(UserAccount user) {
    if (user == null || !user.isEnabled()) {
      throw new ApplicationException(
          ErrorCode.AUTH_ACCOUNT_DISABLED, ErrorCode.AUTH_ACCOUNT_DISABLED.getDefaultMessage());
    }
  }

  private void ensureEnabledForLogin(UserAccount user) {
    if (user == null || !user.isEnabled()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Invalid credentials");
    }
  }

  public void logout(String refreshToken, String accessToken) {
    Claims accessTokenClaims = parseLogoutClaims(accessToken);
    UUID tokenUserPublicId = extractTokenSubject(accessTokenClaims);
    UUID currentSessionId = authSessionService.currentSessionIdFromClaims(accessTokenClaims);
    String authScopeCode =
        accessTokenClaims == null ? null : accessTokenClaims.get("companyCode", String.class);

    if (tokenUserPublicId != null && currentSessionId != null) {
      if (refreshToken != null
          && !refreshToken.isBlank()
          && !authSessionService.refreshTokenBelongsToSession(
              refreshToken, tokenUserPublicId, currentSessionId, authScopeCode)) {
        log.info(
            "Ignoring stale refresh token during logout (actor={}, sessionId={})",
            tokenUserPublicId,
            currentSessionId);
      }
      authSessionService.revokeCurrentSession(tokenUserPublicId, currentSessionId, "logout");
    } else if (refreshToken != null && !refreshToken.isBlank()) {
      refreshTokenService.revoke(refreshToken);
    }

    blacklistAccessToken(accessTokenClaims, tokenUserPublicId);
    auditSessionEventByPublicId(
        AuditEvent.LOGOUT,
        tokenUserPublicId,
        authScopeCode,
        Map.of(
            "operation",
            "logout",
            "reason",
            "logout",
            "sessionId",
            currentSessionId == null ? "unknown" : currentSessionId.toString()));
  }

  public void revokeAllSessionsForAccessToken(String accessToken, String reason) {
    Claims accessTokenClaims = parseLogoutClaims(accessToken);
    UUID tokenUserPublicId = extractTokenSubject(accessTokenClaims);
    String authScopeCode =
        accessTokenClaims == null ? null : accessTokenClaims.get("companyCode", String.class);
    if (tokenUserPublicId != null) {
      revokeActiveSessions(tokenUserPublicId, reason);
    }
    blacklistAccessToken(accessTokenClaims, tokenUserPublicId);
    auditSessionEventByPublicId(
        AuditEvent.TOKEN_REVOKED,
        tokenUserPublicId,
        authScopeCode,
        Map.of(
            "operation",
            "self_revoke_all_sessions",
            "reason",
            reason == null ? "self_revoke_all" : reason));
  }

  private void revokeActiveSessions(UUID userPublicId, String reason) {
    if (userPublicId == null) {
      return;
    }
    String accountKey = userPublicId.toString();
    tokenBlacklistService.revokeAllUserTokens(accountKey);
    refreshTokenService.revokeAllForUser(userPublicId);
    iamCanonicalStorageService.markAllSessionsRevoked(userPublicId, reason);
  }

  private void auditSessionEventByPublicId(
      AuditEvent event, UUID userPublicId, String authScopeCode, Map<String, String> metadata) {
    if (userPublicId == null) {
      return;
    }
    userAccountRepository
        .findByPublicId(userPublicId)
        .ifPresent(user -> auditSessionEvent(event, user, authScopeCode, metadata));
  }

  private void auditSessionEvent(
      AuditEvent event, UserAccount user, String authScopeCode, Map<String, String> metadata) {
    if (user == null || user.getPublicId() == null) {
      return;
    }
    Map<String, String> auditMetadata = new HashMap<>();
    if (metadata != null) {
      auditMetadata.putAll(metadata);
    }
    auditMetadata.put("actorPublicId", user.getPublicId().toString());
    auditMetadata.put("targetUserId", String.valueOf(user.getId()));
    auditMetadata.put("companyCode", authScopeCode);
    auditService.logAuthSuccess(event, user.getEmail(), authScopeCode, auditMetadata);
  }

  private Claims parseLogoutClaims(String accessToken) {
    if (accessToken == null || accessToken.isBlank()) {
      return null;
    }

    try {
      return tokenService.parse(accessToken);
    } catch (Exception ex) {
      log.warn(
          "Failed to parse access token during logout; skipping token-derived identity operations",
          ex);
      return null;
    }
  }

  private UUID extractTokenSubject(Claims claims) {
    if (claims == null) {
      return null;
    }
    String subject = claims.getSubject();
    if (subject == null) {
      return null;
    }
    String normalized = subject.trim();
    if (normalized.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(normalized);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private void blacklistAccessToken(Claims claims, UUID userPublicId) {
    if (claims == null) {
      return;
    }

    String tokenId = claims.getId();
    if (tokenId == null || claims.getExpiration() == null) {
      return;
    }
    Instant expiration = claims.getExpiration().toInstant();

    try {
      tokenBlacklistService.blacklistToken(
          tokenId, expiration, userPublicId != null ? userPublicId.toString() : null, "logout");
    } catch (Exception ex) {
      String actor = SecurityActorResolver.resolveActorWithSystemProcessFallback();
      log.warn(
          "Failed to blacklist access token during logout (actor={}, tokenHash={}, expiresAt={})",
          actor,
          IdempotencyUtils.sha256Hex(tokenId, 12),
          expiration,
          ex);
    }
  }

  private UserAccount requireScopedAccount(String email, String scopeCode) {
    return userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(normalizeEmail(email), scopeCode)
        .orElseThrow(
            () ->
                com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Invalid credentials"));
  }

  private boolean scopeMatches(UserAccount user, String scopeCode) {
    return user != null
        && user.getAuthScopeCode() != null
        && user.getAuthScopeCode().equalsIgnoreCase(scopeCode);
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
