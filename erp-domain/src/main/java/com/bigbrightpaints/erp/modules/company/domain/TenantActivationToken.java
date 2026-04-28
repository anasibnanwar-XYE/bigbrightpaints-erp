package com.bigbrightpaints.erp.modules.company.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_activation_tokens",
    indexes = {
      @Index(name = "idx_tenant_activation_tokens_digest", columnList = "token_digest"),
      @Index(
          name = "idx_tenant_activation_tokens_company_owner",
          columnList = "company_id, owner_user_id, status, created_at")
    })
public class TenantActivationToken extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private Company company;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_user_id", nullable = false)
  private UserAccount ownerUser;

  @Column(name = "token_digest", nullable = false, length = 64)
  private String tokenDigest;

  @Column(nullable = false, length = 32)
  private String status = "ISSUED";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "expired_at")
  private Instant expiredAt;

  protected TenantActivationToken() {}

  private TenantActivationToken(
      Company company, UserAccount ownerUser, String tokenDigest, Instant now, Instant expiresAt) {
    this.company = company;
    this.ownerUser = ownerUser;
    this.tokenDigest = tokenDigest;
    this.createdAt = now;
    this.expiresAt = expiresAt;
    this.status = "ISSUED";
  }

  public static TenantActivationToken digestOnly(
      Company company, UserAccount ownerUser, String tokenDigest, Instant now, Instant expiresAt) {
    return new TenantActivationToken(company, ownerUser, tokenDigest, now, expiresAt);
  }

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public UserAccount getOwnerUser() {
    return ownerUser;
  }

  public String getTokenDigest() {
    return tokenDigest;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getExpiredAt() {
    return expiredAt;
  }

  public void markSent(Instant sentAt) {
    if (sentAt == null) {
      return;
    }
    this.sentAt = sentAt;
    this.status = "SENT";
  }

  public void markUsed(Instant usedAt) {
    if (usedAt == null) {
      return;
    }
    this.usedAt = usedAt;
    this.status = "USED";
  }

  public void markExpired(Instant expiredAt) {
    if (expiredAt == null) {
      return;
    }
    this.expiredAt = expiredAt;
    this.status = "EXPIRED";
  }

  public void markSuperseded(Instant supersededAt) {
    if (supersededAt == null) {
      return;
    }
    this.expiredAt = supersededAt;
    this.status = "SUPERSEDED";
  }
}
