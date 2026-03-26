package com.bigbrightpaints.erp.modules.company.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.bigbrightpaints.erp.modules.company.dto.CompanyEnabledModulesDto;
import com.bigbrightpaints.erp.modules.company.service.CompanyService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantControlPlaneService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@ExtendWith(MockitoExtension.class)
class SuperAdminControllerTest {

  @Mock private CompanyService companyService;

  @Mock private SuperAdminTenantControlPlaneService controlPlaneService;

  @InjectMocks private SuperAdminController superAdminController;

  @Test
  void updateTenantModules_delegatesToCompanyService() {
    CompanyEnabledModulesDto modulesDto =
        new CompanyEnabledModulesDto(15L, "ACME", Set.of("PORTAL"));
    when(controlPlaneService.updateModules(15L, Set.of("PORTAL"))).thenReturn(modulesDto);

    ResponseEntity<ApiResponse<CompanyEnabledModulesDto>> response =
        superAdminController.updateTenantModules(
            15L, new SuperAdminController.TenantModulesUpdateRequest(Set.of("PORTAL")));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isTrue();
    assertThat(response.getBody().message()).isEqualTo("Tenant modules updated");
    assertThat(response.getBody().data()).isEqualTo(modulesDto);
    verify(controlPlaneService).updateModules(15L, Set.of("PORTAL"));
  }
}
