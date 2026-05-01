package com.bigbrightpaints.erp.modules.auth.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.modules.auth.domain.PasswordResetTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPasswordHistory;
import com.bigbrightpaints.erp.modules.auth.domain.UserPasswordHistoryRepository;
import com.bigbrightpaints.erp.modules.auth.web.ChangePasswordRequest;

import jakarta.transaction.Transactional;

@Service
public class PasswordService {

  private static final int PASSWORD_HISTORY_LIMIT = 5;

  private final UserAccountRepository userAccountRepository;
  private final UserPasswordHistoryRepository passwordHistoryRepository;
  private final PasswordEncoder passwordEncoder;
  private final PasswordPolicy passwordPolicy;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final AuditService auditService;

  public PasswordService(
      UserAccountRepository userAccountRepository,
      UserPasswordHistoryRepository passwordHistoryRepository,
      PasswordEncoder passwordEncoder,
      PasswordPolicy passwordPolicy,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      IamCanonicalStorageService iamCanonicalStorageService,
      PasswordResetTokenRepository passwordResetTokenRepository,
      AuditService auditService) {
    this.userAccountRepository = userAccountRepository;
    this.passwordHistoryRepository = passwordHistoryRepository;
    this.passwordEncoder = passwordEncoder;
    this.passwordPolicy = passwordPolicy;
    this.tokenBlacklistService = tokenBlacklistService;
    this.refreshTokenService = refreshTokenService;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.auditService = auditService;
  }

  @Transactional
  public void changePassword(UserAccount user, ChangePasswordRequest request) {
    String normalizedNewPassword = passwordPolicy.normalize(request.newPassword());
    String normalizedConfirmPassword = passwordPolicy.normalize(request.confirmPassword());
    validateNewPasswordCandidate(normalizedNewPassword, normalizedConfirmPassword);
    // If forced change is required, skip current password check to avoid blocking on temp passwords
    if (!user.isMustChangePassword()) {
      if (!passwordEncoder.matches(
          passwordPolicy.normalize(request.currentPassword()), user.getPasswordHash())) {
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
            "Current password is incorrect");
      }
    }
    applyNewPassword(user, normalizedNewPassword, true, "password_change");
  }

  @Transactional
  public void resetPassword(UserAccount user, String newPassword, String confirmPassword) {
    String normalizedNewPassword = passwordPolicy.normalize(newPassword);
    String normalizedConfirmPassword = passwordPolicy.normalize(confirmPassword);
    if (!Objects.equals(normalizedNewPassword, normalizedConfirmPassword)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Password confirmation does not match");
    }
    applyNewPassword(user, normalizedNewPassword, false, "password_reset");
  }

  private void validateNewPasswordCandidate(
      String normalizedNewPassword, String normalizedConfirmPassword) {
    if (!Objects.equals(normalizedNewPassword, normalizedConfirmPassword)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Password confirmation does not match");
    }
    List<String> violations = passwordPolicy.validate(normalizedNewPassword);
    if (!violations.isEmpty()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Password does not meet policy: " + String.join(", ", violations));
    }
  }

  private void applyNewPassword(
      UserAccount user,
      String newPassword,
      boolean invalidateOutstandingResetTokens,
      String operation) {
    List<String> violations = passwordPolicy.validate(newPassword);
    if (!violations.isEmpty()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Password does not meet policy: " + String.join(", ", violations));
    }
    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "New password must be different from current password");
    }
    ensureNotReused(user, newPassword);
    rememberCurrentPassword(user);
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    user.setMustChangePassword(false);
    userAccountRepository.save(user);
    iamCanonicalStorageService.syncUser(user);
    if (invalidateOutstandingResetTokens) {
      invalidateOutstandingResetTokens(user);
    }
    revokeExistingSessions(user, operation);
  }

  private void revokeExistingSessions(UserAccount user, String operation) {
    if (user == null || user.getPublicId() == null) {
      return;
    }
    tokenBlacklistService.revokeAllUserTokens(user.getPublicId().toString());
    refreshTokenService.revokeAllForUser(user.getPublicId());
    auditService.logAuthSuccessRequired(
        AuditEvent.TOKEN_REVOKED,
        user.getEmail(),
        user.getAuthScopeCode(),
        tokenRevocationMetadata(user, operation));
  }

  private Map<String, String> tokenRevocationMetadata(UserAccount user, String operation) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("operation", operation);
    metadata.put("outcome", "all_user_sessions_revoked");
    metadata.put("revocationScope", "all-user-sessions");
    metadata.put("tokenMaterial", "redacted");
    putIfText(metadata, "traceId", RequestTraceContext.traceId());
    putIfText(metadata, "correlationId", RequestTraceContext.correlationId());
    putIfText(metadata, "companyCode", user.getAuthScopeCode());
    metadata.put("actorPublicId", user.getPublicId().toString());
    return metadata;
  }

  private void putIfText(Map<String, String> metadata, String key, String value) {
    if (StringUtils.hasText(value)) {
      metadata.put(key, value);
    }
  }

  private void ensureNotReused(UserAccount user, String candidate) {
    List<UserPasswordHistory> recent =
        passwordHistoryRepository.findTop5ByUserOrderByChangedAtDesc(user);
    boolean reused =
        recent.stream()
            .anyMatch(entry -> passwordEncoder.matches(candidate, entry.getPasswordHash()));
    if (reused) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Cannot reuse one of the last " + PASSWORD_HISTORY_LIMIT + " passwords");
    }
  }

  public void invalidateOutstandingResetTokens(UserAccount user) {
    if (user == null) {
      return;
    }
    passwordResetTokenRepository.deleteByUser(user);
  }

  private void rememberCurrentPassword(UserAccount user) {
    if (!StringUtils.hasText(user.getPasswordHash())) {
      return;
    }
    passwordHistoryRepository.save(new UserPasswordHistory(user, user.getPasswordHash()));
    trimHistory(user);
  }

  private void trimHistory(UserAccount user) {
    List<UserPasswordHistory> ordered =
        passwordHistoryRepository.findByUserOrderByChangedAtDesc(user);
    if (ordered.size() <= PASSWORD_HISTORY_LIMIT) {
      return;
    }
    List<UserPasswordHistory> toDelete = ordered.subList(PASSWORD_HISTORY_LIMIT, ordered.size());
    passwordHistoryRepository.deleteAll(toDelete);
  }
}
