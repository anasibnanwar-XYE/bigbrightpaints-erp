package com.bigbrightpaints.erp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class AuthDisabledUserTokenIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "AUTH-DISABLED";
  private static final String USER_EMAIL = "disabled-user@bbp.com";
  private static final String USER_PASSWORD = "Passw0rd!";

  @Autowired private TestRestTemplate rest;

  @Autowired private UserAccountRepository userAccountRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void seedUser() {
    dataSeeder.ensureUser(
        USER_EMAIL, USER_PASSWORD, "Disabled User", COMPANY_CODE, java.util.List.of("ROLE_ADMIN"));
    UserAccount user = scopedUser();
    user.setEnabled(true);
    user.setPasswordHash(passwordEncoder.encode(USER_PASSWORD));
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userAccountRepository.save(user);
  }

  @Test
  void disabledUserToken_isRejectedEvenWhenJwtStillValid() {
    String token = loginToken();

    UserAccount user = scopedUser();
    user.setEnabled(false);
    userAccountRepository.save(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("X-Company-Code", COMPANY_CODE);
    ResponseEntity<Map> meResponse =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(meResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  void disabledUnknownAndWrongPasswordLoginFailures_shareGenericInvalidCredentialsEnvelope() {
    Map<String, Object> wrongPasswordRequest =
        Map.of(
            "email", USER_EMAIL,
            "password", "wrong-password",
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> wrongPasswordResponse =
        rest.postForEntity("/api/v1/auth/login", wrongPasswordRequest, Map.class);

    Map<String, Object> unknownUserRequest =
        Map.of(
            "email", "unknown-disabled-matrix@bbp.com",
            "password", USER_PASSWORD,
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> unknownUserResponse =
        rest.postForEntity("/api/v1/auth/login", unknownUserRequest, Map.class);

    UserAccount user = scopedUser();
    user.setEnabled(false);
    userAccountRepository.save(user);

    Map<String, Object> disabledUserRequest =
        Map.of(
            "email", USER_EMAIL,
            "password", USER_PASSWORD,
            "companyCode", COMPANY_CODE);

    ResponseEntity<Map> disabledUserResponse =
        rest.postForEntity("/api/v1/auth/login", disabledUserRequest, Map.class);

    assertGenericInvalidCredentials(wrongPasswordResponse);
    assertGenericInvalidCredentials(unknownUserResponse);
    assertGenericInvalidCredentials(disabledUserResponse);
    assertThat(disabledUserResponse.getStatusCode())
        .isEqualTo(wrongPasswordResponse.getStatusCode());
    assertThat(disabledUserResponse.getStatusCode()).isEqualTo(unknownUserResponse.getStatusCode());
    assertThat(sanitizedFailureShape(disabledUserResponse))
        .isEqualTo(sanitizedFailureShape(wrongPasswordResponse))
        .isEqualTo(sanitizedFailureShape(unknownUserResponse));
  }

  private void assertGenericInvalidCredentials(ResponseEntity<Map> loginResponse) {
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(loginResponse.getBody()).isNotNull();
    assertThat(loginResponse.getBody()).containsEntry("success", false);
    assertThat(loginResponse.getBody()).containsEntry("message", "Invalid credentials");
    assertThat(loginResponse.getBody()).doesNotContainKeys("accessToken", "refreshToken");
    Object data = loginResponse.getBody().get("data");
    assertThat(data).isInstanceOf(Map.class);
    Map<?, ?> error = (Map<?, ?>) data;
    assertThat(error.get("code")).isEqualTo("VAL_001");
    assertThat(error.get("message")).isEqualTo("Invalid credentials");
    assertThat(error.get("reason")).isEqualTo("Invalid credentials");
    assertThat(error.containsKey("traceId")).isTrue();
    assertThat(error.containsKey("accessToken")).isFalse();
    assertThat(error.containsKey("refreshToken")).isFalse();
    assertThat(loginResponse.getBody().toString())
        .doesNotContain("AUTH_006")
        .doesNotContain("AUTH_ACCOUNT_DISABLED")
        .doesNotContain("Account is disabled");
  }

  private Map<String, Object> sanitizedFailureShape(ResponseEntity<Map> loginResponse) {
    Map<?, ?> body = loginResponse.getBody();
    Map<?, ?> data = (Map<?, ?>) body.get("data");
    return Map.of(
        "status", loginResponse.getStatusCode().value(),
        "success", body.get("success"),
        "message", body.get("message"),
        "code", data.get("code"),
        "errorMessage", data.get("message"),
        "reason", data.get("reason"),
        "path", data.get("path"));
  }

  @Test
  void disabledUserRefreshToken_isRejectedAfterDisablement() {
    Map<String, Object> loginPayload = loginPayload();
    String refreshToken = loginPayload.get("refreshToken").toString();

    UserAccount user = scopedUser();
    user.setEnabled(false);
    userAccountRepository.save(user);

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of(
                "refreshToken", refreshToken,
                "companyCode", COMPANY_CODE),
            Map.class);

    assertThat(refreshResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
  }

  private String loginToken() {
    return loginPayload().get("accessToken").toString();
  }

  private UserAccount scopedUser() {
    return userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(USER_EMAIL, COMPANY_CODE)
        .orElseThrow();
  }

  private Map<String, Object> loginPayload() {
    Map<String, Object> request =
        Map.of(
            "email", USER_EMAIL,
            "password", USER_PASSWORD,
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> loginResponse =
        rest.postForEntity("/api/v1/auth/login", request, Map.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody()).isNotNull();
    return loginResponse.getBody();
  }
}
