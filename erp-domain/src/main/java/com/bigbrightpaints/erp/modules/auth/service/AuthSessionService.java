package com.bigbrightpaints.erp.modules.auth.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.modules.auth.domain.RefreshTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthSessionService {

  private static final int DEFAULT_SESSION_LIMIT = 25;
  private static final int MAX_DEVICE_LABEL_LENGTH = 80;

  private final JdbcTemplate jdbcTemplate;
  private final RefreshTokenRepository refreshTokenRepository;

  public AuthSessionService(
      JdbcTemplate jdbcTemplate, RefreshTokenRepository refreshTokenRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.refreshTokenRepository = refreshTokenRepository;
  }

  public SessionDeviceMetadata metadataFrom(HttpServletRequest request) {
    String userAgent = request == null ? null : request.getHeader("User-Agent");
    String remoteAddress = request == null ? null : request.getRemoteAddr();
    return new SessionDeviceMetadata(
        sanitizeDeviceLabel(userAgent),
        StringUtils.hasText(userAgent) ? IdempotencyUtils.sha256Hex(userAgent) : null,
        StringUtils.hasText(remoteAddress) ? IdempotencyUtils.sha256Hex(remoteAddress) : null);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listActiveSessions(UserAccount user, UUID currentSessionId) {
    if (user == null || user.getPublicId() == null) {
      return List.of();
    }
    String scopeCode = normalizeScopeCode(user.getAuthScopeCode());
    return jdbcTemplate.query(
        """
        select s.public_id,
               s.issued_at,
               coalesce(s.last_seen_at, s.issued_at) as last_seen_at,
               s.expires_at,
               s.auth_scope_code,
               coalesce(d.device_label, 'Unknown device') as device_label
          from iam_sessions s
          join iam_accounts ia on ia.id = s.account_id
          left join iam_devices d on d.id = s.device_id
         where ia.public_id = ?
           and s.auth_scope_code = ?
           and s.revoked_at is null
           and s.consumed_at is null
           and s.expires_at > ?
         order by coalesce(s.last_seen_at, s.issued_at) desc, s.id desc
         limit ?
        """,
        rs -> {
          java.util.ArrayList<Map<String, Object>> sessions = new java.util.ArrayList<>();
          while (rs.next()) {
            UUID sessionId = rs.getObject("public_id", UUID.class);
            String deviceLabel = sanitizeDeviceLabel(rs.getString("device_label"));
            sessions.add(
                Map.of(
                    "sessionId", sessionId.toString(),
                    "current", sessionId.equals(currentSessionId),
                    "createdAt", rs.getTimestamp("issued_at").toInstant().toString(),
                    "lastSeenAt", rs.getTimestamp("last_seen_at").toInstant().toString(),
                    "expiresAt", rs.getTimestamp("expires_at").toInstant().toString(),
                    "authScopeCode", rs.getString("auth_scope_code"),
                    "deviceName", deviceLabel,
                    "userAgent", deviceLabel));
          }
          return sessions;
        },
        user.getPublicId(),
        scopeCode,
        Timestamp.from(Instant.now()),
        DEFAULT_SESSION_LIMIT);
  }

  @Transactional(readOnly = true)
  public int countActiveSessions(UserAccount user) {
    if (user == null || user.getPublicId() == null) {
      return 0;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where ia.public_id = ?
               and s.auth_scope_code = ?
               and s.revoked_at is null
               and s.consumed_at is null
               and s.expires_at > ?
            """,
            Integer.class,
            user.getPublicId(),
            normalizeScopeCode(user.getAuthScopeCode()),
            Timestamp.from(Instant.now()));
    return count == null ? 0 : count;
  }

  @Transactional
  public boolean revokeSession(UserAccount user, String sessionId, String reason) {
    UUID parsedSessionId = parseUuid(sessionId);
    if (user == null || user.getPublicId() == null || parsedSessionId == null) {
      return false;
    }
    List<String> digests = activeSessionDigestsForUser(user.getPublicId(), parsedSessionId);
    if (digests.isEmpty()) {
      return false;
    }
    int updated =
        jdbcTemplate.update(
            """
            update iam_sessions s
               set revoked_at = coalesce(s.revoked_at, ?),
                   revoked_reason = ?,
                   version = s.version + 1
              from iam_accounts ia
             where s.account_id = ia.id
               and ia.public_id = ?
               and s.public_id = ?
               and s.revoked_at is null
            """,
            Timestamp.from(Instant.now()),
            safeReason(reason),
            user.getPublicId(),
            parsedSessionId);
    digests.forEach(refreshTokenRepository::deleteByTokenDigest);
    return updated > 0;
  }

  @Transactional
  public void revokeCurrentSession(UUID userPublicId, UUID sessionId, String reason) {
    if (userPublicId == null || sessionId == null) {
      return;
    }
    List<String> digests = activeSessionDigestsForUser(userPublicId, sessionId);
    if (digests.isEmpty()) {
      return;
    }
    jdbcTemplate.update(
        """
        update iam_sessions s
           set revoked_at = coalesce(s.revoked_at, ?),
               revoked_reason = ?,
               version = s.version + 1
          from iam_accounts ia
         where s.account_id = ia.id
           and ia.public_id = ?
           and s.public_id = ?
           and s.revoked_at is null
        """,
        Timestamp.from(Instant.now()),
        safeReason(reason),
        userPublicId,
        sessionId);
    digests.forEach(refreshTokenRepository::deleteByTokenDigest);
  }

  @Transactional
  public void revokeAllSessions(UUID userPublicId, String reason) {
    if (userPublicId == null) {
      return;
    }
    List<String> digests =
        jdbcTemplate.queryForList(
            """
            select s.refresh_token_digest
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where ia.public_id = ?
               and s.revoked_at is null
               and s.consumed_at is null
            """,
            String.class,
            userPublicId);
    jdbcTemplate.update(
        """
        update iam_sessions s
           set revoked_at = coalesce(s.revoked_at, ?),
               revoked_reason = ?,
               version = s.version + 1
          from iam_accounts ia
         where s.account_id = ia.id
           and ia.public_id = ?
           and s.revoked_at is null
        """,
        Timestamp.from(Instant.now()),
        safeReason(reason),
        userPublicId);
    digests.forEach(refreshTokenRepository::deleteByTokenDigest);
  }

  @Transactional(readOnly = true)
  public boolean isSessionActive(UUID userPublicId, String authScopeCode, UUID sessionId) {
    if (userPublicId == null || sessionId == null || !StringUtils.hasText(authScopeCode)) {
      return false;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where ia.public_id = ?
               and s.public_id = ?
               and s.auth_scope_code = ?
               and s.revoked_at is null
               and s.consumed_at is null
               and s.expires_at > ?
            """,
            Integer.class,
            userPublicId,
            sessionId,
            normalizeScopeCode(authScopeCode),
            Timestamp.from(Instant.now()));
    return count != null && count > 0;
  }

  @Transactional(readOnly = true)
  public boolean refreshTokenBelongsToSession(
      String refreshToken, UUID userPublicId, UUID sessionId, String authScopeCode) {
    if (!StringUtils.hasText(refreshToken)
        || userPublicId == null
        || sessionId == null
        || !StringUtils.hasText(authScopeCode)) {
      return false;
    }
    String digest = AuthTokenDigests.refreshTokenDigest(refreshToken);
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where ia.public_id = ?
               and s.public_id = ?
               and s.auth_scope_code = ?
               and s.refresh_token_digest = ?
               and s.revoked_at is null
               and s.consumed_at is null
            """,
            Integer.class,
            userPublicId,
            sessionId,
            normalizeScopeCode(authScopeCode),
            digest);
    return count != null && count > 0;
  }

  public UUID currentSessionIdFromClaims(io.jsonwebtoken.Claims claims) {
    if (claims == null) {
      return null;
    }
    return parseUuid(claims.get("sid", String.class));
  }

  private List<String> activeSessionDigestsForUser(UUID userPublicId, UUID sessionId) {
    return jdbcTemplate.queryForList(
        """
        select s.refresh_token_digest
          from iam_sessions s
          join iam_accounts ia on ia.id = s.account_id
         where ia.public_id = ?
           and s.public_id = ?
           and s.revoked_at is null
           and s.consumed_at is null
        """,
        String.class,
        userPublicId,
        sessionId);
  }

  private String sanitizeDeviceLabel(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "Unknown device";
    }
    String sanitized =
        raw.replaceAll("\\p{Cntrl}", " ")
            .replaceAll("[<>\"'`]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    if (!StringUtils.hasText(sanitized)) {
      return "Unknown device";
    }
    return coarseDeviceLabel(sanitized);
  }

  private String coarseDeviceLabel(String sanitized) {
    String lower = sanitized.toLowerCase(Locale.ROOT);
    String label;
    if (lower.contains("edg/") || lower.contains("edge/")) {
      label = "Edge browser";
    } else if (lower.contains("chrome/") || lower.contains("chromium/")) {
      label = "Chrome browser";
    } else if (lower.contains("firefox/")) {
      label = "Firefox browser";
    } else if (lower.contains("safari/")) {
      label = "Safari browser";
    } else if (lower.contains("curl/")) {
      label = "curl client";
    } else if (lower.contains("java/")) {
      label = "Java client";
    } else if (lower.contains("python")) {
      label = "Python client";
    } else {
      label = "Other client";
    }
    return label.length() > MAX_DEVICE_LABEL_LENGTH
        ? label.substring(0, MAX_DEVICE_LABEL_LENGTH)
        : label;
  }

  private String safeReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      return "revoked";
    }
    String normalized = reason.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
    return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
  }

  private String normalizeScopeCode(String scopeCode) {
    return scopeCode == null ? null : scopeCode.trim().toUpperCase(Locale.ROOT);
  }

  private UUID parseUuid(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return UUID.fromString(value.trim());
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
