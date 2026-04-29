package com.bigbrightpaints.erp.modules.company.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
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

  public SuperAdminBillingService(
      CompanyRepository companyRepository,
      SuperAdminBillingSubscriptionRepository subscriptionRepository,
      SuperAdminBillingLedgerEntryRepository ledgerEntryRepository,
      AuditService auditService) {
    this.companyRepository = companyRepository;
    this.subscriptionRepository = subscriptionRepository;
    this.ledgerEntryRepository = ledgerEntryRepository;
    this.auditService = auditService;
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

  @Transactional(readOnly = true)
  public SuperAdminBillingDtos.SubscriptionResponse getSubscription(Long companyId) {
    SuperAdminBillingSubscription subscription = latestSubscription(companyId);
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

  @Transactional(readOnly = true)
  public SuperAdminBillingDtos.BillingStatusSummary billingSummaryFor(Company company) {
    if (company == null || company.getId() == null) {
      throw invalidInput("Company is required for billing summary");
    }
    return subscriptionRepository
        .findTopByCompanyIdOrderByCreatedAtDescIdDesc(company.getId())
        .map(
            subscription -> {
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

  @Transactional(readOnly = true)
  public Map<String, SuperAdminBillingDtos.CurrencyMetrics> getBillingMetrics() {
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

  public long dashboardMrrMinorUnits() {
    return getBillingMetrics().values().stream()
        .mapToLong(SuperAdminBillingDtos.CurrencyMetrics::mrrMinorUnits)
        .sum();
  }

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
    var existing =
        ledgerEntryRepository.findByCompanyIdAndIdempotencyKey(companyId, idempotencyKey);
    if (existing.isPresent()) {
      return new LedgerMutationResult(toLedgerEntryResponse(existing.get()), true);
    }
    long amount = requirePositive(request.amountMinorUnits(), "amountMinorUnits");
    String currency = requireCurrency(request.currency());
    if (!currency.equals(subscription.getCurrency())) {
      throw invalidInput("Ledger currency must match the active subscription currency");
    }
    String reason = requireText(request.reason(), "reason");
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
    entry.setExternalReference(trimToNull(request.externalReference()));
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
    return "ACTIVE".equals(subscription.getStatus())
        || ("MANUAL".equals(subscription.getStatus()) && subscription.getAmountMinorUnits() > 0);
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
          "ACTIVE_AND_BILLABLE_MANUAL_ONLY");
    }
  }
}
