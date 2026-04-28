package com.bigbrightpaints.erp.modules.auth.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.modules.auth.domain.RefreshToken;
import com.bigbrightpaints.erp.modules.auth.domain.RefreshTokenRepository;

@Service
public class RefreshTokenService {

  private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

  private final RefreshTokenRepository refreshTokenRepository;
  private final IamCanonicalStorageService iamCanonicalStorageService;

  public RefreshTokenService(
      RefreshTokenRepository refreshTokenRepository,
      IamCanonicalStorageService iamCanonicalStorageService) {
    this.refreshTokenRepository = refreshTokenRepository;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
  }

  @Transactional
  public String issue(UUID userPublicId, String authScopeCode, Instant expiresAt) {
    return issue(userPublicId, authScopeCode, Instant.now(), expiresAt);
  }

  @Transactional
  public String issue(
      UUID userPublicId, String authScopeCode, Instant issuedAt, Instant expiresAt) {
    return issueSession(userPublicId, authScopeCode, issuedAt, expiresAt, null, null)
        .refreshToken();
  }

  @Transactional
  public IssuedRefreshToken issueSession(
      UUID userPublicId,
      String authScopeCode,
      Instant issuedAt,
      Instant expiresAt,
      SessionDeviceMetadata metadata,
      String previousRefreshTokenDigest) {
    String token = UUID.randomUUID().toString();
    UUID sessionPublicId = UUID.randomUUID();
    RefreshToken record =
        RefreshToken.digestOnly(
            AuthTokenDigests.refreshTokenDigest(token),
            userPublicId,
            authScopeCode,
            issuedAt,
            expiresAt);
    RefreshToken saved = refreshTokenRepository.save(record);
    UUID savedSessionId =
        iamCanonicalStorageService.recordSessionIssued(
            saved, sessionPublicId, previousRefreshTokenDigest, metadata);
    return new IssuedRefreshToken(token, savedSessionId);
  }

  @Transactional
  public Optional<TokenRecord> consume(String refreshToken) {
    return consume(refreshToken, null);
  }

  @Transactional
  public Optional<TokenRecord> consume(String refreshToken, String requiredAuthScopeCode) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return Optional.empty();
    }
    String tokenDigest = AuthTokenDigests.refreshTokenDigest(refreshToken);
    Optional<RefreshToken> record = refreshTokenRepository.findForUpdateByTokenDigest(tokenDigest);
    if (record.isEmpty()) {
      iamCanonicalStorageService
          .markRefreshReplayCompromised(tokenDigest)
          .forEach(refreshTokenRepository::deleteByTokenDigest);
      return Optional.empty();
    }
    RefreshToken stored = record.get();
    if (requiredAuthScopeCode != null
        && !requiredAuthScopeCode.equalsIgnoreCase(stored.getAuthScopeCode())) {
      return Optional.empty();
    }
    if (stored.isExpired(Instant.now())) {
      refreshTokenRepository.delete(stored);
      iamCanonicalStorageService.markSessionRevoked(tokenDigest, "expired");
      return Optional.empty();
    }
    iamCanonicalStorageService.markSessionConsumed(tokenDigest);
    refreshTokenRepository.delete(stored);
    return Optional.of(
        new TokenRecord(
            stored.getUserPublicId(),
            stored.getAuthScopeCode(),
            stored.getIssuedAt(),
            stored.getExpiresAt(),
            stored.getTokenDigest()));
  }

  @Transactional(readOnly = true)
  public Optional<TokenRecord> inspect(String refreshToken, String requiredAuthScopeCode) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return Optional.empty();
    }
    String tokenDigest = AuthTokenDigests.refreshTokenDigest(refreshToken);
    Optional<RefreshToken> record = refreshTokenRepository.findByTokenDigest(tokenDigest);
    if (record.isEmpty()) {
      return Optional.empty();
    }
    RefreshToken stored = record.get();
    if (requiredAuthScopeCode != null
        && !requiredAuthScopeCode.equalsIgnoreCase(stored.getAuthScopeCode())) {
      return Optional.empty();
    }
    if (stored.isExpired(Instant.now())) {
      return Optional.empty();
    }
    return Optional.of(
        new TokenRecord(
            stored.getUserPublicId(),
            stored.getAuthScopeCode(),
            stored.getIssuedAt(),
            stored.getExpiresAt(),
            stored.getTokenDigest()));
  }

  @Transactional
  public void revoke(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    String tokenDigest = AuthTokenDigests.refreshTokenDigest(refreshToken);
    iamCanonicalStorageService.markSessionRevoked(tokenDigest, "revoked");
    refreshTokenRepository.deleteByTokenDigest(tokenDigest);
  }

  @Transactional
  public void revokeAllForUser(UUID userPublicId) {
    if (userPublicId == null) {
      return;
    }
    iamCanonicalStorageService.markAllSessionsRevoked(userPublicId, "revoked_all");
    refreshTokenRepository.deleteByUserPublicId(userPublicId);
  }

  @Scheduled(fixedDelay = 3600000) // 1 hour
  @Transactional
  public void cleanupExpiredTokens() {
    int removed = refreshTokenRepository.deleteExpiredTokens(Instant.now());
    if (removed > 0) {
      logger.info("Refresh token cleanup removed {} expired tokens", removed);
    }
  }

  public record TokenRecord(
      UUID userPublicId,
      String authScopeCode,
      Instant issuedAt,
      Instant expiresAt,
      String refreshTokenDigest) {}

  public record IssuedRefreshToken(String refreshToken, UUID sessionPublicId) {}
}
