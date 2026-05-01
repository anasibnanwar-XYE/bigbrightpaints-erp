package com.bigbrightpaints.erp.truthsuite.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantAdminEmailChangeRequest;
import com.bigbrightpaints.erp.modules.company.domain.TenantAdminEmailChangeRequestRepository;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import com.bigbrightpaints.erp.test.support.TotpTestUtils;

@Tag("critical")
class IamCoreSchemaAndModelHardCutMigrationIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TestRestTemplate rest;
  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private SuperAdminTenantControlPlaneService superAdminTenantControlPlaneService;
  @Autowired private TenantAdminEmailChangeRequestRepository emailChangeRequestRepository;

  @Test
  void canonicalIamTablesExistAfterFlywayV2Migration() {
    assertThat(existingTables())
        .contains(
            "iam_accounts",
            "iam_account_profiles",
            "iam_account_contacts",
            "iam_credentials",
            "iam_mfa_factors",
            "iam_sessions",
            "iam_devices",
            "iam_security_events",
            "mfa_recovery_codes");
  }

  @Test
  void verifierOnlyStorageRemovesRawTokenAndDelimitedRecoveryColumns() {
    assertThat(columns("refresh_tokens")).contains("token_digest").doesNotContain("token");
    assertThat(columns("password_reset_tokens")).contains("token_digest").doesNotContain("token");
    assertThat(columns("app_users")).doesNotContain("mfa_recovery_codes");
    assertThat(columns("iam_sessions"))
        .contains("refresh_token_digest", "revoked_at", "consumed_at")
        .doesNotContain("refresh_token", "token");
  }

  @Test
  void accountProfileContactCredentialAndMfaOwnershipColumnsAreSeparated() {
    assertThat(columns("iam_accounts"))
        .contains(
            "user_id",
            "public_id",
            "account_type",
            "auth_scope_code",
            "company_id",
            "status",
            "locked_until",
            "failed_login_attempts",
            "must_change_password")
        .doesNotContain("password_hash", "secondary_email", "phone_secondary");
    assertThat(columns("iam_account_profiles"))
        .contains("display_name", "preferred_name", "profile_picture_url", "job_title");
    assertThat(columns("iam_account_contacts"))
        .contains("primary_email", "secondary_email", "phone_secondary");
    assertThat(columns("iam_credentials")).contains("password_hash", "must_change_password");
    assertThat(columns("iam_mfa_factors")).contains("factor_type", "encrypted_secret", "status");
  }

  @Test
  void runtimeProfileContactPasswordSessionLockoutAndSecurityEventsStayCurrentInCanonicalIamRows() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String email = "iam-runtime-" + suffix + "@bbp.com";
    String scope = "IAM" + suffix.substring(0, 4).toUpperCase();
    String originalPassword = "Runtime123!";
    String changedPassword = "Runtime456!";
    dataSeeder.ensureUser(email, originalPassword, "IAM Runtime", scope, List.of("ROLE_ADMIN"));

    LoginTokens tokens = login(email, originalPassword, scope);
    Long accountId = accountId(email, scope);
    assertThat(accountId).isNotNull();
    assertThat(singleString("select status from iam_accounts where id = ?", accountId))
        .isEqualTo("ACTIVE");
    assertThat(
            singleString(
                "select refresh_token_digest from iam_sessions where account_id = ?", accountId))
        .isEqualTo(refreshTokenDigest(tokens.refreshToken()));

    ResponseEntity<Map> profile =
        rest.exchange(
            "/api/v1/auth/me/profile",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of(
                    "preferredName", "Canonical", "profilePictureUrl", "https://img.example/p.png"),
                bearerJson(tokens.accessToken())),
            Map.class);
    assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            singleString(
                "select preferred_name from iam_account_profiles where account_id = ?", accountId))
        .isEqualTo("Canonical");

    ResponseEntity<Map> contact =
        rest.exchange(
            "/api/v1/auth/me/contact",
            HttpMethod.PATCH,
            new HttpEntity<>(
                Map.of("secondaryEmail", "SECONDARY@BBP.COM", "phoneSecondary", "+91-555"),
                bearerJson(tokens.accessToken())),
            Map.class);
    assertThat(contact.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            singleString(
                "select secondary_email from iam_account_contacts where account_id = ?", accountId))
        .isEqualTo("secondary@bbp.com");
    assertThat(
            singleString(
                "select phone_secondary from iam_account_contacts where account_id = ?", accountId))
        .isEqualTo("+91-555");

    ResponseEntity<Map> change =
        rest.exchange(
            "/api/v1/auth/password/change",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "currentPassword",
                    originalPassword,
                    "newPassword",
                    changedPassword,
                    "confirmPassword",
                    changedPassword),
                bearerJson(tokens.accessToken())),
            Map.class);
    assertThat(change.getStatusCode()).isEqualTo(HttpStatus.OK);
    String currentHash =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(email, scope)
            .orElseThrow()
            .getPasswordHash();
    assertThat(
            singleString(
                "select password_hash from iam_credentials where account_id = ?", accountId))
        .isEqualTo(currentHash);
    assertThat(
            singleBoolean(
                "select must_change_password from iam_credentials where account_id = ?", accountId))
        .isFalse();
    assertThat(
            singleInstantPresent(
                "select password_changed_at from iam_credentials where account_id = ?", accountId))
        .isTrue();
    assertThat(
            singleInstantPresent(
                "select revoked_at from iam_sessions where account_id = ?", accountId))
        .isTrue();

    ResponseEntity<Map> failedLogin =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", email, "password", "Wrong123!", "companyCode", scope),
            Map.class);
    assertThat(failedLogin.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    assertThat(
            singleInteger("select failed_login_attempts from iam_accounts where id = ?", accountId))
        .isEqualTo(1);

    assertSecurityEventPersisted("LOGIN_SUCCESS", accountId);
    assertThat(
            singleString(
                "select jsonb_typeof(metadata) from iam_security_events where account_id = ? order"
                    + " by id desc limit 1",
                accountId))
        .isEqualTo("object");
  }

  @Test
  void runtimeMfaMutationsStayCurrentInCanonicalIamFactorRows() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String email = "iam-mfa-" + suffix + "@bbp.com";
    String scope = "MFA" + suffix.substring(0, 4).toUpperCase();
    String password = "MfaRuntime123!";
    dataSeeder.ensureUser(email, password, "IAM MFA", scope, List.of("ROLE_ADMIN"));

    LoginTokens tokens = login(email, password, scope);
    Long accountId = accountId(email, scope);

    ResponseEntity<Map> setup =
        rest.exchange(
            "/api/v1/auth/mfa/setup",
            HttpMethod.POST,
            new HttpEntity<>(bearerJson(tokens.accessToken())),
            Map.class);
    assertThat(setup.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> setupData = apiData(setup);
    String secret = setupData.get("secret").toString();
    assertThat(singleString("select status from iam_mfa_factors where account_id = ?", accountId))
        .isEqualTo("PENDING");
    assertThat(
            singleString(
                "select encrypted_secret from iam_mfa_factors where account_id = ?", accountId))
        .isNotEqualTo(secret);

    ResponseEntity<Map> activate =
        rest.exchange(
            "/api/v1/auth/mfa/activate",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("code", TotpTestUtils.generateCurrentCode(secret)),
                bearerJson(tokens.accessToken())),
            Map.class);
    assertThat(activate.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(singleString("select status from iam_mfa_factors where account_id = ?", accountId))
        .isEqualTo("ACTIVE");
    assertThat(
            singleInstantPresent(
                "select activated_at from iam_mfa_factors where account_id = ?", accountId))
        .isTrue();

    LoginTokens mfaTokens =
        login(email, password, scope, TotpTestUtils.generateCurrentCode(secret));
    ResponseEntity<Map> disable =
        rest.exchange(
            "/api/v1/auth/mfa/disable",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of("code", TotpTestUtils.generateCurrentCode(secret)),
                bearerJson(mfaTokens.accessToken())),
            Map.class);
    assertThat(disable.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(
            singleInteger("select count(*) from iam_mfa_factors where account_id = ?", accountId))
        .isZero();
  }

  @Test
  void superAdminTenantAdminEmailChangeKeepsCanonicalIamEmailAndContactCurrent() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String scope = "ADM" + suffix.substring(0, 4).toUpperCase();
    String email = "tenant-admin-" + suffix + "@bbp.com";
    String requestedEmail = "tenant-admin-new-" + suffix + "@bbp.com";
    String password = "AdminChange123!";
    dataSeeder.ensureUser(email, password, "Tenant Email Admin", scope, List.of("ROLE_ADMIN"));

    LoginTokens tokens = login(email, password, scope);
    Long accountId = accountId(email, scope);
    assertThat(singleString("select email from iam_accounts where id = ?", accountId))
        .isEqualTo(email);
    assertThat(
            singleString(
                "select primary_email from iam_account_contacts where account_id = ?", accountId))
        .isEqualTo(email);

    var company = companyRepository.findByCodeIgnoreCase(scope).orElseThrow();
    var admin =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(email, scope)
            .orElseThrow();
    TenantAdminEmailChangeRequest request = new TenantAdminEmailChangeRequest();
    request.setCompanyId(company.getId());
    request.setAdminUserId(admin.getId());
    request.setRequestedBy("super-admin@bbp.com");
    request.setCurrentEmail(email);
    request.setRequestedEmail(requestedEmail);
    request.setVerificationToken("verify-" + suffix);
    request.setVerificationSentAt(java.time.Instant.now());
    request.setExpiresAt(java.time.Instant.now().plusSeconds(600));
    TenantAdminEmailChangeRequest savedRequest = emailChangeRequestRepository.save(request);

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("super-admin@bbp.com", "n/a"));
    try {
      var response =
          superAdminTenantControlPlaneService.confirmAdminEmailChange(
              company.getId(),
              admin.getId(),
              savedRequest.getId(),
              savedRequest.getVerificationToken());

      assertThat(response.updatedEmail()).isEqualTo(requestedEmail);
    } finally {
      SecurityContextHolder.clearContext();
    }

    assertThat(singleString("select email from iam_accounts where id = ?", accountId))
        .isEqualTo(requestedEmail);
    assertThat(
            singleString(
                "select primary_email from iam_account_contacts where account_id = ?", accountId))
        .isEqualTo(requestedEmail);
    assertThat(
            singleInstantPresent(
                "select revoked_at from iam_sessions where refresh_token_digest = ?",
                refreshTokenDigest(tokens.refreshToken())))
        .isTrue();
  }

  private List<String> existingTables() {
    return jdbcTemplate.queryForList(
        """
        select table_name
          from information_schema.tables
         where table_schema = 'public'
        """,
        String.class);
  }

  private List<String> columns(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select column_name
          from information_schema.columns
         where table_schema = 'public'
           and table_name = ?
        """,
        String.class,
        tableName);
  }

  private LoginTokens login(String email, String password, String companyCode) {
    return login(email, password, companyCode, null);
  }

  private LoginTokens login(String email, String password, String companyCode, String mfaCode) {
    ResponseEntity<Map> response =
        rest.postForEntity(
            "/api/v1/auth/login",
            mfaCode == null
                ? Map.of("email", email, "password", password, "companyCode", companyCode)
                : Map.of(
                    "email",
                    email,
                    "password",
                    password,
                    "companyCode",
                    companyCode,
                    "mfaCode",
                    mfaCode),
            Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return new LoginTokens(
        response.getBody().get("accessToken").toString(),
        response.getBody().get("refreshToken").toString());
  }

  private HttpHeaders bearerJson(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> apiData(ResponseEntity<Map> response) {
    assertThat(response.getBody()).isNotNull();
    return (Map<String, Object>) response.getBody().get("data");
  }

  private Long accountId(String email, String scope) {
    return jdbcTemplate.query(
        "select id from iam_accounts where email = ? and auth_scope_code = ?",
        rs -> rs.next() ? rs.getLong("id") : null,
        email.toLowerCase(),
        scope.toUpperCase());
  }

  private String singleString(String sql, Object... args) {
    return jdbcTemplate.queryForObject(sql, String.class, args);
  }

  private Integer singleInteger(String sql, Object... args) {
    return jdbcTemplate.queryForObject(sql, Integer.class, args);
  }

  private Boolean singleBoolean(String sql, Object... args) {
    return jdbcTemplate.queryForObject(sql, Boolean.class, args);
  }

  private boolean singleInstantPresent(String sql, Object... args) {
    return jdbcTemplate.queryForObject(sql, java.time.Instant.class, args) != null;
  }

  private void assertSecurityEventPersisted(String eventType, Long accountId) {
    long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
    while (System.nanoTime() < deadline) {
      Integer count =
          jdbcTemplate.queryForObject(
              "select count(*) from iam_security_events where event_type = ? and account_id = ?",
              Integer.class,
              eventType,
              accountId);
      if (count != null && count > 0) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        break;
      }
    }
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from iam_security_events where event_type = ? and account_id = ?",
                Integer.class,
                eventType,
                accountId))
        .isGreaterThan(0);
  }

  private String refreshTokenDigest(String refreshToken) {
    return IdempotencyUtils.sha256Hex("refresh-token:" + refreshToken);
  }

  private record LoginTokens(String accessToken, String refreshToken) {}
}
