package com.bigbrightpaints.erp.modules.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class TenantOnboardingControllerTest extends AbstractIntegrationTest {

  private static final String ROOT_COMPANY_CODE = "ROOT";
  private static final String SUPER_ADMIN_EMAIL = "super-admin@bbp.com";
  private static final String PASSWORD = "admin123";

  @Autowired private TestRestTemplate rest;

  @Autowired private CompanyRepository companyRepository;

  @Autowired private UserAccountRepository userAccountRepository;

  @SpyBean private EmailService emailService;

  @BeforeEach
  void seedSuperAdmin() {
    dataSeeder.ensureUser(
        SUPER_ADMIN_EMAIL,
        PASSWORD,
        "Super Admin",
        ROOT_COMPANY_CODE,
        List.of("ROLE_SUPER_ADMIN", "ROLE_ADMIN"));
  }

  @Test
  void retiredFlatOnboardingRoute_returnsGoneWithoutCreatingTenantOrSendingCredentials() {
    String superAdminToken = loginToken(SUPER_ADMIN_EMAIL, ROOT_COMPANY_CODE);
    String companyCode = uniqueCode("RET");
    String adminEmail = "retired-onboard-" + UUID.randomUUID() + "@example.com";

    ResponseEntity<Map> response =
        rest.exchange(
            "/api/v1/superadmin/tenants/onboard",
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "name",
                    "Retired Tenant",
                    "code",
                    companyCode,
                    "timezone",
                    "Asia/Kolkata",
                    "firstAdminEmail",
                    adminEmail,
                    "firstAdminDisplayName",
                    "Retired Admin",
                    "coaTemplateCode",
                    "GENERIC"),
                headers(superAdminToken, ROOT_COMPANY_CODE)),
            Map.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).containsEntry("success", Boolean.FALSE);
    assertThat(response.getBody().get("message").toString()).contains("retired");
    assertThat(companyRepository.findByCodeIgnoreCase(companyCode)).isEmpty();
    assertThat(
            userAccountRepository.findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
                adminEmail, companyCode))
        .isEmpty();
    verify(emailService, never())
        .sendUserCredentialsEmailRequired(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString());
  }

  private String uniqueCode(String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
  }

  private HttpHeaders headers(String token, String companyCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Company-Code", companyCode);
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
}
