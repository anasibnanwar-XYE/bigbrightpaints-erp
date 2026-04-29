package com.bigbrightpaints.erp.modules.company.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyLifecycleState;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminBillingLedgerEntry;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminBillingLedgerEntryRepository;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminBillingSubscription;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminBillingSubscriptionRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminBillingDtos;

@Service
public class SuperAdminBillingService {

  private static final Set<String> SUBSCRIPTION_STATUSES =
      Set.of("TRIAL", "MANUAL", "ACTIVE", "CANCELED", "ARCHIVED");
  private static final Set<String> ACTIVE_SUBSCRIPTION_STATUSES =
      Set.of("TRIAL", "MANUAL", "ACTIVE");
  private static final Set<String> CADENCES = Set.of("MONTHLY", "ANNUAL", "CUSTOM");
  private static final Set<String> COLLECTION_MODES = Set.of("MANUAL", "EXTERNAL", "OFFLINE");
  private static final Set<String> BILLING_STATUSES =
      Set.of("TRIAL", "MANUAL", "PAID", "DUE", "OVERDUE", "GRACE", "CANCELED", "ARCHIVED");

  private final CompanyRepository companyRepository;
  private final SuperAdminBillingSubscriptionRepository subscriptionRepository;
  private final SuperAdminBillingLedgerEntryRepository ledgerEntryRepository;
  private final AuditService auditService;
  private final TenantRuntimeEnforcementService tenantRuntimeEnforcementService;

  public SuperAdminBillingService(
      CompanyRepository companyRepository,
      SuperAdminBillingSubscriptionRepository subscriptionRepository,
      SuperAdminBillingLedgerEntryRepository ledgerEntryRepository,
      AuditService auditService,
      TenantRuntimeEnforcementService tenantRuntimeEnforcementService) {
    this.companyRepository = companyRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.auditService = auditService;
    this.tenantRuntimeEnforcementService = tenantRuntimeEnforcementService;
  }

