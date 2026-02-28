package com.bigbrightpaints.erp.modules.company.service;

import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateDto;
import com.bigbrightpaints.erp.modules.company.dto.CompanyLifecycleStateRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminDashboardDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantUsageDto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SuperAdminService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_SUSPENDED = "SUSPENDED";
    private static final String STATUS_DEACTIVATED = "DEACTIVATED";

    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogRepository auditLogRepository;
    private final TenantUsageMetricsService tenantUsageMetricsService;
    private final TenantLifecycleService tenantLifecycleService;

    public SuperAdminService(CompanyRepository companyRepository,
                             UserAccountRepository userAccountRepository,
                             AuditLogRepository auditLogRepository,
                             TenantUsageMetricsService tenantUsageMetricsService,
                             TenantLifecycleService tenantLifecycleService) {
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.auditLogRepository = auditLogRepository;
        this.tenantUsageMetricsService = tenantUsageMetricsService;
        this.tenantLifecycleService = tenantLifecycleService;
    }

    @Transactional(readOnly = true)
    public SuperAdminDashboardDto getDashboard() {
        List<SuperAdminTenantDto> tenants = listTenants(null);
        long totalTenants = tenants.size();
        long activeTenants = tenants.stream().filter(tenant -> STATUS_ACTIVE.equals(tenant.status())).count();
        long suspendedTenants = tenants.stream().filter(tenant -> STATUS_SUSPENDED.equals(tenant.status())).count();
        long deactivatedTenants = tenants.stream().filter(tenant -> STATUS_DEACTIVATED.equals(tenant.status())).count();
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
                deactivatedTenants,
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
        tenantLifecycleService.transition(
                company,
                CompanyLifecycleState.SUSPENDED,
                "suspended-by-superadmin",
                SecurityContextHolder.getContext().getAuthentication());
        return toTenantDto(company);
    }

    @Transactional
    public SuperAdminTenantDto activateTenant(Long companyId) {
        Company company = requireCompany(companyId);
        tenantLifecycleService.transition(
                company,
                CompanyLifecycleState.ACTIVE,
                "activated-by-superadmin",
                SecurityContextHolder.getContext().getAuthentication());
        return toTenantDto(company);
    }

    @Transactional
    public SuperAdminTenantDto deactivateTenant(Long companyId) {
        Company company = requireCompany(companyId);
        tenantLifecycleService.transition(
                company,
                CompanyLifecycleState.DEACTIVATED,
                "deactivated-by-superadmin",
                SecurityContextHolder.getContext().getAuthentication());
        return toTenantDto(company);
    }

    @Transactional
    public CompanyLifecycleStateDto updateLifecycleState(Long companyId, CompanyLifecycleStateRequest request) {
        Company company = requireCompany(companyId);
        return tenantLifecycleService.transition(
                company,
                CompanyLifecycleState.fromRequestValue(request.state()),
                request.reason(),
                SecurityContextHolder.getContext().getAuthentication());
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
        return switch (lifecycleState) {
            case ACTIVE -> STATUS_ACTIVE;
            case SUSPENDED -> STATUS_SUSPENDED;
            case DEACTIVATED -> STATUS_DEACTIVATED;
        };
    }

    private String normalizeStatusFilter(String statusFilter) {
        if (!StringUtils.hasText(statusFilter)) {
            return null;
        }
        String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
        if (STATUS_ACTIVE.equals(normalized)
                || STATUS_SUSPENDED.equals(normalized)
                || STATUS_DEACTIVATED.equals(normalized)) {
            return normalized;
        }
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                "status filter must be ACTIVE, SUSPENDED, or DEACTIVATED");
    }
}
