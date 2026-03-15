package com.bigbrightpaints.erp.modules.portal.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.TenantRuntimeRequestAttributes;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.company.service.TenantRuntimeEnforcementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantRuntimeEnforcementInterceptor implements HandlerInterceptor {

    private final CompanyContextService companyContextService;
    private final TenantRuntimeEnforcementService tenantRuntimeEnforcementService;

    public TenantRuntimeEnforcementInterceptor(CompanyContextService companyContextService,
                                               TenantRuntimeEnforcementService tenantRuntimeEnforcementService) {
        this.companyContextService = companyContextService;
        this.tenantRuntimeEnforcementService = tenantRuntimeEnforcementService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!isEnforcedPath(path)) {
            return true;
        }
        if (Boolean.TRUE.equals(request.getAttribute(TenantRuntimeRequestAttributes.CANONICAL_ADMISSION_APPLIED))) {
            return true;
        }

        Company company = companyContextService.requireCurrentCompany();
        TenantRuntimeEnforcementService.TenantRequestAdmission admission = tenantRuntimeEnforcementService.beginRequest(
                company.getCode(),
                path,
                request.getMethod(),
                resolveCurrentActor(),
                false);
        if (admission == null || !admission.isAdmitted()) {
            throw admissionException(company.getCode(), path, admission);
        }
        request.setAttribute(TenantRuntimeRequestAttributes.INTERCEPTOR_FALLBACK_ADMISSION, admission);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object admission = request.getAttribute(TenantRuntimeRequestAttributes.INTERCEPTOR_FALLBACK_ADMISSION);
        if (admission instanceof TenantRuntimeEnforcementService.TenantRequestAdmission trackedAdmission) {
            tenantRuntimeEnforcementService.completeRequest(trackedAdmission, response.getStatus());
        }
    }

    private boolean isEnforcedPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return path.startsWith("/api/v1/reports/")
                || path.startsWith("/api/v1/accounting/reports/")
                || path.startsWith("/api/v1/portal/")
                || path.startsWith("/api/v1/demo/");
    }

    private RuntimeException admissionException(String companyCode,
                                                String path,
                                                TenantRuntimeEnforcementService.TenantRequestAdmission admission) {
        String normalizedCompanyCode = StringUtils.hasText(companyCode) ? companyCode.trim() : null;
        String normalizedPath = StringUtils.hasText(path) ? path.trim() : null;
        if (admission == null) {
            return new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE, "Tenant runtime admission is unavailable")
                    .withDetail("companyCode", normalizedCompanyCode)
                    .withDetail("path", normalizedPath);
        }
        TenantRuntimeEnforcementService.TenantRuntimeSnapshot snapshot =
                tenantRuntimeEnforcementService.snapshot(normalizedCompanyCode);
        if (StringUtils.hasText(admission.limitType())) {
            return new ApplicationException(ErrorCode.BUSINESS_LIMIT_EXCEEDED, admission.message())
                    .withDetail("companyCode", normalizedCompanyCode)
                    .withDetail("quotaType", admission.limitType())
                    .withDetail("quotaValue", parseIntOrZero(admission.limitValue()))
                    .withDetail("observed", parseIntOrZero(admission.observedValue()))
                    .withDetail("policyReference", snapshot.auditChainId())
                    .withDetail("path", normalizedPath);
        }
        return new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE, admission.message())
                .withDetail("companyCode", normalizedCompanyCode)
                .withDetail("holdState", snapshot.state().name())
                .withDetail("holdReason", snapshot.reasonCode())
                .withDetail("policyReference", snapshot.auditChainId())
                .withDetail("path", normalizedPath);
    }

    private int parseIntOrZero(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String resolveCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            return null;
        }
        return authentication.getName().trim();
    }
}
