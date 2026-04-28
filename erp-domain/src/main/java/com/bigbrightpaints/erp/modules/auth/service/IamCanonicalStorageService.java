package com.bigbrightpaints.erp.modules.auth.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.modules.auth.domain.RefreshToken;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;

@Service
public class IamCanonicalStorageService {

  private static final String TOTP = "TOTP";
  private static final Set<String> SENSITIVE_METADATA_KEYS =
      Set.of(
          "password",
          "newpassword",
          "currentpassword",
          "confirmpassword",
          "token",
          "refreshtoken",
          "accesstoken",
          "resettoken",
          "secret",
          "mfasecret",
          "recoverycode",
          "recoverycodes",
          "hash",
          "digest");
  private static final Set<String> SAFE_SCOPE_METADATA_KEYS =
      Set.of("companycode", "authscopecode", "scopecode", "tenantscope");

  private final JdbcTemplate jdbcTemplate;
  private final UserAccountRepository userAccountRepository;
  private final ObjectMapper objectMapper;

  public IamCanonicalStorageService(
      JdbcTemplate jdbcTemplate,
      UserAccountRepository userAccountRepository,
      ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.userAccountRepository = userAccountRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void syncUser(UserAccount user) {
    if (user == null || user.getId() == null) {
      return;
    }
    upsertAccount(user);
    upsertProfile(user);
    upsertContact(user);
    upsertCredential(user);
    syncMfaFactor(user);
  }

  @Transactional
  public void recordSessionIssued(RefreshToken token) {
    recordSessionIssued(token, UUID.randomUUID(), null, null);
  }

  @Transactional
  public UUID recordSessionIssued(
      RefreshToken token,
      UUID sessionPublicId,
      String previousRefreshTokenDigest,
      SessionDeviceMetadata metadata) {
    if (token == null || token.getUserPublicId() == null) {
      return sessionPublicId;
    }
    ensureAccountForPublicId(token.getUserPublicId());
    Long accountId =
        jdbcTemplate.query(
            "select id from iam_accounts where public_id = ?",
            rs -> rs.next() ? rs.getLong("id") : null,
            token.getUserPublicId());
    if (accountId == null) {
      return sessionPublicId;
    }
    Long deviceId = resolveSessionDevice(accountId, previousRefreshTokenDigest, metadata);
    UUID rotatedSessionPublicId =
        rotateExistingSessionIfPresent(token, accountId, deviceId, previousRefreshTokenDigest);
    if (rotatedSessionPublicId != null) {
      return rotatedSessionPublicId;
    }
    jdbcTemplate.update(
        """
        insert into iam_sessions (
            account_id,
            device_id,
            public_id,
            refresh_token_digest,
            auth_scope_code,
            issued_at,
            last_seen_at,
            expires_at
        )
        values (?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (refresh_token_digest) do update
            set last_seen_at = excluded.last_seen_at,
                expires_at = excluded.expires_at,
                device_id = excluded.device_id,
                consumed_at = null,
                revoked_at = null,
                revoked_reason = null,
                version = iam_sessions.version + 1
        """,
        accountId,
        deviceId,
        sessionPublicId,
        token.getTokenDigest(),
        normalizeScopeCode(token.getAuthScopeCode()),
        timestamp(token.getIssuedAt()),
        timestamp(token.getIssuedAt()),
        timestamp(token.getExpiresAt()));
    return sessionPublicId;
  }

  private UUID rotateExistingSessionIfPresent(
      RefreshToken token, Long accountId, Long deviceId, String previousRefreshTokenDigest) {
    if (!StringUtils.hasText(previousRefreshTokenDigest)) {
      return null;
    }
    ExistingSession existingSession =
        jdbcTemplate.query(
            """
            select id, public_id
              from iam_sessions
             where account_id = ?
               and refresh_token_digest = ?
               and auth_scope_code = ?
             for update
            """,
            rs ->
                rs.next()
                    ? new ExistingSession(rs.getLong("id"), rs.getObject("public_id", UUID.class))
                    : null,
            accountId,
            previousRefreshTokenDigest,
            normalizeScopeCode(token.getAuthScopeCode()));
    if (existingSession == null) {
      return null;
    }
    jdbcTemplate.update(
        """
        update iam_sessions
           set refresh_token_digest = ?,
               device_id = ?,
               last_seen_at = ?,
               expires_at = ?,
               consumed_at = null,
               revoked_at = null,
               revoked_reason = null,
               version = version + 1
         where id = ?
        """,
        token.getTokenDigest(),
        deviceId,
        timestamp(token.getIssuedAt()),
        timestamp(token.getExpiresAt()),
        existingSession.id());
    return existingSession.publicId();
  }

  @Transactional
  public void markSessionConsumed(String refreshTokenDigest) {
    if (!StringUtils.hasText(refreshTokenDigest)) {
      return;
    }
    jdbcTemplate.update(
        """
        update iam_sessions
           set consumed_at = coalesce(consumed_at, ?),
               last_seen_at = ?,
               version = version + 1
         where refresh_token_digest = ?
        """,
        timestampNow(),
        timestampNow(),
        refreshTokenDigest);
  }

  @Transactional
  public void markSessionRevoked(String refreshTokenDigest, String reason) {
    if (!StringUtils.hasText(refreshTokenDigest)) {
      return;
    }
    jdbcTemplate.update(
        """
        update iam_sessions
           set revoked_at = coalesce(revoked_at, ?),
               revoked_reason = ?,
               version = version + 1
         where refresh_token_digest = ?
        """,
        timestampNow(),
        safeReason(reason),
        refreshTokenDigest);
  }

  @Transactional(readOnly = true)
  public boolean hasCanonicalAccount(UUID userPublicId) {
    if (userPublicId == null) {
      return false;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from iam_accounts where public_id = ?", Integer.class, userPublicId);
    return count != null && count > 0;
  }

  @Transactional(readOnly = true)
  public boolean isRefreshSessionActive(RefreshToken token) {
    if (token == null
        || token.getUserPublicId() == null
        || !StringUtils.hasText(token.getTokenDigest())
        || !StringUtils.hasText(token.getAuthScopeCode())) {
      return false;
    }
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where ia.public_id = ?
               and s.refresh_token_digest = ?
               and s.auth_scope_code = ?
               and s.revoked_at is null
               and s.consumed_at is null
               and s.expires_at > ?
            """,
            Integer.class,
            token.getUserPublicId(),
            token.getTokenDigest(),
            normalizeScopeCode(token.getAuthScopeCode()),
            timestampNow());
    return count != null && count > 0;
  }

  @Transactional
  public void markAllSessionsRevoked(UUID userPublicId, String reason) {
    if (userPublicId == null) {
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
           and s.revoked_at is null
        """,
        timestampNow(),
        safeReason(reason),
        userPublicId);
  }

  @Transactional
  public List<String> markRefreshReplayCompromised(String refreshTokenDigest) {
    if (!StringUtils.hasText(refreshTokenDigest)) {
      return List.of();
    }
    ReplaySession replaySession =
        jdbcTemplate.query(
            """
            select ia.id as account_id,
                   ia.public_id as account_public_id,
                   s.device_id,
                   s.consumed_at,
                   s.auth_scope_code
              from iam_sessions s
              join iam_accounts ia on ia.id = s.account_id
             where s.refresh_token_digest = ?
            """,
            rs -> {
              if (!rs.next() || rs.getTimestamp("consumed_at") == null) {
                return null;
              }
              return new ReplaySession(
                  rs.getLong("account_id"),
                  rs.getObject("account_public_id", UUID.class),
                  rs.getObject("device_id", Long.class),
                  rs.getTimestamp("consumed_at").toInstant(),
                  rs.getString("auth_scope_code"));
            },
            refreshTokenDigest);
    if (replaySession == null) {
      replaySession =
          jdbcTemplate.query(
              """
              select ia.id as account_id,
                     ia.public_id as account_public_id,
                     null::bigint as device_id,
                     rt.auth_scope_code
                from refresh_tokens rt
                join iam_accounts ia on ia.public_id = rt.user_public_id
               where rt.token_digest = ?
               limit 1
              """,
              rs -> {
                if (!rs.next()) {
                  return null;
                }
                return new ReplaySession(
                    rs.getLong("account_id"),
                    rs.getObject("account_public_id", UUID.class),
                    rs.getObject("device_id", Long.class),
                    null,
                    normalizeScopeCode(rs.getString("auth_scope_code")));
              },
              refreshTokenDigest);
      if (replaySession == null) {
        return List.of();
      }
    }
    List<String> activeDigests =
        jdbcTemplate.queryForList(
            """
            select refresh_token_digest
              from iam_sessions
             where account_id = ?
               and auth_scope_code = ?
               and revoked_at is null
               and consumed_at is null
            """,
            String.class,
            replaySession.accountId(),
            replaySession.authScopeCode());
    jdbcTemplate.update(
        """
        update iam_sessions
           set revoked_at = coalesce(revoked_at, ?),
               revoked_reason = 'refresh_replay',
               version = version + 1
         where account_id = ?
           and auth_scope_code = ?
           and revoked_at is null
           and consumed_at is null
        """,
        timestampNow(),
        replaySession.accountId(),
        replaySession.authScopeCode());
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("operation", "refresh_replay_detected");
    metadata.put("reason", "refresh_replay");
    metadata.put("sessionReference", "known_consumed_refresh");
    recordSecurityEvent(
        "SESSION_REFRESH_REPLAY",
        "DENIED",
        metadata,
        replaySession.accountPublicId().toString(),
        null,
        null,
        replaySession.authScopeCode());
    return activeDigests;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordSecurityEvent(
      String eventType,
      String outcome,
      Map<String, String> metadata,
      String userId,
      String username,
      Long companyId,
      String authScopeCode) {
    if (!StringUtils.hasText(eventType)) {
      return;
    }
    String resolvedOutcome = normalizeOutcome(outcome);
    String resolvedScope = normalizeNullableScopeCode(authScopeCode);
    Long accountId = resolveAccountId(userId, username, resolvedScope);
    String metadataJson = toJson(redactMetadata(metadata));
    jdbcTemplate.update(
        """
        insert into iam_security_events (
            account_id,
            actor_account_id,
            company_id,
            auth_scope_code,
            event_type,
            outcome,
            reason,
            metadata,
            occurred_at
        )
        values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
        """,
        accountId,
        accountId,
        companyId,
        resolvedScope,
        truncate(eventType, 96),
        resolvedOutcome,
        firstPresent(metadata, "reason"),
        metadataJson,
        timestampNow());
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listSecurityEvents(
      UserAccount user, String eventTypeFilter, int limit) {
    if (user == null || user.getPublicId() == null) {
      return List.of();
    }
    String normalizedScope = normalizeNullableScopeCode(user.getAuthScopeCode());
    int boundedLimit = Math.max(1, Math.min(limit, 100));
    List<Map<String, Object>> events =
        jdbcTemplate.query(
            """
            select e.event_type,
                   e.outcome,
                   e.reason,
                   e.auth_scope_code,
                   e.metadata::text as metadata_json,
                   e.occurred_at,
                   c.code as company_code
              from iam_security_events e
              join iam_accounts ia on ia.id = e.account_id
              left join companies c on c.id = e.company_id
             where ia.public_id = ?
               and (? is null or e.auth_scope_code = ?)
             order by e.occurred_at desc, e.id desc
             limit ?
            """,
            (rs, rowNum) -> {
              Map<String, String> metadata = fromJsonMap(rs.getString("metadata_json"));
              Map<String, Object> row = new LinkedHashMap<>();
              String eventType = rs.getString("event_type");
              String resolvedAuthScopeCode =
                  firstNonBlank(metadata.get("authScopeCode"), rs.getString("auth_scope_code"));
              row.put("type", eventType);
              row.put("eventType", eventType);
              row.put("actor", firstNonBlank(metadata.get("actor"), metadata.get("actorUserId")));
              row.put("targetUserId", firstNonBlank(metadata.get("targetUserId"), null));
              row.put(
                  "sessionId",
                  firstNonBlank(metadata.get("sessionId"), metadata.get("sessionReference")));
              row.put(
                  "companyCode",
                  firstNonBlank(
                      metadata.get("companyCode"),
                      metadata.get("authScopeCode"),
                      metadata.get("tenantScope"),
                      rs.getString("company_code"),
                      resolvedAuthScopeCode));
              row.put("authScopeCode", resolvedAuthScopeCode);
              row.put("outcome", rs.getString("outcome"));
              row.put("reason", firstNonBlank(rs.getString("reason"), metadata.get("reason")));
              Timestamp occurredAt = rs.getTimestamp("occurred_at");
              row.put("createdAt", occurredAt == null ? null : occurredAt.toInstant().toString());
              row.put("metadata", securityEventMetadata(metadata));
              return row;
            },
            user.getPublicId(),
            normalizedScope,
            normalizedScope,
            boundedLimit);
    String normalizedFilter = normalizeEventTypeFilter(eventTypeFilter);
    if (!StringUtils.hasText(normalizedFilter)) {
      return events;
    }
    return events.stream()
        .filter(row -> matchesEventFilter(String.valueOf(row.get("eventType")), normalizedFilter))
        .toList();
  }

  private void upsertAccount(UserAccount user) {
    jdbcTemplate.update(
        """
        insert into iam_accounts (
            user_id,
            public_id,
            account_type,
            auth_scope_code,
            company_id,
            email,
            status,
            locked_until,
            failed_login_attempts,
            must_change_password,
            created_at,
            updated_at
        )
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        on conflict (user_id) do update
            set public_id = excluded.public_id,
                account_type = excluded.account_type,
                auth_scope_code = excluded.auth_scope_code,
                company_id = excluded.company_id,
                email = excluded.email,
                status = excluded.status,
                locked_until = excluded.locked_until,
                failed_login_attempts = excluded.failed_login_attempts,
                must_change_password = excluded.must_change_password,
                updated_at = excluded.updated_at,
                version = iam_accounts.version + 1
        """,
        user.getId(),
        user.getPublicId(),
        user.getCompany() == null ? "PLATFORM" : "TENANT",
        normalizeScopeCode(user.getAuthScopeCode()),
        user.getCompany() == null ? null : user.getCompany().getId(),
        normalizeEmail(user.getEmail()),
        user.isEnabled() ? "ACTIVE" : "DISABLED",
        timestamp(user.getLockedUntil()),
        user.getFailedLoginAttempts(),
        user.isMustChangePassword(),
        timestamp(user.getCreatedAt() == null ? Instant.now() : user.getCreatedAt()),
        timestampNow());
  }

  private void upsertProfile(UserAccount user) {
    jdbcTemplate.update(
        """
        insert into iam_account_profiles (
            account_id,
            display_name,
            preferred_name,
            profile_picture_url,
            job_title,
            updated_at
        )
        select ia.id, ?, ?, ?, ?, ?
          from iam_accounts ia
         where ia.user_id = ?
        on conflict (account_id) do update
            set display_name = excluded.display_name,
                preferred_name = excluded.preferred_name,
                profile_picture_url = excluded.profile_picture_url,
                job_title = excluded.job_title,
                updated_at = excluded.updated_at,
                version = iam_account_profiles.version + 1
        """,
        user.getDisplayName(),
        user.getPreferredName(),
        user.getProfilePictureUrl(),
        user.getJobTitle(),
        timestampNow(),
        user.getId());
  }

  private void upsertContact(UserAccount user) {
    jdbcTemplate.update(
        """
        insert into iam_account_contacts (
            account_id,
            primary_email,
            secondary_email,
            phone_secondary,
            updated_at
        )
        select ia.id, ?, ?, ?, ?
          from iam_accounts ia
         where ia.user_id = ?
        on conflict (account_id) do update
            set primary_email = excluded.primary_email,
                secondary_email = excluded.secondary_email,
                phone_secondary = excluded.phone_secondary,
                updated_at = excluded.updated_at,
                version = iam_account_contacts.version + 1
        """,
        normalizeEmail(user.getEmail()),
        normalizeNullableEmail(user.getSecondaryEmail()),
        blankToNull(user.getPhoneSecondary()),
        timestampNow(),
        user.getId());
  }

  private void upsertCredential(UserAccount user) {
    jdbcTemplate.update(
        """
        insert into iam_credentials (
            account_id,
            password_hash,
            password_changed_at,
            must_change_password,
            updated_at
        )
        select ia.id, ?, ?, ?, ?
          from iam_accounts ia
         where ia.user_id = ?
        on conflict (account_id) do update
            set password_changed_at =
                    case
                      when iam_credentials.password_hash is distinct from excluded.password_hash
                      then excluded.password_changed_at
                      else iam_credentials.password_changed_at
                    end,
                password_hash = excluded.password_hash,
                must_change_password = excluded.must_change_password,
                updated_at = excluded.updated_at,
                version = iam_credentials.version + 1
        """,
        user.getPasswordHash(),
        timestampNow(),
        user.isMustChangePassword(),
        timestampNow(),
        user.getId());
  }

  private void syncMfaFactor(UserAccount user) {
    if (!StringUtils.hasText(user.getMfaSecret())) {
      jdbcTemplate.update(
          """
          delete from iam_mfa_factors
           where account_id = (select id from iam_accounts where user_id = ?)
             and factor_type = ?
          """,
          user.getId(),
          TOTP);
      return;
    }
    jdbcTemplate.update(
        """
        insert into iam_mfa_factors (
            account_id,
            factor_type,
            encrypted_secret,
            status,
            activated_at,
            disabled_at
        )
        select ia.id, ?, ?, ?, ?, null
          from iam_accounts ia
         where ia.user_id = ?
        on conflict (account_id, factor_type) do update
            set encrypted_secret = excluded.encrypted_secret,
                status = excluded.status,
                activated_at =
                    case
                      when excluded.status = 'ACTIVE'
                      then coalesce(iam_mfa_factors.activated_at, excluded.activated_at)
                      else null
                    end,
                disabled_at = null,
                version = iam_mfa_factors.version + 1
        """,
        TOTP,
        user.getMfaSecret(),
        user.isMfaEnabled() ? "ACTIVE" : "PENDING",
        user.isMfaEnabled() ? timestampNow() : null,
        user.getId());
  }

  private void ensureAccountForPublicId(UUID publicId) {
    userAccountRepository.findByPublicId(publicId).ifPresent(this::syncUser);
  }

  private Long resolveSessionDevice(
      Long accountId, String previousRefreshTokenDigest, SessionDeviceMetadata metadata) {
    if (StringUtils.hasText(previousRefreshTokenDigest)) {
      Long previousDeviceId =
          jdbcTemplate.query(
              "select device_id from iam_sessions where refresh_token_digest = ?",
              rs -> rs.next() ? rs.getObject("device_id", Long.class) : null,
              previousRefreshTokenDigest);
      if (previousDeviceId != null) {
        jdbcTemplate.update(
            "update iam_devices set last_seen_at = ?, version = version + 1 where id = ?",
            timestampNow(),
            previousDeviceId);
        return previousDeviceId;
      }
    }
    SessionDeviceMetadata safeMetadata =
        metadata == null ? new SessionDeviceMetadata("Unknown device", null, null) : metadata;
    return jdbcTemplate.query(
        """
        insert into iam_devices (
            account_id,
            public_id,
            device_label,
            user_agent_hash,
            ip_address_hash,
            created_at,
            last_seen_at
        )
        values (?, ?, ?, ?, ?, ?, ?)
        returning id
        """,
        rs -> rs.next() ? rs.getLong("id") : null,
        accountId,
        UUID.randomUUID(),
        truncate(
            StringUtils.hasText(safeMetadata.deviceLabel())
                ? safeMetadata.deviceLabel()
                : "Unknown device",
            255),
        safeMetadata.userAgentHash(),
        safeMetadata.ipAddressHash(),
        timestampNow(),
        timestampNow());
  }

  private Long resolveAccountId(String userId, String username, String authScopeCode) {
    UUID publicId = parseUuid(userId);
    if (publicId != null) {
      ensureAccountForPublicId(publicId);
      Long byPublicId =
          jdbcTemplate.query(
              "select id from iam_accounts where public_id = ?",
              rs -> rs.next() ? rs.getLong("id") : null,
              publicId);
      if (byPublicId != null) {
        return byPublicId;
      }
    }
    if (StringUtils.hasText(username) && StringUtils.hasText(authScopeCode)) {
      return jdbcTemplate.query(
          "select id from iam_accounts where email = ? and auth_scope_code = ?",
          rs -> rs.next() ? rs.getLong("id") : null,
          normalizeEmail(username),
          normalizeScopeCode(authScopeCode));
    }
    return null;
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

  private Map<String, String> redactMetadata(Map<String, String> metadata) {
    Map<String, String> redacted = new LinkedHashMap<>();
    if (metadata == null) {
      return redacted;
    }
    metadata.forEach(
        (key, value) -> {
          if (!StringUtils.hasText(key)) {
            return;
          }
          String normalizedKey = key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
          if (isSensitiveMetadataKey(normalizedKey)) {
            redacted.put(key, "[REDACTED]");
          } else if (value != null) {
            redacted.put(key, truncate(value, 512));
          }
        });
    return redacted;
  }

  private boolean isSensitiveMetadataKey(String normalizedKey) {
    if (!StringUtils.hasText(normalizedKey)) {
      return false;
    }
    if (SAFE_SCOPE_METADATA_KEYS.contains(normalizedKey)) {
      return false;
    }
    if (SENSITIVE_METADATA_KEYS.stream().anyMatch(normalizedKey::contains)) {
      return true;
    }
    return normalizedKey.contains("code");
  }

  private Map<String, String> fromJsonMap(String metadataJson) {
    if (!StringUtils.hasText(metadataJson)) {
      return Map.of();
    }
    try {
      Map<String, String> parsed =
          objectMapper.readValue(metadataJson, new TypeReference<Map<String, String>>() {});
      return redactMetadata(parsed);
    } catch (Exception ex) {
      return Map.of();
    }
  }

  private Map<String, String> securityEventMetadata(Map<String, String> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return Map.of();
    }
    List<String> allowlist =
        List.of(
            "operation",
            "reason",
            "targetUserId",
            "sessionId",
            "sessionReference",
            "companyCode",
            "authScopeCode",
            "tenantScope",
            "outcome",
            "action");
    Map<String, String> safe = new LinkedHashMap<>();
    for (String key : allowlist) {
      String value = metadata.get(key);
      if (StringUtils.hasText(value)) {
        safe.put(key, value);
      }
    }
    return safe;
  }

  private String normalizeEventTypeFilter(String eventTypeFilter) {
    return StringUtils.hasText(eventTypeFilter)
        ? eventTypeFilter.trim().toUpperCase(Locale.ROOT)
        : null;
  }

  private boolean matchesEventFilter(String eventType, String filter) {
    if (!StringUtils.hasText(filter)) {
      return true;
    }
    String normalizedEvent =
        StringUtils.hasText(eventType) ? eventType.toUpperCase(Locale.ROOT) : "";
    if ("SESSION".equals(filter)) {
      return normalizedEvent.contains("SESSION")
          || normalizedEvent.startsWith("TOKEN")
          || "LOGOUT".equals(normalizedEvent)
          || "LOGIN_SUCCESS".equals(normalizedEvent);
    }
    return normalizedEvent.startsWith(filter);
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String toJson(Map<String, String> metadata) {
    try {
      return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
    } catch (JsonProcessingException ex) {
      return "{}";
    }
  }

  private String firstPresent(Map<String, String> metadata, String key) {
    if (metadata == null || !StringUtils.hasText(key)) {
      return null;
    }
    return truncate(metadata.get(key), 255);
  }

  private String normalizeOutcome(String outcome) {
    if (!StringUtils.hasText(outcome)) {
      return "SUCCESS";
    }
    String normalized = outcome.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "SUCCESS" -> "SUCCESS";
      case "FAILURE" -> "FAILURE";
      case "DENIED", "WARNING" -> "DENIED";
      default -> "SUCCESS";
    };
  }

  private String normalizeEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeNullableEmail(String email) {
    return StringUtils.hasText(email) ? normalizeEmail(email) : null;
  }

  private String normalizeScopeCode(String scopeCode) {
    return scopeCode == null ? null : scopeCode.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeNullableScopeCode(String scopeCode) {
    return StringUtils.hasText(scopeCode) ? normalizeScopeCode(scopeCode) : null;
  }

  private String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String safeReason(String reason) {
    return truncate(StringUtils.hasText(reason) ? reason.trim() : "revoked", 255);
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  private Timestamp timestampNow() {
    return timestamp(Instant.now());
  }

  private Timestamp timestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  private record ReplaySession(
      Long accountId,
      UUID accountPublicId,
      Long deviceId,
      Instant consumedAt,
      String authScopeCode) {}

  private record ExistingSession(Long id, UUID publicId) {}
}
