package com.bigbrightpaints.erp.modules.admin.service;

import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.admin.dto.AdminApprovalInboxResponse;
import com.bigbrightpaints.erp.modules.admin.dto.AdminApprovalItemDto;
import com.bigbrightpaints.erp.modules.admin.dto.AdminDashboardDto;
import com.bigbrightpaints.erp.modules.admin.dto.TenantRuntimeMetricsDto;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;

@Service
public class AdminDashboardService {

  private final CompanyContextService companyContextService;
  private final AdminApprovalService adminApprovalService;
  private final TenantRuntimePolicyService tenantRuntimePolicyService;
  private final UserAccountRepository userAccountRepository;
  private final SupportTicketRepository supportTicketRepository;
  private final AuditLogRepository auditLogRepository;

  public AdminDashboardService(
      CompanyContextService companyContextService,
      AdminApprovalService adminApprovalService,
      TenantRuntimePolicyService tenantRuntimePolicyService,
      UserAccountRepository userAccountRepository,
      SupportTicketRepository supportTicketRepository,
      AuditLogRepository auditLogRepository) {
    this.companyContextService = companyContextService;
    this.adminApprovalService = adminApprovalService;
    this.tenantRuntimePolicyService = tenantRuntimePolicyService;
    this.userAccountRepository = userAccountRepository;
    this.supportTicketRepository = supportTicketRepository;
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional(readOnly = true)
  public AdminDashboardDto dashboard() {
    Company company = companyContextService.requireCurrentCompany();
    Long companyId = company.getId();

    AdminApprovalInboxResponse inbox = adminApprovalService.getInbox();
    List<AdminApprovalItemDto> approvals = inbox.items();

    AdminDashboardDto.ApprovalSummary approvalSummary =
        new AdminDashboardDto.ApprovalSummary(
            inbox.pendingCount(),
            countByOrigin(approvals, AdminApprovalItemDto.OriginType.CREDIT_REQUEST),
            countByOrigin(approvals, AdminApprovalItemDto.OriginType.CREDIT_LIMIT_OVERRIDE_REQUEST),
            countByOrigin(approvals, AdminApprovalItemDto.OriginType.PAYROLL_RUN),
            countByOrigin(approvals, AdminApprovalItemDto.OriginType.PERIOD_CLOSE_REQUEST),
            countByOrigin(approvals, AdminApprovalItemDto.OriginType.EXPORT_REQUEST));

    long totalUsers = userAccountRepository.countByCompany_Id(companyId);
    long enabledUsers = userAccountRepository.countByCompany_IdAndEnabledTrue(companyId);
    long mfaEnabledUsers = userAccountRepository.countByCompany_IdAndMfaEnabledTrue(companyId);

    AdminDashboardDto.UserSummary userSummary =
        new AdminDashboardDto.UserSummary(
            totalUsers, enabledUsers, Math.max(totalUsers - enabledUsers, 0), mfaEnabledUsers);

    AdminDashboardDto.SupportSummary supportSummary =
        new AdminDashboardDto.SupportSummary(
            supportTicketRepository.countByCompanyAndStatus(company, SupportTicketStatus.OPEN),
            supportTicketRepository.countByCompanyAndStatus(company, SupportTicketStatus.IN_PROGRESS),
            supportTicketRepository.countByCompanyAndStatus(company, SupportTicketStatus.RESOLVED),
            supportTicketRepository.countByCompanyAndStatus(company, SupportTicketStatus.CLOSED));

    long distinctSessions = auditLogRepository.countDistinctSessionActivityByCompanyId(companyId);
    long apiActivity = auditLogRepository.countApiActivityByCompanyId(companyId);
    long apiFailures = auditLogRepository.countApiFailureActivityByCompanyId(companyId);

    AdminDashboardDto.SecuritySummary securitySummary =
        new AdminDashboardDto.SecuritySummary(distinctSessions, apiActivity, apiFailures);

    List<AdminDashboardDto.ActivityItem> recentActivity =
        auditLogRepository.findTop50ByCompanyIdOrderByTimestampDesc(companyId).stream()
            .limit(12)
            .map(this::toActivityItem)
            .toList();

    TenantRuntimeMetricsDto runtime = tenantRuntimePolicyService.metrics();

    return new AdminDashboardDto(
        recentActivity, approvalSummary, userSummary, supportSummary, runtime, securitySummary);
  }

  private long countByOrigin(List<AdminApprovalItemDto> items, AdminApprovalItemDto.OriginType type) {
    return items.stream().filter(item -> item.originType() == type).count();
  }

  private AdminDashboardDto.ActivityItem toActivityItem(AuditLog auditLog) {
    String details =
        StringUtils.hasText(auditLog.getRequestPath())
            ? auditLog.getRequestMethod() + " " + auditLog.getRequestPath()
            : StringUtils.hasText(auditLog.getDetails()) ? auditLog.getDetails() : null;
    return new AdminDashboardDto.ActivityItem(
        auditLog.getTimestamp() != null
            ? auditLog.getTimestamp().atZone(ZoneOffset.UTC).toInstant()
            : null,
        auditLog.getEventType() != null ? auditLog.getEventType().name() : "UNKNOWN",
        auditLog.getUsername(),
        details);
  }
}
