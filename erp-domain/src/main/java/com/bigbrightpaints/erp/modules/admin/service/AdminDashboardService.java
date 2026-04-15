package com.bigbrightpaints.erp.modules.admin.service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.SystemRole;

@Service
public class AdminDashboardService {

  private static final Set<String> TENANT_ADMIN_HIDDEN_ROLES =
      Set.of(SystemRole.ADMIN.getRoleName(), SystemRole.SUPER_ADMIN.getRoleName());

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

    List<UserAccount> companyUsers = userAccountRepository.findByCompany_Id(companyId);
    List<UserAccount> visibleUsers =
        companyUsers.stream().filter(this::isTenantAdminVisibleUser).toList();
    Set<String> hiddenActorKeys =
        companyUsers.stream()
            .filter(this::isTenantAdminProtectedUser)
            .map(UserAccount::getEmail)
            .map(this::normalizeActorKey)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());

    long totalUsers = visibleUsers.size();
    long enabledUsers = visibleUsers.stream().filter(UserAccount::isEnabled).count();
    long mfaEnabledUsers = visibleUsers.stream().filter(UserAccount::isMfaEnabled).count();

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
            .filter(auditLog -> !isProtectedActorActivity(auditLog, hiddenActorKeys))
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

  private boolean isTenantAdminVisibleUser(UserAccount user) {
    return !isTenantAdminProtectedUser(user);
  }

  private boolean isTenantAdminProtectedUser(UserAccount user) {
    if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
      return false;
    }
    return user.getRoles().stream()
        .filter(Objects::nonNull)
        .map(Role::getName)
        .map(this::normalizeRoleNameForComparison)
        .anyMatch(TENANT_ADMIN_HIDDEN_ROLES::contains);
  }

  private boolean isProtectedActorActivity(AuditLog auditLog, Set<String> hiddenActorKeys) {
    if (auditLog == null || hiddenActorKeys == null || hiddenActorKeys.isEmpty()) {
      return false;
    }
    return hiddenActorKeys.contains(normalizeActorKey(auditLog.getUsername()));
  }

  private String normalizeRoleNameForComparison(String roleName) {
    if (!StringUtils.hasText(roleName)) {
      return null;
    }
    String normalized = roleName.trim().toUpperCase(Locale.ROOT);
    if (normalized.startsWith("ROLE_")) {
      return normalized;
    }
    return "ROLE_" + normalized;
  }

  private String normalizeActorKey(String actor) {
    if (!StringUtils.hasText(actor)) {
      return null;
    }
    return actor.trim().toLowerCase(Locale.ROOT);
  }
}
