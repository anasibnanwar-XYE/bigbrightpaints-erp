package com.bigbrightpaints.erp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.bigbrightpaints.erp.modules.auth.domain.MfaRecoveryCodeRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import com.bigbrightpaints.erp.test.support.TotpTestUtils;

public class MfaControllerIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "MFA";
  private static final String USER_EMAIL = "mfa-user@bbp.com";
  private static final String USER_PASSWORD = "ChangeMe123!";

  @Autowired private TestRestTemplate rest;

  @Autowired private UserAccountRepository userAccountRepository;
  @Autowired private MfaRecoveryCodeRepository mfaRecoveryCodeRepository;

  @BeforeEach
  void seedUser() {
    configureRestTemplate();
    UserAccount user =
        dataSeeder.ensureUser(
            USER_EMAIL, USER_PASSWORD, "MFA User", COMPANY_CODE, List.of("ROLE_ADMIN"));
    user.setMfaEnabled(false);
    user.setMfaSecret(null);
    UserAccount saved = userAccountRepository.save(user);
    mfaRecoveryCodeRepository.deleteAllByUser(saved);
  }

  private void configureRestTemplate() {
    CloseableHttpClient client =
        HttpClients.custom()
            .disableAutomaticRetries()
            .disableRedirectHandling()
            .disableAuthCaching()
            .build();
    HttpComponentsClientHttpRequestFactory factory =
        new HttpComponentsClientHttpRequestFactory(client);
    factory.setConnectTimeout(Duration.ofSeconds(10));
    factory.setConnectionRequestTimeout(Duration.ofSeconds(10));
    rest.getRestTemplate().setRequestFactory(factory);
  }

  @Test
  void enrollment_and_activation_require_totp_for_login() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);
    assertThat(setup.qrUri()).contains("mfa-user");
    assertThat(setup.qrUri()).contains("MFA");

    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    ResponseEntity<Map> activateResp =
        postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token);
    assertThat(activateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> activateData = apiData(activateResp);
    assertThat(activateData.get("enabled")).isEqualTo(Boolean.TRUE);

    ResponseEntity<Map> missingMfaLogin = login(null, null);
    assertThat(missingMfaLogin.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_REQUIRED);
    assertMfaChallenge(missingMfaLogin);

    String loginCode = TotpTestUtils.generateCurrentCode(setup.secret());
    ResponseEntity<Map> loginResponse = login(loginCode, null);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody()).isNotNull();
    assertThat(loginResponse.getBody().get("accessToken")).isNotNull();
  }

  @Test
  void recovery_code_is_consumed_after_login() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);

    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token);

    String recoveryCode = setup.recoveryCodes().getFirst();

    ResponseEntity<Map> firstLogin = login(null, recoveryCode);
    assertThat(firstLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(firstLogin.getBody()).isNotNull();
    assertThat(firstLogin.getBody().get("accessToken")).isNotNull();

    ResponseEntity<Map> secondLogin = login(null, recoveryCode);
    assertThat(secondLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void setup_is_inactive_until_valid_activation_and_invalid_activation_preserves_state() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);

    UserAccount afterSetup = scopedUser();
    assertThat(afterSetup.isMfaEnabled()).isFalse();
    assertThat(afterSetup.getMfaSecret()).isNotBlank();
    assertThat(afterSetup.getMfaSecret()).isNotEqualTo(setup.secret());
    assertThat(unusedRecoveryHashes(afterSetup)).hasSize(setup.recoveryCodes().size());
    assertThat(unusedRecoveryHashes(afterSetup)).doesNotContainAnyElementsOf(setup.recoveryCodes());

    ResponseEntity<Map> invalidActivate =
        postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", "abc123"), token);
    assertThat(invalidActivate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(scopedUser().isMfaEnabled()).isFalse();

    ResponseEntity<Map> passwordOnlyLoginWhileInactive = login(null, null);
    assertThat(passwordOnlyLoginWhileInactive.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(passwordOnlyLoginWhileInactive.getBody()).isNotNull();
    assertThat(passwordOnlyLoginWhileInactive.getBody().get("accessToken")).isNotNull();

    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    ResponseEntity<Map> validActivate =
        postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token);
    assertThat(validActivate.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(scopedUser().isMfaEnabled()).isTrue();
  }

  @Test
  void disable_requires_valid_self_verifier_and_clears_current_mfa_material() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);
    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    assertThat(
            postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> missingVerifier =
        postWithBearer("/api/v1/auth/mfa/disable", Map.of(), token);
    assertThat(missingVerifier.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(scopedUser().isMfaEnabled()).isTrue();

    ResponseEntity<Map> invalidVerifier =
        postWithBearer("/api/v1/auth/mfa/disable", Map.of("code", "abc123"), token);
    assertThat(invalidVerifier.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    assertThat(scopedUser().isMfaEnabled()).isTrue();

    String disableCode = TotpTestUtils.generateCurrentCode(setup.secret());
    ResponseEntity<Map> disable =
        postWithBearer("/api/v1/auth/mfa/disable", Map.of("code", disableCode), token);
    assertThat(disable.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> disableData = apiData(disable);
    assertThat(disableData.get("enabled")).isEqualTo(Boolean.FALSE);

    UserAccount afterDisable = scopedUser();
    assertThat(afterDisable.isMfaEnabled()).isFalse();
    assertThat(afterDisable.getMfaSecret()).isNull();
    assertThat(unusedRecoveryHashes(afterDisable)).isEmpty();
  }

  @Test
  void recovery_code_regeneration_returns_new_codes_and_replaces_old_verifiers() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);
    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    assertThat(
            postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    List<String> originalHashes = unusedRecoveryHashes(scopedUser());
    ResponseEntity<Map> missingProof =
        postWithBearer("/api/v1/auth/mfa/recovery-codes/regenerate", Map.of(), token);
    assertThat(missingProof.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(unusedRecoveryHashes(scopedUser())).isEqualTo(originalHashes);
    assertThat(String.valueOf(missingProof.getBody())).doesNotContain("recoveryCodes");

    ResponseEntity<Map> invalidProof =
        postWithBearer(
            "/api/v1/auth/mfa/recovery-codes/regenerate", Map.of("code", "abc123"), token);
    assertThat(invalidProof.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
    assertThat(unusedRecoveryHashes(scopedUser())).isEqualTo(originalHashes);
    assertThat(String.valueOf(invalidProof.getBody())).doesNotContain("recoveryCodes");

    ResponseEntity<Map> regenerate =
        postWithBearer(
            "/api/v1/auth/mfa/recovery-codes/regenerate", Map.of("code", activationCode), token);
    assertThat(regenerate.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> regenerateData = apiData(regenerate);
    assertThat(regenerateData.get("enabled")).isEqualTo(Boolean.TRUE);
    @SuppressWarnings("unchecked")
    List<String> regeneratedCodes =
        ((List<Object>) regenerateData.get("recoveryCodes"))
            .stream().map(Object::toString).toList();
    assertThat(regeneratedCodes).hasSize(setup.recoveryCodes().size());
    assertThat(regeneratedCodes).doesNotContainAnyElementsOf(setup.recoveryCodes());
    assertThat(unusedRecoveryHashes(scopedUser()))
        .doesNotContainAnyElementsOf(regeneratedCodes)
        .hasSize(regeneratedCodes.size());

    ResponseEntity<Map> oldRecoveryCodeLogin = login(null, setup.recoveryCodes().getFirst());
    assertThat(oldRecoveryCodeLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    ResponseEntity<Map> regeneratedRecoveryCodeLogin = login(null, regeneratedCodes.getFirst());
    assertThat(regeneratedRecoveryCodeLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void recovery_code_regeneration_revokes_pre_change_access_and_refresh_tokens() {
    String token = obtainAccessToken(null, null);
    SetupPayload setup = startEnrollment(token);
    String activationCode = TotpTestUtils.generateCurrentCode(setup.secret());
    assertThat(
            postWithBearer("/api/v1/auth/mfa/activate", Map.of("code", activationCode), token)
                .getStatusCode())
        .isEqualTo(HttpStatus.OK);

    LoginTokens firstSession =
        obtainTokens(TotpTestUtils.generateCurrentCode(setup.secret()), null);
    LoginTokens secondSession =
        obtainTokens(TotpTestUtils.generateCurrentCode(setup.secret()), null);
    assertThat(getWithBearer("/api/v1/auth/me", firstSession.accessToken()).getStatusCode())
        .isEqualTo(HttpStatus.OK);
    assertThat(getWithBearer("/api/v1/auth/me", secondSession.accessToken()).getStatusCode())
        .isEqualTo(HttpStatus.OK);

    ResponseEntity<Map> regenerate =
        postWithBearer(
            "/api/v1/auth/mfa/recovery-codes/regenerate",
            Map.of("code", TotpTestUtils.generateCurrentCode(setup.secret())),
            firstSession.accessToken());
    assertThat(regenerate.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(apiData(regenerate).get("recoveryCodes")).isNotNull();

    assertAuthenticatedTokenDenied(getWithBearer("/api/v1/auth/me", firstSession.accessToken()));
    assertAuthenticatedTokenDenied(getWithBearer("/api/v1/auth/me", secondSession.accessToken()));
    assertRefreshDenied(refresh(firstSession.refreshToken()));
    assertRefreshDenied(refresh(secondSession.refreshToken()));
  }

  private SetupPayload startEnrollment(String token) {
    ResponseEntity<Map> setupResp = postWithBearer("/api/v1/auth/mfa/setup", null, token);
    assertThat(setupResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> setupData = apiData(setupResp);
    String secret = setupData.get("secret").toString();
    String qrUri = setupData.get("qrUri").toString();
    @SuppressWarnings("unchecked")
    List<String> recoveryCodes =
        ((List<Object>) setupData.get("recoveryCodes")).stream().map(Object::toString).toList();
    return new SetupPayload(secret, qrUri, recoveryCodes);
  }

  private String obtainAccessToken(String mfaCode, String recoveryCode) {
    return obtainTokens(mfaCode, recoveryCode).accessToken();
  }

  private LoginTokens obtainTokens(String mfaCode, String recoveryCode) {
    ResponseEntity<Map> response = login(mfaCode, recoveryCode);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    String token = (String) response.getBody().get("accessToken");
    assertThat(token).isNotBlank();
    String refreshToken = (String) response.getBody().get("refreshToken");
    assertThat(refreshToken).isNotBlank();
    return new LoginTokens(token, refreshToken);
  }

  private ResponseEntity<Map> login(String mfaCode, String recoveryCode) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("email", USER_EMAIL);
    payload.put("password", USER_PASSWORD);
    payload.put("companyCode", COMPANY_CODE);
    if (mfaCode != null) {
      payload.put("mfaCode", mfaCode);
    }
    if (recoveryCode != null) {
      payload.put("recoveryCode", recoveryCode);
    }
    return rest.postForEntity("/api/v1/auth/login", payload, Map.class);
  }

  private ResponseEntity<Map> postWithBearer(String path, Map<String, ?> body, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, ?>> entity = new HttpEntity<>(body, headers);
    return rest.exchange(path, HttpMethod.POST, entity, Map.class);
  }

  private ResponseEntity<Map> getWithBearer(String path, String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
  }

  private ResponseEntity<Map> refresh(String refreshToken) {
    return rest.postForEntity(
        "/api/v1/auth/refresh-token",
        Map.of("refreshToken", refreshToken, "companyCode", COMPANY_CODE),
        Map.class);
  }

  private void assertAuthenticatedTokenDenied(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(String.valueOf(response.getBody())).doesNotContain("accessToken", "refreshToken");
  }

  private void assertRefreshDenied(ResponseEntity<Map> response) {
    assertThat(response.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertThat(String.valueOf(response.getBody())).doesNotContain("accessToken", "refreshToken");
  }

  private UserAccount scopedUser() {
    return userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(USER_EMAIL, COMPANY_CODE)
        .orElseThrow();
  }

  private List<String> unusedRecoveryHashes(UserAccount user) {
    return mfaRecoveryCodeRepository.findUnusedByUser(user).stream()
        .map(code -> code.getCodeHash())
        .toList();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> apiData(ResponseEntity<Map> response) {
    Map<String, Object> body = response.getBody();
    Assertions.assertThat(body).isNotNull();
    return (Map<String, Object>) body.get("data");
  }

  @SuppressWarnings("unchecked")
  private void assertMfaChallenge(ResponseEntity<Map> response) {
    Map<String, Object> body = response.getBody();
    assertThat(body).isNotNull();
    assertThat(body.get("success")).isEqualTo(Boolean.FALSE);
    Map<String, Object> data = (Map<String, Object>) body.get("data");
    assertThat(data).isNotNull();
    assertThat(data.get("required")).isEqualTo(Boolean.TRUE);
  }

  private record SetupPayload(String secret, String qrUri, List<String> recoveryCodes) {}

  private record LoginTokens(String accessToken, String refreshToken) {}
}
