package com.bigbrightpaints.erp.modules.portal.service;

import com.bigbrightpaints.erp.modules.company.service.ModuleGatingInterceptor;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantRuntimeEnforcementConfig implements WebMvcConfigurer {

    private final TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor;
    private final TenantUsageMetricsInterceptor tenantUsageMetricsInterceptor;
    private final ModuleGatingInterceptor moduleGatingInterceptor;

    public TenantRuntimeEnforcementConfig(TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor,
                                          TenantUsageMetricsInterceptor tenantUsageMetricsInterceptor,
                                          ModuleGatingInterceptor moduleGatingInterceptor) {
        this.tenantRuntimeEnforcementInterceptor = tenantRuntimeEnforcementInterceptor;
        this.tenantUsageMetricsInterceptor = tenantUsageMetricsInterceptor;
        this.moduleGatingInterceptor = moduleGatingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantUsageMetricsInterceptor)
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(moduleGatingInterceptor)
                .addPathPatterns("/api/v1/**");
        registry.addInterceptor(tenantRuntimeEnforcementInterceptor)
                .addPathPatterns("/api/v1/**");
    }
}
