package com.bigbrightpaints.erp.modules.admin;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class ChangelogControllerSecurityIT extends AbstractIntegrationTest {

  private static final String TENANT = "CHANGELOGSEC";
  private static final String ADMIN_EMAIL = "changelog.admin@bbp.com";
  private static final String ADMIN_PASSWORD = "Admin@123";
  private static final String SUPER_ADMIN_EMAIL = "changelog.super@bbp.com";
  private static final String SUPER_ADMIN_PASSWORD = "SuperAdmin@123";
  private static final String DEALER_EMAIL = "changelog.dealer@bbp.com";
  private static final String DEALER_PASSWORD = "Dealer@123";

  @Autowired private TestRestTemplate rest;

  @BeforeEach
  void seedUsers() {
    dataSeeder.ensureUser(
        ADMIN_EMAIL, ADMIN_PASSWORD, "Changelog Admin", TENANT, List.of("ROLE_ADMIN"));
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        SUPER_ADMIN_PASSWORD,
        "Changelog Super",
        TENANT,
        List.of("ROLE_SUPER_ADMIN"));
    dataSeeder.ensureUser(
        DEALER_EMAIL, DEALER_PASSWORD, "Changelog Dealer", TENANT, List.of("ROLE_DEALER"));
  }

  @Test
  void adminEndpoints_requireAdminRole() {
    String dealerToken = login(DEALER_EMAIL, DEALER_PASSWORD, TENANT);
    HttpHeaders headers = authHeaders(dealerToken, TENANT);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/admin/changelog",
            HttpMethod.POST,
            new HttpEntity<>(requestPayload("1.2.0", true), headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void adminCanCreateAndDeleteChangelogEntry() {
    String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, TENANT);
    HttpHeaders headers = authHeaders(adminToken, TENANT);

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/admin/changelog",
            HttpMethod.POST,
            new HttpEntity<>(requestPayload("1.2.1", true), headers),
            Map.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(createResponse.getBody()).isNotNull();
    Map<String, Object> createdData = (Map<String, Object>) createResponse.getBody().get("data");
    Number id = (Number) createdData.get("id");

    ResponseEntity<Void> deleteResponse =
        rest.exchange(
            "/api/v1/admin/changelog/" + id.longValue(),
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            Void.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void superAdminCanUpdateChangelogEntry() {
    String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, TENANT);
    HttpHeaders adminHeaders = authHeaders(adminToken, TENANT);

    ResponseEntity<Map> createResponse =
        rest.exchange(
            "/api/v1/admin/changelog",
            HttpMethod.POST,
            new HttpEntity<>(requestPayload("1.3.0", false), adminHeaders),
            Map.class);

    Number id = (Number) ((Map<String, Object>) createResponse.getBody().get("data")).get("id");

    String superToken = login(SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD, TENANT);
    HttpHeaders superHeaders = authHeaders(superToken, TENANT);

    ResponseEntity<Map> updateResponse =
        rest.exchange(
            "/api/v1/admin/changelog/" + id.longValue(),
            HttpMethod.PUT,
            new HttpEntity<>(
                Map.of(
                    "version", "1.3.1",
                    "title", "Updated title",
                    "body", "Updated body",
                    "isHighlighted", true),
                superHeaders),
            Map.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> updatedData = (Map<String, Object>) updateResponse.getBody().get("data");
    assertThat(updatedData.get("version")).isEqualTo("1.3.1");
    assertThat(updatedData.get("isHighlighted")).isEqualTo(Boolean.TRUE);
  }

  @Test
  void publicEndpoints_areAccessibleWithoutAuthentication() {
    ResponseEntity<Map> listResponse = rest.getForEntity("/api/v1/changelog", Map.class);
    ResponseEntity<Map> highlightedResponse =
        rest.getForEntity("/api/v1/changelog/latest-highlighted", Map.class);

    assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(highlightedResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
  }

  @Test
  void semverValidation_rejectsInvalidVersion() {
    String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD, TENANT);
    HttpHeaders headers = authHeaders(adminToken, TENANT);

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/admin/changelog",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "version", "version-one",
                    "title", "Invalid semver",
                    "body", "Should fail",
                    "isHighlighted", false),
                headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private HttpHeaders authHeaders(String token, String companyCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", companyCode);
    return headers;
  }

  private Map<String, Object> requestPayload(String version, boolean highlighted) {
    return Map.of(
        "version",
        version,
        "title",
        "Release " + version,
        "body",
        "- Entry " + version,
        "isHighlighted",
        highlighted);
  }

  private String login(String email, String password, String companyCode) {
    ResponseEntity<Map> loginResp =
        rest.postForEntity(
            "/api/v1/auth/login",
            Map.of("email", email, "password", password, "companyCode", companyCode),
            Map.class);
    assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    return (String) loginResp.getBody().get("accessToken");
  }
}
