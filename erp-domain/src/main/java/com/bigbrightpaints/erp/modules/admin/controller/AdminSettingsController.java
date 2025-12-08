package com.bigbrightpaints.erp.modules.admin.controller;

import com.bigbrightpaints.erp.core.config.SystemSettingsService;
import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.modules.admin.dto.*;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.sales.domain.CreditRequest;
import com.bigbrightpaints.erp.modules.sales.domain.CreditRequestRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminSettingsController {

    private final SystemSettingsService systemSettingsService;
    private final EmailService emailService;
    private final CompanyContextService companyContextService;
    private final CreditRequestRepository creditRequestRepository;
    private final PayrollRunRepository payrollRunRepository;

    public AdminSettingsController(SystemSettingsService systemSettingsService,
                                   EmailService emailService,
                                   CompanyContextService companyContextService,
                                   CreditRequestRepository creditRequestRepository,
                                   PayrollRunRepository payrollRunRepository) {
        this.systemSettingsService = systemSettingsService;
        this.emailService = emailService;
        this.companyContextService = companyContextService;
        this.creditRequestRepository = creditRequestRepository;
        this.payrollRunRepository = payrollRunRepository;
    }

    @GetMapping("/settings")
    public ApiResponse<SystemSettingsDto> getSettings() {
        return ApiResponse.success("Settings fetched", systemSettingsService.snapshot());
    }

    @PutMapping("/settings")
    public ApiResponse<SystemSettingsDto> updateSettings(@Valid @RequestBody SystemSettingsUpdateRequest request) {
        SystemSettingsDto dto = systemSettingsService.update(request);
        return ApiResponse.success("Settings updated", dto);
    }

    @PostMapping("/notify")
    public ApiResponse<String> notifyUser(@Valid @RequestBody AdminNotifyRequest request) {
        emailService.sendSimpleEmail(request.to(), request.subject(), request.body());
        return ApiResponse.success("Notification sent", "Email dispatched");
    }

    @GetMapping("/approvals")
    public ApiResponse<AdminApprovalsResponse> approvals() {
        Company company = companyContextService.requireCurrentCompany();
        List<AdminApprovalItemDto> creditApprovals = creditRequestRepository
                .findByCompanyAndStatusOrderByCreatedAtDesc(company, "PENDING")
                .stream()
                .map(cr -> approvalItem("CREDIT_REQUEST", cr.getId(), cr.getPublicId(),
                        "CR-" + cr.getId(), cr.getStatus(),
                        cr.getDealer() != null ? cr.getDealer().getName() + " - " + cr.getAmountRequested()
                                : cr.getAmountRequested().toPlainString(),
                        cr.getCreatedAt()))
                .toList();

        List<AdminApprovalItemDto> payrollApprovals = payrollRunRepository
                .findByCompanyAndStatusOrderByCreatedAtDesc(company, PayrollRun.PayrollStatus.CALCULATED)
                .stream()
                .map(pr -> approvalItem("PAYROLL_RUN", pr.getId(), pr.getPublicId(),
                        pr.getRunNumber(), pr.getStatus().name(),
                        pr.getRunType().name() + " " + pr.getPeriodStart() + " - " + pr.getPeriodEnd(),
                        pr.getCreatedAt()))
                .toList();

        AdminApprovalsResponse response = new AdminApprovalsResponse(creditApprovals, payrollApprovals);
        return ApiResponse.success("Approvals fetched", response);
    }

    private AdminApprovalItemDto approvalItem(String type, Long id, UUID publicId, String reference,
                                              String status, String summary, Instant createdAt) {
        return new AdminApprovalItemDto(type, id, publicId, reference, status, summary, createdAt);
    }
}
