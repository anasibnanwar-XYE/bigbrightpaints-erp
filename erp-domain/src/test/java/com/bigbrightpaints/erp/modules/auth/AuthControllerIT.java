package com.bigbrightpaints.erp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.core.security.JwtTokenService;
import com.bigbrightpaints.erp.modules.auth.domain.PasswordResetToken;
import com.bigbrightpaints.erp.modules.auth.domain.PasswordResetTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.service.AuthTokenDigests;
import com.bigbrightpaints.erp.modules.auth.service.IamCanonicalStorageService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

public class AuthControllerIT extends AbstractIntegrationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

  @Autowired private TestRestTemplate rest;

  @Autowired private UserAccountRepository userAccountRepository;

  @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private JwtTokenService tokenService;

  @Autowired private IamCanonicalStorageService iamCanonicalStorageService;

  @SpyBean private EmailService emailService;

  private static final String COMPANY_CODE = "ACME";
  private static final String PLATFORM_SCOPE = "PLATFORM";
  private static final String ADMIN_EMAIL = "admin@bbp.com";
  private static final String ADMIN_PASSWORD = "admin123";
  private static final String SUPER_ADMIN_EMAIL = "platform-superadmin@bbp.com";
  private static final String SUPER_ADMIN_LOGIN_VALUE = "ChangeMe123!";
  private static final String USER_EMAIL = "reset-target@bbp.com";
  private static final String USER_PASSWORD = "User@12345";

  @org.junit.jupiter.api.BeforeEach
  void seedUserAndCompany() {
    UserAccount adminUser =
        dataSeeder.ensureUser(
            ADMIN_EMAIL, ADMIN_PASSWORD, "Admin", COMPANY_CODE, java.util.List.of("ROLE_ADMIN"));
    adminUser.setMustChangePassword(false);
    userAccountRepository.save(adminUser);

    UserAccount resetTarget =
        dataSeeder.ensureUser(
            USER_EMAIL,
            USER_PASSWORD,
            "Reset Target",
            COMPANY_CODE,
            java.util.List.of("ROLE_SALES"));
    resetTarget.setMustChangePassword(false);
    userAccountRepository.save(resetTarget);

    UserAccount platformSuperAdmin =
        dataSeeder.ensureUser(
            SUPER_ADMIN_EMAIL,
            SUPER_ADMIN_LOGIN_VALUE,
            "Platform Super Admin",
            PLATFORM_SCOPE,
            java.util.List.of("ROLE_SUPER_ADMIN"));
    platformSuperAdmin.setEnabled(true);
    platformSuperAdmin.setMustChangePassword(false);
    platformSuperAdmin.setFailedLoginAttempts(0);
    platformSuperAdmin.setLockedUntil(null);
    userAccountRepository.save(platformSuperAdmin);
  }

  @Test
  void login_and_me_flow_succeeds() {
    Map<String, Object> body =
        Map.of(
            "email", ADMIN_EMAIL,
            "password", ADMIN_PASSWORD,
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> loginResp = rest.postForEntity("/api/v1/auth/login", body, Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResp.getBody()).isNotNull();
    String accessToken = (String) loginResp.getBody().get("accessToken");
    assertThat(accessToken).isNotBlank();
    Map<String, Object> accessClaims = decodeJwtClaims(accessToken);
    assertThat(accessClaims).containsEntry("companyCode", COMPANY_CODE);
    assertThat(accessClaims).doesNotContainKey("cid");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    ResponseEntity<Map> meResp =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map data = (Map) meResp.getBody().get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("email")).isEqualTo(ADMIN_EMAIL);
    assertThat(data.get("companyCode")).isEqualTo(COMPANY_CODE);
    assertThat(data).doesNotContainKey("companyId");
    List<String> roles = (List<String>) data.get("roles");
    assertThat(roles).isNotNull();
    assertThat(roles).contains("ROLE_ADMIN");
    List<String> permissions = (List<String>) data.get("permissions");
    assertThat(permissions).isNotNull();
  }

  @Test
  void platformSuperAdminLoginAndMeExposePlatformScopeWithoutSecretFields() {
    ResponseEntity<Map> loginResp =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", SUPER_ADMIN_EMAIL,
                "password", SUPER_ADMIN_LOGIN_VALUE,
                "companyCode", PLATFORM_SCOPE),
            Map.class);

    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResp.getHeaders().get(HttpHeaders.SET_COOKIE)).isNullOrEmpty();
    assertThat(loginResp.getBody()).isNotNull();
    assertThat(loginResp.getBody()).containsEntry("tokenType", "Bearer");
    assertThat(loginResp.getBody()).containsEntry("companyCode", PLATFORM_SCOPE);
    assertThat(loginResp.getBody()).containsEntry("scopeType", "PLATFORM");
    @SuppressWarnings("unchecked")
    List<String> loginRoles = (List<String>) loginResp.getBody().get("roles");
    assertThat(loginRoles).contains("ROLE_SUPER_ADMIN");
    String accessToken = loginResp.getBody().get("accessToken").toString();

    ResponseEntity<Map> meResp =
        rest.exchange(
            "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken, PLATFORM_SCOPE)),
            Map.class);

    assertThat(meResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResp.getBody()).isNotNull();
    @SuppressWarnings("unchecked")
    Map<String, Object> meData = (Map<String, Object>) meResp.getBody().get("data");
    assertThat(meData)
        .containsEntry("email", SUPER_ADMIN_EMAIL)
        .containsEntry("companyCode", PLATFORM_SCOPE)
        .containsEntry("scopeType", "PLATFORM");
    @SuppressWarnings("unchecked")
    List<String> meRoles = (List<String>) meData.get("roles");
    assertThat(meRoles).contains("ROLE_SUPER_ADMIN");
    assertThat(meData.keySet())
        .doesNotContain("password", "passwordHash", "accessToken", "refreshToken", "token");
  }

  @Test
  void decodeJwtClaims_acceptsUnpaddedPayloadSegment() {
    String headerSegment =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
    String payloadSegment =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                "{\"companyCode\":\"ACME\",\"sub\":\"1\"}".getBytes(StandardCharsets.UTF_8));
    assertThat(payloadSegment.length() % 4).isNotZero();

    Map<String, Object> claims =
        decodeJwtClaims(headerSegment + "." + payloadSegment + ".signature");

    assertThat(claims).containsEntry("companyCode", "ACME");
    assertThat(claims).containsEntry("sub", "1");
  }

  @Test
  void legacyCompanyIdHeader_doesNotEstablishAuthenticatedContext() {
    String accessToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.set("X-Company-Id", COMPANY_CODE);

    ResponseEntity<Map> response =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsEntry("success", false);
  }

  @Test
  void refresh_token_revoked_after_logout() {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    assertThat(loginPayload).isNotNull();
    String accessToken = loginPayload.get("accessToken").toString();
    String refreshToken = loginPayload.get("refreshToken").toString();

    ResponseEntity<Map> refreshResp =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshToken, "companyCode", COMPANY_CODE),
            Map.class);
    assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> refreshPayload = refreshResp.getBody();
    assertThat(refreshPayload).isNotNull();
    String refreshedAccessToken = refreshPayload.get("accessToken").toString();
    String refreshedRefreshToken = refreshPayload.get("refreshToken").toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(refreshedAccessToken);
    headers.set("X-Company-Code", COMPANY_CODE);
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Void> queryLogoutResp =
        rest.exchange(
            "/api/v1/auth/logout?refreshToken=" + refreshedRefreshToken,
            HttpMethod.POST,
            new HttpEntity<>(headers),
            Void.class);
    assertThat(queryLogoutResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(me(refreshedAccessToken).getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Void> logoutResp =
        rest.exchange(
            "/api/v1/auth/logout",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("refreshToken", refreshedRefreshToken), headers),
            Void.class);
    assertThat(logoutResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    ResponseEntity<Map> revokedRefresh =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshedRefreshToken, "companyCode", COMPANY_CODE),
            Map.class);
    assertThat(revokedRefresh.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    assertThat(me(accessToken).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(me(refreshedAccessToken).getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  void login_lists_current_session_with_sanitized_device_metadata() {
    String userAgentCanary = "ADV_CANARY_UA_PRIVACY_MARKER";
    Map<String, Object> loginPayload =
        loginWithUserAgent(
            ADMIN_EMAIL, ADMIN_PASSWORD, userAgentCanary + "<script>alert(1)</script>\r\nInjected");
    String accessToken = loginPayload.get("accessToken").toString();
    Map<String, Object> accessClaims = decodeJwtClaims(accessToken);
    assertThat(accessClaims.get("sid")).as("access token carries opaque session id").isNotNull();

    ResponseEntity<Map> sessionsResponse = sessions(accessToken);

    assertThat(sessionsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> sessions = responseDataList(sessionsResponse);
    assertThat(sessions).isNotEmpty();
    Map<String, Object> session =
        sessions.stream()
            .filter(row -> accessClaims.get("sid").equals(row.get("sessionId")))
            .findFirst()
            .orElseThrow();
    assertThat(session.get("sessionId")).isEqualTo(accessClaims.get("sid"));
    assertThat(session.get("current")).isEqualTo(true);
    assertThat(session.get("authScopeCode")).isEqualTo(COMPANY_CODE);
    assertThat(session.get("createdAt")).isNotNull();
    assertThat(session.get("lastSeenAt")).isNotNull();
    assertThat(session.get("expiresAt")).isNotNull();
    assertThat(session.keySet())
        .doesNotContain(
            "accessToken",
            "refreshToken",
            "refreshTokenDigest",
            "tokenDigest",
            "passwordHash",
            "mfaSecret",
            "recoveryCodes");
    assertThat(session.get("deviceName").toString()).doesNotContain("<", ">", "\r", "\n");
    assertThat(session.get("userAgent").toString()).doesNotContain("<", ">", "\r", "\n");
    assertThat(session.get("deviceName").toString()).doesNotContain(userAgentCanary);
    assertThat(session.get("userAgent").toString()).doesNotContain(userAgentCanary);
  }

  @Test
  void user_revokes_other_current_and_all_sessions_without_cross_session_leakage() {
    Map<String, Object> deviceA = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    Map<String, Object> deviceB = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String accessA = deviceA.get("accessToken").toString();
    String accessB = deviceB.get("accessToken").toString();
    String refreshB = deviceB.get("refreshToken").toString();

    String sessionB = decodeJwtClaims(accessB).get("sid").toString();
    ResponseEntity<Void> revokeOther =
        rest.exchange(
            "/api/v1/auth/sessions/" + sessionB,
            HttpMethod.DELETE,
            new HttpEntity<>(bearer(accessA)),
            Void.class);

    assertThat(revokeOther.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(me(accessB).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(refresh(refreshB).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(me(accessA).getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseDataList(sessions(accessA)))
        .extracting(row -> row.get("sessionId"))
        .doesNotContain(sessionB);

    ResponseEntity<Void> revokeCurrent =
        rest.exchange(
            "/api/v1/auth/sessions/current",
            HttpMethod.DELETE,
            new HttpEntity<>(bearer(accessA)),
            Void.class);
    assertThat(revokeCurrent.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(me(accessA).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    Map<String, Object> deviceC = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    Map<String, Object> deviceD = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String accessC = deviceC.get("accessToken").toString();
    String accessD = deviceD.get("accessToken").toString();
    String refreshD = deviceD.get("refreshToken").toString();
    ResponseEntity<Void> revokeAll =
        rest.exchange(
            "/api/v1/auth/sessions",
            HttpMethod.DELETE,
            new HttpEntity<>(bearer(accessC)),
            Void.class);

    assertThat(revokeAll.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(me(accessC).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(me(accessD).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(refresh(refreshD).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void refresh_rotation_is_scope_bound_and_replay_revokes_rotated_family() {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String originalAccess = loginPayload.get("accessToken").toString();
    String originalRefresh = loginPayload.get("refreshToken").toString();
    String originalSessionId = decodeJwtClaims(originalAccess).get("sid").toString();
    Map<String, Object> originalSession =
        responseDataList(sessions(originalAccess)).stream()
            .filter(row -> originalSessionId.equals(row.get("sessionId")))
            .findFirst()
            .orElseThrow();

    ResponseEntity<Map> wrongScopeRefresh =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", originalRefresh, "companyCode", "OTHER"),
            Map.class);
    assertThat(wrongScopeRefresh.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    ResponseEntity<Map> firstRefresh = refresh(originalRefresh);
    assertThat(firstRefresh.getStatusCode()).isEqualTo(HttpStatus.OK);
    String rotatedAccess = firstRefresh.getBody().get("accessToken").toString();
    String rotatedRefresh = firstRefresh.getBody().get("refreshToken").toString();
    String rotatedSessionId = decodeJwtClaims(rotatedAccess).get("sid").toString();
    assertThat(rotatedSessionId).isEqualTo(originalSessionId);
    List<Map<String, Object>> rotatedSessions = responseDataList(sessions(rotatedAccess));
    assertThat(rotatedSessions)
        .filteredOn(row -> rotatedSessionId.equals(row.get("sessionId")))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.get("current")).isEqualTo(true);
              assertThat(row.get("createdAt")).isEqualTo(originalSession.get("createdAt"));
              assertThat(row.get("authScopeCode")).isEqualTo(COMPANY_CODE);
            });
    Integer activeSessionLineages =
        jdbcTemplate.queryForObject(
            """
            select count(*)
              from iam_sessions s
             where s.public_id = ?
               and s.auth_scope_code = ?
               and s.revoked_at is null
               and s.consumed_at is null
            """,
            Integer.class,
            UUID.fromString(rotatedSessionId),
            COMPANY_CODE);
    assertThat(activeSessionLineages).isEqualTo(1);

    iamCanonicalStorageService.recordSecurityEvent(
        "SESSION_SCOPE_REDACTION_PROBE",
        "SUCCESS",
        Map.of(
            "operation",
            "scope_probe",
            "companyCode",
            COMPANY_CODE,
            "authScopeCode",
            COMPANY_CODE,
            "recoveryCode",
            "123456"),
        decodeJwtClaims(rotatedAccess).get("sub").toString(),
        ADMIN_EMAIL,
        null,
        COMPANY_CODE);
    String metadataJson =
        jdbcTemplate.queryForObject(
            """
            select metadata::text
              from iam_security_events
             where event_type = 'SESSION_SCOPE_REDACTION_PROBE'
             order by occurred_at desc, id desc
             limit 1
            """,
            String.class);
    assertThat(metadataJson)
        .contains("\"companyCode\": \"ACME\"")
        .contains("\"authScopeCode\": \"ACME\"")
        .contains("\"recoveryCode\": \"[REDACTED]\"");

    UserAccount admin = scopedUser(ADMIN_EMAIL);
    Long iamAccountId =
        jdbcTemplate.queryForObject(
            "select id from iam_accounts where public_id = ?", Long.class, admin.getPublicId());
    Long companyId = admin.getCompany().getId();
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
        values (?, ?, ?, ?, ?, ?, ?, ?::jsonb, now())
        """,
        iamAccountId,
        iamAccountId,
        companyId,
        " [ReDaCtEd] ",
        "SESSION_PRE_REDACTED_SCOPE_PROBE",
        "SUCCESS",
        "pre_redacted_scope_probe",
        """
        {
          "operation": "pre_redacted_scope_probe",
          "companyCode": " [REDACTED] ",
          "authScopeCode": " <redacted> ",
          "tenantScope": "   ",
          "sessionId": "pre-redacted-safe-session",
          "token": "raw-token-must-not-appear",
          "refreshTokenDigest": "digest-must-not-appear",
          "recoveryCode": "raw-recovery-code-must-not-appear"
        }
        """);

    ResponseEntity<Map> eventResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?type=SESSION",
            HttpMethod.GET,
            new HttpEntity<>(bearer(rotatedAccess)),
            Map.class);
    assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> eventRows = responsePageContent(eventResponse);
    assertThat(eventRows)
        .anySatisfy(
            row -> {
              assertThat(row.get("companyCode")).isEqualTo(COMPANY_CODE);
              assertThat(row.get("authScopeCode")).isEqualTo(COMPANY_CODE);
              assertThat(row.get("companyCode")).isNotEqualTo("[REDACTED]");
            });
    assertThat(eventRows)
        .filteredOn(row -> "SESSION_PRE_REDACTED_SCOPE_PROBE".equals(row.get("type")))
        .isNotEmpty()
        .anySatisfy(
            row -> {
              assertThat(row.get("companyCode")).isEqualTo(COMPANY_CODE);
              assertThat(row.get("authScopeCode")).isEqualTo(COMPANY_CODE);
              @SuppressWarnings("unchecked")
              Map<String, Object> metadata = (Map<String, Object>) row.get("metadata");
              assertThat(metadata).containsEntry("operation", "pre_redacted_scope_probe");
              assertThat(metadata).doesNotContainKeys("companyCode", "authScopeCode");
              assertThat(row.toString())
                  .doesNotContain("raw-token-must-not-appear")
                  .doesNotContain("digest-must-not-appear")
                  .doesNotContain("raw-recovery-code-must-not-appear");
            });

    ResponseEntity<Map> replay = refresh(originalRefresh);
    assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(me(rotatedAccess).getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(refresh(rotatedRefresh).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Integer replayEvents =
        jdbcTemplate.queryForObject(
            "select count(*) from iam_security_events where event_type = 'SESSION_REFRESH_REPLAY'",
            Integer.class);
    assertThat(replayEvents).isNotNull().isGreaterThanOrEqualTo(1);
  }

  @Test
  void bearer_tokens_without_active_sid_fail_closed() {
    UserAccount admin = scopedUser(ADMIN_EMAIL);
    Instant issuedAt = Instant.now();
    String sidlessAccessToken =
        tokenService.generateAccessToken(
            admin.getPublicId().toString(),
            COMPANY_CODE,
            Map.of("email", ADMIN_EMAIL, "name", "Admin"),
            issuedAt);
    String inactiveSidAccessToken =
        tokenService.generateAccessToken(
            admin.getPublicId().toString(),
            COMPANY_CODE,
            Map.of("email", ADMIN_EMAIL, "name", "Admin", "sid", UUID.randomUUID().toString()),
            issuedAt);

    assertThat(me(sidlessAccessToken).getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(me(inactiveSidAccessToken).getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  void staleTokensIssuedBeforeMustChangePasswordAreDenied() {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String staleAccessToken = loginPayload.get("accessToken").toString();
    String staleRefreshToken = loginPayload.get("refreshToken").toString();

    markMustChangePassword(ADMIN_EMAIL);

    assertThat(me(staleAccessToken).getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(refresh(staleRefreshToken).getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);

    Map<String, Object> freshLoginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    assertThat(freshLoginPayload).containsEntry("mustChangePassword", true);
    String corridorToken = freshLoginPayload.get("accessToken").toString();
    ResponseEntity<Map> meResponse = me(corridorToken);
    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<?, ?> meData = (Map<?, ?>) meResponse.getBody().get("data");
    assertThat(meData).containsEntry("mustChangePassword", true);
  }

  @Test
  void disabledPlatformSuperAdminCannotUsePreviouslyIssuedTokens() {
    ResponseEntity<Map> loginResp =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", SUPER_ADMIN_EMAIL,
                "password", SUPER_ADMIN_LOGIN_VALUE,
                "companyCode", PLATFORM_SCOPE),
            Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResp.getBody()).isNotNull();
    String accessToken = loginResp.getBody().get("accessToken").toString();
    String refreshToken = loginResp.getBody().get("refreshToken").toString();

    UserAccount superAdmin =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(SUPER_ADMIN_EMAIL, PLATFORM_SCOPE)
            .orElseThrow();
    superAdmin.setEnabled(false);
    userAccountRepository.save(superAdmin);

    ResponseEntity<Map> meResponse =
        rest.exchange(
            "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken, PLATFORM_SCOPE)),
            Map.class);
    assertThat(meResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshToken, "companyCode", PLATFORM_SCOPE),
            Map.class);
    assertThat(refreshResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void concurrent_refresh_replay_race_settles_to_revoked_family() throws Exception {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String originalRefresh = loginPayload.get("refreshToken").toString();

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      List<Future<ResponseEntity<Map>>> attempts =
          List.of(
              executor.submit(() -> concurrentRefreshAttempt(originalRefresh, ready, start)),
              executor.submit(() -> concurrentRefreshAttempt(originalRefresh, ready, start)));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      List<ResponseEntity<Map>> responses =
          List.of(
              attempts.get(0).get(10, TimeUnit.SECONDS), attempts.get(1).get(10, TimeUnit.SECONDS));
      long successes =
          responses.stream().filter(response -> response.getStatusCode().is2xxSuccessful()).count();
      assertThat(successes).isLessThanOrEqualTo(1);

      responses.stream()
          .filter(response -> response.getStatusCode().is2xxSuccessful())
          .findFirst()
          .ifPresent(
              response -> {
                String rotatedAccess = response.getBody().get("accessToken").toString();
                String rotatedRefresh = response.getBody().get("refreshToken").toString();
                assertThat(refresh(originalRefresh).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(me(rotatedAccess).getStatusCode())
                    .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
                assertThat(refresh(rotatedRefresh).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
              });
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void canonical_self_profile_session_and_security_routes_are_routable_after_hard_cut() {
    String accessToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString();

    ResponseEntity<Map> profileResponse =
        rest.exchange(
            "/api/v1/auth/me/profile",
            HttpMethod.PATCH,
            new HttpEntity<>(Map.of("preferredName", "Admin"), bearerJson(accessToken)),
            Map.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> contactResponse =
        rest.exchange(
            "/api/v1/auth/me/contact",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of("secondaryEmail", "secondary@bbp.com"), bearerJson(accessToken)),
            Map.class);
    assertThat(contactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertRoutePresent(
        "/api/v1/auth/me/security", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)));
    assertRoutePresent(
        "/api/v1/auth/me/security-events", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)));
    assertRoutePresent("/api/v1/auth/mfa", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)));
    assertRoutePresent(
        "/api/v1/auth/mfa/recovery-codes/regenerate",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("code", "000000"), bearerJson(accessToken)));
    assertRoutePresent(
        "/api/v1/auth/sessions", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)));
    assertRoutePresent(
        "/api/v1/auth/sessions/probe-session-id",
        HttpMethod.DELETE,
        new HttpEntity<>(bearer(accessToken)));
    assertRoutePresent(
        "/api/v1/auth/sessions/current",
        HttpMethod.DELETE,
        new HttpEntity<>(bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString())));
    assertRoutePresent(
        "/api/v1/auth/sessions",
        HttpMethod.DELETE,
        new HttpEntity<>(bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString())));

    assertRouteAbsent(
        "/api/v1/auth/profile",
        HttpMethod.GET,
        new HttpEntity<>(bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString())));
    assertRouteAbsent(
        "/api/v1/auth/password/forgot/superadmin",
        HttpMethod.POST,
        new HttpEntity<>(Map.of("email", ADMIN_EMAIL, "companyCode", COMPANY_CODE), jsonHeaders()));
  }

  @Test
  void retired_auth_aliases_are_absent_for_every_actor_state() {
    UserAccount mustChangeUser =
        dataSeeder.ensureUser(
            "must-change-auth-retired@bbp.com",
            "MustChange123!",
            "Must Change Auth Retired",
            COMPANY_CODE,
            java.util.List.of("ROLE_SALES"));
    mustChangeUser.setMustChangePassword(true);
    userAccountRepository.save(mustChangeUser);

    List<HttpHeaders> actorHeaders =
        List.of(
            new HttpHeaders(),
            bearer(login(USER_EMAIL, USER_PASSWORD).get("accessToken").toString()),
            bearer(login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString()),
            bearer(
                login("must-change-auth-retired@bbp.com", "MustChange123!")
                    .get("accessToken")
                    .toString()));

    for (HttpHeaders headers : actorHeaders) {
      assertRouteAbsent("/api/v1/auth/profile", HttpMethod.GET, new HttpEntity<>(headers));
      assertRouteAbsent(
          "/api/v1/auth/profile", HttpMethod.PATCH, new HttpEntity<>(Map.of(), headers));
      HttpHeaders jsonHeaders = new HttpHeaders();
      jsonHeaders.putAll(headers);
      jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
      assertRouteAbsent(
          "/api/v1/auth/password/forgot/superadmin",
          HttpMethod.POST,
          new HttpEntity<>(Map.of("email", ADMIN_EMAIL, "companyCode", COMPANY_CODE), jsonHeaders));
    }
  }

  @Test
  void self_profile_and_contact_updates_are_allowlisted_validated_audited_and_unverified() {
    List<String> resetRecipients = Collections.synchronizedList(new ArrayList<>());
    doAnswer(
            invocation -> {
              resetRecipients.add(invocation.getArgument(0, String.class));
              return null;
            })
        .when(emailService)
        .sendPasswordResetEmailRequired(anyString(), anyString(), anyString(), anyString());
    String accessToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString();
    UserAccount before = scopedUser(ADMIN_EMAIL);
    before.setJobTitle("Tenant-controlled title");
    userAccountRepository.save(before);

    ResponseEntity<Map> forbiddenProfileAttempt =
        rest.exchange(
            "/api/v1/auth/me/profile",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of(
                    "email", "attacker@bbp.com",
                    "companyCode", "OTHER",
                    "roles", List.of("ROLE_SUPER_ADMIN"),
                    "enabled", false,
                    "jobTitle", "Self-promoted"),
                bearerJson(accessToken)),
            Map.class);
    assertThat(forbiddenProfileAttempt.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.OK);
    UserAccount afterForbiddenProfile = scopedUser(ADMIN_EMAIL);
    assertThat(afterForbiddenProfile.getEmail()).isEqualTo(ADMIN_EMAIL);
    assertThat(afterForbiddenProfile.getAuthScopeCode()).isEqualTo(COMPANY_CODE);
    assertThat(afterForbiddenProfile.isEnabled()).isTrue();
    assertThat(afterForbiddenProfile.getJobTitle()).isEqualTo("Tenant-controlled title");
    assertThat(afterForbiddenProfile.getRoles())
        .extracting(role -> role.getName())
        .doesNotContain("ROLE_SUPER_ADMIN");

    ResponseEntity<Map> profileResponse =
        rest.exchange(
            "/api/v1/auth/me/profile",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of(
                    "preferredName", "Admin Self",
                    "profilePictureUrl", "https://img.example/avatar.png"),
                bearerJson(accessToken)),
            Map.class);
    assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> profile = responseData(profileResponse);
    assertThat(profile)
        .containsEntry("preferredName", "Admin Self")
        .containsEntry("profilePictureUrl", "https://img.example/avatar.png");
    assertThat(profile.keySet()).containsOnly("preferredName", "profilePictureUrl");

    ResponseEntity<Map> contactResponse =
        rest.exchange(
            "/api/v1/auth/me/contact",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of(
                    "secondaryEmail", "RESET-SHADOW@BBP.COM",
                    "phoneSecondary", "+91-555-0100"),
                bearerJson(accessToken)),
            Map.class);
    assertThat(contactResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> contact = responseData(contactResponse);
    assertThat(contact)
        .containsEntry("secondaryEmail", "reset-shadow@bbp.com")
        .containsEntry("phoneSecondary", "+91-555-0100")
        .containsEntry("secondaryEmailVerified", false)
        .containsEntry("phoneSecondaryVerified", false);
    assertThat(contact.keySet())
        .containsOnly(
            "secondaryEmail", "phoneSecondary", "secondaryEmailVerified", "phoneSecondaryVerified");

    ResponseEntity<Map> invalidContactResponse =
        rest.exchange(
            "/api/v1/auth/me/contact",
            HttpMethod.PATCH,
            new HttpEntity<>(Map.of("secondaryEmail", "not-an-email"), bearerJson(accessToken)),
            Map.class);
    assertThat(invalidContactResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(scopedUser(ADMIN_EMAIL).getSecondaryEmail()).isEqualTo("reset-shadow@bbp.com");

    rest.postForEntity(
        "/api/v1/auth/password/forgot",
        Map.of("email", ADMIN_EMAIL, "companyCode", COMPANY_CODE),
        Map.class);
    rest.postForEntity(
        "/api/v1/auth/password/forgot",
        Map.of("email", "reset-shadow@bbp.com", "companyCode", COMPANY_CODE),
        Map.class);
    assertThat(resetRecipients).contains(ADMIN_EMAIL);
    assertThat(resetRecipients).doesNotContain("reset-shadow@bbp.com");
    jdbcTemplate.update(
        "delete from password_reset_tokens where user_id = ?", scopedUser(ADMIN_EMAIL).getId());

    String profileAudit =
        awaitSecurityEventMetadata(ADMIN_EMAIL, "DATA_UPDATE", "self_profile_update");
    String contactAudit =
        awaitSecurityEventMetadata(ADMIN_EMAIL, "DATA_UPDATE", "self_contact_update");
    assertThat(profileAudit)
        .contains("\"changedFields\": \"preferredName,profilePictureUrl\"")
        .doesNotContain("Admin Self", "https://img.example/avatar.png");
    assertThat(contactAudit)
        .contains("\"changedFields\": \"phoneSecondary,secondaryEmail\"")
        .doesNotContain("reset-shadow@bbp.com", "+91-555-0100");
  }

  @Test
  void self_security_summary_and_history_are_stable_subject_bound_and_privacy_safe() {
    String suffix = Long.toString(System.nanoTime());
    String email = "history-" + suffix + "@bbp.com";
    String renamedEmail = "history-renamed-" + suffix + "@bbp.com";
    String password = "HistoryUser123!";
    UserAccount historyUser =
        dataSeeder.ensureUser(email, password, "History User", COMPANY_CODE, List.of("ROLE_SALES"));
    historyUser.setMustChangePassword(false);
    userAccountRepository.save(historyUser);
    iamCanonicalStorageService.syncUser(historyUser);
    String accessToken = login(email, password).get("accessToken").toString();
    Long companyId = historyUser.getCompany().getId();

    iamCanonicalStorageService.recordSecurityEvent(
        "SELF_HISTORY_BEFORE_EMAIL_CHANGE",
        "SUCCESS",
        Map.of(
            "operation",
            "before_email_change",
            "sessionId",
            "raw-self-session-id",
            "refreshToken",
            "raw-refresh-token"),
        historyUser.getPublicId().toString(),
        email,
        companyId,
        COMPANY_CODE);

    historyUser = scopedUser(email);
    historyUser.setEmail(renamedEmail);
    userAccountRepository.save(historyUser);
    iamCanonicalStorageService.syncUser(historyUser);

    iamCanonicalStorageService.recordSecurityEvent(
        "SELF_HISTORY_AFTER_EMAIL_CHANGE",
        "SUCCESS",
        Map.of("operation", "after_email_change", "sessionReference", "raw-session-reference"),
        historyUser.getPublicId().toString(),
        renamedEmail,
        companyId,
        COMPANY_CODE);

    UserAccount otherUser =
        dataSeeder.ensureUser(
            "history-other-" + suffix + "@bbp.com",
            password,
            "Other History User",
            COMPANY_CODE,
            List.of("ROLE_SALES"));
    iamCanonicalStorageService.syncUser(otherUser);
    iamCanonicalStorageService.recordSecurityEvent(
        "SELF_HISTORY_OTHER_USER",
        "SUCCESS",
        Map.of("operation", "other_user_history"),
        otherUser.getPublicId().toString(),
        otherUser.getEmail(),
        otherUser.getCompany().getId(),
        COMPANY_CODE);

    ResponseEntity<Map> summaryResponse =
        rest.exchange(
            "/api/v1/auth/me/security",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(summaryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> summary = responseData(summaryResponse);
    assertThat(summary)
        .containsEntry("mfaEnabled", false)
        .containsEntry("mustChangePassword", false)
        .containsEntry("locked", false);
    assertThat(((Number) summary.get("activeSessionCount")).intValue()).isGreaterThanOrEqualTo(1);
    assertThat(summary.toString())
        .doesNotContain("passwordHash", "refreshToken", "mfaSecret", "recoveryCode");

    ResponseEntity<Map> historyResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?limit=10",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(historyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> historyPage = responseData(historyResponse);
    assertThat(historyPage)
        .containsEntry("page", 0)
        .containsEntry("size", 10)
        .containsKeys("content", "totalElements", "totalPages");
    List<Map<String, Object>> events = responsePageContent(historyResponse);
    assertThat(events)
        .extracting(event -> event.get("type"))
        .contains("SELF_HISTORY_BEFORE_EMAIL_CHANGE", "SELF_HISTORY_AFTER_EMAIL_CHANGE")
        .doesNotContain("SELF_HISTORY_OTHER_USER");
    assertThat(indexOfEvent(events, "SELF_HISTORY_AFTER_EMAIL_CHANGE"))
        .isLessThan(indexOfEvent(events, "SELF_HISTORY_BEFORE_EMAIL_CHANGE"));
    assertThat(historyResponse.getBody().toString())
        .doesNotContain(
            "raw-self-session-id",
            "raw-session-reference",
            "raw-refresh-token",
            "refreshToken",
            "accessToken",
            "passwordHash",
            "mfaSecret",
            "recoveryCode");
    assertThat(events)
        .allSatisfy(
            event ->
                assertThat(event.keySet()).doesNotContain("actor", "targetUserId", "sessionId"));
  }

  @Test
  void self_security_summary_reports_locked_only_for_active_lock_window() {
    String suffix = Long.toString(System.nanoTime());
    String email = "lock-window-" + suffix + "@bbp.com";
    String password = "LockWindow123!";
    dataSeeder.ensureUser(email, password, "Lock Window", COMPANY_CODE, List.of("ROLE_SALES"));
    String accessToken = login(email, password).get("accessToken").toString();

    UserAccount user = scopedUser(email);
    user.setLockedUntil(Instant.now().minusSeconds(60));
    userAccountRepository.save(user);

    ResponseEntity<Map> expiredLockResponse =
        rest.exchange(
            "/api/v1/auth/me/security",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(expiredLockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseData(expiredLockResponse)).containsEntry("locked", false);

    user = scopedUser(email);
    user.setLockedUntil(Instant.now().plusSeconds(300));
    userAccountRepository.save(user);

    ResponseEntity<Map> activeLockResponse =
        rest.exchange(
            "/api/v1/auth/me/security",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(activeLockResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void self_security_history_filters_before_bounded_stable_pagination() {
    String suffix = Long.toString(System.nanoTime());
    String email = "history-page-" + suffix + "@bbp.com";
    String password = "HistoryUser123!";
    UserAccount pagingUser =
        dataSeeder.ensureUser(
            email, password, "History Page User", COMPANY_CODE, List.of("ROLE_SALES"));
    pagingUser.setMustChangePassword(false);
    userAccountRepository.save(pagingUser);
    iamCanonicalStorageService.syncUser(pagingUser);
    Long iamAccountId =
        jdbcTemplate.queryForObject(
            "select id from iam_accounts where public_id = ?",
            Long.class,
            pagingUser.getPublicId());
    Long companyId = pagingUser.getCompany().getId();
    Instant fixedTime = Instant.parse("2026-04-28T00:00:00Z");

    insertSecurityEvent(iamAccountId, companyId, "PAGE_SESSION_FIRST", fixedTime);
    insertSecurityEvent(iamAccountId, companyId, "PAGE_SESSION_SECOND", fixedTime);
    insertSecurityEvent(iamAccountId, companyId, "PAGE_SESSION_THIRD", fixedTime);
    insertSecurityEvent(iamAccountId, companyId, "PAGE_PROFILE_NEWER_1", fixedTime.plusSeconds(10));
    insertSecurityEvent(iamAccountId, companyId, "PAGE_PROFILE_NEWER_2", fixedTime.plusSeconds(10));

    String accessToken = login(email, password).get("accessToken").toString();
    ResponseEntity<Map> firstPageResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?type=PAGE_SESSION&page=0&size=2",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);

    assertThat(firstPageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> page = responseData(firstPageResponse);
    assertThat(page)
        .containsEntry("page", 0)
        .containsEntry("size", 2)
        .containsEntry("totalElements", 3)
        .containsEntry("totalPages", 2);
    List<Map<String, Object>> firstPage = responsePageContent(firstPageResponse);
    assertThat(firstPage)
        .extracting(event -> event.get("type"))
        .containsExactly("PAGE_SESSION_THIRD", "PAGE_SESSION_SECOND");
    assertThat(firstPageResponse.getBody().toString()).doesNotContain("PAGE_PROFILE_NEWER");

    ResponseEntity<Map> boundedSizeResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?type=PAGE_SESSION&size=500",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(boundedSizeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseData(boundedSizeResponse))
        .containsEntry("size", 100)
        .containsEntry("totalElements", 3);

    ResponseEntity<Map> boundedLimitResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?type=PAGE_SESSION&limit=500",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(boundedLimitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseData(boundedLimitResponse)).containsEntry("size", 100);

    ResponseEntity<Map> sizePrecedenceResponse =
        rest.exchange(
            "/api/v1/auth/me/security-events?type=PAGE_SESSION&size=2&limit=500",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(sizePrecedenceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseData(sizePrecedenceResponse)).containsEntry("size", 2);
  }

  @Test
  void password_change_revokes_existing_access_and_refresh_tokens() {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String accessToken = loginPayload.get("accessToken").toString();
    String refreshToken = loginPayload.get("refreshToken").toString();

    HttpHeaders headers = bearerJson(accessToken);
    ResponseEntity<Map> changeResponse =
        rest.exchange(
            "/api/v1/auth/password/change",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "currentPassword", ADMIN_PASSWORD,
                    "newPassword", "NewAdmin123!",
                    "confirmPassword", "NewAdmin123!"),
                headers),
            Map.class);

    assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me(accessToken).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshToken, "companyCode", COMPANY_CODE),
            Map.class);
    assertThat(refreshResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);

    Map<String, Object> reloginPayload = login(ADMIN_EMAIL, "NewAdmin123!");
    assertThat(reloginPayload.get("accessToken")).isNotNull();
  }

  @Test
  void password_reset_revokes_existing_access_and_refresh_tokens() {
    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String accessToken = loginPayload.get("accessToken").toString();
    String refreshToken = loginPayload.get("refreshToken").toString();

    UserAccount user = scopedUser(ADMIN_EMAIL);
    passwordResetTokenRepository.deleteByUser(user);
    String resetToken = "digest-reset-token";
    passwordResetTokenRepository.save(
        PasswordResetToken.digestOnly(
            user,
            passwordResetDigest(resetToken),
            AuthTokenDigests.DIGEST_ALGORITHM,
            AuthTokenDigests.DIGEST_VERSION,
            Instant.now().plusSeconds(600)));

    ResponseEntity<Map> resetResponse =
        rest.postForEntity(
            "/api/v1/auth/password/reset",
            Map.of(
                "token", resetToken,
                "newPassword", "ResetAdmin123!",
                "confirmPassword", "ResetAdmin123!"),
            Map.class);

    assertThat(resetResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(me(accessToken).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshToken, "companyCode", COMPANY_CODE),
            Map.class);
    assertThat(refreshResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);

    Map<String, Object> reloginPayload = login(ADMIN_EMAIL, "ResetAdmin123!");
    assertThat(reloginPayload.get("accessToken")).isNotNull();
  }

  @Test
  void
      must_change_password_user_can_detect_corridor_but_is_blocked_from_admin_surface_until_password_changed() {
    markMustChangePassword(ADMIN_EMAIL);

    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    assertThat(loginPayload.get("mustChangePassword")).isEqualTo(true);

    String accessToken = loginPayload.get("accessToken").toString();
    ResponseEntity<Map> meResponse =
        rest.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)), Map.class);

    assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(meResponse.getBody()).isNotNull();
    Map<?, ?> meData = (Map<?, ?>) meResponse.getBody().get("data");
    assertThat(meData).isNotNull();
    assertThat(meData.get("mustChangePassword")).isEqualTo(true);

    ResponseEntity<Map> retiredProfileResponse =
        rest.exchange(
            "/api/v1/auth/profile",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(retiredProfileResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> retiredForgotSuperadminResponse =
        rest.exchange(
            "/api/v1/auth/password/forgot/superadmin",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("email", ADMIN_EMAIL, "companyCode", COMPANY_CODE), bearerJson(accessToken)),
            Map.class);
    assertThat(retiredForgotSuperadminResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> retiredAdminSettingsResponse =
        rest.exchange(
            "/api/v1/admin/settings",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(retiredAdminSettingsResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> retiredAdminRolesResponse =
        rest.exchange(
            "/api/v1/admin/roles",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(retiredAdminRolesResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    ResponseEntity<Map> blockedAdminResponse =
        rest.exchange(
            "/api/v1/admin/users",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken)),
            Map.class);
    assertThat(blockedAdminResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    ResponseEntity<Map> changePasswordResponse =
        rest.exchange(
            "/api/v1/auth/password/change",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "currentPassword", ADMIN_PASSWORD,
                    "newPassword", "TempChanged123!",
                    "confirmPassword", "TempChanged123!"),
                bearerJson(accessToken)),
            Map.class);
    assertThat(changePasswordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    assertThat(me(accessToken).getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    Map<String, Object> reloginPayload = login(ADMIN_EMAIL, "TempChanged123!");
    assertThat(reloginPayload.get("mustChangePassword")).isEqualTo(false);

    ResponseEntity<Map> adminResponseAfterPasswordChange =
        rest.exchange(
            "/api/v1/admin/users",
            HttpMethod.GET,
            new HttpEntity<>(bearer(reloginPayload.get("accessToken").toString())),
            Map.class);
    assertThat(adminResponseAfterPasswordChange.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void must_change_password_corridor_still_enforces_company_header_matching() {
    markMustChangePassword(ADMIN_EMAIL);

    Map<String, Object> loginPayload = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    String accessToken = loginPayload.get("accessToken").toString();

    ResponseEntity<Map> mismatchedCompanyResponse =
        rest.exchange(
            "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken, "OTHER")),
            Map.class);

    assertThat(mismatchedCompanyResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void tenantBindingMismatch_returnsControlledAuthErrorContract() {
    String accessToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString();

    ResponseEntity<Map> mismatchedCompanyResponse =
        rest.exchange(
            "/api/v1/auth/me",
            HttpMethod.GET,
            new HttpEntity<>(bearer(accessToken, "OTHER")),
            Map.class);

    assertThat(mismatchedCompanyResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(mismatchedCompanyResponse.getBody()).isNotNull();
    assertThat(mismatchedCompanyResponse.getBody()).containsEntry("success", false);
    assertThat(mismatchedCompanyResponse.getBody()).containsEntry("message", "Access denied");
    Object payload = mismatchedCompanyResponse.getBody().get("data");
    assertThat(payload).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> error = (Map<String, Object>) payload;
    assertThat(error).containsEntry("code", "AUTH_004");
    assertThat(error).containsEntry("message", "Access denied");
    assertThat(error).containsEntry("reason", "COMPANY_CONTEXT_MISMATCH");
    assertThat(error)
        .containsEntry(
            "reasonDetail", "Company header does not match authenticated company context");
    assertThat(error).containsKey("traceId");
  }

  @Test
  void overlappingPublicAndAdminResetRequests_leaveLatestResetLinkUsable() throws Exception {
    UserAccount resetTarget = scopedUser(USER_EMAIL);
    String adminAccessToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken").toString();

    CountDownLatch bothEmailsQueued = new CountDownLatch(2);
    List<String> deliveredTokens = Collections.synchronizedList(new ArrayList<>());
    doAnswer(
            invocation -> {
              deliveredTokens.add(invocation.getArgument(2, String.class));
              bothEmailsQueued.countDown();
              assertThat(bothEmailsQueued.await(5, TimeUnit.SECONDS)).isTrue();
              return null;
            })
        .when(emailService)
        .sendPasswordResetEmailRequired(
            eq(USER_EMAIL), eq("Reset Target"), anyString(), eq(COMPANY_CODE));

    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<ResponseEntity<Map>> publicForgot =
          executor.submit(
              () ->
                  rest.exchange(
                      "/api/v1/auth/password/forgot",
                      HttpMethod.POST,
                      new HttpEntity<>(
                          Map.of("email", USER_EMAIL, "companyCode", COMPANY_CODE), jsonHeaders()),
                      Map.class));
      Future<ResponseEntity<Map>> adminForceReset =
          executor.submit(
              () ->
                  rest.exchange(
                      "/api/v1/admin/users/" + resetTarget.getId() + "/force-reset-password",
                      HttpMethod.POST,
                      new HttpEntity<>(bearer(adminAccessToken)),
                      Map.class));

      assertThat(publicForgot.get(5, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(adminForceReset.get(5, TimeUnit.SECONDS).getStatusCode()).isEqualTo(HttpStatus.OK);
    } finally {
      executor.shutdownNow();
    }

    assertThat(deliveredTokens).hasSize(2);
    ResponseEntity<Map> staleReset = resetPassword(deliveredTokens.getFirst(), "ResetUser123!");
    ResponseEntity<Map> latestReset = resetPassword(deliveredTokens.getLast(), "ResetUser123!");

    assertThat(staleReset.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(staleReset.getBody()).isNotNull();
    assertThat(staleReset.getBody().get("message")).isEqualTo("Invalid or expired token");
    assertThat(latestReset.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(latestReset.getBody()).isNotNull();
    assertThat(latestReset.getBody().get("success")).isEqualTo(true);
  }

  private Map<String, Object> login(String email, String password) {
    Map<String, Object> body =
        Map.of(
            "email", email,
            "password", password,
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> loginResp = rest.postForEntity("/api/v1/auth/login", body, Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResp.getBody()).isNotNull();
    return loginResp.getBody();
  }

  private Map<String, Object> loginWithUserAgent(String email, String password, String userAgent) {
    HttpHeaders headers = jsonHeaders();
    headers.set("User-Agent", userAgent);
    ResponseEntity<Map> loginResp =
        rest.exchange(
            "/api/v1/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("email", email, "password", password, "companyCode", COMPANY_CODE), headers),
            Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResp.getBody()).isNotNull();
    return loginResp.getBody();
  }

  private ResponseEntity<Map> refresh(String refreshToken) {
    return rest.postForEntity(
        "/api/v1/auth/refresh-token",
        Map.of("refreshToken", refreshToken, "companyCode", COMPANY_CODE),
        Map.class);
  }

  private ResponseEntity<Map> concurrentRefreshAttempt(
      String refreshToken, CountDownLatch ready, CountDownLatch start) throws Exception {
    ready.countDown();
    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
    return refresh(refreshToken);
  }

  private ResponseEntity<Map> sessions(String accessToken) {
    return rest.exchange(
        "/api/v1/auth/sessions", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)), Map.class);
  }

  private List<Map<String, Object>> responseDataList(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    Object data = response.getBody().get("data");
    assertThat(data).isInstanceOf(List.class);
    return (List<Map<String, Object>>) data;
  }

  private List<Map<String, Object>> responsePageContent(ResponseEntity<Map> response) {
    Map<String, Object> page = responseData(response);
    Object content = page.get("content");
    assertThat(content).isInstanceOf(List.class);
    return (List<Map<String, Object>>) content;
  }

  private Map<String, Object> responseData(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    Object data = response.getBody().get("data");
    assertThat(data).isInstanceOf(Map.class);
    return (Map<String, Object>) data;
  }

  private void insertSecurityEvent(
      Long iamAccountId, Long companyId, String eventType, Instant occurredAt) {
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
        iamAccountId,
        iamAccountId,
        companyId,
        COMPANY_CODE,
        eventType,
        "SUCCESS",
        "pagination_contract",
        "{\"operation\":\"pagination_contract\"}",
        java.sql.Timestamp.from(occurredAt));
  }

  private int indexOfEvent(List<Map<String, Object>> events, String eventType) {
    for (int i = 0; i < events.size(); i++) {
      if (eventType.equals(events.get(i).get("type"))) {
        return i;
      }
    }
    throw new AssertionError("Missing security event " + eventType);
  }

  private String awaitSecurityEventMetadata(String email, String eventType, String operation) {
    UUID publicId = scopedUser(email).getPublicId();
    for (int i = 0; i < 30; i++) {
      String metadata =
          jdbcTemplate.query(
              """
              select e.metadata::text
                from iam_security_events e
                join iam_accounts ia on ia.id = e.account_id
               where ia.public_id = ?
                 and e.event_type = ?
                 and e.metadata ->> 'operation' = ?
               order by e.occurred_at desc, e.id desc
               limit 1
              """,
              rs -> rs.next() ? rs.getString(1) : null,
              publicId,
              eventType,
              operation);
      if (metadata != null) {
        return metadata;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted waiting for security event", ex);
      }
    }
    throw new AssertionError("Security event not recorded for operation " + operation);
  }

  private void assertRouteAbsent(String path, HttpMethod method, HttpEntity<?> entity) {
    ResponseEntity<Map> response = rest.exchange(path, method, entity, Map.class);
    assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
  }

  private void assertRoutePresent(String path, HttpMethod method, HttpEntity<?> entity) {
    ResponseEntity<Map> response = rest.exchange(path, method, entity, Map.class);
    assertThat(response.getStatusCode())
        .isNotIn(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED);
  }

  private ResponseEntity<Map> me(String accessToken) {
    return rest.exchange(
        "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(bearer(accessToken)), Map.class);
  }

  private ResponseEntity<Map> resetPassword(String token, String newPassword) {
    return rest.exchange(
        "/api/v1/auth/password/reset",
        HttpMethod.POST,
        new HttpEntity<>(
            Map.of(
                "token", token,
                "newPassword", newPassword,
                "confirmPassword", newPassword),
            jsonHeaders()),
        Map.class);
  }

  private void markMustChangePassword(String email) {
    UserAccount user = scopedUser(email);
    user.setMustChangePassword(true);
    userAccountRepository.save(user);
  }

  private UserAccount scopedUser(String email) {
    return userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(email, COMPANY_CODE)
        .orElseThrow();
  }

  private HttpHeaders bearer(String accessToken) {
    return bearer(accessToken, COMPANY_CODE);
  }

  private HttpHeaders bearer(String accessToken, String companyCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.set("X-Company-Code", companyCode);
    return headers;
  }

  private HttpHeaders jsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private HttpHeaders bearerJson(String accessToken) {
    HttpHeaders headers = bearer(accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private Map<String, Object> decodeJwtClaims(String jwt) {
    String[] parts = jwt.split("\\.");
    assertThat(parts).hasSize(3);
    byte[] decoded = Base64.getUrlDecoder().decode(padBase64UrlSegment(parts[1]));
    try {
      return OBJECT_MAPPER.readValue(new String(decoded, StandardCharsets.UTF_8), Map.class);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decode JWT payload", ex);
    }
  }

  private String padBase64UrlSegment(String segment) {
    int remainder = segment.length() % 4;
    if (remainder == 0) {
      return segment;
    }
    return segment + "=".repeat(4 - remainder);
  }

  private String passwordResetDigest(String token) {
    return IdempotencyUtils.sha256Hex("password-reset-token:" + token);
  }
}
