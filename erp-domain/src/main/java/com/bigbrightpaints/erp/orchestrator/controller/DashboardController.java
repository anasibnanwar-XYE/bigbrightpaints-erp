package com.bigbrightpaints.erp.orchestrator.controller;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.orchestrator.service.DashboardAggregationService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

@RestController
@RequestMapping("/api/v1/orchestrator/dashboard")
public class DashboardController {

    private final DashboardAggregationService dashboardAggregationService;

    public DashboardController(DashboardAggregationService dashboardAggregationService) {
        this.dashboardAggregationService = dashboardAggregationService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> adminDashboard() {
        return ResponseEntity.ok(dashboardAggregationService.adminDashboard(requireCompanyCode()));
    }

    @GetMapping("/factory")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')")
    public ResponseEntity<Map<String, Object>> factoryDashboard() {
        return ResponseEntity.ok(dashboardAggregationService.factoryDashboard(requireCompanyCode()));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')")
    public ResponseEntity<Map<String, Object>> financeDashboard() {
        return ResponseEntity.ok(dashboardAggregationService.financeDashboard(requireCompanyCode()));
    }

    private String requireCompanyCode() {
        String companyCode = CompanyContextHolder.getCompanyCode();
        if (!StringUtils.hasText(companyCode)) {
            throw new IllegalStateException("Company context is required");
        }
        return companyCode.trim();
    }
}
