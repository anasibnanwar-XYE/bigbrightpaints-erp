package com.bigbrightpaints.erp.modules.company.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.admin.dto.CreateUserRequest;
import com.bigbrightpaints.erp.modules.admin.service.AdminUserService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupAccountingRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupCompanyDetailsRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupGstRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupInviteTeamRequest;
import com.bigbrightpaints.erp.modules.company.dto.OwnerSetupStatusResponse;

@Service
public class OwnerSetupService {
  private static final List<String> TENANT_INVITE_ROLE_OPTIONS =
      List.of("ROLE_ACCOUNTING", "ROLE_FACTORY", "ROLE_SALES", "ROLE_DEALER");
  private static final Set<String> TENANT_INVITE_ROLE_SET = Set.copyOf(TENANT_INVITE_ROLE_OPTIONS);

  private final CompanyRepository companyRepository;
  private final AdminUserService adminUserService;
  private final AuditService auditService;

  public OwnerSetupService(
      CompanyRepository companyRepository,
      AdminUserService adminUserService,
      AuditService auditService) {
    this.companyRepository = companyRepository;
    this.adminUserService = adminUserService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public OwnerSetupStatusResponse getStatus() {
    Company company = requireCurrentTenantSetupActor(false);
    return toStatus(company, null);
  }

  @Transactional
  public OwnerSetupStatusResponse completeCompanyDetails(OwnerSetupCompanyDetailsRequest request) {
    Company company = requireCurrentTenantSetupActor(true);
    requireSetupPendingOrFinished(company);
    if (company.getOnboardingCompletedAt() != null) {
      return toStatus(company, null);
    }
    company.setName(normalizeRequiredText(request.name(), "name"));
    company.setTimezone(normalizeRequiredText(request.timezone(), "timezone"));
    company.setStateCode(normalizeOptionalUpper(request.stateCode()));
    Instant now = CompanyTime.now(company);
    if (company.getSetupCompanyDetailsCompletedAt() == null) {
      company.setSetupCompanyDetailsCompletedAt(now);
    }
    companyRepository.saveAndFlush(company);
    Long auditEventId = logSetupAudit(company, "owner-setup-company-details", "company-details");
    return toStatus(company, auditEventId);
  }

  @Transactional
  public OwnerSetupStatusResponse completeGst(OwnerSetupGstRequest request) {
    Company company = requireCurrentTenantSetupActor(true);
    requireSetupPendingOrFinished(company);
    if (company.getOnboardingCompletedAt() != null) {
      return toStatus(company, null);
    }
    requireStepComplete(company.getSetupCompanyDetailsCompletedAt(), "company-details");
    boolean enabled = Boolean.TRUE.equals(request.enabled());
    if (enabled) {
      BigDecimal requestedRate =
          request.defaultGstRate() == null ? BigDecimal.valueOf(18) : request.defaultGstRate();
      if (requestedRate.compareTo(BigDecimal.ZERO) < 0
          || requestedRate.compareTo(BigDecimal.valueOf(100)) > 0) {
        throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
            "defaultGstRate must be between 0 and 100");
      }
      company.setDefaultGstRate(requestedRate);
    } else {
      company.setDefaultGstRate(BigDecimal.ZERO);
    }
    if (StringUtils.hasText(request.stateCode())) {
      company.setStateCode(normalizeOptionalUpper(request.stateCode()));
    }
    Instant now = CompanyTime.now(company);
    if (company.getSetupGstCompletedAt() == null) {
      company.setSetupGstCompletedAt(now);
    }
    companyRepository.saveAndFlush(company);
    Long auditEventId = logSetupAudit(company, "owner-setup-gst", "gst");
    return toStatus(company, auditEventId);
  }

