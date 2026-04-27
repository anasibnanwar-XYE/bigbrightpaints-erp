package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "iam_sessions",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_iam_sessions_public_id", columnNames = "public_id"),
      @UniqueConstraint(
          name = "uq_iam_sessions_refresh_token_digest",
          columnNames = "refresh_token_digest")
    })
public class IamSession extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "device_id")
  private IamDevice device;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @Column(name = "refresh_token_digest", nullable = false, length = 64)
  private String refreshTokenDigest;

  @Column(name = "auth_scope_code", nullable = false, length = 64)
  private String authScopeCode;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "consumed_at")
  private Instant consumedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revoked_reason")
  private String revokedReason;

  protected IamSession() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public IamDevice getDevice() {
    return device;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public String getRefreshTokenDigest() {
    return refreshTokenDigest;
  }

  public String getAuthScopeCode() {
    return authScopeCode;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }
}