  @Transactional
  public SuperAdminBillingDtos.SubscriptionResponse createSubscription(
      Long companyId, SuperAdminBillingDtos.SubscriptionRequest request) {
    if (request == null) {
      throw invalidInput("Subscription payload is required");
    }
    Company company = lockCompany(companyId);
    String status = requireAllowed(request.status(), "status", SUBSCRIPTION_STATUSES);
    if (ACTIVE_SUBSCRIPTION_STATUSES.contains(status)
        && !subscriptionRepository.lockActiveByCompanyId(companyId).isEmpty()) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_DUPLICATE_ENTRY, "Tenant already has an active subscription");
    }
    SuperAdminBillingSubscription subscription = new SuperAdminBillingSubscription();
    subscription.setCompany(company);
    subscription.setPlanId(normalizeToken(request.planId(), "planId"));
    subscription.setCustomPlanName(trimToNull(request.customPlanName()));
    subscription.setStatus(status);
    subscription.setCadence(requireAllowed(request.cadence(), "cadence", CADENCES));
    subscription.setAmountMinorUnits(
        requireNonNegative(request.amountMinorUnits(), "amountMinorUnits"));
    subscription.setCurrency(requireCurrency(request.currency()));
    subscription.setCollectionMode(
        requireAllowed(request.collectionMode(), "collectionMode", COLLECTION_MODES));
    subscription.setPeriodStartAt(requireInstant(request.periodStartAt(), "periodStartAt"));
    subscription.setPeriodEndAt(request.periodEndAt());
    subscription.setRenewalAt(request.renewalAt());
    subscription.setDueAt(request.dueAt());
    subscription.setTrialStartAt(request.trialStartAt());
    subscription.setTrialEndAt(request.trialEndAt());
    subscription.setGraceUntilAt(request.graceUntilAt());
    subscription.setExternalReference(trimToNull(request.externalReference()));
    Instant now = CompanyTime.now(company);
    if ("CANCELED".equals(status)) {
      subscription.setCanceledAt(now);
    }
    if ("ARCHIVED".equals(status)) {
      subscription.setArchivedAt(now);
    }
    try {
      subscriptionRepository.saveAndFlush(subscription);
    } catch (DataIntegrityViolationException ex) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_DUPLICATE_ENTRY, "Tenant already has an active subscription", ex);
    }
    String billingStatus = deriveBillingStatus(subscription, balance(companyId), now);
    company.setCommercialPlanId(subscription.getPlanId());
    company.setCommercialBillingStatus(billingStatus);
    company.setCommercialTrialEndsAt(subscription.getTrialEndAt());
    companyRepository.saveAndFlush(company);
    Long auditEventId =
        auditRequired(
            company,
            "billing-subscription-created",
            Map.of(
                "subscriptionId",
                String.valueOf(subscription.getId()),
                "planId",
                subscription.getPlanId(),
                "status",
                subscription.getStatus(),
                "billingStatus",
                billingStatus,
                "amountMinorUnits",
                String.valueOf(subscription.getAmountMinorUnits()),
                "currency",
                subscription.getCurrency(),
                "reasonDetail",
                safeReason(request.reason())));
    subscription.setAuditEventId(auditEventId);
    subscriptionRepository.saveAndFlush(subscription);
    return toSubscriptionResponse(subscription, billingStatus);
  }

  @Transactional
  public SuperAdminBillingDtos.SubscriptionResponse getSubscription(Long companyId) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    applyDueScheduledCommercialState(subscription, CompanyTime.now(subscription.getCompany()));
    return toSubscriptionResponse(
        subscription,
        deriveBillingStatus(
            subscription, balance(companyId), CompanyTime.now(subscription.getCompany())));
  }

  @Transactional
  public LedgerMutationResult createInvoice(
      Long companyId, SuperAdminBillingDtos.LedgerEntryRequest request) {
    return createLedgerEntry(companyId, "INVOICE", "DEBIT", request, null);
  }

  @Transactional
  public LedgerMutationResult createPayment(
      Long companyId, SuperAdminBillingDtos.LedgerEntryRequest request) {
    return createLedgerEntry(companyId, "PAYMENT", "CREDIT", request, null);
  }

  @Transactional
  public LedgerMutationResult createAdjustment(
      Long companyId, SuperAdminBillingDtos.AdjustmentRequest request) {
    if (request == null) {
      throw invalidInput("Adjustment payload is required");
    }
    String direction = requireAllowed(request.direction(), "direction", Set.of("DEBIT", "CREDIT"));
    SuperAdminBillingDtos.LedgerEntryRequest normalized =
        new SuperAdminBillingDtos.LedgerEntryRequest(
            request.amountMinorUnits(),
            request.currency(),
            request.reason(),
            request.idempotencyKey(),
            request.externalReference());
    return createLedgerEntry(companyId, "ADJUSTMENT", direction, normalized, direction);
  }

  @Transactional(readOnly = true)
  public SuperAdminBillingDtos.LedgerResponse getLedger(Long companyId) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    List<SuperAdminBillingDtos.LedgerEntryResponse> entries =
        ledgerEntryRepository.findByCompanyIdOrderByCreatedAtAscIdAsc(companyId).stream()
            .map(this::toLedgerEntryResponse)
            .toList();
    long balance = balance(companyId);
    String billingStatus =
        deriveBillingStatus(subscription, balance, CompanyTime.now(subscription.getCompany()));
    return new SuperAdminBillingDtos.LedgerResponse(
        companyId,
        subscription.getCompany().getCode(),
        subscription.getId(),
        balance,
        subscription.getCurrency(),
        billingStatus,
        entries,
        privacy());
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse getCommercialState(Long companyId) {
    Company company = lockCompany(companyId);
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    Instant now = CompanyTime.now(company);
    applyDueScheduledCommercialState(subscription, now);
    String commercialState = resolveCommercialState(company);
    String billingStatus =
        Set.of("ACTIVE", "TRIAL_ACTIVE").contains(commercialState)
            ? deriveBillingStatus(subscription, balance(companyId), now)
            : companyBillingStatusOrManual(company);
    return commercialStateResponse(
        company,
        subscription,
        commercialState,
        billingStatus,
        accessRuntimeState(commercialState),
        safeReason(company.getLifecycleReason()),
        effectiveAtFor(commercialState, subscription, now),
        null);
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse startGrace(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    applyDueScheduledCommercialState(subscription, CompanyTime.now(subscription.getCompany()));
    requireNoPendingTerminalAction(subscription, "GRACE");
    Instant effectiveAt = resolveEffectiveAt(subscription.getCompany(), request);
    Instant graceUntilAt =
        request != null && request.graceUntilAt() != null
            ? request.graceUntilAt()
            : subscription.getGraceUntilAt();
    if (graceUntilAt == null || !graceUntilAt.isAfter(effectiveAt)) {
      throw invalidInput("graceUntilAt must be after the grace effective time");
    }
    return applyCommercialState(
        subscription,
        "GRACE",
        "GRACE",
        CompanyLifecycleState.ACTIVE,
        TenantRuntimeEnforcementService.TenantRuntimeState.ACTIVE,
        "GRACE",
        "commercial-state-grace-started",
        request,
        effectiveAt,
        graceUntilAt);
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse suspendReadOnly(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    applyDueScheduledCommercialState(subscription, CompanyTime.now(subscription.getCompany()));
    requireNoPendingTerminalAction(subscription, "SUSPEND_READ_ONLY");
    Instant effectiveAt = resolveEffectiveAt(subscription.getCompany(), request);
    String billingStatus = deriveBillingStatus(subscription, balance(companyId), effectiveAt);
    return applyCommercialState(
        subscription,
        "SUSPEND_READ_ONLY",
        "SUSPENDED_READ_ONLY",
        CompanyLifecycleState.SUSPENDED,
        TenantRuntimeEnforcementService.TenantRuntimeState.HOLD,
        billingStatus,
        "commercial-state-suspended-read-only",
        request,
        effectiveAt,
        subscription.getGraceUntilAt());
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse suspendBlocked(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    applyDueScheduledCommercialState(subscription, CompanyTime.now(subscription.getCompany()));
    requireNoPendingTerminalAction(subscription, "SUSPEND_BLOCKED");
    Instant effectiveAt = resolveEffectiveAt(subscription.getCompany(), request);
    String billingStatus = deriveBillingStatus(subscription, balance(companyId), effectiveAt);
    return applyCommercialState(
        subscription,
        "SUSPEND_BLOCKED",
        "SUSPENDED_BLOCKED",
        CompanyLifecycleState.SUSPENDED,
        TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED,
        billingStatus,
        "commercial-state-suspended-blocked",
        request,
        effectiveAt,
        subscription.getGraceUntilAt());
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse resume(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    applyDueScheduledCommercialState(subscription, CompanyTime.now(subscription.getCompany()));
    requireNoPendingTerminalAction(subscription, "RESUME");
    Instant effectiveAt = resolveEffectiveAt(subscription.getCompany(), request);
    String billingStatus = deriveBillingStatus(subscription, balance(companyId), effectiveAt);
    String activeState = "TRIAL".equals(billingStatus) ? "TRIAL_ACTIVE" : "ACTIVE";
    return applyCommercialState(
        subscription,
        "RESUME",
        activeState,
        CompanyLifecycleState.ACTIVE,
        TenantRuntimeEnforcementService.TenantRuntimeState.ACTIVE,
        billingStatus,
        "commercial-state-resumed",
        request,
        effectiveAt,
        subscription.getGraceUntilAt());
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse cancel(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    Instant now = CompanyTime.now(subscription.getCompany());
    applyDueScheduledCommercialState(subscription, now);
    Instant effectiveAt =
        request != null && request.effectiveAt() != null ? request.effectiveAt() : now;
    if (effectiveAt.isAfter(now)) {
      return scheduleTerminalCommercialState(subscription, "CANCEL", request, effectiveAt);
    }
    return applyCommercialState(
        subscription,
        "CANCEL",
        "CANCELED",
        CompanyLifecycleState.DEACTIVATED,
        TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED,
        "CANCELED",
        "commercial-state-canceled",
        request,
        effectiveAt,
        subscription.getGraceUntilAt());
  }

  @Transactional
  public SuperAdminBillingDtos.CommercialStateResponse archive(
      Long companyId, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
    Instant now = CompanyTime.now(subscription.getCompany());
    applyDueScheduledCommercialState(subscription, now);
    Instant effectiveAt =
        request != null && request.effectiveAt() != null ? request.effectiveAt() : now;
    if (effectiveAt.isAfter(now)) {
      return scheduleTerminalCommercialState(subscription, "ARCHIVE", request, effectiveAt);
    }
    return applyCommercialState(
        subscription,
        "ARCHIVE",
        "ARCHIVED",
        CompanyLifecycleState.DEACTIVATED,
        TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED,
        "ARCHIVED",
        "commercial-state-archived",
        request,
        effectiveAt,
        subscription.getGraceUntilAt());
  }

  @Transactional
  public SuperAdminBillingDtos.BillingStatusSummary billingSummaryFor(Company company) {
    if (company == null || company.getId() == null) {
      throw invalidInput("Company is required for billing summary");
    }
    return subscriptionRepository
        .findTopByCompanyIdOrderByCreatedAtDescIdDesc(company.getId())
        .map(
            subscription -> {
              applyDueScheduledCommercialState(subscription, CompanyTime.now(company));
              long balance = balance(company.getId());
              String status = deriveBillingStatus(subscription, balance, CompanyTime.now(company));
              return new SuperAdminBillingDtos.BillingStatusSummary(
                  status,
                  balance,
                  subscription.getCurrency(),
                  subscription.getTrialEndAt(),
                  subscription.getId(),
                  (int) ledgerEntryRepository.countByCompanyId(company.getId()));
            })
        .orElseGet(
            () ->
                new SuperAdminBillingDtos.BillingStatusSummary(
                    companyBillingStatusOrManual(company),
                    0,
                    StringUtils.hasText(company.getBaseCurrency())
                        ? company.getBaseCurrency()
                        : "INR",
                    company.getCommercialTrialEndsAt(),
                    null,
                    0));
  }

  @Transactional
  public Map<String, SuperAdminBillingDtos.CurrencyMetrics> getBillingMetrics() {
    applyDueScheduledCommercialStates();
    Map<String, MutableMetrics> metrics = new LinkedHashMap<>();
    for (SuperAdminBillingSubscription subscription : subscriptionRepository.findAllForMetrics()) {
      String currency = subscription.getCurrency();
      MutableMetrics mutable =
          metrics.computeIfAbsent(currency, ignored -> new MutableMetrics(currency));
      if (!isIncludedInRecurringRevenue(subscription)) {
        mutable.excludedSubscriptionCount++;
        continue;
      }
      long mrr = mrrMinorUnits(subscription);
      mutable.mrrMinorUnits = safeAdd(mutable.mrrMinorUnits, mrr);
      mutable.arrMinorUnits = safeAdd(mutable.arrMinorUnits, safeMultiply(mrr, 12));
      mutable.activeSubscriptionCount++;
    }
    Map<String, SuperAdminBillingDtos.CurrencyMetrics> result = new LinkedHashMap<>();
    metrics.forEach((currency, mutable) -> result.put(currency, mutable.toDto()));
    return result;
  }

  @Transactional
  public long dashboardMrrMinorUnits() {
    return getBillingMetrics().values().stream()
        .mapToLong(SuperAdminBillingDtos.CurrencyMetrics::mrrMinorUnits)
        .sum();
  }

  @Transactional
  public long dashboardArrMinorUnits() {
    return getBillingMetrics().values().stream()
        .mapToLong(SuperAdminBillingDtos.CurrencyMetrics::arrMinorUnits)
        .sum();
  }

  private LedgerMutationResult createLedgerEntry(
      Long companyId,
      String entryType,
      String direction,
      SuperAdminBillingDtos.LedgerEntryRequest request,
      String adjustmentDirection) {
    if (request == null) {
      throw invalidInput(entryType.toLowerCase(Locale.ROOT) + " payload is required");
    }
    Company company = lockCompany(companyId);
    String idempotencyKey = requireText(request.idempotencyKey(), "idempotencyKey");
    SuperAdminBillingSubscription subscription = activeSubscription(companyId);
    long amount = requirePositive(request.amountMinorUnits(), "amountMinorUnits");
    String currency = requireCurrency(request.currency());
    String reason = requireText(request.reason(), "reason");
    String externalReference = trimToNull(request.externalReference());
    var existing =
        ledgerEntryRepository.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey);
    if (existing.isPresent()) {
      requireIdenticalLedgerReplay(
          existing.get(),
          subscription,
          entryType,
          direction,
          amount,
          currency,
          reason,
          externalReference);
      return new LedgerMutationResult(toLedgerEntryResponse(existing.get()), true);
    }
    if (!currency.equals(subscription.getCurrency())) {
      throw invalidInput("Ledger currency must match the active subscription currency");
    }
    long before = balance(companyId);
    long signedAmount = "DEBIT".equals(direction) ? amount : -amount;
    long after = safeAdd(before, signedAmount);
    String billingStatus = deriveBillingStatus(subscription, after, CompanyTime.now(company));
    SuperAdminBillingLedgerEntry entry = new SuperAdminBillingLedgerEntry();
    entry.setCompany(company);
    entry.setSubscription(subscription);
    entry.setEntryType(entryType);
    entry.setDirection(direction);
    entry.setAmountMinorUnits(amount);
    entry.setCurrency(currency);
    entry.setReason(reason);
    entry.setExternalReference(externalReference);
    entry.setIdempotencyKey(idempotencyKey);
    entry.setBalanceBeforeMinorUnits(before);
    entry.setBalanceAfterMinorUnits(after);
    entry.setBillingStatusAfter(billingStatus);
    entry.setCreatedBy(currentActor());
    ledgerEntryRepository.saveAndFlush(entry);
    company.setCommercialBillingStatus(billingStatus);
    companyRepository.saveAndFlush(company);
    Long auditEventId =
        auditRequired(
            company,
            "billing-ledger-" + entryType.toLowerCase(Locale.ROOT) + "-created",
            Map.of(
                "subscriptionId",
                String.valueOf(subscription.getId()),
                "ledgerEntryId",
                String.valueOf(entry.getId()),
                "entryType",
                entryType,
                "direction",
                adjustmentDirection == null ? direction : adjustmentDirection,
                "oldBalanceMinorUnits",
                String.valueOf(before),
                "newBalanceMinorUnits",
                String.valueOf(after),
                "billingStatus",
                billingStatus,
                "currency",
                currency,
                "reasonDetail",
                safeReason(reason)));
    entry.setAuditEventId(auditEventId);
    ledgerEntryRepository.saveAndFlush(entry);
    return new LedgerMutationResult(toLedgerEntryResponse(entry), false);
  }

  private void requireIdenticalLedgerReplay(
      SuperAdminBillingLedgerEntry existing,
      SuperAdminBillingSubscription subscription,
      String entryType,
      String direction,
      long amount,
      String currency,
      String reason,
      String externalReference) {
    boolean identical =
        Objects.equals(existing.getSubscription().getId(), subscription.getId())
            && Objects.equals(existing.getEntryType(), entryType)
            && Objects.equals(existing.getDirection(), direction)
            && Objects.equals(existing.getAmountMinorUnits(), amount)
            && Objects.equals(existing.getCurrency(), currency)
            && Objects.equals(existing.getReason(), reason)
            && Objects.equals(existing.getExternalReference(), externalReference);
    if (!identical) {
      throw new ApplicationException(
          ErrorCode.CONCURRENCY_CONFLICT,
          "Idempotency key already used with different billing ledger payload");
    }
  }

  private SuperAdminBillingSubscription activeSubscription(Long companyId) {
    List<SuperAdminBillingSubscription> active =
        subscriptionRepository.lockActiveByCompanyId(companyId);
    if (active.isEmpty()) {
      throw invalidInput("Tenant does not have an active billing subscription");
    }
    return active.get(0);
  }

  private SuperAdminBillingSubscription latestSubscription(Long companyId) {
    return subscriptionRepository
        .findTopByCompanyIdOrderByCreatedAtDescIdDesc(companyId)
        .orElseThrow(() -> invalidInput("Tenant does not have a billing subscription"));
  }

  @Scheduled(fixedDelayString = "${erp.superadmin.billing.lifecycle-sweep-ms:60000}")
  @Transactional
  public void applyDueScheduledCommercialStates() {
    Instant now = CompanyTime.now();
    for (SuperAdminBillingSubscription subscription :
        subscriptionRepository.lockPendingCommercialActions()) {
      applyDueScheduledCommercialState(subscription, now);
    }
  }

  private void applyDueScheduledCommercialState(
      SuperAdminBillingSubscription subscription, Instant now) {
    if (subscription == null
        || !StringUtils.hasText(subscription.getPendingCommercialAction())
        || subscription.getPendingCommercialEffectiveAt() == null
        || subscription.getPendingCommercialEffectiveAt().isAfter(now)) {
      return;
    }
    String action = subscription.getPendingCommercialAction();
    if ("CANCEL".equals(action)) {
      applyScheduledTerminalCommercialState(subscription, "CANCELED", "commercial-state-canceled");
      return;
    }
    if ("ARCHIVE".equals(action)) {
      applyScheduledTerminalCommercialState(subscription, "ARCHIVED", "commercial-state-archived");
    }
  }

  private void applyScheduledTerminalCommercialState(
      SuperAdminBillingSubscription subscription, String terminalState, String auditReason) {
    Company company = subscription.getCompany();
    String previousCommercialState = resolveCommercialState(company);
    String previousBillingStatus = companyBillingStatusOrManual(company);
    CompanyLifecycleState previousLifecycle =
        company.getLifecycleState() == null
            ? CompanyLifecycleState.ACTIVE
            : company.getLifecycleState();
    Instant effectiveAt = subscription.getPendingCommercialEffectiveAt();
    String pendingReason = subscription.getPendingCommercialReason();
    subscription.setStatus(terminalState);
    if ("CANCELED".equals(terminalState)) {
      subscription.setCanceledAt(effectiveAt);
    } else {
      subscription.setArchivedAt(effectiveAt);
    }
    subscription.setPendingCommercialAction(null);
    subscription.setPendingCommercialEffectiveAt(null);
    subscription.setPendingCommercialReason(null);
    company.setLifecycleState(CompanyLifecycleState.DEACTIVATED);
    company.setLifecycleReason(terminalState);
    company.setCommercialBillingStatus(terminalState);
    companyRepository.saveAndFlush(company);
    tenantRuntimeEnforcementService.updatePolicy(
        company.getCode(),
        TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED,
        terminalState,
        safeRuntimeLimit(company.getQuotaMaxConcurrentRequests()),
        safeRuntimeLimit(company.getQuotaMaxApiRequests()),
        safeRuntimeLimit(company.getQuotaMaxActiveUsers()),
        "commercial-lifecycle-scheduler");
    Long auditEventId =
        auditRequired(
            company,
            auditReason,
            Map.ofEntries(
                Map.entry("oldCommercialState", previousCommercialState),
                Map.entry("newCommercialState", terminalState),
                Map.entry("oldBillingStatus", previousBillingStatus),
                Map.entry("newBillingStatus", terminalState),
                Map.entry("oldLifecycleState", previousLifecycle.name()),
                Map.entry("newLifecycleState", CompanyLifecycleState.DEACTIVATED.name()),
                Map.entry(
                    "runtimeState",
                    TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED.name()),
                Map.entry("effectiveAt", effectiveAt.toString()),
                Map.entry("subscriptionId", String.valueOf(subscription.getId())),
                Map.entry("reasonDetail", safeReason(pendingReason))));
    subscription.setAuditEventId(auditEventId);
    subscriptionRepository.saveAndFlush(subscription);
  }

  private SuperAdminBillingDtos.CommercialStateResponse scheduleTerminalCommercialState(
      SuperAdminBillingSubscription subscription,
      String actionCode,
      SuperAdminBillingDtos.CommercialStateActionRequest request,
      Instant effectiveAt) {
    requireNoPendingTerminalAction(subscription, actionCode);
    String fingerprint = commercialActionFingerprint(actionCode, request, null);
    String currentCommercialState = resolveCommercialState(subscription.getCompany());
    String currentBillingStatus =
        deriveBillingStatus(
            subscription,
            balance(subscription.getCompany().getId()),
            CompanyTime.now(subscription.getCompany()));
    SuperAdminBillingDtos.CommercialStateResponse replay =
        replayCommercialActionIfIdentical(
            subscription,
            actionCode,
            fingerprint,
            currentCommercialState,
            currentBillingStatus,
            accessRuntimeState(currentCommercialState),
            effectiveAt,
            safeReason(request == null ? null : request.reason()));
    if (replay != null) {
      return replay;
    }
    Company company = subscription.getCompany();
    String terminalState = "CANCEL".equals(actionCode) ? "CANCELED" : "ARCHIVED";
    if ("CANCEL".equals(actionCode)) {
      subscription.setCanceledAt(effectiveAt);
    } else {
      subscription.setArchivedAt(effectiveAt);
    }
    subscription.setPendingCommercialAction(actionCode);
    subscription.setPendingCommercialEffectiveAt(effectiveAt);
    subscription.setPendingCommercialReason(safeReason(request == null ? null : request.reason()));
    Long auditEventId =
        auditRequired(
            company,
            "commercial-state-" + terminalState.toLowerCase(Locale.ROOT) + "-scheduled",
            Map.ofEntries(
                Map.entry("oldCommercialState", currentCommercialState),
                Map.entry("newCommercialState", currentCommercialState),
                Map.entry("scheduledCommercialState", terminalState),
                Map.entry("oldBillingStatus", currentBillingStatus),
                Map.entry("newBillingStatus", currentBillingStatus),
                Map.entry("runtimeState", accessRuntimeState(currentCommercialState).name()),
                Map.entry("effectiveAt", effectiveAt.toString()),
                Map.entry("subscriptionId", String.valueOf(subscription.getId())),
                Map.entry("reasonDetail", safeReason(request == null ? null : request.reason()))));
    subscription.setLastCommercialAction(actionCode);
    subscription.setLastCommercialActionFingerprint(fingerprint);
    subscription.setLastCommercialActionEffectiveAt(effectiveAt);
    subscription.setLastCommercialActionAuditEventId(auditEventId);
    subscription.setAuditEventId(auditEventId);
    subscriptionRepository.saveAndFlush(subscription);
    return commercialStateResponse(
        company,
        subscription,
        currentCommercialState,
        currentBillingStatus,
        accessRuntimeState(currentCommercialState),
        safeReason(request == null ? null : request.reason()),
        effectiveAt,
        auditEventId);
  }

  private void requireNoPendingTerminalAction(
      SuperAdminBillingSubscription subscription, String requestedAction) {
    if (subscription == null || !StringUtils.hasText(subscription.getPendingCommercialAction())) {
      return;
    }
    if (!Objects.equals(subscription.getPendingCommercialAction(), requestedAction)) {
      throw new ApplicationException(
          ErrorCode.CONCURRENCY_CONFLICT,
          "Pending commercial lifecycle action must apply before another lifecycle action");
    }
  }

  private SuperAdminBillingDtos.CommercialStateResponse replayCommercialActionIfIdentical(
      SuperAdminBillingSubscription subscription,
      String actionCode,
      String fingerprint,
      String commercialState,
      String billingStatus,
      TenantRuntimeEnforcementService.TenantRuntimeState runtimeState,
      Instant effectiveAt,
      String reason) {
    if (subscription == null
        || !Objects.equals(subscription.getLastCommercialAction(), actionCode)) {
      return null;
    }
    if (!Objects.equals(subscription.getLastCommercialActionFingerprint(), fingerprint)) {
      throw new ApplicationException(
          ErrorCode.CONCURRENCY_CONFLICT,
          "Commercial lifecycle action replay payload conflicts with the previous request");
    }
    Company company = subscription.getCompany();
    Instant replayEffectiveAt =
        subscription.getLastCommercialActionEffectiveAt() == null
            ? effectiveAt
            : subscription.getLastCommercialActionEffectiveAt();
    return commercialStateResponse(
        company,
        subscription,
        commercialState,
        billingStatus,
        runtimeState,
        reason,
        replayEffectiveAt,
        subscription.getLastCommercialActionAuditEventId());
  }

  private String commercialActionFingerprint(
      String actionCode,
      SuperAdminBillingDtos.CommercialStateActionRequest request,
      Instant graceUntilAt) {
    String raw =
        String.join(
            "|",
            actionCode == null ? "" : actionCode,
            safeReason(request == null ? null : request.reason()),
            request == null || request.effectiveAt() == null
                ? "IMMEDIATE"
                : request.effectiveAt().toString(),
            graceUntilAt == null ? "" : graceUntilAt.toString());
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 digest is unavailable", ex);
    }
  }

  private SuperAdminBillingDtos.CommercialStateResponse applyCommercialState(
      SuperAdminBillingSubscription subscription,
      String actionCode,
      String commercialState,
      CompanyLifecycleState lifecycleState,
      TenantRuntimeEnforcementService.TenantRuntimeState runtimeState,
      String billingStatus,
      String auditReason,
      SuperAdminBillingDtos.CommercialStateActionRequest request,
      Instant effectiveAt,
      Instant graceUntilAt) {
    Company company = subscription.getCompany();
    String actionFingerprint = commercialActionFingerprint(actionCode, request, graceUntilAt);
    SuperAdminBillingDtos.CommercialStateResponse replay =
        replayCommercialActionIfIdentical(
            subscription,
            actionCode,
            actionFingerprint,
            commercialState,
            billingStatus,
            runtimeState,
            effectiveAt,
            safeReason(request == null ? null : request.reason()));
    if (replay != null) {
      return replay;
    }
    requireNoPendingTerminalAction(subscription, actionCode);
    String previousCommercialState = resolveCommercialState(company);
    String previousBillingStatus = companyBillingStatusOrManual(company);
    CompanyLifecycleState previousLifecycle =
        company.getLifecycleState() == null
            ? CompanyLifecycleState.ACTIVE
            : company.getLifecycleState();
    if ("GRACE".equals(actionCode)) {
      subscription.setGraceUntilAt(graceUntilAt);
    }
    if ("CANCEL".equals(actionCode)) {
      subscription.setStatus("CANCELED");
      subscription.setCanceledAt(effectiveAt);
    }
    if ("ARCHIVE".equals(actionCode)) {
      subscription.setStatus("ARCHIVED");
      subscription.setArchivedAt(effectiveAt);
    }
    if ("RESUME".equals(actionCode)
        && Set.of("CANCELED", "ARCHIVED").contains(subscription.getStatus())) {
      subscription.setStatus("ACTIVE");
    }
    company.setLifecycleState(lifecycleState);
    company.setLifecycleReason(commercialState);
    company.setCommercialBillingStatus(billingStatus);
    companyRepository.saveAndFlush(company);
    tenantRuntimeEnforcementService.updatePolicy(
        company.getCode(),
        runtimeState,
        commercialState,
        safeRuntimeLimit(company.getQuotaMaxConcurrentRequests()),
        safeRuntimeLimit(company.getQuotaMaxApiRequests()),
        safeRuntimeLimit(company.getQuotaMaxActiveUsers()),
        currentActor());
    Long auditEventId =
        auditRequired(
            company,
            auditReason,
            Map.ofEntries(
                Map.entry("oldCommercialState", previousCommercialState),
                Map.entry("newCommercialState", commercialState),
                Map.entry("oldBillingStatus", previousBillingStatus),
                Map.entry("newBillingStatus", billingStatus),
                Map.entry("oldLifecycleState", previousLifecycle.name()),
                Map.entry("newLifecycleState", lifecycleState.name()),
                Map.entry("runtimeState", runtimeState.name()),
                Map.entry("effectiveAt", effectiveAt.toString()),
                Map.entry("subscriptionId", String.valueOf(subscription.getId())),
                Map.entry("reasonDetail", safeReason(request == null ? null : request.reason()))));
    subscription.setPendingCommercialAction(null);
    subscription.setPendingCommercialEffectiveAt(null);
    subscription.setPendingCommercialReason(null);
    subscription.setLastCommercialAction(actionCode);
    subscription.setLastCommercialActionFingerprint(actionFingerprint);
    subscription.setLastCommercialActionEffectiveAt(effectiveAt);
    subscription.setLastCommercialActionAuditEventId(auditEventId);
    subscription.setAuditEventId(auditEventId);
    subscriptionRepository.saveAndFlush(subscription);
    return commercialStateResponse(
        company,
        subscription,
        commercialState,
        billingStatus,
        runtimeState,
        safeReason(request == null ? null : request.reason()),
        effectiveAt,
        auditEventId);
  }

  private SuperAdminBillingDtos.CommercialStateResponse commercialStateResponse(
      Company company,
      SuperAdminBillingSubscription subscription,
      String commercialState,
      String billingStatus,
      TenantRuntimeEnforcementService.TenantRuntimeState runtimeState,
      String reason,
      Instant effectiveAt,
      Long auditEventId) {
    SuperAdminBillingDtos.AccessPolicy policy = accessPolicy(commercialState, runtimeState);
    return new SuperAdminBillingDtos.CommercialStateResponse(
        company.getId(),
        company.getCode(),
        subscription.getId(),
        commercialState,
        billingStatus,
        company.getLifecycleState() == null
            ? CompanyLifecycleState.ACTIVE.name()
            : company.getLifecycleState().name(),
        runtimeState.name(),
        reason,
        effectiveAt,
        subscription.getGraceUntilAt(),
        subscription.getCanceledAt(),
        subscription.getArchivedAt(),
        policy.loginAllowed(),
        policy.safeReadsAllowed(),
        policy.writesAllowed(),
        policy.backgroundWorkAllowed(),
        !"ARCHIVED".equals(commercialState),
        auditEventId,
        accessMatrix());
  }

  private Map<String, SuperAdminBillingDtos.AccessPolicy> accessMatrix() {
    Map<String, SuperAdminBillingDtos.AccessPolicy> matrix = new LinkedHashMap<>();
    matrix.put(
        "GRACE",
        new SuperAdminBillingDtos.AccessPolicy(
            true, true, true, true, "ACTIVE", "resume or escalate after grace expiry"));
    matrix.put(
        "SUSPENDED_READ_ONLY",
        new SuperAdminBillingDtos.AccessPolicy(
            true, true, false, false, "HOLD", "resume or escalate to blocked"));
    matrix.put(
        "SUSPENDED_BLOCKED",
        new SuperAdminBillingDtos.AccessPolicy(
            false, false, false, false, "BLOCKED", "resume by Super Admin after resolution"));
    matrix.put(
        "CANCELED",
        new SuperAdminBillingDtos.AccessPolicy(
            false, false, false, false, "BLOCKED", "reactivation requires Super Admin resume"));
    matrix.put(
        "ARCHIVED",
        new SuperAdminBillingDtos.AccessPolicy(
            false, false, false, false, "BLOCKED", "history-only; no hard delete"));
    return matrix;
  }

  private SuperAdminBillingDtos.AccessPolicy accessPolicy(
      String commercialState, TenantRuntimeEnforcementService.TenantRuntimeState runtimeState) {
    return accessMatrix()
        .getOrDefault(
            commercialState,
            new SuperAdminBillingDtos.AccessPolicy(
                true, true, true, true, runtimeState.name(), "normal active access"));
  }

  private String resolveCommercialState(Company company) {
    if (company != null && StringUtils.hasText(company.getLifecycleReason())) {
      String status = company.getLifecycleReason().trim().toUpperCase(Locale.ROOT);
      if (Set.of(
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
              "SEED_FAILED")
          .contains(status)) {
        return status;
      }
    }
    CompanyLifecycleState lifecycle =
        company == null || company.getLifecycleState() == null
            ? CompanyLifecycleState.ACTIVE
            : company.getLifecycleState();
    return switch (lifecycle) {
      case ACTIVE -> "ACTIVE";
      case SUSPENDED -> "SUSPENDED_BLOCKED";
      case DEACTIVATED -> "ARCHIVED";
    };
  }

  private TenantRuntimeEnforcementService.TenantRuntimeState accessRuntimeState(
      String commercialState) {
    return switch (commercialState) {
      case "SUSPENDED_READ_ONLY" -> TenantRuntimeEnforcementService.TenantRuntimeState.HOLD;
      case "SUSPENDED_BLOCKED", "CANCELED", "ARCHIVED" ->
          TenantRuntimeEnforcementService.TenantRuntimeState.BLOCKED;
      default -> TenantRuntimeEnforcementService.TenantRuntimeState.ACTIVE;
    };
  }

  private Instant effectiveAtFor(
      String commercialState, SuperAdminBillingSubscription subscription, Instant fallback) {
    return switch (commercialState) {
      case "CANCELED" ->
          subscription.getCanceledAt() == null ? fallback : subscription.getCanceledAt();
      case "ARCHIVED" ->
          subscription.getArchivedAt() == null ? fallback : subscription.getArchivedAt();
      default -> fallback;
    };
  }

  private Instant resolveEffectiveAt(
      Company company, SuperAdminBillingDtos.CommercialStateActionRequest request) {
    return request != null && request.effectiveAt() != null
        ? request.effectiveAt()
        : CompanyTime.now(company);
  }

  private Integer safeRuntimeLimit(long value) {
    if (value <= 0L) {
      return null;
    }
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
  }

  private Company lockCompany(Long companyId) {
    return companyRepository
        .lockById(companyId)
        .orElseThrow(
            () ->
                new ApplicationException(ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Company not found"));
  }

  private SuperAdminBillingDtos.SubscriptionResponse toSubscriptionResponse(
      SuperAdminBillingSubscription subscription, String billingStatus) {
    Company company = subscription.getCompany();
    return new SuperAdminBillingDtos.SubscriptionResponse(
        subscription.getId(),
        company.getId(),
        company.getCode(),
        subscription.getPlanId(),
        subscription.getCustomPlanName(),
        subscription.getStatus(),
        billingStatus,
        subscription.getCadence(),
        subscription.getAmountMinorUnits(),
        subscription.getCurrency(),
        subscription.getCollectionMode(),
        subscription.getPeriodStartAt(),
        subscription.getPeriodEndAt(),
        subscription.getRenewalAt(),
        subscription.getDueAt(),
        subscription.getTrialStartAt(),
        subscription.getTrialEndAt(),
        subscription.getGraceUntilAt(),
        subscription.getCanceledAt(),
        subscription.getArchivedAt(),
        subscription.getExternalReference(),
        subscription.getAuditEventId());
  }

  private SuperAdminBillingDtos.LedgerEntryResponse toLedgerEntryResponse(
      SuperAdminBillingLedgerEntry entry) {
    return new SuperAdminBillingDtos.LedgerEntryResponse(
        entry.getId(),
        entry.getCompany().getId(),
        entry.getCompany().getCode(),
        entry.getSubscription().getId(),
        entry.getEntryType(),
        entry.getDirection(),
        entry.getAmountMinorUnits(),
        entry.getCurrency(),
        entry.getReason(),
        entry.getExternalReference(),
        entry.getIdempotencyKey(),
        entry.getBalanceBeforeMinorUnits(),
        entry.getBalanceAfterMinorUnits(),
        entry.getBillingStatusAfter(),
        entry.getCreatedAt(),
        entry.getAuditEventId());
  }

  private long balance(Long companyId) {
    Long balance = ledgerEntryRepository.balanceForCompany(companyId);
    return balance == null ? 0 : balance;
  }

  private String deriveBillingStatus(
      SuperAdminBillingSubscription subscription, long balance, Instant now) {
    if (subscription == null) {
      throw invalidState("Billing subscription is required");
    }
    String subscriptionStatus = subscription.getStatus();
    if ("ARCHIVED".equals(subscriptionStatus)) {
      return "ARCHIVED";
    }
    if ("CANCELED".equals(subscriptionStatus)) {
      return "CANCELED";
    }
    if ("TRIAL".equals(subscriptionStatus) && balance <= 0) {
      return "TRIAL";
    }
    if ("MANUAL".equals(subscriptionStatus) && balance <= 0) {
      return "MANUAL";
    }
    if (balance <= 0) {
      return "PAID";
    }
    if (subscription.getGraceUntilAt() != null
        && subscription.getDueAt() != null
        && !subscription.getDueAt().isAfter(now)
        && subscription.getGraceUntilAt().isAfter(now)) {
      return "GRACE";
    }
    if (subscription.getDueAt() != null && subscription.getDueAt().isBefore(now)) {
      return "OVERDUE";
    }
    return "DUE";
  }

  private String companyBillingStatusOrManual(Company company) {
    if (company != null && StringUtils.hasText(company.getCommercialBillingStatus())) {
      String status = company.getCommercialBillingStatus().trim().toUpperCase(Locale.ROOT);
      if (BILLING_STATUSES.contains(status)) {
        return status;
      }
    }
    return "MANUAL";
  }

  private boolean isIncludedInRecurringRevenue(SuperAdminBillingSubscription subscription) {
    if (subscription == null || subscription.getAmountMinorUnits() == null) {
      return false;
    }
    Instant now = CompanyTime.now(subscription.getCompany());
    if (subscription.getPeriodStartAt() == null || subscription.getPeriodStartAt().isAfter(now)) {
      return false;
    }
    if (subscription.getPeriodEndAt() != null && !subscription.getPeriodEndAt().isAfter(now)) {
      return false;
    }
    if (subscription.getCanceledAt() != null && !subscription.getCanceledAt().isAfter(now)) {
      return false;
    }
    if (subscription.getArchivedAt() != null && !subscription.getArchivedAt().isAfter(now)) {
      return false;
    }
    if ("ACTIVE".equals(subscription.getStatus())) {
      return true;
    }
    if ("MANUAL".equals(subscription.getStatus())) {
      return subscription.getAmountMinorUnits() > 0;
    }
    return ("CANCELED".equals(subscription.getStatus()) && subscription.getCanceledAt() != null)
        || ("ARCHIVED".equals(subscription.getStatus()) && subscription.getArchivedAt() != null);
  }

  private long mrrMinorUnits(SuperAdminBillingSubscription subscription) {
    if ("MONTHLY".equals(subscription.getCadence())) {
      return subscription.getAmountMinorUnits();
    }
    if ("ANNUAL".equals(subscription.getCadence())) {
      return BigDecimal.valueOf(subscription.getAmountMinorUnits())
          .divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP)
          .longValue();
    }
    return 0;
  }

  private SuperAdminBillingDtos.BillingPrivacy privacy() {
    return new SuperAdminBillingDtos.BillingPrivacy(
        true,
        List.of("subscription", "manualInvoices", "payments", "adjustments", "balance", "status"),
        List.of("tenant-private-business-records", "erp-operation-rows", "tax-or-file-documents"),
        "Super Admin billing exposes platform subscription/payment summaries only");
  }

  private Long auditRequired(Company company, String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new LinkedHashMap<>();
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
    if (auditLog == null || auditLog.getId() == null) {
      throw invalidState("Billing audit event was not persisted");
    }
    return auditLog.getId();
  }

  private String currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication == null || !StringUtils.hasText(authentication.getName())
        ? "system"
        : authentication.getName();
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

  private String normalizeToken(String value, String fieldName) {
    return requireText(value, fieldName).toUpperCase(Locale.ROOT);
  }

  private String requireAllowed(String value, String fieldName, Set<String> allowed) {
    String normalized = normalizeToken(value, fieldName);
    if (!allowed.contains(normalized)) {
      throw invalidInput(fieldName + " must be one of " + allowed);
    }
    return normalized;
  }

  private String requireCurrency(String value) {
    String currency = normalizeToken(value, "currency");
    if (currency.length() != 3 || !currency.chars().allMatch(Character::isLetter)) {
      throw invalidInput("currency must be a three-letter ISO code");
    }
    return currency;
  }

  private String requireText(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw invalidInput(fieldName + " is required");
    }
    return value.trim();
  }

  private Instant requireInstant(Instant value, String fieldName) {
    if (value == null) {
      throw invalidInput(fieldName + " is required");
    }
    return value;
  }

  private long requireNonNegative(Long value, String fieldName) {
    if (value == null || value < 0) {
      throw invalidInput(fieldName + " must be greater than or equal to 0");
    }
    return value;
  }

  private long requirePositive(Long value, String fieldName) {
    if (value == null || value <= 0) {
      throw invalidInput(fieldName + " must be greater than 0");
    }
    return value;
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String safeReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      return "not-provided";
    }
    String trimmed = reason.trim();
    return trimmed.length() <= 180 ? trimmed : trimmed.substring(0, 180);
  }

  private ApplicationException invalidInput(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, message);
  }

  private ApplicationException invalidState(String message) {
    return new ApplicationException(ErrorCode.VALIDATION_INVALID_STATE, message);
  }

  private long safeAdd(long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException ex) {
      return right >= 0 ? Long.MAX_VALUE : Long.MIN_VALUE;
    }
  }

  private long safeMultiply(long left, long right) {
    try {
      return Math.multiplyExact(left, right);
    } catch (ArithmeticException ex) {
      return Long.MAX_VALUE;
    }
  }

  public record LedgerMutationResult(
      SuperAdminBillingDtos.LedgerEntryResponse response, boolean replay) {}

  private static final class MutableMetrics {
    private final String currency;
    private long mrrMinorUnits;
    private long arrMinorUnits;
    private int activeSubscriptionCount;
    private int excludedSubscriptionCount;

    private MutableMetrics(String currency) {
      this.currency = currency;
    }

    private SuperAdminBillingDtos.CurrencyMetrics toDto() {
      return new SuperAdminBillingDtos.CurrencyMetrics(
          currency,
          mrrMinorUnits,
          arrMinorUnits,
          activeSubscriptionCount,
          excludedSubscriptionCount,
          "HALF_UP_TO_MINOR_UNIT",
          "BILLABLE_STATUS_WITHIN_EFFECTIVE_WINDOW");
    }
  }
}
