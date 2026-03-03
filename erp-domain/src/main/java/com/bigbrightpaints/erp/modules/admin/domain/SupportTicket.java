package com.bigbrightpaints.erp.modules.admin.domain;

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
import java.time.Instant;
import java.util.UUID;

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

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_notification_sent_at")
    private Instant resolvedNotificationSentAt;

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
