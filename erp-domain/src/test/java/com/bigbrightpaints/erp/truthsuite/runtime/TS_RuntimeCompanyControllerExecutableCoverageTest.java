package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.bigbrightpaints.erp.modules.company.controller.SuperAdminController;
import com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateDto;
import com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateRequest;
import com.bigbrightpaints.erp.modules.company.service.CompanyService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantEntitlementService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminUsageService;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageRollupService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@Tag("critical")
@Tag("reconciliation")
class TS_RuntimeCompanyControllerExecutableCoverageTest {

  @Test
  void canonicalLifecycleUpdate_delegatesToControlPlaneService() {
    CompanyService companyService = mock(CompanyService.class);
    SuperAdminTenantControlPlaneService controlPlaneService =
        mock(SuperAdminTenantControlPlaneService.class);
    SuperAdminTenantEntitlementService entitlementService =
        mock(SuperAdminTenantEntitlementService.class);
    SuperAdminController controller =
        new SuperAdminController(
            companyService,
            controlPlaneService,
            entitlementService,
            mock(TenantUsageRollupService.class),
            mock(SuperAdminUsageService.class));
    CompanyLifecycleStateRequest request =
        new CompanyLifecycleStateRequest("SUSPENDED", "reconciliation");
    CompanyLifecycleStateDto responseDto =
        new CompanyLifecycleStateDto(42L, "ACME", "ACTIVE", "SUSPENDED", "reconciliation");
    when(controlPlaneService.updateLifecycleState(42L, request)).thenReturn(responseDto);

    ResponseEntity<ApiResponse<CompanyLifecycleStateDto>> response =
        controller.updateLifecycleState(42L, request);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isTrue();
    assertThat(response.getBody().data()).isEqualTo(responseDto);
    verify(controlPlaneService).updateLifecycleState(42L, request);
  }

  @Test
  void retiredAdminPasswordReset_returnsGoneWithoutDelegating() {
    CompanyService companyService = mock(CompanyService.class);
    SuperAdminTenantControlPlaneService controlPlaneService =
        mock(SuperAdminTenantControlPlaneService.class);
    SuperAdminTenantEntitlementService entitlementService =
        mock(SuperAdminTenantEntitlementService.class);
    SuperAdminController controller =
        new SuperAdminController(
            companyService,
            controlPlaneService,
            entitlementService,
            mock(TenantUsageRollupService.class),
            mock(SuperAdminUsageService.class));

    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        controller.retiredTenantAdminPasswordReset(
            42L,
            Map.of("adminEmail", "admin@ske.com"),
            new MockHttpServletRequest(
                "POST", "/api/v1/superadmin/tenants/42/support/admin-password-reset"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().data())
        .containsEntry("code", "retired-superadmin-admin-password-reset")
        .containsEntry("path", "/api/v1/superadmin/tenants/42/support/admin-password-reset")
        .containsKeys("message", "reason", "traceId");
    verify(controlPlaneService, never()).resetTenantAdminPassword(42L, "admin@ske.com", null);
  }
}
