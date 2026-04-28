package com.bigbrightpaints.erp.modules.company.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.modules.company.dto.CoATemplateDto;
import com.bigbrightpaints.erp.modules.company.service.CoATemplateService;
import com.bigbrightpaints.erp.modules.company.service.TenantOnboardingService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/superadmin/tenants")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminTenantOnboardingController {

  private final CoATemplateService coATemplateService;
  private final TenantOnboardingService tenantOnboardingService;

  public SuperAdminTenantOnboardingController(
      CoATemplateService coATemplateService, TenantOnboardingService tenantOnboardingService) {
    this.coATemplateService = coATemplateService;
    this.tenantOnboardingService = tenantOnboardingService;
  }

  @GetMapping("/coa-templates")
  public ResponseEntity<ApiResponse<List<CoATemplateDto>>> listCoATemplates() {
    return ResponseEntity.ok(
        ApiResponse.success("CoA templates fetched", coATemplateService.listActiveTemplates()));
  }

  @Hidden
  @PostMapping("/onboard")
  public ResponseEntity<ApiResponse<Map<String, Object>>> retiredOnboardTenant(
      @RequestBody(required = false) Object ignored, HttpServletRequest request) {
    return SuperAdminRetiredRouteErrors.gone(
        "retired-superadmin-flat-onboarding",
        "Flat Super Admin tenant onboarding is retired; use the V1 Add Client activation flow",
        request,
        "/api/v1/superadmin/tenants/onboard");
  }
}
