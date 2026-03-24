package com.bigbrightpaints.erp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfigurationSource;

import com.bigbrightpaints.erp.core.config.SystemSettingsService;

@Configuration
public class CorsConfig {

  private final SystemSettingsService systemSettingsService;

  public CorsConfig(SystemSettingsService systemSettingsService) {
    this.systemSettingsService = systemSettingsService;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    return request -> systemSettingsService.buildCorsConfiguration();
  }
}
