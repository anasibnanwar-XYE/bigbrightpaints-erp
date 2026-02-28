package com.bigbrightpaints.erp.modules.portal.service;

import com.bigbrightpaints.erp.modules.company.service.TenantUsageMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantRuntimeEnforcementConfig implements WebMvcConfigurer {

    private final TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor;
    private final TenantUsageMetricsInterceptor tenantUsageMetricsInterceptor;

    public TenantRuntimeEnforcementConfig(TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor,
                                          TenantUsageMetricsInterceptor tenantUsageMetricsInterceptor) {
        this.tenantRuntimeEnforcementInterceptor = tenantRuntimeEnforcementInterceptor;
        this.tenantUsageMetricsInterceptor = tenantUsageMetricsInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantUsageMetricsInterceptor)
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(tenantRuntimeEnforcementInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
