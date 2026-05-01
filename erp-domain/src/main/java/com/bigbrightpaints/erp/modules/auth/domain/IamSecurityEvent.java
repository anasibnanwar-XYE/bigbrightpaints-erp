package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.*;

@Entity
@Table(name = "iam_security_events")
public class IamSecurityEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id")
  private IamAccount account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_account_id")
  private IamAccount actorAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(name = "auth_scope_code", length = 64)
  private String authScopeCode;

  @Column(name = "event_type", nullable = false, length = 96)
  private String eventType;

  @Column(nullable = false, length = 32)
  private String outcome;

  @Column private String reason;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, String> metadata = new LinkedHashMap<>();

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt = Instant.now();

  protected IamSecurityEvent() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public IamAccount getActorAccount() {
    return actorAccount;
  }

  public Company getCompany() {
    return company;
  }

  public String getAuthScopeCode() {
    return authScopeCode;
  }

  public String getEventType() {
    return eventType;
  }

  public String getOutcome() {
    return outcome;
  }

  public String getReason() {
    return reason;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
