package com.bigbrightpaints.erp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import com.bigbrightpaints.erp.core.security.JwtProperties;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@DisplayName("Auth Hardening: lockout after failed attempts")
public class AuthHardeningIT extends AbstractIntegrationTest {

  private static final String COMPANY = "AUTHLOCK";
  private static final String USER_EMAIL = "lockout@bbp.com";
  private static final String PASSWORD = "Passw0rd!";

  @Autowired private TestRestTemplate rest;

  @Autowired private UserAccountRepository userAccountRepository;

  @Autowired private JwtProperties jwtProperties;

  @BeforeEach
  void setup() {
    configureRestTemplate();
    UserAccount user =
        dataSeeder.ensureUser(
            USER_EMAIL,
            PASSWORD,
            "Lock Tester",
            COMPANY,
            java.util.List.of("ROLE_ADMIN", "ROLE_ACCOUNTING"));
    user.setEnabled(true);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    userAccountRepository.save(user);
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
  void lockout_after_five_failures() {
    Map<String, Object> badReq =
        Map.of(
            "email", USER_EMAIL,
            "password", "wrong",
            "companyCode", COMPANY);
    for (int i = 0; i < 5; i++) {
      ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login", badReq, Map.class);
      assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
    UserAccount user = scopedUser();
    assertThat(user.getFailedLoginAttempts()).isGreaterThanOrEqualTo(5);
    assertThat(user.getLockedUntil()).isNotNull();
    Instant lockedUntil = user.getLockedUntil();

    // Even with a correct password, lockout should persist until window expires
    Map<String, Object> goodReq =
        Map.of(
            "email", USER_EMAIL,
            "password", PASSWORD,
            "companyCode", COMPANY);
    ResponseEntity<Map> lockedResp = rest.postForEntity("/api/v1/auth/login", goodReq, Map.class);
    assertThat(lockedResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(lockedResp.getBody()).isNotNull();
    assertThat(lockedResp.getBody()).containsEntry("success", false);
    assertThat(lockedResp.getBody()).containsEntry("message", "Account is locked");
    Object lockedData = lockedResp.getBody().get("data");
    assertThat(lockedData).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> lockedError = (Map<String, Object>) lockedData;
    assertThat(lockedError).containsEntry("code", "AUTH_005");
    assertThat(lockedError).containsEntry("message", "Account is locked");
    assertThat(lockedError).containsKey("traceId");
    user = scopedUser();
    assertThat(user.getLockedUntil()).isAfterOrEqualTo(lockedUntil);

    // Manually clear lock for verification
    user.setLockedUntil(Instant.now().minusSeconds(60));
    user.setFailedLoginAttempts(0);
    userAccountRepository.save(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Map> success =
        rest.postForEntity("/api/v1/auth/login", new HttpEntity<>(goodReq, headers), Map.class);
    assertThat(success.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void lockout_revokes_existing_access_and_refresh_tokens() {
    ResponseEntity<Map> loginResponse =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", USER_EMAIL,
                "password", PASSWORD,
                "companyCode", COMPANY),
            Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getBody()).isNotNull();
    String accessToken = loginResponse.getBody().get("accessToken").toString();
    String refreshToken = loginResponse.getBody().get("refreshToken").toString();

    Map<String, Object> badReq =
        Map.of(
            "email", USER_EMAIL,
            "password", "wrong",
            "companyCode", COMPANY);
    for (int i = 0; i < 5; i++) {
      ResponseEntity<Map> resp = rest.postForEntity("/api/v1/auth/login", badReq, Map.class);
      assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    HttpHeaders accessHeaders = new HttpHeaders();
    accessHeaders.setBearerAuth(accessToken);
    accessHeaders.set("X-Company-Code", COMPANY);
    ResponseEntity<Map> meResponse =
        rest.exchange(
            "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(accessHeaders), Map.class);
    assertThat(meResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of(
                "refreshToken", refreshToken,
                "companyCode", COMPANY),
            Map.class);
    assertThat(refreshResponse.getStatusCode())
        .isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
  }

  @Test
  void legacyCompanyIdHeader_isRejectedOnPublicAuthEndpoints() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Id", COMPANY);

    ResponseEntity<Map> forgotResponse =
        rest.exchange(
            "/api/v1/auth/password/forgot",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("email", USER_EMAIL, "companyCode", COMPANY), headers),
            Map.class);

    assertThat(forgotResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(forgotResponse.getBody()).isNotNull();
    assertThat(forgotResponse.getBody()).containsEntry("success", false);
  }

  @Test
  void bearerTokenMissingJwtId_isRejectedFailClosed() {
    UserAccount user = scopedUser();
    String tokenWithoutId = signedAccessTokenWithoutJwtId(user);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(tokenWithoutId);
    headers.set("X-Company-Code", COMPANY);

    ResponseEntity<Map> meResponse =
        rest.exchange("/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(meResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  void authEndpointsRemainBearerOnlyAndDoNotSetAuthenticationCookies() {
    ResponseEntity<Map> loginResponse =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of(
                "email", USER_EMAIL,
                "password", PASSWORD,
                "companyCode", COMPANY),
            Map.class);

    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).isNullOrEmpty();
    String accessToken = loginResponse.getBody().get("accessToken").toString();
    String refreshToken = loginResponse.getBody().get("refreshToken").toString();

    ResponseEntity<Map> refreshResponse =
        rest.postForEntity(
            "/api/v1/auth/refresh-token",
            Map.of("refreshToken", refreshToken, "companyCode", COMPANY),
            Map.class);
    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).isNullOrEmpty();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<Void> logoutResponse =
        rest.exchange(
            "/api/v1/auth/logout",
            HttpMethod.POST,
            new HttpEntity<>(Map.of("refreshToken", refreshToken), headers),
            Void.class);
    assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(logoutResponse.getHeaders().get(HttpHeaders.SET_COOKIE)).isNullOrEmpty();
  }

  @Test
  void publicAuthEndpointsReturnControlledErrorsForMalformedJson() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    for (String path :
        java.util.List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/password/forgot",
            "/api/v1/auth/password/reset")) {
      ResponseEntity<Map> response =
          rest.exchange(path, HttpMethod.POST, new HttpEntity<>("{not-json", headers), Map.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody()).containsEntry("success", false);
    }
  }

  private UserAccount scopedUser() {
    return userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(USER_EMAIL, COMPANY)
        .orElseThrow();
  }

  private String signedAccessTokenWithoutJwtId(UserAccount user) {
    Instant now = Instant.now();
    Key signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    return Jwts.builder()
        .setSubject(user.getPublicId().toString())
        .claim("companyCode", COMPANY)
        .claim("sid", UUID.randomUUID().toString())
        .claim("iatMs", now.toEpochMilli())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(300)))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }
}
