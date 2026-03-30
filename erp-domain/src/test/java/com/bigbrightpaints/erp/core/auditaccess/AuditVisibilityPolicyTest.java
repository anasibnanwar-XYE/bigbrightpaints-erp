package com.bigbrightpaints.erp.core.auditaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.security.AuthScopeService;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;

class AuditVisibilityPolicyTest {

  @Test
  void platformVisibility_looksUpOnlyTheConfiguredPlatformCompanyId() {
    CompanyRepository companyRepository = mock(CompanyRepository.class);
    AuthScopeService authScopeService = mock(AuthScopeService.class);
    when(authScopeService.getPlatformScopeCode()).thenReturn("PLATFORM");
    when(companyRepository.findIdByCodeIgnoreCase("PLATFORM")).thenReturn(Optional.of(42L));
    AuditVisibilityPolicy policy = new AuditVisibilityPolicy(companyRepository, authScopeService);

    policy.platformVisibility();

    verify(companyRepository).findIdByCodeIgnoreCase("PLATFORM");
    verify(companyRepository, never()).findAll();
  }

  @Test
  void resolveCompanyCodes_batchesRequestedIdsAndTrimsResolvedCodes() {
    CompanyRepository companyRepository = mock(CompanyRepository.class);
    AuthScopeService authScopeService = mock(AuthScopeService.class);
    when(companyRepository.findCompanyCodesByIdIn(Set.of(7L, 8L)))
        .thenReturn(
            List.of(
                projection(7L, " TENANT-A "),
                projection(8L, " "),
                projection(null, "TENANT-Z")));
    AuditVisibilityPolicy policy = new AuditVisibilityPolicy(companyRepository, authScopeService);

    Map<Long, String> companyCodes = policy.resolveCompanyCodes(Set.of(7L, 8L));

    assertThat(companyCodes).containsExactly(Map.entry(7L, "TENANT-A"));
    verify(companyRepository).findCompanyCodesByIdIn(Set.of(7L, 8L));
  }

  private CompanyRepository.CompanyCodeProjection projection(Long id, String code) {
    return new CompanyRepository.CompanyCodeProjection() {
      @Override
      public Long getId() {
        return id;
      }

      @Override
      public String getCode() {
        return code;
      }
    };
  }
}
