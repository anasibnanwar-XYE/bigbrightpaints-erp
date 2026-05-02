package com.bigbrightpaints.erp.modules.company.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.modules.company.dto.CoATemplateDto;
import com.bigbrightpaints.erp.modules.company.service.CoATemplateService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/superadmin/tenants")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminTenantOnboardingController {

  private final CoATemplateService coATemplateService;

  public SuperAdminTenantOnboardingController(CoATemplateService coATemplateService) {
    this.coATemplateService = coATemplateService;
  }

  @GetMapping("/coa-templates")
  public ResponseEntity<ApiResponse<List<CoATemplateDto>>> listCoATemplates() {
    return ResponseEntity.ok(
        ApiResponse.success("CoA templates fetched", coATemplateService.listActiveTemplates()));
  }
}
