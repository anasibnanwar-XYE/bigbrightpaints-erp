package com.bigbrightpaints.erp.modules.company.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.core.security.TokenBlacklistService;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.auth.domain.PasswordResetTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.auth.service.IamCanonicalStorageService;
import com.bigbrightpaints.erp.modules.auth.service.RefreshTokenService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantActivationToken;
import com.bigbrightpaints.erp.modules.company.domain.TenantActivationTokenRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantAdminEmailChangeRequest;
import com.bigbrightpaints.erp.modules.company.domain.TenantAdminEmailChangeRequestRepository;
import com.bigbrightpaints.erp.modules.company.domain.TenantSupportWarning;
import com.bigbrightpaints.erp.modules.company.domain.TenantSupportWarningRepository;
import com.bigbrightpaints.erp.modules.company.dto.*;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SuperAdminTenantControlPlaneService {

  private static final SecureRandom ACTIVATION_RANDOM = new SecureRandom();
  private static final String ACTIVATION_TOKEN_SCOPE = "tenant-activation:v1";
  private static final ConcurrentMap<String, ReentrantLock> ADD_CLIENT_CREATE_LOCKS =
      new ConcurrentHashMap<>();

  private static final Set<String> ADD_CLIENT_PLAN_IDS =
      Set.of("TRIAL", "STARTER", "GROWTH", "ENTERPRISE", "CUSTOM");
  private static final Set<String> ADD_CLIENT_BILLING_STATUSES =
      Set.of("TRIAL", "MANUAL", "PAID", "DUE");
  private static final Set<String> ADD_CLIENT_SUPPORT_TIERS =
      Set.of("STANDARD", "PRIORITY", "DEDICATED");
  private static final Set<String> ADD_CLIENT_MODULES =
      Set.of(
          "ACCOUNTING",
          "SALES",
          "INVENTORY",
          "PURCHASING",
          "PRODUCTION",
          "HR",
          "REPORTS",
          "PORTAL");
  private static final Set<String> ADD_CLIENT_COA_TEMPLATES =
      Set.of("SME", "DISTRIBUTION", "MANUFACTURING");

  private static final Set<String> CANONICAL_TENANT_STATUSES =
      Set.of(
          "DRAFT",
          "PENDING_ACTIVATION",
          "SETUP_PENDING",
          "TRIAL_ACTIVE",
          "ACTIVE",
          "GRACE",
          "SUSPENDED_READ_ONLY",
          "SUSPENDED_BLOCKED",
          "CANCELED",
          "ARCHIVED",
          "SEED_FAILED");

  private static final Map<String, String> LEGACY_STATUS_ALIASES =
      Map.of(
          "SUSPENDED", "SUSPENDED_BLOCKED",
          "DEACTIVATED", "ARCHIVED");

  private final CompanyRepository companyRepository;
  private final UserAccountRepository userAccountRepository;
  private final AuditLogRepository auditLogRepository;
  private final AuditService auditService;
  private final EmailService emailService;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final TenantSupportWarningRepository tenantSupportWarningRepository;
  private final TenantAdminEmailChangeRequestRepository tenantAdminEmailChangeRequestRepository;
  private final TenantActivationTokenRepository tenantActivationTokenRepository;
  private final TenantRuntimeEnforcementService tenantRuntimeEnforcementService;
  private final TenantReviewIntelligenceToggleService tenantReviewIntelligenceToggleService;
  private final CompanyService companyService;
  private final IamCanonicalStorageService iamCanonicalStorageService;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public SuperAdminTenantControlPlaneService(
      CompanyRepository companyRepository,
      UserAccountRepository userAccountRepository,
      AuditLogRepository auditLogRepository,
      AuditService auditService,
      EmailService emailService,
      TokenBlacklistService tokenBlacklistService,
      RefreshTokenService refreshTokenService,
      TenantSupportWarningRepository tenantSupportWarningRepository,
      TenantAdminEmailChangeRequestRepository tenantAdminEmailChangeRequestRepository,
      TenantActivationTokenRepository tenantActivationTokenRepository,
      TenantRuntimeEnforcementService tenantRuntimeEnforcementService,
      TenantReviewIntelligenceToggleService tenantReviewIntelligenceToggleService,
      CompanyService companyService,
      IamCanonicalStorageService iamCanonicalStorageService,
      PasswordResetTokenRepository passwordResetTokenRepository,
      RoleRepository roleRepository,
      PasswordEncoder passwordEncoder) {
    this.companyRepository = companyRepository;
    this.userAccountRepository = userAccountRepository;
    this.auditLogRepository = auditLogRepository;
    this.auditService = auditService;
    this.emailService = emailService;
    this.tokenBlacklistService = tokenBlacklistService;
    this.refreshTokenService = refreshTokenService;
    this.tenantSupportWarningRepository = tenantSupportWarningRepository;
    this.tenantAdminEmailChangeRequestRepository = tenantAdminEmailChangeRequestRepository;
    this.tenantActivationTokenRepository = tenantActivationTokenRepository;
    this.tenantRuntimeEnforcementService = tenantRuntimeEnforcementService;
    this.tenantReviewIntelligenceToggleService = tenantReviewIntelligenceToggleService;
    this.companyService = companyService;
    this.iamCanonicalStorageService = iamCanonicalStorageService;
    this.passwordResetTokenRepository = passwordResetTokenRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<SuperAdminTenantSummaryDto> listTenants(String statusFilter) {
    return listTenants(statusFilter, null, 0, 100, "companyCode,asc").content();
  }

  @Transactional(readOnly = true)
  public PageResponse<SuperAdminTenantSummaryDto> listTenants(
      String statusFilter, String query, int page, int size, String sort) {
    int safePage = validatePage(page);
    int safeSize = validateSize(size);
    String normalizedStatus = normalizeStatusFilter(statusFilter);
    String normalizedQuery = normalizeSearchQuery(query);
    SortSpec sortSpec = parseSort(sort);
    List<TenantListCandidate> candidates =
        companyRepository.findAll().stream()
            .map(this::toTenantListCandidate)
            .filter(candidate -> statusMatches(candidate, normalizedStatus))
            .filter(candidate -> searchMatches(candidate, normalizedQuery))
            .sorted(sortSpec.comparator())
            .toList();
    long requestedOffset = (long) safePage * safeSize;
    int start = requestedOffset >= candidates.size() ? candidates.size() : (int) requestedOffset;
    int end = (int) Math.min(requestedOffset + safeSize, (long) candidates.size());
    List<SuperAdminTenantSummaryDto> content =
        candidates.subList(start, end).stream()
            .map(
                candidate ->
                    toSummary(
                        candidate.company(),
                        candidate.metrics(),
                        candidate.mainAdmin(),
                        candidate.lastActivityAt(),
                        candidate.status()))
            .toList();
    return PageResponse.of(content, candidates.size(), safePage, safeSize);
  }

  @Transactional(readOnly = true)
  public SuperAdminTenantDetailDto getTenantDetail(Long companyId) {
    return toDetail(requireCompany(companyId));
  }

  public SuperAdminAddClientOptionsDto getAddClientOptions() {
    return new SuperAdminAddClientOptionsDto(
        section(
            "company",
            "Company",
            field(
                "name",
                "text",
                true,
                null,
                List.of(),
                List.of(),
                "Required; 160 characters or fewer"),
            field(
                "code",
                "code",
                true,
                null,
                List.of(),
                List.of(),
                "Required; uppercase letters, numbers, underscore, or hyphen"),
            field("timezone", "timezone", true, "Asia/Kolkata", List.of(), List.of(), "IANA zone"),
            field("stateCode", "text", false, null, List.of(), List.of(), "Two-letter GST state"),
            field("baseCurrency", "enum", true, "INR", List.of("INR"), List.of(), "ISO currency"),
            field("defaultGstRate", "decimal", false, "18.00", List.of(), List.of(), "0 to 100"),
            field(
                "coaTemplateCode",
                "enum",
                false,
                "SME",
                List.of("SME", "DISTRIBUTION", "MANUFACTURING"),
                List.of(),
                "Default accounting template")),
        section(
            "owner",
            "Owner",
            field("email", "email", true, null, List.of(), List.of(), "Valid owner email"),
            field("displayName", "text", true, null, List.of(), List.of(), "Owner full name"),
            field("phone", "phone", false, null, List.of(), List.of(), "Optional phone marker")),
        section(
            "commercial",
            "Commercial",
            field(
                "planId",
                "enum",
                true,
                "TRIAL",
                List.of("TRIAL", "STARTER", "GROWTH", "ENTERPRISE", "CUSTOM"),
                List.of(),
                "Plan template id"),
            field(
                "billingStatus",
                "enum",
                true,
                "MANUAL",
                List.of("TRIAL", "MANUAL", "PAID", "DUE"),
                List.of("planId"),
                "Initial billing state"),
            field("trialDays", "number", false, 14, List.of(), List.of("planId"), "0 or greater"),
            field(
                "supportTier",
                "enum",
                true,
                "STANDARD",
                List.of("STANDARD", "PRIORITY", "DEDICATED"),
                List.of("planId"),
                "Support tier")),
        section(
            "quotas",
            "Quotas",
            field("maxActiveUsers", "number", false, 10, List.of(), List.of(), "0 means unlimited"),
            field(
                "maxApiRequests",
                "number",
                false,
                10000,
                List.of(),
                List.of(),
                "Monthly API quota; 0 means unlimited"),
            field(
                "maxStorageBytes",
                "number",
                false,
                1073741824L,
                List.of(),
                List.of(),
                "Storage quota; 0 means unlimited"),
            field(
                "maxConcurrentRequests",
                "number",
                false,
                8,
                List.of(),
                List.of(),
                "Concurrent request cap; 0 means unlimited"),
            field("softLimitEnabled", "boolean", false, false, List.of(), List.of(), "Warn first"),
            field("hardLimitEnabled", "boolean", false, true, List.of(), List.of(), "Fail closed")),
        section(
            "modules",
            "Modules",
            field(
                "enabled",
                "multi-select",
                true,
                List.of("ACCOUNTING", "SALES", "INVENTORY"),
                List.of(
                    "ACCOUNTING",
                    "SALES",
                    "INVENTORY",
                    "PURCHASING",
                    "PRODUCTION",
                    "HR",
                    "REPORTS",
                    "PORTAL"),
                List.of(),
                "Select enabled V1 modules")),
        section(
            "support",
            "Support",
            field("notes", "textarea", false, null, List.of(), List.of(), "Platform-only note"),
            field("tags", "tags", false, List.of(), List.of(), List.of(), "Uppercase safe tags")),
        List.of(
            new SuperAdminAddClientOptionsDto.CreateModeOption(
                "DRAFT",
                "Create as Draft",
                "Create the client without sending activation email",
                new SuperAdminAddClientOptionsDto.ActivationEffect("DRAFT", "NOT_SENT", false)),
            new SuperAdminAddClientOptionsDto.CreateModeOption(
                "SEND_ACTIVATION",
                "Create and Send Activation Email",
                "Create the client, issue a digest-only activation token, and send one email",
                new SuperAdminAddClientOptionsDto.ActivationEffect(
                    "PENDING_ACTIVATION", "SENT", true))),
        draftSeedPolicy());
  }

  @Transactional
  public SuperAdminAddClientCreateResponse createAddClient(
      SuperAdminAddClientCreateRequest request) {
    if (request == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Add Client payload is required");
    }
    validateCreatePayloadShape(request);
    String companyCode = normalizeCreateCode(request.company().code());
    String ownerEmail = normalizeRequiredEmail(request.owner().email(), "owner.email");
    validateOwnerEmailFormat(ownerEmail);
    validateCreatePayloadSemantics(request);
    List<ReentrantLock> createLocks = acquireAddClientCreateLocks(companyCode, ownerEmail);
    boolean releaseLocksInFinally = true;
    try {
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        releaseLocksInFinally = false;
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                releaseAddClientCreateLocks(createLocks);
              }
            });
      }
      if (companyRepository.findByCodeIgnoreCase(companyCode).isPresent()) {
        throw duplicateConflict("Company code already exists", "company.code");
      }
      if (userAccountRepository.existsByEmailIgnoreCase(ownerEmail)) {
        throw duplicateConflict("Owner email already exists", "owner.email");
      }

      Company company = new Company();
      company.setName(request.company().name().trim());
      company.setCode(companyCode);
      company.setTimezone(request.company().timezone().trim());
      company.setStateCode(normalizeOptionalUpper(request.company().stateCode()));
      company.setBaseCurrency(request.company().baseCurrency().trim().toUpperCase(Locale.ROOT));
      company.setDefaultGstRate(request.company().defaultGstRate());
      company.setOnboardingCoaTemplateCode(
          normalizeOptionalUpper(request.company().coaTemplateCode()));
      company.setOnboardingAdminEmail(ownerEmail);
      company.setCommercialPlanId(request.commercial().planId().trim().toUpperCase(Locale.ROOT));
      company.setCommercialBillingStatus(
          request.commercial().billingStatus().trim().toUpperCase(Locale.ROOT));
      company.setCommercialSupportTier(
          request.commercial().supportTier().trim().toUpperCase(Locale.ROOT));
      company.setCommercialTrialEndsAt(resolveTrialEndsAt(request.commercial().trialDays()));
      company.setQuotaMaxActiveUsers(request.quotas().maxActiveUsers());
      company.setQuotaMaxApiRequests(request.quotas().maxApiRequests());
      company.setQuotaMaxStorageBytes(request.quotas().maxStorageBytes());
      company.setQuotaMaxConcurrentRequests(request.quotas().maxConcurrentRequests());
      company.setQuotaSoftLimitEnabled(request.quotas().softLimitEnabled());
      company.setQuotaHardLimitEnabled(request.quotas().hardLimitEnabled());
      company.setEnabledModules(normalizeModules(request.modules().enabled()));
      company.setSupportNotes(request.support().notes());
      company.setSupportTags(request.support().tags());
      company.setActivationStatus("NOT_SENT");
      Company savedCompany = companyRepository.saveAndFlush(company);

      UserAccount owner =
          createPendingOwner(savedCompany, ownerEmail, request.owner().displayName());
      savedCompany.setMainAdminUserId(owner.getId());
      savedCompany.setOnboardingAdminUserId(owner.getId());

      ActivationIssue activationIssue = null;
      if (request.createMode() == SuperAdminAddClientCreateRequest.CreateMode.SEND_ACTIVATION) {
        activationIssue = issueActivation(savedCompany, owner);
        savedCompany.setOnboardingCredentialsEmailedAt(activationIssue.sentAt());
        savedCompany.setActivationStatus("SENT");
        savedCompany.setActivationSentAt(activationIssue.sentAt());
        savedCompany.setActivationExpiresAt(activationIssue.expiresAt());
      }
      companyRepository.saveAndFlush(savedCompany);

      Long auditEventId =
          logAuditRequired(
              savedCompany,
              request.createMode() == SuperAdminAddClientCreateRequest.CreateMode.DRAFT
                  ? "tenant-created-draft"
                  : "tenant-created-pending-activation",
              Map.of(
                  "createMode",
                  request.createMode().name(),
                  "activationStatus",
                  savedCompany.getActivationStatus(),
                  "ownerUserId",
                  String.valueOf(owner.getId()),
                  "planId",
                  savedCompany.getCommercialPlanId()));

      return addClientResponse(savedCompany, owner, activationIssue, auditEventId);
    } catch (DataIntegrityViolationException ex) {
      throw duplicateConflict("Duplicate Add Client tenant code or owner email", "createRequest");
    } finally {
      if (releaseLocksInFinally) {
        releaseAddClientCreateLocks(createLocks);
      }
    }
  }

  @Transactional
  public CompanyLifecycleStateDto updateLifecycleState(
      Long companyId, CompanyLifecycleStateRequest request) {
    return companyService.updateLifecycleState(companyId, request);
  }

  @Transactional
  public CompanyEnabledModulesDto updateModules(Long companyId, Set<String> enabledModules) {
    return companyService.updateEnabledModules(companyId, enabledModules);
  }

  @Transactional
  public CompanyAdminCredentialResetDto resetTenantAdminPassword(
      Long companyId, String adminEmail, String reason) {
    return companyService.resetTenantAdminPassword(companyId, adminEmail, reason);
  }

  @Transactional
  public CompanySupportWarningDto issueSupportWarning(
      Long companyId,
      String warningCategory,
      String message,
      String requestedLifecycleState,
      Integer gracePeriodHours) {
    Company company = requireCompany(companyId);
    String actor = currentActor();
    String normalizedLifecycleState = normalizeRequestedLifecycleState(requestedLifecycleState);
    int resolvedGracePeriodHours = resolveGracePeriodHours(gracePeriodHours);
    if (!StringUtils.hasText(message)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Support warning message is required");
    }
    TenantSupportWarning warning = new TenantSupportWarning();
    warning.setCompany(company);
    warning.setWarningCategory(normalizeWarningCategory(warningCategory));
    warning.setMessage(message.trim());
    warning.setRequestedLifecycleState(normalizedLifecycleState);
    warning.setGracePeriodHours(resolvedGracePeriodHours);
    warning.setIssuedBy(actor);
    warning.setIssuedAt(CompanyTime.now(company));
    TenantSupportWarning saved = tenantSupportWarningRepository.save(warning);
    logAuditSuccess(
        company,
        "tenant-support-warning-issued",
        Map.of(
            "warningId", String.valueOf(saved.getId()),
            "warningCategory", saved.getWarningCategory(),
            "requestedLifecycleState", saved.getRequestedLifecycleState(),
            "gracePeriodHours", String.valueOf(saved.getGracePeriodHours())));
    return new CompanySupportWarningDto(
        company.getId(),
        company.getCode(),
        String.valueOf(saved.getId()),
        saved.getWarningCategory(),
        saved.getMessage(),
        saved.getRequestedLifecycleState(),
        saved.getGracePeriodHours(),
        saved.getIssuedBy(),
        saved.getIssuedAt());
  }

  @Transactional
  public SuperAdminTenantLimitsDto updateLimits(
      Long companyId,
      Long quotaMaxActiveUsers,
      Long quotaMaxApiRequests,
      Long quotaMaxStorageBytes,
      Long quotaMaxConcurrentRequests,
      Boolean quotaSoftLimitEnabled,
      Boolean quotaHardLimitEnabled) {
    Company company = requireCompany(companyId);
    if (quotaMaxActiveUsers == null
        && quotaMaxApiRequests == null
        && quotaMaxStorageBytes == null
        && quotaMaxConcurrentRequests == null
        && quotaSoftLimitEnabled == null
        && quotaHardLimitEnabled == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Tenant limits payload is required");
    }
    if (quotaMaxActiveUsers != null) {
      company.setQuotaMaxActiveUsers(quotaMaxActiveUsers);
    }
    if (quotaMaxApiRequests != null) {
      company.setQuotaMaxApiRequests(quotaMaxApiRequests);
    }
    if (quotaMaxStorageBytes != null) {
      company.setQuotaMaxStorageBytes(quotaMaxStorageBytes);
    }
    if (quotaMaxConcurrentRequests != null) {
      company.setQuotaMaxConcurrentRequests(quotaMaxConcurrentRequests);
    }
    if (quotaSoftLimitEnabled != null) {
      company.setQuotaSoftLimitEnabled(quotaSoftLimitEnabled);
    }
    if (quotaHardLimitEnabled != null) {
      company.setQuotaHardLimitEnabled(quotaHardLimitEnabled);
    }
    companyRepository.save(company);
    tenantRuntimeEnforcementService.updatePolicy(
        company.getCode(),
        null,
        "ERP37_LIMITS_UPDATE",
        safeInteger(company.getQuotaMaxConcurrentRequests()),
        safeInteger(company.getQuotaMaxApiRequests()),
        safeInteger(company.getQuotaMaxActiveUsers()),
        currentActor());
    logAuditSuccess(
        company,
        "tenant-limits-updated",
        Map.of(
            "quotaMaxActiveUsers", String.valueOf(company.getQuotaMaxActiveUsers()),
            "quotaMaxApiRequests", String.valueOf(company.getQuotaMaxApiRequests()),
            "quotaMaxStorageBytes", String.valueOf(company.getQuotaMaxStorageBytes()),
            "quotaMaxConcurrentRequests", String.valueOf(company.getQuotaMaxConcurrentRequests())));
    return new SuperAdminTenantLimitsDto(
        company.getId(),
        company.getCode(),
        company.getQuotaMaxActiveUsers(),
        company.getQuotaMaxApiRequests(),
        company.getQuotaMaxStorageBytes(),
        company.getQuotaMaxConcurrentRequests(),
        company.isQuotaSoftLimitEnabled(),
        company.isQuotaHardLimitEnabled());
  }

  @Transactional
  public SuperAdminTenantSupportContextDto updateSupportContext(
      Long companyId, String supportNotes, Set<String> supportTags) {
    Company company = requireCompany(companyId);
    if (supportNotes != null) {
      company.setSupportNotes(supportNotes);
    }
    if (supportTags != null) {
      company.setSupportTags(supportTags);
    }
    companyRepository.save(company);
    logAuditSuccess(company, "tenant-support-context-updated", Map.of());
    return new SuperAdminTenantSupportContextDto(
        company.getId(), company.getCode(), company.getSupportNotes(), company.getSupportTags());
  }

  @Transactional(readOnly = true)
  public SuperAdminTenantReviewIntelligenceToggleDto getReviewIntelligenceToggle(Long companyId) {
    Company company = requireCompany(companyId);
    TenantReviewIntelligenceToggleService.ToggleSnapshot snapshot =
        tenantReviewIntelligenceToggleService.snapshot(company.getId());
    return new SuperAdminTenantReviewIntelligenceToggleDto(
        company.getId(), company.getCode(), snapshot.enabled(), snapshot.updatedAt());
  }

  @Transactional
  public SuperAdminTenantReviewIntelligenceToggleDto updateReviewIntelligenceToggle(
      Long companyId, boolean enabled) {
    Company company = requireCompany(companyId);
    TenantReviewIntelligenceToggleService.ToggleSnapshot snapshot =
        tenantReviewIntelligenceToggleService.update(company.getId(), enabled);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("reviewIntelligenceEnabled", Boolean.toString(snapshot.enabled()));
    if (snapshot.updatedAt() != null) {
      metadata.put("reviewIntelligenceUpdatedAt", snapshot.updatedAt().toString());
    }
    logAuditSuccess(company, "tenant-review-intelligence-toggle-updated", metadata);
    return new SuperAdminTenantReviewIntelligenceToggleDto(
        company.getId(), company.getCode(), snapshot.enabled(), snapshot.updatedAt());
  }

  @Transactional
  public SuperAdminTenantForceLogoutDto forceLogoutAllUsers(Long companyId, String reason) {
    Company company = requireCompany(companyId);
    String actor = currentActor();
    List<UserAccount> users = userAccountRepository.findByCompany_Id(companyId);
    assertTenantExclusiveUsers(company, users, "tenant force logout");
    String normalizedReason = normalizeOptionalReason(reason, "support-request");
    for (UserAccount user : users) {
      if (user == null || user.getPublicId() == null) {
        continue;
      }
      tokenBlacklistService.revokeAllUserTokens(user.getPublicId().toString());
      refreshTokenService.revokeAllForUser(user.getPublicId());
    }
    Instant occurredAt = CompanyTime.now(company);
    logAuditSuccess(
        company,
        "tenant-force-logout",
        Map.of(
            "revokedUserCount",
            String.valueOf(users.size()),
            "forceLogoutReason",
            normalizedReason));
    return new SuperAdminTenantForceLogoutDto(
        company.getId(), company.getCode(), users.size(), normalizedReason, actor, occurredAt);
  }

  @Transactional
  public MainAdminSummaryDto replaceMainAdmin(Long companyId, Long adminUserId) {
    Company company = requireCompany(companyId);
    UserAccount targetUser = requireAssignedAdmin(company, adminUserId);
    company.setMainAdminUserId(targetUser.getId());
    companyRepository.save(company);
    logAuditSuccess(
        company,
        "tenant-main-admin-replaced",
        Map.of(
            "mainAdminUserId", String.valueOf(targetUser.getId()),
            "mainAdminEmail", targetUser.getEmail()));
    return toMainAdminSummary(company, targetUser);
  }

  @Transactional
  public SuperAdminTenantAdminEmailChangeRequestDto requestAdminEmailChange(
      Long companyId, Long adminUserId, String requestedEmail) {
    Company company = requireCompany(companyId);
    UserAccount adminUser = requireAssignedAdmin(company, adminUserId);
    assertTenantExclusiveUser(company, adminUser, "tenant admin email change");
    String normalizedRequestedEmail = normalizeRequiredEmail(requestedEmail, "newEmail");
    if (normalizedRequestedEmail.equalsIgnoreCase(adminUser.getEmail())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "newEmail must differ from the current admin email");
    }
    if (userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            normalizedRequestedEmail, company.getCode())
        .isPresent()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Email already exists: " + normalizedRequestedEmail);
    }
    TenantAdminEmailChangeRequest changeRequest = new TenantAdminEmailChangeRequest();
    changeRequest.setCompanyId(company.getId());
    changeRequest.setAdminUserId(adminUser.getId());
    changeRequest.setRequestedBy(currentActor());
    changeRequest.setCurrentEmail(adminUser.getEmail());
    changeRequest.setRequestedEmail(normalizedRequestedEmail);
    changeRequest.setVerificationToken(UUID.randomUUID().toString());
    changeRequest.setVerificationSentAt(CompanyTime.now(company));
    changeRequest.setExpiresAt(changeRequest.getVerificationSentAt().plusSeconds(60L * 60L * 24L));
    TenantAdminEmailChangeRequest saved =
        tenantAdminEmailChangeRequestRepository.save(changeRequest);
    emailService.sendAdminEmailChangeVerificationRequired(
        normalizedRequestedEmail,
        adminUser.getDisplayName(),
        company.getCode(),
        saved.getVerificationToken(),
        saved.getExpiresAt());
    logAuditSuccess(
        company,
        "tenant-admin-email-change-requested",
        Map.of(
            "requestId", String.valueOf(saved.getId()),
            "adminUserId", String.valueOf(adminUser.getId()),
            "currentEmail", adminUser.getEmail(),
            "requestedEmail", normalizedRequestedEmail));
    return new SuperAdminTenantAdminEmailChangeRequestDto(
        saved.getId(),
        company.getId(),
        company.getCode(),
        adminUser.getId(),
        saved.getCurrentEmail(),
        saved.getRequestedEmail(),
        saved.getVerificationSentAt(),
        saved.getExpiresAt());
  }

  @Transactional
  public SuperAdminTenantAdminEmailChangeConfirmationDto confirmAdminEmailChange(
      Long companyId, Long adminUserId, Long requestId, String verificationToken) {
    Company company = requireCompany(companyId);
    UserAccount adminUser = requireAssignedAdmin(company, adminUserId);
    assertTenantExclusiveUser(company, adminUser, "tenant admin email change");
    TenantAdminEmailChangeRequest changeRequest =
        tenantAdminEmailChangeRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Email change request not found"));
    if (!companyId.equals(changeRequest.getCompanyId())
        || !adminUserId.equals(changeRequest.getAdminUserId())) {
      throw new AccessDeniedException("Email change request does not match the targeted admin");
    }
    if (changeRequest.isConsumed()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Email change request has already been consumed");
    }
    if (!StringUtils.hasText(verificationToken)
        || !verificationToken.trim().equals(changeRequest.getVerificationToken())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Invalid verification token");
    }
    if (changeRequest.getExpiresAt() != null
        && changeRequest.getExpiresAt().isBefore(CompanyTime.now(company))) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Email change verification token has expired");
    }
    if (!adminUser.getEmail().equalsIgnoreCase(changeRequest.getCurrentEmail())) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Email change request is stale because the admin email has already changed");
    }
    if (userAccountRepository
        .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(
            changeRequest.getRequestedEmail(), company.getCode())
        .filter(existingUser -> !existingUser.getId().equals(adminUser.getId()))
        .isPresent()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Email already exists: " + changeRequest.getRequestedEmail());
    }
    Instant now = CompanyTime.now(company);
    changeRequest.setVerifiedAt(now);
    changeRequest.setConfirmedAt(now);
    changeRequest.setConsumed(true);
    adminUser.setEmail(changeRequest.getRequestedEmail());
    UserAccount savedAdmin = userAccountRepository.save(adminUser);
    iamCanonicalStorageService.syncUser(savedAdmin);
    tenantAdminEmailChangeRequestRepository.save(changeRequest);
    tokenBlacklistService.revokeAllUserTokens(savedAdmin.getPublicId().toString());
    refreshTokenService.revokeAllForUser(savedAdmin.getPublicId());
    passwordResetTokenRepository.deleteByUser(savedAdmin);
    logAuditSuccess(
        company,
        "tenant-admin-email-change-confirmed",
        Map.of(
            "requestId", String.valueOf(changeRequest.getId()),
            "adminUserId", String.valueOf(savedAdmin.getId()),
            "updatedEmail", savedAdmin.getEmail()));
    return new SuperAdminTenantAdminEmailChangeConfirmationDto(
        changeRequest.getId(),
        company.getId(),
        company.getCode(),
        savedAdmin.getId(),
        savedAdmin.getEmail(),
        changeRequest.getVerifiedAt(),
        changeRequest.getConfirmedAt());
  }

  private TenantListCandidate toTenantListCandidate(Company company) {
    CompanyTenantMetricsDto metrics = buildMetrics(company);
    UserAccount mainAdmin = resolveMainAdmin(company);
    Instant lastActivityAt = resolveLastActivityAt(company.getId());
    return new TenantListCandidate(
        company, metrics, mainAdmin, lastActivityAt, resolveTenantStatus(company, metrics));
  }

  private SuperAdminTenantSummaryDto toSummary(Company company) {
    CompanyTenantMetricsDto metrics = buildMetrics(company);
    return toSummary(
        company,
        metrics,
        resolveMainAdmin(company),
        resolveLastActivityAt(company.getId()),
        resolveTenantStatus(company, metrics));
  }

  private SuperAdminTenantSummaryDto toSummary(
      Company company,
      CompanyTenantMetricsDto metrics,
      UserAccount mainAdmin,
      Instant lastActivityAt,
      String status) {
    SuperAdminTenantSummaryDto.UsageSummary usage =
        new SuperAdminTenantSummaryDto.UsageSummary(
            metrics.activeUserCount(),
            metrics.quotaMaxActiveUsers(),
            metrics.apiActivityCount(),
            metrics.quotaMaxApiRequests(),
            metrics.auditStorageBytes(),
            metrics.quotaMaxStorageBytes(),
            metrics.currentConcurrentRequests(),
            metrics.quotaMaxConcurrentRequests());
    SuperAdminTenantSummaryDto.HealthSummary health = healthSummary(status, metrics);
    return new SuperAdminTenantSummaryDto(
        company.getId(),
        company.getCode(),
        company.getName(),
        company.getTimezone(),
        status,
        resolvePlanId(company),
        resolveBillingStatus(company, status),
        usage,
        resolveTrialEndsAt(company, status),
        health,
        metrics.lifecycleState(),
        metrics.activeUserCount(),
        metrics.quotaMaxActiveUsers(),
        metrics.apiActivityCount(),
        metrics.quotaMaxApiRequests(),
        metrics.auditStorageBytes(),
        metrics.quotaMaxStorageBytes(),
        metrics.currentConcurrentRequests(),
        metrics.quotaMaxConcurrentRequests(),
        company.getEnabledModules(),
        toMainAdminSummary(company, mainAdmin),
        lastActivityAt);
  }

  private SuperAdminTenantDetailDto toDetail(Company company) {
    CompanyTenantMetricsDto metrics = buildMetrics(company);
    UserAccount mainAdmin = resolveMainAdmin(company);
    Instant lastActivityAt = resolveLastActivityAt(company.getId());
    String status = resolveTenantStatus(company, metrics);
    SuperAdminTenantSummaryDto.HealthSummary health = healthSummary(status, metrics);
    SuperAdminTenantDetailDto.Limits limits =
        new SuperAdminTenantDetailDto.Limits(
            metrics.quotaMaxActiveUsers(),
            metrics.quotaMaxApiRequests(),
            metrics.quotaMaxStorageBytes(),
            metrics.quotaMaxConcurrentRequests(),
            metrics.quotaSoftLimitEnabled(),
            metrics.quotaHardLimitEnabled());
    SuperAdminTenantDetailDto.Usage usage =
        new SuperAdminTenantDetailDto.Usage(
            metrics.activeUserCount(),
            metrics.apiActivityCount(),
            metrics.apiErrorCount(),
            metrics.apiErrorRateInBasisPoints(),
            metrics.auditStorageBytes(),
            metrics.currentConcurrentRequests(),
            lastActivityAt);
    List<SuperAdminTenantDetailDto.SupportTimelineEvent> supportTimeline =
        buildSupportTimeline(company);
    MainAdminSummaryDto mainAdminSummary = toMainAdminSummary(company, mainAdmin);
    return new SuperAdminTenantDetailDto(
        company.getId(),
        company.getCode(),
        company.getName(),
        company.getTimezone(),
        company.getStateCode(),
        resolveLifecycle(company),
        company.getEnabledModules(),
        new SuperAdminTenantDetailDto.Onboarding(
            company.getOnboardingCoaTemplateCode(),
            company.getOnboardingAdminEmail(),
            company.getOnboardingAdminUserId(),
            company.getOnboardingAdminUserId() != null,
            company.getOnboardingCompletedAt()),
        mainAdminSummary,
        limits,
        usage,
        new SuperAdminTenantDetailDto.SupportContext(
            company.getSupportNotes(), company.getSupportTags()),
        supportTimeline,
        new SuperAdminTenantDetailDto.AvailableActions(
            true, true, true, true, true, true, true, true),
        status,
        new SuperAdminTenantDetailDto.Overview(
            company.getId(),
            company.getCode(),
            company.getName(),
            company.getTimezone(),
            company.getStateCode(),
            status,
            resolveLifecycle(company),
            resolveBillingStatus(company, status),
            health,
            mainAdminSummary,
            lastActivityAt,
            tabStateForStatus(status, "AVAILABLE", "Overview summary is available")),
        new SuperAdminTenantDetailDto.PlanSummary(
            resolvePlanId(company),
            "Trial",
            "STANDARD",
            limits,
            tabStateForStatus(status, "AVAILABLE", "Plan limits summary is available")),
        new SuperAdminTenantDetailDto.BillingSummary(
            resolveBillingStatus(company, status),
            0,
            company.getBaseCurrency(),
            resolveTrialEndsAt(company, status),
            tabStateForStatus(status, "EMPTY", "No platform billing records yet")),
        new SuperAdminTenantDetailDto.SupportSummary(
            company.getSupportTags(),
            supportTimeline.size(),
            tabState("AVAILABLE", "Support summary is available for " + status)),
        new SuperAdminTenantDetailDto.BugsSummary(
            0, 0, tabState("EMPTY", "No bug reports yet for " + status)),
        new SuperAdminTenantDetailDto.AuditSummary(
            supportTimeline.size(),
            lastActivityAt,
            tabState("AVAILABLE", "Audit summary is available for " + status)),
        new SuperAdminTenantDetailDto.SettingsSummary(
            company.getTimezone(),
            company.getEnabledModules(),
            tabStateForStatus(status, "AVAILABLE", "Settings summary is available")));
  }

  private CompanyTenantMetricsDto buildMetrics(Company company) {
    return companyService.getTenantMetricsForSuperAdmin(company.getId());
  }

  private SuperAdminAddClientOptionsDto.Section section(
      String key, String title, SuperAdminAddClientOptionsDto.Field... fields) {
    return new SuperAdminAddClientOptionsDto.Section(key, title, List.of(fields));
  }

  private SuperAdminAddClientOptionsDto.Field field(
      String key,
      String type,
      boolean required,
      Object defaultValue,
      List<String> enumValues,
      List<String> dependencies,
      String validationHint) {
    return new SuperAdminAddClientOptionsDto.Field(
        key, type, required, defaultValue, enumValues, dependencies, validationHint);
  }

  private SuperAdminAddClientOptionsDto.SeedPolicy draftSeedPolicy() {
    return new SuperAdminAddClientOptionsDto.SeedPolicy(
        "M4_DRAFT_SEED_POLICY_V1",
        true,
        List.of(
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "company_record",
                "Company record",
                "COMPLETE",
                true,
                "Company identity and commercial metadata are captured at create time"),
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "owner_profile",
                "Owner profile",
                "COMPLETE",
                true,
                "Pending owner account metadata is captured without a usable password"),
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "commercial_policy",
                "Commercial policy",
                "COMPLETE",
                true,
                "Plan, billing status, support tier, modules, and quotas are captured"),
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "accounting_defaults",
                "Accounting defaults",
                "PENDING",
                false,
                "Default accounting setup is deferred to the owner setup/seeding milestone"),
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "gst_defaults",
                "GST defaults",
                "PENDING",
                false,
                "GST setup is deferred to the owner setup milestone"),
            new SuperAdminAddClientOptionsDto.SeedCategory(
                "role_templates",
                "Role templates",
                "PENDING",
                false,
                "Tenant role template seeding is deferred to the default seeding milestone")),
        "Activation may be sent only after company, owner, and commercial policy metadata are"
            + " COMPLETE; deferred setup categories remain pending until the owner setup"
            + " corridor.");
  }

  private void validateCreatePayloadShape(SuperAdminAddClientCreateRequest request) {
    if (request.company() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "company is required");
    }
    if (request.owner() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "owner is required");
    }
    if (request.commercial() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "commercial is required");
    }
    if (request.quotas() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "quotas is required");
    }
    if (request.modules() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "modules is required");
    }
    if (request.support() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "support is required");
    }
    if (request.createMode() == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "createMode is required");
    }
  }

  private void validateCreatePayloadSemantics(SuperAdminAddClientCreateRequest request) {
    requireText(request.company().name(), "company.name");
    requireText(request.company().timezone(), "company.timezone");
    requireAllowedUpper(request.company().baseCurrency(), "company.baseCurrency", Set.of("INR"));
    requireOptionalAllowedUpper(
        request.company().coaTemplateCode(), "company.coaTemplateCode", ADD_CLIENT_COA_TEMPLATES);
    requireText(request.owner().displayName(), "owner.displayName");
    requireAllowedUpper(request.commercial().planId(), "commercial.planId", ADD_CLIENT_PLAN_IDS);
    requireAllowedUpper(
        request.commercial().billingStatus(),
        "commercial.billingStatus",
        ADD_CLIENT_BILLING_STATUSES);
    requireAllowedUpper(
        request.commercial().supportTier(), "commercial.supportTier", ADD_CLIENT_SUPPORT_TIERS);
    requireNonNegative(request.quotas().maxActiveUsers(), "quotas.maxActiveUsers");
    requireNonNegative(request.quotas().maxApiRequests(), "quotas.maxApiRequests");
    requireNonNegative(request.quotas().maxStorageBytes(), "quotas.maxStorageBytes");
    requireNonNegative(request.quotas().maxConcurrentRequests(), "quotas.maxConcurrentRequests");
    Set<String> modules = request.modules().enabled();
    if (modules == null || modules.isEmpty()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "modules.enabled must include at least one module");
    }
    for (String module : modules) {
      requireAllowedUpper(module, "modules.enabled", ADD_CLIENT_MODULES);
    }
  }

  private List<ReentrantLock> acquireAddClientCreateLocks(String companyCode, String ownerEmail) {
    List<String> keys =
        List.of("company:" + companyCode, "owner-email:" + ownerEmail).stream().sorted().toList();
    List<ReentrantLock> locks = new ArrayList<>();
    for (String key : keys) {
      ReentrantLock lock =
          ADD_CLIENT_CREATE_LOCKS.computeIfAbsent(key, ignored -> new ReentrantLock());
      lock.lock();
      locks.add(lock);
    }
    return locks;
  }

  private void releaseAddClientCreateLocks(List<ReentrantLock> locks) {
    for (int index = locks.size() - 1; index >= 0; index--) {
      locks.get(index).unlock();
    }
  }

  private ApplicationException duplicateConflict(String message, String field) {
    return new ApplicationException(ErrorCode.BUSINESS_DUPLICATE_ENTRY, message)
        .withDetail("field", field);
  }

  private void requireText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          fieldName + " is required");
    }
  }

  private void requireAllowedUpper(String value, String fieldName, Set<String> allowed) {
    requireText(value, fieldName);
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if (!allowed.contains(normalized)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          fieldName + " must be one of " + allowed);
    }
  }

  private void requireOptionalAllowedUpper(String value, String fieldName, Set<String> allowed) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    requireAllowedUpper(value, fieldName, allowed);
  }

  private void requireNonNegative(Long value, String fieldName) {
    if (value != null && value < 0) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          fieldName + " must be greater than or equal to 0");
    }
  }

  private void validateOwnerEmailFormat(String ownerEmail) {
    if (!ownerEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "owner.email must be a valid email address");
    }
  }

  private Set<String> normalizeModules(Set<String> modules) {
    return modules.stream()
        .map(module -> module.trim().toUpperCase(Locale.ROOT))
        .collect(java.util.stream.Collectors.toSet());
  }

  private String normalizeOptionalUpper(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : value;
  }

  private Instant resolveTrialEndsAt(Integer trialDays) {
    if (trialDays == null || trialDays <= 0) {
      return null;
    }
    return Instant.now().plus(trialDays, ChronoUnit.DAYS);
  }

  private String normalizeCreateCode(String code) {
    if (!StringUtils.hasText(code)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "company.code is required");
    }
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private UserAccount createPendingOwner(Company company, String ownerEmail, String displayName) {
    Role adminRole =
        roleRepository
            .findByName("ROLE_ADMIN")
            .orElseThrow(
                () ->
                    com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                        "ROLE_ADMIN must exist before Add Client creation"));
    UserAccount owner =
        new UserAccount(
            ownerEmail,
            company.getCode(),
            passwordEncoder.encode("activation-pending-" + UUID.randomUUID()),
            StringUtils.hasText(displayName) ? displayName.trim() : "Tenant Owner");
    owner.setCompany(company);
    owner.setEnabled(false);
    owner.setMustChangePassword(true);
    owner.addRole(adminRole);
    UserAccount saved = userAccountRepository.saveAndFlush(owner);
    iamCanonicalStorageService.syncUser(saved);
    return saved;
  }

  private ActivationIssue issueActivation(Company company, UserAccount owner) {
    Instant now = CompanyTime.now(company);
    Instant expiresAt = now.plus(72, ChronoUnit.HOURS);
    String rawToken = newActivationToken();
    TenantActivationToken activationToken =
        tenantActivationTokenRepository.saveAndFlush(
            TenantActivationToken.digestOnly(
                company, owner, activationTokenDigest(rawToken), now, expiresAt));
    emailService.sendTenantActivationEmailRequired(
        owner.getEmail(),
        owner.getDisplayName(),
        company.getName(),
        company.getCode(),
        rawToken,
        expiresAt);
    activationToken.markSent(now);
    tenantActivationTokenRepository.saveAndFlush(activationToken);
    return new ActivationIssue(activationToken.getId(), now, expiresAt);
  }

  private String newActivationToken() {
    byte[] bytes = new byte[32];
    ACTIVATION_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String activationTokenDigest(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed =
          digest.digest((ACTIVATION_TOKEN_SCOPE + ":" + rawToken).getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hashed.length * 2);
      for (byte value : hashed) {
        hex.append(String.format("%02x", value));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Activation token digest algorithm is unavailable", ex);
    }
  }

  private SuperAdminAddClientCreateResponse addClientResponse(
      Company company, UserAccount owner, ActivationIssue activationIssue, Long auditEventId) {
    boolean sent = activationIssue != null;
    return new SuperAdminAddClientCreateResponse(
        company.getId(),
        company.getCode(),
        company.getName(),
        sent ? "PENDING_ACTIVATION" : "DRAFT",
        new SuperAdminAddClientCreateResponse.Owner(
            owner.getId(), owner.getEmail(), owner.getDisplayName(), "PENDING_ACTIVATION"),
        company.getCommercialPlanId(),
        company.getCommercialBillingStatus(),
        company.getCommercialTrialEndsAt(),
        company.getCommercialSupportTier(),
        new SuperAdminAddClientCreateResponse.Quotas(
            company.getQuotaMaxActiveUsers(),
            company.getQuotaMaxApiRequests(),
            company.getQuotaMaxStorageBytes(),
            company.getQuotaMaxConcurrentRequests(),
            company.isQuotaSoftLimitEnabled(),
            company.isQuotaHardLimitEnabled()),
        company.getEnabledModules(),
        new SuperAdminAddClientCreateResponse.Activation(
            sent ? "SENT" : "NOT_SENT",
            sent ? activationIssue.sentAt() : null,
            sent ? activationIssue.expiresAt() : null,
            sent ? activationIssue.tokenId() : null,
            sent ? "EMAIL_SENT" : "NOT_SENT",
            List.of("secretMaterial", "activationLink", "credentialMaterial")),
        draftSeedPolicy(),
        auditEventId);
  }

  private List<SuperAdminTenantDetailDto.SupportTimelineEvent> buildSupportTimeline(
      Company company) {
    List<SuperAdminTenantDetailDto.SupportTimelineEvent> timeline = new ArrayList<>();
    for (TenantSupportWarning warning :
        tenantSupportWarningRepository.findByCompany_IdOrderByIssuedAtDesc(company.getId())) {
      timeline.add(
          new SuperAdminTenantDetailDto.SupportTimelineEvent(
              "WARNING",
              warning.getWarningCategory(),
              warning.getRequestedLifecycleState(),
              warning.getWarningCategory(),
              warning.getIssuedBy(),
              warning.getIssuedAt()));
    }
    for (AuditLog auditLog :
        auditLogRepository.findTop50ByCompanyIdOrderByTimestampDesc(company.getId())) {
      timeline.add(
          new SuperAdminTenantDetailDto.SupportTimelineEvent(
              "AUDIT",
              auditLog.getEventType().name(),
              auditStatus(auditLog),
              auditLog.getEventType().name(),
              StringUtils.hasText(auditLog.getUsername()) ? auditLog.getUsername() : "system",
              toInstant(auditLog.getTimestamp())));
    }
    timeline.sort(
        Comparator.comparing(
                SuperAdminTenantDetailDto.SupportTimelineEvent::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(SuperAdminTenantDetailDto.SupportTimelineEvent::category));
    return timeline.size() > 50 ? timeline.subList(0, 50) : timeline;
  }

  private String auditStatus(AuditLog auditLog) {
    return auditLog.getStatus() == null ? "SUCCESS" : auditLog.getStatus().name();
  }

  private MainAdminSummaryDto toMainAdminSummary(Company company, UserAccount mainAdmin) {
    if (mainAdmin == null) {
      return new MainAdminSummaryDto(company.getMainAdminUserId(), null, null, false, false);
    }
    return new MainAdminSummaryDto(
        mainAdmin.getId(),
        mainAdmin.getEmail(),
        mainAdmin.getDisplayName(),
        mainAdmin.isEnabled(),
        true);
  }

  private UserAccount resolveMainAdmin(Company company) {
    if (company == null || company.getMainAdminUserId() == null) {
      return null;
    }
    return userAccountRepository.findById(company.getMainAdminUserId()).orElse(null);
  }

  private UserAccount requireAssignedAdmin(Company company, Long adminUserId) {
    if (company == null || company.getId() == null || adminUserId == null) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Target admin is required");
    }
    UserAccount user =
        userAccountRepository
            .findByIdAndCompany_Id(adminUserId, company.getId())
            .orElseThrow(
                () ->
                    com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                        "Admin user not found for company"));
    boolean adminRole =
        user.getRoles().stream()
            .map(Role::getName)
            .filter(StringUtils::hasText)
            .anyMatch(
                roleName ->
                    "ROLE_ADMIN".equalsIgnoreCase(roleName)
                        || "ROLE_SUPER_ADMIN".equalsIgnoreCase(roleName));
    if (!adminRole) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "Target user is not an admin for company: " + company.getCode());
    }
    return user;
  }

  private Company requireCompany(Long companyId) {
    return companyRepository
        .findById(companyId)
        .orElseThrow(
            () ->
                com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Company not found"));
  }

  private String resolveLifecycle(Company company) {
    return company == null || company.getLifecycleState() == null
        ? CompanyLifecycleState.ACTIVE.name()
        : company.getLifecycleState().name();
  }

  private String resolveTenantStatus(Company company, CompanyTenantMetricsDto metrics) {
    String metricsStatus =
        metrics == null ? null : normalizeCanonicalStatus(metrics.lifecycleState(), true);
    String lifecycle =
        metrics == null || !StringUtils.hasText(metrics.lifecycleState())
            ? resolveLifecycle(company)
            : metrics.lifecycleState().trim().toUpperCase(Locale.ROOT);
    if (CompanyLifecycleState.SUSPENDED.name().equals(lifecycle)) {
      return "SUSPENDED_BLOCKED";
    }
    if (CompanyLifecycleState.DEACTIVATED.name().equals(lifecycle)) {
      return "ARCHIVED";
    }
    String onboardingStatus = resolveOnboardingTenantStatus(company);
    if (onboardingStatus != null) {
      return onboardingStatus;
    }
    if (metricsStatus != null
        && !"ACTIVE".equals(metricsStatus)
        && !"TRIAL_ACTIVE".equals(metricsStatus)) {
      return metricsStatus;
    }
    return metricsStatus == null ? "ACTIVE" : metricsStatus;
  }

  private String resolveOnboardingTenantStatus(Company company) {
    if (company != null
        && company.getOnboardingCompletedAt() == null
        && "USED".equals(normalizeActivationStatus(company.getActivationStatus()))) {
      return "SETUP_PENDING";
    }
    if (company != null
        && company.getOnboardingCompletedAt() == null
        && (company.getOnboardingCredentialsEmailedAt() != null
            || company.getActivationSentAt() != null
            || Set.of("SENT", "EXPIRED", "SUPERSEDED")
                .contains(normalizeActivationStatus(company.getActivationStatus())))) {
      return "PENDING_ACTIVATION";
    }
    if (company != null && company.getOnboardingCompletedAt() == null) {
      String activationStatus = normalizeActivationStatus(company.getActivationStatus());
      if (StringUtils.hasText(company.getOnboardingAdminEmail())
          && (!StringUtils.hasText(activationStatus) || "NOT_SENT".equals(activationStatus))) {
        return "DRAFT";
      }
    }
    if (company != null
        && company.getOnboardingAdminUserId() != null
        && company.getOnboardingCompletedAt() == null) {
      return "SETUP_PENDING";
    }
    return null;
  }

  private String normalizeActivationStatus(String activationStatus) {
    if (!StringUtils.hasText(activationStatus)) {
      return "NOT_SENT";
    }
    return activationStatus.trim().toUpperCase(Locale.ROOT);
  }

  private Instant resolveLastActivityAt(Long companyId) {
    return auditLogRepository
        .findTop1ByCompanyIdOrderByTimestampDesc(companyId)
        .map(AuditLog::getTimestamp)
        .map(this::toInstant)
        .orElse(null);
  }

  private String normalizeStatusFilter(String statusFilter) {
    if (!StringUtils.hasText(statusFilter)) {
      return null;
    }
    String normalized = statusFilter.trim().toUpperCase(Locale.ROOT);
    if (CANONICAL_TENANT_STATUSES.contains(normalized)
        || LEGACY_STATUS_ALIASES.containsKey(normalized)) {
      return normalized;
    }
    throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
        "status filter must be one of DRAFT, PENDING_ACTIVATION, SETUP_PENDING, TRIAL_ACTIVE,"
            + " ACTIVE, GRACE, SUSPENDED_READ_ONLY, SUSPENDED_BLOCKED, CANCELED, ARCHIVED,"
            + " SEED_FAILED, or legacy aliases SUSPENDED/DEACTIVATED");
  }

  private boolean statusMatches(TenantListCandidate candidate, String normalizedStatus) {
    if (!StringUtils.hasText(normalizedStatus)) {
      return true;
    }
    if ("SUSPENDED".equals(normalizedStatus)) {
      return CompanyLifecycleState.SUSPENDED.name().equals(resolveLifecycle(candidate.company()))
          || "SUSPENDED_READ_ONLY".equals(candidate.status())
          || "SUSPENDED_BLOCKED".equals(candidate.status());
    }
    if ("DEACTIVATED".equals(normalizedStatus)) {
      return CompanyLifecycleState.DEACTIVATED.name().equals(resolveLifecycle(candidate.company()))
          || "ARCHIVED".equals(candidate.status());
    }
    return normalizedStatus.equals(candidate.status());
  }

  private String normalizeCanonicalStatus(String rawStatus, boolean allowLegacyAliases) {
    if (!StringUtils.hasText(rawStatus)) {
      return null;
    }
    String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
    if (CANONICAL_TENANT_STATUSES.contains(normalized)) {
      return normalized;
    }
    return allowLegacyAliases ? LEGACY_STATUS_ALIASES.get(normalized) : null;
  }

  private String normalizeSearchQuery(String query) {
    if (!StringUtils.hasText(query)) {
      return null;
    }
    return normalizeSearchText(query);
  }

  private boolean searchMatches(TenantListCandidate candidate, String normalizedQuery) {
    if (!StringUtils.hasText(normalizedQuery)) {
      return true;
    }
    Company company = candidate.company();
    UserAccount mainAdmin = candidate.mainAdmin();
    return containsNormalized(company.getName(), normalizedQuery)
        || containsNormalized(company.getCode(), normalizedQuery)
        || containsNormalized(company.getOnboardingAdminEmail(), normalizedQuery)
        || (mainAdmin != null && containsNormalized(mainAdmin.getEmail(), normalizedQuery))
        || (mainAdmin != null && containsNormalized(mainAdmin.getDisplayName(), normalizedQuery));
  }

  private boolean containsNormalized(String value, String normalizedQuery) {
    return StringUtils.hasText(value) && normalizeSearchText(value).contains(normalizedQuery);
  }

  private String normalizeSearchText(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    StringBuilder normalized = new StringBuilder();
    for (char ch : value.trim().toLowerCase(Locale.ROOT).toCharArray()) {
      if (Character.isLetterOrDigit(ch)) {
        normalized.append(ch);
      }
    }
    return normalized.toString();
  }

  private int validatePage(int page) {
    if (page < 0) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "page must be greater than or equal to 0");
    }
    return page;
  }

  private int validateSize(int size) {
    if (size < 1 || size > 100) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "size must be between 1 and 100");
    }
    return size;
  }

  private SortSpec parseSort(String sort) {
    String raw = StringUtils.hasText(sort) ? sort.trim() : "companyCode,asc";
    String[] parts = raw.split(",", -1);
    String field = parts[0].trim();
    String direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";
    if (parts.length > 2 || !Set.of("asc", "desc").contains(direction)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "sort must use '<field>,asc' or '<field>,desc'");
    }
    Comparator<TenantListCandidate> comparator =
        switch (field) {
          case "companyCode", "code" ->
              Comparator.comparing(
                  candidate -> safeLower(candidate.company().getCode()), Comparator.naturalOrder());
          case "companyName", "name" ->
              Comparator.comparing(
                  candidate -> safeLower(candidate.company().getName()), Comparator.naturalOrder());
          case "status" ->
              Comparator.comparing(TenantListCandidate::status, Comparator.naturalOrder());
          case "plan" ->
              Comparator.comparing(
                  candidate -> resolvePlanId(candidate.company()), Comparator.naturalOrder());
          case "billingStatus" ->
              Comparator.comparing(
                  candidate -> resolveBillingStatus(candidate.status()), Comparator.naturalOrder());
          case "lastActivityAt" ->
              Comparator.comparing(
                  TenantListCandidate::lastActivityAt,
                  Comparator.nullsLast(Comparator.naturalOrder()));
          case "health" ->
              Comparator.comparingInt(
                  candidate -> healthSummary(candidate.status(), candidate.metrics()).riskScore());
          default ->
              throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                  "sort field must be one of companyCode, companyName, status, plan,"
                      + " billingStatus, lastActivityAt, or health");
        };
    Comparator<TenantListCandidate> stableComparator =
        comparator.thenComparing(
            candidate -> safeLower(candidate.company().getCode()), Comparator.naturalOrder());
    return new SortSpec("desc".equals(direction) ? stableComparator.reversed() : stableComparator);
  }

  private String safeLower(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "";
  }

  private String resolvePlanId(Company company) {
    return company == null || !StringUtils.hasText(company.getCommercialPlanId())
        ? "TRIAL"
        : company.getCommercialPlanId();
  }

  private String resolveBillingStatus(String status) {
    if ("ARCHIVED".equals(status)) {
      return "ARCHIVED";
    }
    if ("CANCELED".equals(status)) {
      return "CANCELED";
    }
    if ("GRACE".equals(status)) {
      return "GRACE";
    }
    if ("TRIAL_ACTIVE".equals(status)) {
      return "TRIAL";
    }
    return "MANUAL";
  }

  private String resolveBillingStatus(Company company, String status) {
    if (company != null && StringUtils.hasText(company.getCommercialBillingStatus())) {
      return company.getCommercialBillingStatus();
    }
    return resolveBillingStatus(status);
  }

  private Instant resolveTrialEndsAt(Company company, String status) {
    return company == null ? null : company.getCommercialTrialEndsAt();
  }

  private SuperAdminTenantSummaryDto.HealthSummary healthSummary(
      String status, CompanyTenantMetricsDto metrics) {
    int errorRate =
        metrics == null ? 0 : (int) Math.min(metrics.apiErrorRateInBasisPoints(), 10000);
    if ("SUSPENDED_BLOCKED".equals(status)
        || "ARCHIVED".equals(status)
        || "CANCELED".equals(status)) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "BLOCKED",
          Math.max(75, errorRate / 100),
          "Tenant access is restricted by lifecycle status");
    }
    if ("SUSPENDED_READ_ONLY".equals(status)) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "READ_ONLY", Math.max(60, errorRate / 100), "Tenant writes are restricted");
    }
    if ("SEED_FAILED".equals(status)) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "SETUP_FAILED", Math.max(80, errorRate / 100), "Tenant seed repair is required");
    }
    if ("GRACE".equals(status)) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "AT_RISK", Math.max(50, errorRate / 100), "Tenant is in billing grace");
    }
    if ("DRAFT".equals(status)
        || "PENDING_ACTIVATION".equals(status)
        || "SETUP_PENDING".equals(status)) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "SETUP_REQUIRED", Math.max(25, errorRate / 100), "Tenant setup is not complete");
    }
    if (errorRate >= 500) {
      return new SuperAdminTenantSummaryDto.HealthSummary(
          "DEGRADED", errorRate / 100, "API error rate needs review");
    }
    return new SuperAdminTenantSummaryDto.HealthSummary(
        "HEALTHY", errorRate / 100, "No platform risk signals");
  }

  private SuperAdminTenantDetailDto.TabState tabState(String state, String message) {
    return new SuperAdminTenantDetailDto.TabState(state, message);
  }

  private SuperAdminTenantDetailDto.TabState tabStateForStatus(
      String status, String defaultState, String defaultMessage) {
    String normalized = normalizeCanonicalStatus(status, false);
    if (normalized == null) {
      return tabState(defaultState, defaultMessage);
    }
    return switch (normalized) {
      case "DRAFT" ->
          tabState(
              "PENDING_SETUP", "DRAFT tenant needs activation before this summary is actionable");
      case "PENDING_ACTIVATION" ->
          tabState(
              "PENDING_ACTIVATION", "PENDING_ACTIVATION tenant is waiting for owner activation");
      case "SETUP_PENDING" ->
          tabState("SETUP_REQUIRED", "SETUP_PENDING tenant is completing first-login setup");
      case "SEED_FAILED" ->
          tabState("ACTION_REQUIRED", "SEED_FAILED tenant needs seed repair before normal use");
      case "GRACE" -> tabState("ACTION_REQUIRED", "GRACE tenant needs billing follow-up");
      case "SUSPENDED_READ_ONLY" ->
          tabState("READ_ONLY", "SUSPENDED_READ_ONLY tenant permits safe reads only");
      case "SUSPENDED_BLOCKED" -> tabState("BLOCKED", "SUSPENDED_BLOCKED tenant access is blocked");
      case "CANCELED" -> tabState("CANCELED", "CANCELED tenant is no longer billable");
      case "ARCHIVED" -> tabState("ARCHIVED", "ARCHIVED tenant is preserved for history");
      default -> tabState(defaultState, defaultMessage + " for " + normalized);
    };
  }

  private String normalizeRequiredEmail(String email, String fieldName) {
    if (!StringUtils.hasText(email)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          fieldName + " is required");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeOptionalReason(String value, String fallback) {
    if (!StringUtils.hasText(value)) {
      return fallback;
    }
    return value.trim();
  }

  private String normalizeWarningCategory(String warningCategory) {
    return StringUtils.hasText(warningCategory)
        ? warningCategory.trim().toUpperCase(Locale.ROOT)
        : "GENERAL";
  }

  private String normalizeRequestedLifecycleState(String requestedLifecycleState) {
    if (!StringUtils.hasText(requestedLifecycleState)) {
      return CompanyLifecycleState.SUSPENDED.name();
    }
    String normalized = requestedLifecycleState.trim().toUpperCase(Locale.ROOT);
    if (normalized.equals(CompanyLifecycleState.SUSPENDED.name())
        || normalized.equals(CompanyLifecycleState.DEACTIVATED.name())) {
      return normalized;
    }
    throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
        "requestedLifecycleState must be SUSPENDED or DEACTIVATED");
  }

  private int resolveGracePeriodHours(Integer gracePeriodHours) {
    if (gracePeriodHours == null) {
      return 24;
    }
    if (gracePeriodHours < 1 || gracePeriodHours > 720) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
          "gracePeriodHours must be between 1 and 720");
    }
    return gracePeriodHours;
  }

  private int safeInteger(long value) {
    if (value > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return value < 0L ? 0 : (int) value;
  }

  private void assertTenantExclusiveUsers(
      Company company, List<UserAccount> users, String operationDescription) {
    if (users == null || users.isEmpty()) {
      return;
    }
    List<String> sharedUserEmails = new ArrayList<>();
    for (UserAccount user : users) {
      if (isSharedAcrossCompanies(user)) {
        sharedUserEmails.add(
            StringUtils.hasText(user.getEmail()) ? user.getEmail().trim() : "UNKNOWN");
      }
    }
    if (!sharedUserEmails.isEmpty()) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Cannot perform "
              + operationDescription
              + " while shared users are assigned to "
              + company.getCode()
              + ": "
              + String.join(", ", sharedUserEmails));
    }
  }

  private void assertTenantExclusiveUser(
      Company company, UserAccount user, String operationDescription) {
    if (isSharedAcrossCompanies(user)) {
      throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
          "Cannot perform "
              + operationDescription
              + " for shared user "
              + user.getEmail()
              + " in "
              + company.getCode()
              + "; assign a tenant-exclusive admin first");
    }
  }

  private boolean isSharedAcrossCompanies(UserAccount user) {
    // Auth v2 hard-cut binds each account to exactly one tenant company.
    if (user == null) {
      return false;
    }
    return false;
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return "anonymous";
    }
    return authentication.getName().trim();
  }

  private void logAuditSuccess(Company company, String reason, Map<String, String> metadata) {
    if (auditService == null) {
      return;
    }
    HashMap<String, String> auditMetadata = new HashMap<>();
    if (metadata != null) {
      auditMetadata.putAll(metadata);
    }
    auditMetadata.put("actor", currentActor());
    String actorPublicId = currentActorPublicId();
    if (StringUtils.hasText(actorPublicId)) {
      auditMetadata.put("actorPublicId", actorPublicId);
    }
    auditMetadata.put("reason", reason);
    auditMetadata.put("targetCompanyCode", company.getCode());
    auditMetadata.put("targetCompanyId", String.valueOf(company.getId()));
    auditService.logAuthSuccess(
        AuditEvent.CONFIGURATION_CHANGED, currentActor(), company.getCode(), auditMetadata);
  }

  private Long logAuditRequired(Company company, String reason, Map<String, String> metadata) {
    if (auditService == null) {
      return null;
    }
    HashMap<String, String> auditMetadata = new HashMap<>();
    if (metadata != null) {
      auditMetadata.putAll(metadata);
    }
    auditMetadata.put("actor", currentActor());
    String actorPublicId = currentActorPublicId();
    if (StringUtils.hasText(actorPublicId)) {
      auditMetadata.put("actorPublicId", actorPublicId);
    }
    auditMetadata.put("reason", reason);
    auditMetadata.put("targetCompanyCode", company.getCode());
    auditMetadata.put("targetCompanyId", String.valueOf(company.getId()));
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), company.getCode(), auditMetadata);
    return auditLog == null ? null : auditLog.getId();
  }

  private Instant toInstant(LocalDateTime timestamp) {
    return timestamp == null ? null : timestamp.atZone(ZoneOffset.UTC).toInstant();
  }

  private String currentActorPublicId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal userPrincipal
        && userPrincipal.getUser() != null
        && userPrincipal.getUser().getPublicId() != null) {
      return userPrincipal.getUser().getPublicId().toString();
    }
    return null;
  }

  private record TenantListCandidate(
      Company company,
      CompanyTenantMetricsDto metrics,
      UserAccount mainAdmin,
      Instant lastActivityAt,
      String status) {}

  private record SortSpec(Comparator<TenantListCandidate> comparator) {}

  private record ActivationIssue(Long tokenId, Instant sentAt, Instant expiresAt) {}
}
