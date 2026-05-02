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
import jakarta.persistence.Table;

@Entity
@Table(name = "support_ticket_messages")
public class SupportTicketMessage extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "ticket_id")
  private SupportTicket ticket;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(name = "author_user_id")
  private Long authorUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "author_role", nullable = false, length = 32)
  private SupportTicketMessageAuthorRole authorRole;

  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", nullable = false, length = 32)
  private SupportTicketMessageVisibility visibility;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "audit_event_id")
  private Long auditEventId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (publicId == null) {
      publicId = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = CompanyTime.now(company);
    }
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public SupportTicket getTicket() {
    return ticket;
  }

  public void setTicket(SupportTicket ticket) {
    this.ticket = ticket;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public Long getAuthorUserId() {
    return authorUserId;
  }

  public void setAuthorUserId(Long authorUserId) {
    this.authorUserId = authorUserId;
  }

  public SupportTicketMessageAuthorRole getAuthorRole() {
    return authorRole;
  }

  public void setAuthorRole(SupportTicketMessageAuthorRole authorRole) {
    this.authorRole = authorRole;
  }

  public SupportTicketMessageVisibility getVisibility() {
    return visibility;
  }

  public void setVisibility(SupportTicketMessageVisibility visibility) {
    this.visibility = visibility;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Long getAuditEventId() {
    return auditEventId;
  }

  public void setAuditEventId(Long auditEventId) {
    this.auditEventId = auditEventId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
