package com.bigbrightpaints.erp.modules.company.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateArchiveRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateCreateRequest;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminPlanTemplateUpdateRequest;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminPlanTemplateService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminPlanTemplateController {

  private final SuperAdminPlanTemplateService planTemplateService;

  public SuperAdminPlanTemplateController(SuperAdminPlanTemplateService planTemplateService) {
    this.planTemplateService = planTemplateService;
  }

  @GetMapping("/plans")
  public ResponseEntity<ApiResponse<List<SuperAdminPlanTemplateDto>>> listPlans(
      @RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Plan templates fetched", planTemplateService.listPlans(includeArchived)));
  }

  @GetMapping("/plans/{stableId}")
  public ResponseEntity<ApiResponse<SuperAdminPlanTemplateDto>> getPlan(
      @PathVariable String stableId,
      @RequestParam(value = "version", required = false) Integer version,
      @RequestParam(value = "includeArchived", defaultValue = "false") boolean includeArchived) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Plan template fetched",
            planTemplateService.getPlan(stableId, version, includeArchived)));
  }

  @PostMapping("/plans")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<ApiResponse<SuperAdminPlanTemplateDto>> createPlan(
      @Valid @RequestBody SuperAdminPlanTemplateCreateRequest request) {
    SuperAdminPlanTemplateDto response = planTemplateService.createPlan(request);
    return ResponseEntity.created(URI.create("/api/v1/superadmin/plans/" + response.stableId()))
        .body(ApiResponse.success("Plan template created", response));
  }

  @PutMapping("/plans/{stableId}")
  public ResponseEntity<ApiResponse<SuperAdminPlanTemplateDto>> updatePlan(
      @PathVariable String stableId,
      @Valid @RequestBody SuperAdminPlanTemplateUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Plan template updated", planTemplateService.updatePlan(stableId, request)));
  }

  @PostMapping("/plans/{stableId}/archive")
  public ResponseEntity<ApiResponse<SuperAdminPlanTemplateDto>> archivePlan(
      @PathVariable String stableId,
      @RequestBody(required = false) @Valid SuperAdminPlanTemplateArchiveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Plan template archived", planTemplateService.archivePlan(stableId, request)));
  }
}
