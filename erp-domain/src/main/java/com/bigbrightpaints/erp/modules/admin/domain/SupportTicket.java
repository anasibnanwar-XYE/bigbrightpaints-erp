package com.bigbrightpaints.erp.modules.admin.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_tickets")
public class SupportTicket extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false)
  private SupportTicketCategory category;

  @Column(name = "subject", nullable = false, length = 255)
  private String subject;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 32)
  private SupportTicketPriority priority = SupportTicketPriority.NORMAL;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SupportTicketStatus status;

  @Column(name = "github_issue_number")
  private Long githubIssueNumber;

  @Column(name = "github_issue_url", length = 512)
  private String githubIssueUrl;

  @Column(name = "github_issue_state", length = 32)
  private String githubIssueState;

  @Column(name = "github_synced_at")
  private Instant githubSyncedAt;

  @Column(name = "github_last_error", columnDefinition = "TEXT")
  private String githubLastError;

  @Column(name = "github_last_sync_at")
  private Instant githubLastSyncAt;

  @Column(name = "bug_reproduction_steps", columnDefinition = "TEXT")
  private String bugReproductionSteps;

  @Column(name = "bug_environment", length = 64)
  private String bugEnvironment;

  @Column(name = "bug_release", length = 128)
  private String bugRelease;

  @Column(name = "bug_trace_id", length = 128)
  private String bugTraceId;

  @Column(name = "bug_metadata_json", columnDefinition = "TEXT")
  private String bugMetadataJson;

  @Column(name = "sentry_issue_id", length = 128)
  private String sentryIssueId;

  @Column(name = "sentry_issue_url", length = 512)
  private String sentryIssueUrl;

  @Column(name = "sentry_issue_status", length = 64)
  private String sentryIssueStatus;

  @Column(name = "sentry_linked_at")
  private Instant sentryLinkedAt;

  @Column(name = "sentry_synced_at")
  private Instant sentrySyncedAt;

  @Column(name = "sentry_last_sync_at")
  private Instant sentryLastSyncAt;

  @Column(name = "sentry_last_error", columnDefinition = "TEXT")
  private String sentryLastError;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "resolved_notification_sent_at")
  private Instant resolvedNotificationSentAt;

  @Column(name = "sla_policy_id", length = 64)
  private String slaPolicyId;

  @Column(name = "sla_support_tier", length = 32)
  private String slaSupportTier;

  @Column(name = "first_response_due_at")
  private Instant firstResponseDueAt;

  @Column(name = "resolution_due_at")
  private Instant resolutionDueAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "sla_status", nullable = false, length = 32)
  private SupportTicketSlaStatus slaStatus = SupportTicketSlaStatus.PENDING;

  @Column(name = "first_responded_at")
  private Instant firstRespondedAt;

  @Column(name = "breached_at")
  private Instant breachedAt;

  @Column(name = "converted_to_incident_at")
  private Instant convertedToIncidentAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  public void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (status == null) {
      status = SupportTicketStatus.OPEN;
    }
    if (priority == null) {
      priority = SupportTicketPriority.NORMAL;
    }
    if (slaStatus == null) {
      slaStatus =
          category == SupportTicketCategory.FEATURE_REQUEST
              ? SupportTicketSlaStatus.NOT_APPLICABLE
              : SupportTicketSlaStatus.PENDING;
    }
    Instant now = CompanyTime.now(company);
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = CompanyTime.now(company);
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public SupportTicketCategory getCategory() {
    return category;
  }

  public void setCategory(SupportTicketCategory category) {
    this.category = category;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public SupportTicketPriority getPriority() {
    return priority;
  }

  public void setPriority(SupportTicketPriority priority) {
    this.priority = priority == null ? SupportTicketPriority.NORMAL : priority;
  }

  public SupportTicketStatus getStatus() {
    return status;
  }

  public void setStatus(SupportTicketStatus status) {
    this.status = status;
  }

  public Long getGithubIssueNumber() {
    return githubIssueNumber;
  }

  public void setGithubIssueNumber(Long githubIssueNumber) {
    this.githubIssueNumber = githubIssueNumber;
  }

  public String getGithubIssueUrl() {
    return githubIssueUrl;
  }

  public void setGithubIssueUrl(String githubIssueUrl) {
    this.githubIssueUrl = githubIssueUrl;
  }

  public String getGithubIssueState() {
    return githubIssueState;
  }

  public void setGithubIssueState(String githubIssueState) {
    this.githubIssueState = githubIssueState;
  }

  public Instant getGithubSyncedAt() {
    return githubSyncedAt;
  }

  public void setGithubSyncedAt(Instant githubSyncedAt) {
    this.githubSyncedAt = githubSyncedAt;
  }

  public String getGithubLastError() {
    return githubLastError;
  }

  public void setGithubLastError(String githubLastError) {
    this.githubLastError = githubLastError;
  }

  public Instant getGithubLastSyncAt() {
    return githubLastSyncAt;
  }

  public void setGithubLastSyncAt(Instant githubLastSyncAt) {
    this.githubLastSyncAt = githubLastSyncAt;
  }

  public String getBugReproductionSteps() {
    return bugReproductionSteps;
  }

  public void setBugReproductionSteps(String bugReproductionSteps) {
    this.bugReproductionSteps = bugReproductionSteps;
  }

  public String getBugEnvironment() {
    return bugEnvironment;
  }

  public void setBugEnvironment(String bugEnvironment) {
    this.bugEnvironment = bugEnvironment;
  }

  public String getBugRelease() {
    return bugRelease;
  }

  public void setBugRelease(String bugRelease) {
    this.bugRelease = bugRelease;
  }

  public String getBugTraceId() {
    return bugTraceId;
  }

  public void setBugTraceId(String bugTraceId) {
    this.bugTraceId = bugTraceId;
  }

  public String getBugMetadataJson() {
    return bugMetadataJson;
  }

  public void setBugMetadataJson(String bugMetadataJson) {
    this.bugMetadataJson = bugMetadataJson;
  }

  public String getSentryIssueId() {
    return sentryIssueId;
  }

  public void setSentryIssueId(String sentryIssueId) {
    this.sentryIssueId = sentryIssueId;
  }

  public String getSentryIssueUrl() {
    return sentryIssueUrl;
  }

  public void setSentryIssueUrl(String sentryIssueUrl) {
    this.sentryIssueUrl = sentryIssueUrl;
  }

  public String getSentryIssueStatus() {
    return sentryIssueStatus;
  }

  public void setSentryIssueStatus(String sentryIssueStatus) {
    this.sentryIssueStatus = sentryIssueStatus;
  }

  public Instant getSentryLinkedAt() {
    return sentryLinkedAt;
  }

  public void setSentryLinkedAt(Instant sentryLinkedAt) {
    this.sentryLinkedAt = sentryLinkedAt;
  }

  public Instant getSentrySyncedAt() {
    return sentrySyncedAt;
  }

  public void setSentrySyncedAt(Instant sentrySyncedAt) {
    this.sentrySyncedAt = sentrySyncedAt;
  }

  public Instant getSentryLastSyncAt() {
    return sentryLastSyncAt;
  }

  public void setSentryLastSyncAt(Instant sentryLastSyncAt) {
    this.sentryLastSyncAt = sentryLastSyncAt;
  }

  public String getSentryLastError() {
    return sentryLastError;
  }

  public void setSentryLastError(String sentryLastError) {
    this.sentryLastError = sentryLastError;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public Instant getResolvedNotificationSentAt() {
    return resolvedNotificationSentAt;
  }

  public void setResolvedNotificationSentAt(Instant resolvedNotificationSentAt) {
    this.resolvedNotificationSentAt = resolvedNotificationSentAt;
  }

  public String getSlaPolicyId() {
    return slaPolicyId;
  }

  public void setSlaPolicyId(String slaPolicyId) {
    this.slaPolicyId = slaPolicyId;
  }

  public String getSlaSupportTier() {
    return slaSupportTier;
  }

  public void setSlaSupportTier(String slaSupportTier) {
    this.slaSupportTier = slaSupportTier;
  }

  public Instant getFirstResponseDueAt() {
    return firstResponseDueAt;
  }

  public void setFirstResponseDueAt(Instant firstResponseDueAt) {
    this.firstResponseDueAt = firstResponseDueAt;
  }

  public Instant getResolutionDueAt() {
    return resolutionDueAt;
  }

  public void setResolutionDueAt(Instant resolutionDueAt) {
    this.resolutionDueAt = resolutionDueAt;
  }

  public SupportTicketSlaStatus getSlaStatus() {
    return slaStatus;
  }

  public void setSlaStatus(SupportTicketSlaStatus slaStatus) {
    this.slaStatus = slaStatus;
  }

  public Instant getFirstRespondedAt() {
    return firstRespondedAt;
  }

  public void setFirstRespondedAt(Instant firstRespondedAt) {
    this.firstRespondedAt = firstRespondedAt;
  }

  public Instant getBreachedAt() {
    return breachedAt;
  }

  public void setBreachedAt(Instant breachedAt) {
    this.breachedAt = breachedAt;
  }

  public Instant getConvertedToIncidentAt() {
    return convertedToIncidentAt;
  }

  public void setConvertedToIncidentAt(Instant convertedToIncidentAt) {
    this.convertedToIncidentAt = convertedToIncidentAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
