package com.bigbrightpaints.erp.modules.sales.controller;

import com.bigbrightpaints.erp.modules.sales.service.DealerPortalService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dealer Portal API - endpoints for authenticated dealer users to view their own data.
 * All endpoints require ROLE_DEALER and automatically scope data to the logged-in dealer.
 */
@RestController
@RequestMapping("/api/v1/dealer-portal")
@PreAuthorize("hasAuthority('ROLE_DEALER')")
public class DealerPortalController {

    private final DealerPortalService dealerPortalService;

    public DealerPortalController(DealerPortalService dealerPortalService) {
        this.dealerPortalService = dealerPortalService;
    }

    /**
     * Get dealer dashboard with summary of balance, credit, aging.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> dashboard = dealerPortalService.getMyDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dealer dashboard", dashboard));
    }

    /**
     * Get complete ledger with all transactions and running balance.
     */
    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyLedger() {
        Map<String, Object> ledger = dealerPortalService.getMyLedger();
        return ResponseEntity.ok(ApiResponse.success("Your ledger", ledger));
    }

    /**
     * Get all invoices with status and outstanding amounts.
     */
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyInvoices() {
        Map<String, Object> invoices = dealerPortalService.getMyInvoices();
        return ResponseEntity.ok(ApiResponse.success("Your invoices", invoices));
    }

    /**
     * Get outstanding balance with aging buckets (current, 1-30, 31-60, 61-90, 90+).
     */
    @GetMapping("/aging")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyAging() {
        Map<String, Object> aging = dealerPortalService.getMyOutstandingAndAging();
        return ResponseEntity.ok(ApiResponse.success("Outstanding & aging", aging));
    }
}
