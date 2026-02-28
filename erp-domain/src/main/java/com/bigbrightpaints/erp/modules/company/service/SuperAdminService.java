package com.bigbrightpaints.erp.modules.company.service;

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminDashboardDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantUsageDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SuperAdminService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final TenantUsageMetricsService tenantUsageMetricsService;

    public SuperAdminService(CompanyRepository companyRepository,
                             UserAccountRepository userAccountRepository,
                             AuditLogRepository auditLogRepository,
                             TenantUsageMetricsService tenantUsageMetricsService) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogRepository = auditLogRepository;
        this.tenantUsageMetricsService = tenantUsageMetricsService;
    }

    @Transactional(readOnly = true)
    public SuperAdminDashboardDto getDashboard() {
        List<SuperAdminTenantDto> tenants = listTenants(null);
        long totalTenants = tenants.size();
        long activeTenants = tenants.stream().filter(tenant -> STATUS_ACTIVE.equals(tenant.status())).count();
        long suspendedTenants = tenants.stream().filter(tenant -> STATUS_SUSPENDED.equals(tenant.status())).count();
        long totalUsers = tenants.stream().mapToLong(SuperAdminTenantDto::activeUsers).sum();
        long totalApiCalls = tenants.stream().mapToLong(SuperAdminTenantDto::apiCallCount).sum();
        long totalStorageBytes = tenants.stream().mapToLong(SuperAdminTenantDto::storageBytes).sum();
        Instant recentActivityAt = tenants.stream()
                .map(SuperAdminTenantDto::lastActivityAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new SuperAdminDashboardDto(
                totalTenants,
                activeTenants,
                suspendedTenants,
                totalUsers,
                totalApiCalls,
                totalStorageBytes,
                recentActivityAt);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminTenantDto> listTenants(String statusFilter) {
        String normalizedFilter = normalizeStatusFilter(statusFilter);
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::toTenantDto)
                .filter(tenant -> normalizedFilter == null || normalizedFilter.equals(tenant.status()))
                .toList();
    }

    @Transactional
    public SuperAdminTenantDto suspendTenant(Long companyId) {
        Company company = requireCompany(companyId);
        company.setLifecycleState(CompanyLifecycleState.HOLD);
        company.setLifecycleReason("suspended-by-superadmin");
        return toTenantDto(company);
    }

    @Transactional
    public SuperAdminTenantDto activateTenant(Long companyId) {
        Company company = requireCompany(companyId);
        company.setLifecycleState(CompanyLifecycleState.ACTIVE);
        company.setLifecycleReason("activated-by-superadmin");
        return toTenantDto(company);
    }

    @Transactional(readOnly = true)
    public SuperAdminTenantUsageDto getTenantUsage(Long companyId) {
        Company company = requireCompany(companyId);
        return new SuperAdminTenantUsageDto(
                company.getId(),
                company.getCode(),
                resolveStatus(company),
                tenantUsageMetricsService.getApiCallCount(company.getId()),
                userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(company.getId()),
                auditLogRepository.estimateAuditStorageBytesByCompanyId(company.getId()),
                tenantUsageMetricsService.getLastActivityAt(company.getId()));
    }

    private SuperAdminTenantDto toTenantDto(Company company) {
        long activeUsers = userAccountRepository.countDistinctByCompanies_IdAndEnabledTrue(company.getId());
        long apiCalls = tenantUsageMetricsService.getApiCallCount(company.getId());
        long storageBytes = auditLogRepository.estimateAuditStorageBytesByCompanyId(company.getId());
        Instant lastActivityAt = tenantUsageMetricsService.getLastActivityAt(company.getId());
        return new SuperAdminTenantDto(
                company.getId(),
                company.getCode(),
                company.getName(),
                resolveStatus(company),
                activeUsers,
                apiCalls,
                storageBytes,
                lastActivityAt);
    }

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Company not found"));
    }

    private String resolveStatus(Company company) {
        CompanyLifecycleState lifecycleState = company.getLifecycleState() == null
                ? CompanyLifecycleState.ACTIVE
                : company.getLifecycleState();
        return lifecycleState == CompanyLifecycleState.ACTIVE ? STATUS_ACTIVE : STATUS_SUSPENDED;
    }

    private String normalizeStatusFilter(String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return null;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if (STATUS_ACTIVE.equals(normalized) || STATUS_SUSPENDED.equals(normalized)) {
            return normalized;
        }
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                "status filter must be ACTIVE or SUSPENDED");
    }
}
