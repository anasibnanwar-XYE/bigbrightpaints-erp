package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyModule;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.company.service.ModuleGatingService;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminTenantEntitlementService;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class TS_RuntimeModuleGatingServiceExecutableCoverageTest {

  @Mock private CompanyContextService companyContextService;
  @Mock private SuperAdminTenantEntitlementService entitlementService;

  @AfterEach
  void tearDown() {
    CompanyContextHolder.clear();
  }

  @Test
  void requireEnabledForCurrentCompany_allowsEnabledModule() {
    Company company = company("ACME", CompanyModule.HR_PAYROLL.name());
    when(companyContextService.requireCurrentCompany()).thenReturn(company);
    when(entitlementService.isFeatureEnabled(company, CompanyModule.HR_PAYROLL)).thenReturn(true);
    ModuleGatingService service =
        new ModuleGatingService(companyContextService, entitlementService);

    service.requireEnabledForCurrentCompany(CompanyModule.HR_PAYROLL, "/api/v1/portal/workforce");

    verify(companyContextService).requireCurrentCompany();
  }

  @Test
  void requireEnabled_throwsWithProvidedCompanyCodeAndPath() {
    ModuleGatingService service =
        new ModuleGatingService(companyContextService, entitlementService);
    Company company = company("ACME", CompanyModule.PORTAL.name());
    when(entitlementService.isFeatureEnabled(company, CompanyModule.HR_PAYROLL)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.requireEnabled(
                    company, CompanyModule.HR_PAYROLL, "/api/v1/portal/workforce"))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MODULE_DISABLED);
              assertThat(ex.getDetails())
                  .containsEntry("module", "HR_PAYROLL")
                  .containsEntry("companyCode", "ACME")
                  .containsEntry("path", "/api/v1/portal/workforce");
            });
  }

  @Test
  void requireEnabled_fallsBackToCompanyContextWhenCompanyCodeBlank() {
    ModuleGatingService service =
        new ModuleGatingService(companyContextService, entitlementService);
    CompanyContextHolder.setCompanyCode("CTX-COMP");
    Company company = company("   ", CompanyModule.PORTAL.name());
    when(entitlementService.isFeatureEnabled(company, CompanyModule.HR_PAYROLL)).thenReturn(false);

    assertThatThrownBy(() -> service.requireEnabled(company, CompanyModule.HR_PAYROLL, "   "))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MODULE_DISABLED);
              assertThat(ex.getDetails())
                  .containsEntry("module", "HR_PAYROLL")
                  .containsEntry("companyCode", "CTX-COMP")
                  .doesNotContainKey("path");
            });
  }

  @Test
  void requireEnabled_omitsCompanyCodeWhenUnavailable() {
    ModuleGatingService service =
        new ModuleGatingService(companyContextService, entitlementService);
    when(entitlementService.isFeatureEnabled(null, CompanyModule.HR_PAYROLL)).thenReturn(false);

    assertThatThrownBy(
            () ->
                service.requireEnabled(null, CompanyModule.HR_PAYROLL, "/api/v1/portal/workforce"))
        .isInstanceOfSatisfying(
            ApplicationException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MODULE_DISABLED);
              assertThat(ex.getDetails())
                  .containsEntry("module", "HR_PAYROLL")
                  .containsEntry("path", "/api/v1/portal/workforce")
                  .doesNotContainKey("companyCode");
            });
  }

  @Test
  void isEnabled_handlesCoreAndDisabledModulesWithoutContextLookup() {
    ModuleGatingService service =
        new ModuleGatingService(companyContextService, entitlementService);
    Company company = company("ACME", CompanyModule.PORTAL.name());
    when(entitlementService.isFeatureEnabled(company, CompanyModule.HR_PAYROLL)).thenReturn(false);
    when(entitlementService.isFeatureEnabled(null, CompanyModule.HR_PAYROLL)).thenReturn(false);

    assertThat(service.isEnabled(company, CompanyModule.ACCOUNTING)).isTrue();
    assertThat(service.isEnabled(company, CompanyModule.HR_PAYROLL)).isFalse();
    assertThat(service.isEnabled(null, CompanyModule.HR_PAYROLL)).isFalse();
    verify(companyContextService, never()).requireCurrentCompany();
  }

  private Company company(String code, String... enabledModules) {
    Company company = new Company();
    company.setCode(code);
    company.setEnabledModules(Set.of(enabledModules));
    return company;
  }
}
