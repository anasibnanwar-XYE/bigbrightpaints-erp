package com.bigbrightpaints.erp.modules.admin.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "support_ticket_timeline_entries")
public class SupportTicketTimelineEntry extends VersionedEntity {

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

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(name = "from_status", length = 32)
  private String fromStatus;

  @Column(name = "to_status", length = 32)
  private String toStatus;

  @Column(name = "from_category", length = 32)
  private String fromCategory;

  @Column(name = "to_category", length = 32)
  private String toCategory;

  @Column(name = "note", length = 512)
  private String note;

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

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getFromStatus() {
    return fromStatus;
  }

  public void setFromStatus(String fromStatus) {
    this.fromStatus = fromStatus;
  }

  public String getToStatus() {
    return toStatus;
  }

  public void setToStatus(String toStatus) {
    this.toStatus = toStatus;
  }

  public String getFromCategory() {
    return fromCategory;
  }

  public void setFromCategory(String fromCategory) {
    this.fromCategory = fromCategory;
  }

  public String getToCategory() {
    return toCategory;
  }

  public void setToCategory(String toCategory) {
    this.toCategory = toCategory;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
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
