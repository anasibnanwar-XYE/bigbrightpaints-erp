package com.bigbrightpaints.erp.modules.portal.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.modules.company.service.ModuleGatingInterceptor;
import com.bigbrightpaints.erp.modules.company.service.TenantUsageMetricsInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@ExtendWith(MockitoExtension.class)
class TenantRuntimeEnforcementConfigTest {

    @Mock
    private TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor;
    @Mock
    private TenantUsageMetricsInterceptor tenantUsageMetricsInterceptor;
    @Mock
    private ModuleGatingInterceptor moduleGatingInterceptor;
    @Mock
    private InterceptorRegistry registry;
    @Mock
    private InterceptorRegistration usageRegistration;
    @Mock
    private InterceptorRegistration moduleRegistration;
    @Mock
    private InterceptorRegistration runtimeRegistration;

    @Test
    void addInterceptors_registersTenantRuntimeInterceptorForApiV1Paths() {
        when(registry.addInterceptor(tenantUsageMetricsInterceptor)).thenReturn(usageRegistration);
        when(registry.addInterceptor(moduleGatingInterceptor)).thenReturn(moduleRegistration);
        when(registry.addInterceptor(tenantRuntimeEnforcementInterceptor)).thenReturn(runtimeRegistration);
        TenantRuntimeEnforcementConfig config = new TenantRuntimeEnforcementConfig(
                tenantRuntimeEnforcementInterceptor,
                tenantUsageMetricsInterceptor,
                moduleGatingInterceptor);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(tenantUsageMetricsInterceptor);
        verify(registry).addInterceptor(moduleGatingInterceptor);
        verify(registry).addInterceptor(tenantRuntimeEnforcementInterceptor);
        verify(usageRegistration).addPathPatterns("/api/v1/**");
        verify(moduleRegistration).addPathPatterns("/api/v1/**");
        verify(runtimeRegistration).addPathPatterns("/api/v1/**");
    }
}
