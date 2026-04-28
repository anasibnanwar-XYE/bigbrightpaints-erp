package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SuperAdminTenantDetailDto(
    Long companyId,
    String companyCode,
    String companyName,
    String timezone,
    String stateCode,
    String lifecycleState,
    String lifecycleReason,
    Set<String> enabledModules,
    Onboarding onboarding,
    MainAdminSummaryDto mainAdmin,
    Limits limits,
    Usage usage,
    SupportContext supportContext,
    List<SupportTimelineEvent> supportTimeline,
    AvailableActions availableActions,
    String status,
    Overview overview,
    PlanSummary plan,
    BillingSummary billing,
    SupportSummary support,
    BugsSummary bugs,
    AuditSummary audit,
    SettingsSummary settings) {

  public SuperAdminTenantDetailDto(
      Long companyId,
      String companyCode,
      String companyName,
      String timezone,
      String stateCode,
      String lifecycleState,
      String lifecycleReason,
      Set<String> enabledModules,
      Onboarding onboarding,
      MainAdminSummaryDto mainAdmin,
      Limits limits,
      Usage usage,
      SupportContext supportContext,
      List<SupportTimelineEvent> supportTimeline,
      AvailableActions availableActions) {
    this(
        companyId,
        companyCode,
        companyName,
        timezone,
        stateCode,
        lifecycleState,
        lifecycleReason,
        enabledModules,
        onboarding,
        mainAdmin,
        limits,
        usage,
        supportContext,
        supportTimeline,
        availableActions,
        lifecycleState,
        new Overview(
            companyId,
            companyCode,
            companyName,
            timezone,
            stateCode,
            lifecycleState,
            lifecycleState,
            "MANUAL",
            new SuperAdminTenantSummaryDto.HealthSummary("UNKNOWN", 0, "Health summary pending"),
            mainAdmin,
            usage == null ? null : usage.lastActivityAt(),
            new TabState("AVAILABLE", "Overview summary is available")),
        new PlanSummary(
            "TRIAL",
            "Trial",
            "STANDARD",
            limits,
            new TabState("AVAILABLE", "Plan limits summary is available")),
        new BillingSummary(
            "MANUAL", 0, "INR", null, new TabState("EMPTY", "No billing records yet")),
        new SupportSummary(
            supportContext == null ? Set.of() : supportContext.supportTags(),
            supportTimeline == null ? 0 : supportTimeline.size(),
            new TabState("AVAILABLE", "Support summary is available")),
        new BugsSummary(0, 0, new TabState("EMPTY", "No bug reports yet")),
        new AuditSummary(
            supportTimeline == null ? 0 : supportTimeline.size(),
            usage == null ? null : usage.lastActivityAt(),
            new TabState("AVAILABLE", "Audit summary is available")),
        new SettingsSummary(
            timezone, enabledModules, new TabState("AVAILABLE", "Settings summary is available")));
  }

  public record Onboarding(
      String templateCode,
      String adminEmail,
      Long adminUserId,
      boolean tenantAdminProvisioned,
      Instant completedAt) {}

  public record Limits(
      long quotaMaxActiveUsers,
      long quotaMaxApiRequests,
      long quotaMaxStorageBytes,
      long quotaMaxConcurrentRequests,
      boolean quotaSoftLimitEnabled,
      boolean quotaHardLimitEnabled) {}

  public record Usage(
      long activeUserCount,
      long apiActivityCount,
      long apiErrorCount,
      long apiErrorRateInBasisPoints,
      long auditStorageBytes,
      long currentConcurrentRequests,
      Instant lastActivityAt) {}

  public record SupportContext(String supportNotes, Set<String> supportTags) {}

  public record SupportTimelineEvent(
      String category, String title, String message, String actor, Instant occurredAt) {}

  public record AvailableActions(
      boolean canUpdateLifecycle,
      boolean canUpdateLimits,
      boolean canUpdateModules,
      boolean canIssueWarnings,
      boolean canManageActivation,
      boolean canForceLogout,
      boolean canReplaceMainAdmin,
      boolean canRequestAdminEmailChange) {}

  public record TabState(String state, String message) {}

  public record Overview(
      Long companyId,
      String companyCode,
      String companyName,
      String timezone,
      String stateCode,
      String status,
      String lifecycleState,
      String billingStatus,
      SuperAdminTenantSummaryDto.HealthSummary health,
      MainAdminSummaryDto mainAdmin,
      Instant lastActivityAt,
      TabState tabState) {}

  public record PlanSummary(
      String planId, String displayName, String supportTier, Limits limits, TabState tabState) {}

  public record BillingSummary(
      String billingStatus,
      long balanceDueMinorUnits,
      String currency,
      Instant trialEndsAt,
      TabState tabState) {}

  public record SupportSummary(Set<String> tags, int eventCount, TabState tabState) {}

  public record BugsSummary(int openCount, int criticalCount, TabState tabState) {}

  public record AuditSummary(int recentEventCount, Instant lastActivityAt, TabState tabState) {}

  public record SettingsSummary(String timezone, Set<String> enabledModules, TabState tabState) {}
}
