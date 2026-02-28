package com.bigbrightpaints.erp.modules.company.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminDashboardDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantUsageDto;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private TenantUsageMetricsService tenantUsageMetricsService;

    private TenantLifecycleService tenantLifecycleService;

    private SuperAdminService superAdminService;

    @BeforeEach
    void setUp() {
        tenantLifecycleService = new TenantLifecycleService(null);
        superAdminService = new SuperAdminService(
                companyRepository,
                userAccountRepository,
                auditLogRepository,
                tenantUsageMetricsService,
                tenantLifecycleService);
    }

    @Test
    void getDashboard_aggregatesTenantMetrics() {
        Company active = company(1L, "ALPHA", CompanyLifecycleState.ACTIVE);
        Company suspended = company(2L, "BETA", CompanyLifecycleState.SUSPENDED);
        when(companyRepository.findAll()).thenReturn(List.of(suspended, active));

        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(1L)).thenReturn(4L);
        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(2L)).thenReturn(2L);

        when(tenantUsageMetricsService.getApiCallCount(1L)).thenReturn(10L);
        when(tenantUsageMetricsService.getApiCallCount(2L)).thenReturn(5L);

        when(auditLogRepository.estimateAuditStorageBytesByCompanyId(1L)).thenReturn(200L);
        when(auditLogRepository.estimateAuditStorageBytesByCompanyId(2L)).thenReturn(300L);

        when(tenantUsageMetricsService.getLastActivityAt(1L)).thenReturn(Instant.parse("2026-01-01T10:00:00Z"));
        when(tenantUsageMetricsService.getLastActivityAt(2L)).thenReturn(Instant.parse("2026-01-01T11:00:00Z"));

        SuperAdminDashboardDto dashboard = superAdminService.getDashboard();

        assertThat(dashboard.totalTenants()).isEqualTo(2L);
        assertThat(dashboard.activeTenants()).isEqualTo(1L);
        assertThat(dashboard.suspendedTenants()).isEqualTo(1L);
        assertThat(dashboard.deactivatedTenants()).isEqualTo(0L);
        assertThat(dashboard.totalUsers()).isEqualTo(6L);
        assertThat(dashboard.totalApiCalls()).isEqualTo(15L);
        assertThat(dashboard.totalStorageBytes()).isEqualTo(500L);
        assertThat(dashboard.recentActivityAt()).isEqualTo(Instant.parse("2026-01-01T11:00:00Z"));
    }

    @Test
    void listTenants_filtersByStatus() {
        Company active = company(1L, "ALPHA", CompanyLifecycleState.ACTIVE);
        Company deactivated = company(2L, "BETA", CompanyLifecycleState.DEACTIVATED);
        when(companyRepository.findAll()).thenReturn(List.of(active, deactivated));
        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(1L)).thenReturn(1L);
        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(2L)).thenReturn(1L);

        List<SuperAdminTenantDto> activeOnly = superAdminService.listTenants("ACTIVE");
        List<SuperAdminTenantDto> deactivatedOnly = superAdminService.listTenants("DEACTIVATED");

        assertThat(activeOnly).extracting(SuperAdminTenantDto::companyCode).containsExactly("ALPHA");
        assertThat(deactivatedOnly).extracting(SuperAdminTenantDto::companyCode).containsExactly("BETA");

        assertThatThrownBy(() -> superAdminService.listTenants("paused"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("status filter must be ACTIVE, SUSPENDED, or DEACTIVATED");
    }

    @Test
    void suspendAndActivateTenant_updatesLifecycleState() {
        Company company = company(3L, "ACME", CompanyLifecycleState.ACTIVE);
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company));
        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(3L)).thenReturn(3L);

        SuperAdminTenantDto suspended = superAdminService.suspendTenant(3L);
        SuperAdminTenantDto activated = superAdminService.activateTenant(3L);

        assertThat(suspended.status()).isEqualTo("SUSPENDED");
        assertThat(company.getLifecycleState()).isEqualTo(CompanyLifecycleState.ACTIVE);
        assertThat(activated.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getTenantUsage_returnsUsageMetrics() {
        Company company = company(8L, "TEN", CompanyLifecycleState.SUSPENDED);
        when(companyRepository.findById(8L)).thenReturn(Optional.of(company));
        when(tenantUsageMetricsService.getApiCallCount(8L)).thenReturn(12L);
        when(userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(8L)).thenReturn(7L);
        when(auditLogRepository.estimateAuditStorageBytesByCompanyId(8L)).thenReturn(1234L);
        when(tenantUsageMetricsService.getLastActivityAt(8L)).thenReturn(Instant.parse("2026-02-10T10:15:30Z"));

        SuperAdminTenantUsageDto usage = superAdminService.getTenantUsage(8L);

        assertThat(usage.companyCode()).isEqualTo("TEN");
        assertThat(usage.status()).isEqualTo("SUSPENDED");
        assertThat(usage.apiCallCount()).isEqualTo(12L);
        assertThat(usage.activeUsers()).isEqualTo(7L);
        assertThat(usage.storageBytes()).isEqualTo(1234L);
        assertThat(usage.lastActivityAt()).isEqualTo(Instant.parse("2026-02-10T10:15:30Z"));
    }

    private Company company(Long id, String code, CompanyLifecycleState lifecycleState) {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", id);
        ReflectionTestUtils.setField(company, "publicId", UUID.randomUUID());
        company.setName("Company " + code);
        company.setCode(code);
        company.setTimezone("UTC");
        company.setLifecycleState(lifecycleState);
        return company;
    }
}