  @Transactional
  public OwnerSetupStatusResponse completeAccounting(OwnerSetupAccountingRequest request) {
    Company company = requireCurrentTenantSetupActor(true);
    requireSetupPendingOrFinished(company);
    if (company.getOnboardingCompletedAt() != null) {
      return toStatus(company, null);
    }
    if (request == null || !Boolean.TRUE.equals(request.confirmDefaults())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "confirmDefaults must be true");
    }
    requireStepComplete(company.getSetupCompanyDetailsCompletedAt(), "company-details");
    if (isGstEnabled(company)) {
      requireStepComplete(company.getSetupGstCompletedAt(), "gst");
    }
    Instant now = CompanyTime.now(company);
    if (company.getSetupAccountingCompletedAt() == null) {
      company.setSetupAccountingCompletedAt(now);
    }
    companyRepository.saveAndFlush(company);
    Long auditEventId = logSetupAudit(company, "owner-setup-accounting", "accounting");
    return toStatus(company, auditEventId);
  }

  @Transactional
  public OwnerSetupStatusResponse inviteTeam(OwnerSetupInviteTeamRequest request) {
    Company company = requireCurrentTenantSetupActor(true);
    requireSetupPendingOrFinished(company);
    if (company.getOnboardingCompletedAt() != null) {
      return toStatus(company, null);
    }
    requireStepComplete(company.getSetupCompanyDetailsCompletedAt(), "company-details");
    if (isGstEnabled(company)) {
      requireStepComplete(company.getSetupGstCompletedAt(), "gst");
    }
    requireStepComplete(company.getSetupAccountingCompletedAt(), "accounting");
    boolean skip = request != null && Boolean.TRUE.equals(request.skip());
    List<OwnerSetupInviteTeamRequest.Invitation> invitations =
        request == null || request.invitations() == null ? List.of() : request.invitations();
    if (!skip && invitations.isEmpty()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Provide at least one invitation or set skip=true");
    }
    for (OwnerSetupInviteTeamRequest.Invitation invitation : invitations) {
      adminUserService.createUser(
          new CreateUserRequest(
              invitation.email(),
              invitation.displayName(),
              List.of(normalizeInviteRole(invitation.role()))));
    }
    Instant now = CompanyTime.now(company);
    if (company.getSetupInviteTeamCompletedAt() == null) {
      company.setSetupInviteTeamCompletedAt(now);
    }
    companyRepository.saveAndFlush(company);
    Long auditEventId =
        logSetupAudit(
            company,
            skip ? "owner-setup-invite-team-skipped" : "owner-setup-invite-team",
            "invite-team");
    return toStatus(company, auditEventId);
  }

  @Transactional
  public OwnerSetupStatusResponse finish() {
    Company company = requireCurrentTenantSetupActor(true);
    if (company.getOnboardingCompletedAt() == null) {
      requireSetupPendingOrFinished(company);
      requireStepComplete(company.getSetupCompanyDetailsCompletedAt(), "company-details");
      if (isGstEnabled(company)) {
        requireStepComplete(company.getSetupGstCompletedAt(), "gst");
      }
      requireStepComplete(company.getSetupAccountingCompletedAt(), "accounting");
      requireStepComplete(company.getSetupInviteTeamCompletedAt(), "invite-team");
      Instant now = CompanyTime.now(company);
      company.setOnboardingCompletedAt(now);
      company.setSetupFinishedAt(now);
      company.setLifecycleState(CompanyLifecycleState.ACTIVE);
      company.setLifecycleReason(resolveFinishedLifecycleReason(company));
      companyRepository.saveAndFlush(company);
      Long auditEventId = logSetupAudit(company, "owner-setup-finished", "finish");
      return toStatus(company, auditEventId);
    }
    if (company.getSetupFinishedAt() == null) {
      company.setSetupFinishedAt(company.getOnboardingCompletedAt());
      companyRepository.saveAndFlush(company);
    }
    return toStatus(company, null);
  }

  private Company requireCurrentTenantSetupActor(boolean lock) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Setup requires an authenticated tenant owner/admin");
    }
    if (hasAuthority(authentication, "ROLE_SUPER_ADMIN")
        || !hasAuthority(authentication, "ROLE_ADMIN")) {
      throw new AccessDeniedException("Setup requires a tenant owner/admin actor");
    }
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof UserPrincipal userPrincipal)) {
      throw new AccessDeniedException("Setup requires a tenant owner/admin actor");
    }
    UserAccount user = userPrincipal.getUser();
    if (user == null || user.getCompany() == null || user.getCompany().getId() == null) {
      throw new AccessDeniedException("Setup requires a tenant owner/admin actor");
    }
    String contextCompanyCode = CompanyContextHolder.getCompanyCode();
    if (StringUtils.hasText(contextCompanyCode)
        && !contextCompanyCode.equalsIgnoreCase(user.getCompany().getCode())) {
      throw new AccessDeniedException("Setup company context mismatch");
    }
    return (lock
            ? companyRepository.lockById(user.getCompany().getId())
            : companyRepository.findById(user.getCompany().getId()))
        .orElseThrow();
  }

  private boolean hasAuthority(Authentication authentication, String authority) {
    return authentication.getAuthorities().stream()
        .anyMatch(granted -> authority.equalsIgnoreCase(granted.getAuthority()));
  }

  private void requireSetupPendingOrFinished(Company company) {
    if (company.getOnboardingCompletedAt() != null) {
      return;
    }
    if (!"USED".equals(normalizeStatus(company.getActivationStatus()))) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Owner activation must be completed before setup");
    }
  }

  private void requireStepComplete(Instant completedAt, String step) {
    if (completedAt == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Complete " + step + " before continuing setup");
    }
  }

  private String normalizeInviteRole(String role) {
    String normalized =
        StringUtils.hasText(role) ? role.trim().toUpperCase(Locale.ROOT) : "ROLE_SALES";
    if (!normalized.startsWith("ROLE_")) {
      normalized = "ROLE_" + normalized;
    }
    if (!TENANT_INVITE_ROLE_SET.contains(normalized)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Invite role must be one of " + TENANT_INVITE_ROLE_OPTIONS);
    }
    return normalized;
  }

  private String normalizeRequiredText(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          field + " is required");
    }
    return value.trim();
  }

  private String normalizeOptionalUpper(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
  }

  private boolean isGstEnabled(Company company) {
    return company != null
        && company.getDefaultGstRate() != null
        && company.getDefaultGstRate().compareTo(BigDecimal.ZERO) > 0;
  }

  private String resolveFinishedLifecycleReason(Company company) {
    String planId = company.getCommercialPlanId();
    String billingStatus = company.getCommercialBillingStatus();
    if ("TRIAL".equalsIgnoreCase(planId)
        || "TRIAL".equalsIgnoreCase(billingStatus)
        || company.getCommercialTrialEndsAt() != null) {
      return "TRIAL_ACTIVE";
    }
    return null;
  }

  private OwnerSetupStatusResponse toStatus(Company company, Long auditEventId) {
    List<OwnerSetupStatusResponse.Step> steps = setupSteps(company);
    String nextStep =
        steps.stream()
            .filter(step -> step.required() && !step.completed())
            .map(OwnerSetupStatusResponse.Step::key)
            .findFirst()
            .orElse(null);
    boolean setupRequired = company.getOnboardingCompletedAt() == null;
    return new OwnerSetupStatusResponse(
        company.getId(),
        company.getCode(),
        setupRequired ? "SETUP_PENDING" : resolveCompletedTenantStatus(company),
        setupRequired,
        steps,
        nextStep,
        TENANT_INVITE_ROLE_OPTIONS,
        company.getOnboardingCompletedAt(),
        auditEventId);
  }

  private List<OwnerSetupStatusResponse.Step> setupSteps(Company company) {
    List<OwnerSetupStatusResponse.Step> steps = new ArrayList<>();
    steps.add(
        step(
            "company-details",
            "Company details",
            true,
            company.getSetupCompanyDetailsCompletedAt()));
    if (isGstEnabled(company) || company.getSetupGstCompletedAt() != null) {
      steps.add(step("gst", "GST setup", true, company.getSetupGstCompletedAt()));
    }
    steps.add(
        step("accounting", "Accounting setup", true, company.getSetupAccountingCompletedAt()));
    steps.add(step("invite-team", "Invite team", true, company.getSetupInviteTeamCompletedAt()));
    steps.add(step("finish", "Finish setup", true, company.getSetupFinishedAt()));
    if (company.getOnboardingCompletedAt() != null) {
      steps =
          steps.stream()
              .map(
                  step ->
                      step.completed()
                          ? step
                          : new OwnerSetupStatusResponse.Step(
                              step.key(),
                              step.label(),
                              step.required(),
                              true,
                              company.getOnboardingCompletedAt()))
              .toList();
    }
    return List.copyOf(steps);
  }

  private OwnerSetupStatusResponse.Step step(
      String key, String label, boolean required, Instant completedAt) {
    return new OwnerSetupStatusResponse.Step(
        key, label, required, completedAt != null, completedAt);
  }

  private String resolveCompletedTenantStatus(Company company) {
    String lifecycleReason = normalizeStatus(company.getLifecycleReason());
    if ("TRIAL_ACTIVE".equals(lifecycleReason)) {
      return "TRIAL_ACTIVE";
    }
    return "ACTIVE";
  }

  private String normalizeStatus(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
  }

  private Long logSetupAudit(Company company, String reason, String step) {
    Map<String, String> metadata = new LinkedHashMap<>();
    metadata.put("reason", reason);
    metadata.put("setupStep", step);
    metadata.put("targetCompanyId", String.valueOf(company.getId()));
    metadata.put("targetCompanyCode", company.getCode());
    metadata.put("actor", currentActor());
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), company.getCode(), metadata);
    return auditLog == null ? null : auditLog.getId();
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "anonymous"
        : authentication.getName().trim();
  }
}
