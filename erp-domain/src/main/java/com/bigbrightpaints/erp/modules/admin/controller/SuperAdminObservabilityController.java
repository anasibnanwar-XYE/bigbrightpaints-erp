package com.bigbrightpaints.erp.modules.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.observability.DatadogTelemetryService;
import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/superadmin/observability")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminObservabilityController {

  private final DatadogTelemetryService datadogTelemetryService;

  public SuperAdminObservabilityController(DatadogTelemetryService datadogTelemetryService) {
    this.datadogTelemetryService = datadogTelemetryService;
  }

  @GetMapping("/datadog/status")
  public ResponseEntity<ApiResponse<DatadogTelemetryService.DatadogTelemetryStatus>>
      datadogStatus() {
    return ResponseEntity.ok(
        ApiResponse.success("Datadog telemetry status fetched", datadogTelemetryService.status()));
  }
}
