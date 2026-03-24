package com.bigbrightpaints.erp.modules.factory;

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
import org.springframework.http.ResponseEntity;

import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class PackingControllerSecurityIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "FACTORY-SEC";
  private static final String FACTORY_EMAIL = "factory-sec@bbp.com";
  private static final String SALES_EMAIL = "sales-sec@bbp.com";
  private static final String PASSWORD = "Pass123!";

  @Autowired private TestRestTemplate rest;

  @BeforeEach
  void seedUsers() {
    dataSeeder.ensureUser(
        FACTORY_EMAIL, PASSWORD, "Factory Sec", COMPANY_CODE, List.of("ROLE_FACTORY"));
    dataSeeder.ensureUser(SALES_EMAIL, PASSWORD, "Sales Sec", COMPANY_CODE, List.of("ROLE_SALES"));
  }

  @Test
  void unpackedBatches_forbidsSalesRole() {
    HttpHeaders headers = authHeaders(loginToken(SALES_EMAIL));
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/factory/unpacked-batches",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void unpackedBatches_allowsFactoryRole() {
    HttpHeaders headers = authHeaders(loginToken(FACTORY_EMAIL));
    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/factory/unpacked-batches",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  private String loginToken(String email) {
    Map<String, Object> request =
        Map.of(
            "email", email,
            "password", PASSWORD,
            "companyCode", COMPANY_CODE);
    ResponseEntity<Map> response = rest.postForEntity("/api/v1/auth/login", request, Map.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    return (String) response.getBody().get("accessToken");
  }

  private HttpHeaders authHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("X-Company-Code", COMPANY_CODE);
    return headers;
  }
}
