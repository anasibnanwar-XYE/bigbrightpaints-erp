package com.bigbrightpaints.erp.modules.portal.service;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.bigbrightpaints.erp.modules.company.service.ModuleGatingInterceptor;

@Configuration
public class TenantRuntimeEnforcementConfig implements WebMvcConfigurer {

  private final TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor;
  private final ModuleGatingInterceptor moduleGatingInterceptor;

  public TenantRuntimeEnforcementConfig(
      TenantRuntimeEnforcementInterceptor tenantRuntimeEnforcementInterceptor,
      ModuleGatingInterceptor moduleGatingInterceptor) {
    this.tenantRuntimeEnforcementInterceptor = tenantRuntimeEnforcementInterceptor;
    this.moduleGatingInterceptor = moduleGatingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(moduleGatingInterceptor).addPathPatterns("/api/v1/**");
    registry.addInterceptor(tenantRuntimeEnforcementInterceptor).addPathPatterns("/api/v1/**");
  }
}
