package com.bigbrightpaints.erp.modules.portal.controller;

import com.bigbrightpaints.erp.modules.portal.dto.DashboardInsights;
import com.bigbrightpaints.erp.modules.portal.dto.OperationsInsights;
import com.bigbrightpaints.erp.modules.portal.dto.WorkforceInsights;
import com.bigbrightpaints.erp.modules.portal.service.PortalInsightsService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portal")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PortalInsightsController {

    private final PortalInsightsService portalInsightsService;

    public PortalInsightsController(PortalInsightsService portalInsightsService) {
        this.portalInsightsService = portalInsightsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardInsights>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard insights", portalInsightsService.dashboard()));
    }

    @GetMapping("/operations")
    public ResponseEntity<ApiResponse<OperationsInsights>> operations() {
        return ResponseEntity.ok(ApiResponse.success("Operations insights", portalInsightsService.operations()));
    }

    @GetMapping("/workforce")
    public ResponseEntity<ApiResponse<WorkforceInsights>> workforce() {
        return ResponseEntity.ok(ApiResponse.success("Workforce insights", portalInsightsService.workforce()));
    }
}
