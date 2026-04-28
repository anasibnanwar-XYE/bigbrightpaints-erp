package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class SuperAdminApiContractIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "ACME";
  private static final String ROOT_COMPANY_CODE = "ROOT";
  private static final String SUPER_ADMIN_EMAIL = "super-admin-contract@bbp.com";
  private static final String PASSWORD = "admin123";

  @Autowired private TestRestTemplate rest;
  @Autowired private CompanyRepository companyRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private String superAdminToken;
  private Long tenantId;

  @BeforeEach
  void seedSuperAdmin() {
    dataSeeder.ensureUser(
        "admin-contract@bbp.com", PASSWORD, "Admin Contract", COMPANY_CODE, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Super Admin Contract",
        ROOT_COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
    Company tenant = companyRepository.findByCodeIgnoreCase(COMPANY_CODE).orElseThrow();
    tenant.setLifecycleState(CompanyLifecycleState.ACTIVE);
    tenant.setLifecycleReason(null);
    companyRepository.save(tenant);
    tenantId = tenant.getId();
    superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
  }

  @Test
  void successEnvelope_includesTraceMetadataAndHeader() {
    HttpHeaders headers = superAdminHeaders();
    headers.set("X-Correlation-ID", "m1-contract-success");

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/dashboard", HttpMethod.GET, new HttpEntity<>(headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
    assertSecurityHeaders(response);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsKeys("success", "data", "timestamp", "metadata");
    assertThat(body.get("success")).isEqualTo(true);
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
    assertThat(metadata)
        .containsEntry("correlationId", "m1-contract-success")
        .containsKey("traceId");
    assertThat(metadata.get("traceId")).isEqualTo(response.getHeaders().getFirst("X-Trace-Id"));
  }

  @Test
  void mutationSuccessEnvelope_includesTraceMetadataForDelete() {
    HttpHeaders headers = superAdminHeaders();
    headers.set("X-Correlation-ID", "m1-contract-delete-success");

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/superadmin/changelog",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "version",
                    "99.0.1",
                    "title",
                    "Delete envelope contract",
                    "body",
                    "Contract fixture for Super Admin delete envelope",
                    "isHighlighted",
                    false),
                headers),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    @SuppressWarnings("unchecked")
    Map<String, Object> createdData = (Map<String, Object>) createResponse.getBody().get("data");
    Number id = (Number) createdData.get("id");

    ResponseEntity<Map> deleteResponse =
        rest.exchange(
            "/api/v1/superadmin/changelog/" + id.longValue(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertStandardSuccess(deleteResponse, "m1-contract-delete-success");
    assertThat(deleteResponse.getBody()).containsEntry("message", "Changelog entry deleted");
  }

  @Test
  void unknownBodyFieldsAreRejectedWithoutChangingState() {
    HttpHeaders headers = superAdminHeaders();
    headers.set("X-Correlation-ID", "m1-contract-error");

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants/" + tenantId + "/lifecycle",
            HttpMethod.PUT,
            new HttpEntity<>(
                "{\"state\":\"SUSPENDED\",\"reason\":\"probe\",\"branch\":\"forbidden\"}", headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertStandardError(response, "VAL_001");
    assertThat(readLifecycleState()).isEqualTo("ACTIVE");
  }

  @Test
  void parserMediaMethodAndSizeErrorsUseSafeEnvelope() {
    assertSafeError(
        exchangeRaw(
            HttpMethod.PUT,
            "/api/v1/superadmin/tenants/" + tenantId + "/lifecycle",
            "{\"state\":\"ACTIVE\",",
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON),
        HttpStatus.BAD_REQUEST,
        "VAL_001");

    assertSafeError(
        exchangeRaw(
            HttpMethod.PUT,
            "/api/v1/superadmin/tenants/" + tenantId + "/lifecycle",
            "{\"state\":\"ACTIVE\",\"state\":\"SUSPENDED\",\"reason\":\"duplicate\"}",
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON),
        HttpStatus.BAD_REQUEST,
        "VAL_001");

    assertSafeError(
        exchangeRaw(
            HttpMethod.PUT,
            "/api/v1/superadmin/tenants/" + tenantId + "/lifecycle",
            "{\"state\":\"ACTIVE\"}",
            MediaType.TEXT_PLAIN,
            MediaType.APPLICATION_JSON),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "VAL_001");

    assertSafeError(
        exchangeRaw(
            HttpMethod.GET, "/api/v1/superadmin/dashboard", null, null, MediaType.APPLICATION_XML),
        HttpStatus.NOT_ACCEPTABLE,
        "VAL_001");

    assertSafeError(
        exchangeRaw(
            HttpMethod.DELETE,
            "/api/v1/superadmin/dashboard",
            null,
            null,
            MediaType.APPLICATION_JSON),
        HttpStatus.METHOD_NOT_ALLOWED,
        "VAL_001");

    assertSafeError(
        exchangeRaw(
            HttpMethod.GET,
            "/api/v1/superadmin/dashboard?probe=" + "x".repeat(2_100),
            null,
            null,
            MediaType.APPLICATION_JSON),
        HttpStatus.BAD_REQUEST,
        "VAL_004");

    assertSafeError(
        exchangeRaw(
            HttpMethod.PUT,
            "/api/v1/superadmin/tenants/" + tenantId + "/lifecycle",
            "{\"reason\":\"" + "x".repeat(1_050_000) + "\"}",
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_JSON),
        HttpStatus.PAYLOAD_TOO_LARGE,
        "FILE_004");

    assertThat(readLifecycleState()).isEqualTo("ACTIVE");
  }

  @Test
  void retiredSuperAdminRoutesUseStandardSafeErrorEnvelope() {
    assertRetiredRouteEnvelope(
        "/api/v1/superadmin/tenants/onboard",
        Map.of(
            "name",
            "Retired Contract Tenant",
            "code",
            "RET-CONTRACT",
            "temporaryPassword",
            "must-not-leak",
            "firstAdminEmail",
            "retired-contract@example.com"),
        "retired-superadmin-flat-onboarding");

    assertRetiredRouteEnvelope(
        "/api/v1/superadmin/tenants/" + tenantId + "/support/admin-password-reset",
        Map.of(
            "adminEmail",
            "admin-contract@bbp.com",
            "password",
            "must-not-leak",
            "token",
            "must-not-leak"),
        "retired-superadmin-admin-password-reset");
  }

  @Test
  void protectedSuperAdminAuthFailuresUseStandardSafeErrorEnvelope() {
    HttpHeaders noAuthHeaders = contractErrorHeaders();
    ResponseEntity<Map> noAuthResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(noAuthHeaders),
            Map.class);
    assertThat(noAuthResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertSafeError(noAuthResponse, noAuthResponse.getStatusCode(), "AUTH_003");
    assertErrorPath(noAuthResponse, "/api/v1/superadmin/dashboard");

    HttpHeaders malformedTokenHeaders = contractErrorHeaders();
    malformedTokenHeaders.setBearerAuth("not-a-valid-jwt");
    ResponseEntity<Map> malformedTokenResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(malformedTokenHeaders),
            Map.class);
    assertThat(malformedTokenResponse.getStatusCode())
        .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    assertSafeError(malformedTokenResponse, malformedTokenResponse.getStatusCode(), "AUTH_003");
    assertErrorPath(malformedTokenResponse, "/api/v1/superadmin/dashboard");

    String adminToken = loginToken("admin-contract@bbp.com", COMPANY_CODE);
    HttpHeaders tenantAdminHeaders = contractErrorHeaders();
    tenantAdminHeaders.setBearerAuth(adminToken);
    tenantAdminHeaders.set("X-Company-Code", COMPANY_CODE);
    ResponseEntity<Map> accessDeniedResponse =
        rest.exchange(
            "/api/v1/superadmin/dashboard",
            HttpMethod.GET,
            new HttpEntity<>(tenantAdminHeaders),
            Map.class);
    assertSafeError(accessDeniedResponse, HttpStatus.FORBIDDEN, "AUTH_004");
    assertErrorPath(accessDeniedResponse, "/api/v1/superadmin/dashboard");
  }

  private void assertStandardSuccess(ResponseEntity<Map> response, String expectedCorrelationId) {
    assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
    assertSecurityHeaders(response);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body)
        .containsEntry("success", true)
        .containsKeys("message", "timestamp", "metadata");
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
    assertThat(metadata)
        .containsEntry("correlationId", expectedCorrelationId)
        .containsKey("traceId");
    assertThat(metadata.get("traceId")).isEqualTo(response.getHeaders().getFirst("X-Trace-Id"));
  }

  private ResponseEntity<Map> exchangeRaw(
      HttpMethod method, String path, String body, MediaType contentType, MediaType accept) {
    HttpHeaders headers = superAdminHeaders();
    headers.set("X-Correlation-ID", "m1-contract-error");
    if (contentType != null) {
      headers.setContentType(contentType);
    }
    if (accept != null) {
      headers.setAccept(List.of(accept));
    }
    return rest.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
  }

  private void assertSafeError(
      ResponseEntity<Map> response, HttpStatusCode expectedStatus, String expectedCode) {
    assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
    assertStandardError(response, expectedCode);
    assertThat(response.getHeaders().getFirst("X-Trace-Id")).isNotBlank();
    assertThat(String.valueOf(response.getBody()))
        .doesNotContain("com.fasterxml")
        .doesNotContain("org.springframework")
        .doesNotContain("SQLException")
        .doesNotContain("stackTrace")
        .doesNotContain("java.");
  }

  private void assertSecurityHeaders(ResponseEntity<?> response) {
    assertThat(response.getHeaders().getFirst("Cache-Control"))
        .isEqualTo("no-store, no-cache, max-age=0, must-revalidate");
    assertThat(response.getHeaders().getFirst("Pragma")).isEqualTo("no-cache");
    assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("DENY");
    assertThat(response.getHeaders().getFirst("Referrer-Policy")).isEqualTo("no-referrer");
    assertThat(response.getHeaders().getFirst("Content-Security-Policy"))
        .contains("default-src 'none'", "frame-ancestors 'none'", "base-uri 'none'");
    assertThat(response.getHeaders().getFirst("Permissions-Policy"))
        .contains("geolocation=()", "camera=()", "microphone=()", "payment=()");
  }

  private void assertStandardError(ResponseEntity<Map> response, String expectedCode) {
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertThat(body).isNotNull();
    assertThat(body).containsEntry("success", false).containsKeys("message", "data", "metadata");
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) body.get("data");
    assertThat(data)
        .containsEntry("code", expectedCode)
        .containsKeys("message", "reason", "traceId", "path");
    @SuppressWarnings("unchecked")
    Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
    assertThat(metadata).containsKey("traceId").containsEntry("correlationId", "m1-contract-error");
    assertThat(data.get("traceId")).isEqualTo(metadata.get("traceId"));
  }

  private void assertRetiredRouteEnvelope(String path, Map<String, Object> payload, String code) {
    HttpHeaders headers = superAdminHeaders();
    headers.set("X-Correlation-ID", "m1-contract-error");

    ResponseEntity<Map> response =
        rest.exchange(path, HttpMethod.POST, new HttpEntity<>(payload, headers), Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    assertStandardError(response, code);
    @SuppressWarnings("unchecked")
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data.get("message").toString()).contains("retired");
    assertThat(data.get("reason")).isEqualTo(data.get("message"));
    assertThat(data).containsEntry("path", path);
    assertThat(String.valueOf(response.getBody()))
        .doesNotContain("must-not-leak")
        .doesNotContain("temporaryPassword")
        .doesNotContain("password=")
        .doesNotContain("token=")
        .doesNotContain("SQLException")
        .doesNotContain("org.springframework")
        .doesNotContain("java.");
  }

  private HttpHeaders contractErrorHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.set("X-Correlation-ID", "m1-contract-error");
    return headers;
  }

  @SuppressWarnings("unchecked")
  private void assertErrorPath(ResponseEntity<Map> response, String expectedPath) {
    Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
    assertThat(data).containsEntry("path", expectedPath);
    assertThat(data.get("reason")).isEqualTo(data.get("message"));
  }

  private HttpHeaders superAdminHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(superAdminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", ROOT_COMPANY_CODE);
    return headers;
  }

  private String loginToken(String email, String companyCode) {
    Map<String, Object> request =
        Map.of(
            "email", email,
            "password", PASSWORD,
            "companyCode", companyCode);
    ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
    return (String) response.getBody().get("accessToken");
  }

  private String readLifecycleState() {
    return jdbcTemplate.queryForObject(
        "select lifecycle_state from companies where id = ?", String.class, tenantId);
  }
}
