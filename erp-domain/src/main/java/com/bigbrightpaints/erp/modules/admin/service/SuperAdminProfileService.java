package com.bigbrightpaints.erp.modules.admin.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSetting;
import com.bigbrightpaints.erp.core.config.SystemSettingsRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminPasswordChangeResponseDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileSessionDto;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminProfileUpdateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SuperAdminSessionRevokeResponseDto;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.PasswordService;
import com.bigbrightpaints.erp.modules.auth.service.RefreshTokenService;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;

@Service
public class SuperAdminProfileService {

  private static final String PROFILE_SETTING_PREFIX = "profile.";
  private static final String TIMEZONE_SUFFIX = ".timezone";
  private static final String LANGUAGE_SUFFIX = ".language";
  private static final String DEFAULT_TIMEZONE = "UTC";
  private static final String DEFAULT_LANGUAGE = "en";

  private final UserAccountRepository userAccountRepository;
  private final SystemSettingsRepository settingsRepository;
  private final AuditLogRepository auditLogRepository;
  private final RefreshTokenService refreshTokenService;
  private final PasswordService passwordService;
  private final AuditService auditService;

  public SuperAdminProfileService(
      UserAccountRepository userAccountRepository,
      SystemSettingsRepository settingsRepository,
      AuditLogRepository auditLogRepository,
      RefreshTokenService refreshTokenService,
      PasswordService passwordService,
      AuditService auditService) {
    this.userAccountRepository = userAccountRepository;
    this.settingsRepository = settingsRepository;
    this.auditLogRepository = auditLogRepository;
    this.refreshTokenService = refreshTokenService;
    this.passwordService = passwordService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public SuperAdminProfileDto profile(UserPrincipal principal) {
    UserAccount user = requireSuperAdmin(principal);
    return toProfile(user);
  }

  @Transactional
  public SuperAdminProfileDto updateProfile(
      UserPrincipal principal, SuperAdminProfileUpdateRequest request) {
    UserAccount user = requireSuperAdmin(principal);
    if (request == null || isEmptyUpdate(request)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Profile update payload is required");
    }
    if (request.displayName() != null) {
      user.setDisplayName(normalizeRequiredText(request.displayName(), "displayName", 120));
    }
    if (request.phone() != null) {
      user.setPhoneSecondary(normalizeOptionalText(request.phone(), 64));
    }
    if (request.avatarUrl() != null) {
      user.setProfilePictureUrl(normalizeOptionalText(request.avatarUrl(), 500));
    }
    if (request.timezone() != null) {
      settingsRepository.save(
          new SystemSetting(
              timezoneKey(user), normalizeRequiredText(request.timezone(), "timezone", 64)));
    }
    if (request.language() != null) {
      settingsRepository.save(
          new SystemSetting(
              languageKey(user), normalizeRequiredText(request.language(), "language", 16)));
    }
    userAccountRepository.save(user);
    recordAudit(
        AuditEvent.CONFIGURATION_CHANGED,
        user,
        "superadmin-profile-updated",
        Map.of(
            "updatedDisplayName", Boolean.toString(request.displayName() != null),
            "updatedPhone", Boolean.toString(request.phone() != null),
            "updatedAvatar", Boolean.toString(request.avatarUrl() != null),
            "updatedTimezone", Boolean.toString(request.timezone() != null),
            "updatedLanguage", Boolean.toString(request.language() != null)));
    return toProfile(user);
  }

  @Transactional
  public SuperAdminPasswordChangeResponseDto changePassword(
      UserPrincipal principal, ChangePasswordRequest request) {
    UserAccount user = requireSuperAdmin(principal);
    passwordService.changePassword(user, request);
    Instant changedAt = Instant.now();
    recordAudit(
        AuditEvent.PASSWORD_CHANGED,
        user,
        "superadmin-profile-password-changed",
        Map.of("sessionPolicy", "all-user-sessions-revoked"));
    return new SuperAdminPasswordChangeResponseDto(
        "PASSWORD_CHANGED", changedAt, "all-user-sessions-revoked", "audit:event=PASSWORD_CHANGED");
  }

  @Transactional(readOnly = true)
  public List<SuperAdminProfileSessionDto> sessions(UserPrincipal principal) {
    UserAccount user = requireSuperAdmin(principal);
    return sessionDtos(user);
  }

  @Transactional
  public SuperAdminSessionRevokeResponseDto revokeSession(
      UserPrincipal principal, String sessionId) {
    UserAccount user = requireSuperAdmin(principal);
    Long refreshTokenId = parseSessionId(sessionId);
    boolean revoked = refreshTokenService.revokeSession(user.getPublicId(), refreshTokenId);
    Instant revokedAt = Instant.now();
    AuditLog audit =
        recordRequiredAudit(
            AuditEvent.TOKEN_REVOKED,
            user,
            "superadmin-profile-session-revoked",
            Map.of(
                "sessionId",
                safeSessionId(refreshTokenId),
                "revoked",
                Boolean.toString(revoked),
                "tokenMaterial",
                "redacted"));
    return new SuperAdminSessionRevokeResponseDto(
        safeSessionId(refreshTokenId),
        revoked,
        revokedAt,
        "audit:event=TOKEN_REVOKED,id=" + audit.getId());
  }

  private SuperAdminProfileDto toProfile(UserAccount user) {
    return new SuperAdminProfileDto(
        user.getDisplayName(),
        user.getEmail(),
        user.getPhoneSecondary(),
        user.getProfilePictureUrl(),
        settingValue(timezoneKey(user), DEFAULT_TIMEZONE),
        settingValue(languageKey(user), DEFAULT_LANGUAGE),
        lastLoginAt(user),
        sessionDtos(user));
  }

  private List<SuperAdminProfileSessionDto> sessionDtos(UserAccount user) {
    Instant now = Instant.now();
    return refreshTokenService.sessionsForUser(user.getPublicId()).stream()
        .map(
            token ->
                new SuperAdminProfileSessionDto(
                    safeSessionId(token.getId()),
                    token.getAuthScopeCode(),
                    token.getIssuedAt(),
                    token.getExpiresAt(),
                    !token.isExpired(now),
                    "not-captured",
                    "redacted"))
        .toList();
  }

  private Instant lastLoginAt(UserAccount user) {
    return auditLogRepository
        .findFirstByEventTypeAndUsernameIgnoreCaseOrderByTimestampDesc(
            AuditEvent.LOGIN_SUCCESS, user.getEmail())
        .map(AuditLog::getTimestamp)
        .map(this::toInstant)
        .orElse(null);
  }

  private Instant toInstant(LocalDateTime timestamp) {
    return timestamp == null ? null : timestamp.atZone(ZoneOffset.UTC).toInstant();
  }

  private UserAccount requireSuperAdmin(UserPrincipal principal) {
    if (principal == null
        || principal.getUser() == null
        || principal.getUser().getPublicId() == null) {
      throw new AccessDeniedException("Super Admin profile requires authentication");
    }
    UserAccount user =
        userAccountRepository
            .findByPublicId(principal.getUser().getPublicId())
            .orElseThrow(
                () -> new AccessDeniedException("Super Admin profile requires authentication"));
    boolean superAdmin =
        user.getRoles().stream()
            .anyMatch(role -> "ROLE_SUPER_ADMIN".equalsIgnoreCase(role.getName()));
    if (!superAdmin) {
      throw new AccessDeniedException("Super Admin profile requires platform authority");
    }
    return user;
  }

  private boolean isEmptyUpdate(SuperAdminProfileUpdateRequest request) {
    return request.displayName() == null
        && request.phone() == null
        && request.avatarUrl() == null
        && request.timezone() == null
        && request.language() == null;
  }

  private String normalizeRequiredText(String value, String fieldName, int maxLength) {
    String normalized = normalizeOptionalText(value, maxLength);
    if (!StringUtils.hasText(normalized)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          fieldName + " is required");
    }
    return normalized;
  }

  private String normalizeOptionalText(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Value must be at most " + maxLength + " characters");
    }
    return normalized.isEmpty() ? null : normalized;
  }

  private String settingValue(String key, String fallback) {
    return settingsRepository
        .findById(key)
        .map(SystemSetting::getValue)
        .filter(StringUtils::hasText)
        .orElse(fallback);
  }

  private String timezoneKey(UserAccount user) {
    return profileKey(user.getPublicId(), TIMEZONE_SUFFIX);
  }

  private String languageKey(UserAccount user) {
    return profileKey(user.getPublicId(), LANGUAGE_SUFFIX);
  }

  private String profileKey(UUID userPublicId, String suffix) {
    return PROFILE_SETTING_PREFIX + userPublicId + suffix;
  }

  private Long parseSessionId(String sessionId) {
    if (!StringUtils.hasText(sessionId) || !sessionId.startsWith("refresh:")) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "sessionId must refer to a refresh session");
    }
    try {
      return Long.parseLong(sessionId.substring("refresh:".length()));
    } catch (NumberFormatException ex) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "sessionId must refer to a refresh session");
    }
  }

  private String safeSessionId(Long refreshTokenId) {
    return "refresh:" + refreshTokenId;
  }

  private void recordAudit(
      AuditEvent event, UserAccount user, String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new HashMap<>();
    auditMetadata.put("reason", reason);
    auditMetadata.put("actorPublicId", user.getPublicId().toString());
    auditMetadata.put("scope", user.getAuthScopeCode());
    if (metadata != null) {
      auditMetadata.putAll(metadata);
    }
    auditService.logAuthSuccess(
        event, user.getEmail().toLowerCase(Locale.ROOT), user.getAuthScopeCode(), auditMetadata);
  }

  private AuditLog recordRequiredAudit(
      AuditEvent event, UserAccount user, String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new HashMap<>();
    auditMetadata.put("reason", reason);
    auditMetadata.put("actorPublicId", user.getPublicId().toString());
    auditMetadata.put("scope", user.getAuthScopeCode());
    if (metadata != null) {
      auditMetadata.putAll(metadata);
    }
    return auditService.logAuthSuccessRequired(
        event, user.getEmail().toLowerCase(Locale.ROOT), user.getAuthScopeCode(), auditMetadata);
  }
}
