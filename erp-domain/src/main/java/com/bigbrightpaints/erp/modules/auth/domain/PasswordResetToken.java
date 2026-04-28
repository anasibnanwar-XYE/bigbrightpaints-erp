package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
      @Index(name = "idx_password_reset_tokens_token_digest", columnList = "token_digest")
    })
public class PasswordResetToken extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "token_digest", nullable = false, length = 64)
  private String tokenDigest;

  @Column(name = "digest_algorithm", nullable = false, length = 32)
  private String digestAlgorithm;

  @Column(name = "digest_version", nullable = false)
  private Integer digestVersion;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "delivered_at")
  private Instant deliveredAt;

  protected PasswordResetToken() {}

  private PasswordResetToken(
      UserAccount user,
      String tokenDigest,
      String digestAlgorithm,
      int digestVersion,
      Instant expiresAt) {
    this.user = user;
    this.tokenDigest = tokenDigest;
    this.digestAlgorithm = digestAlgorithm;
    this.digestVersion = digestVersion;
    this.expiresAt = expiresAt;
    this.createdAt = Instant.now();
  }

  public static PasswordResetToken digestOnly(
      UserAccount user,
      String tokenDigest,
      String digestAlgorithm,
      int digestVersion,
      Instant expiresAt) {
    return new PasswordResetToken(user, tokenDigest, digestAlgorithm, digestVersion, expiresAt);
  }

  public Long getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public String getTokenDigest() {
    return tokenDigest;
  }

  public String getDigestAlgorithm() {
    return digestAlgorithm;
  }

  public Integer getDigestVersion() {
    return digestVersion;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getDeliveredAt() {
    return deliveredAt;
  }

  public void markUsed() {
    this.usedAt = Instant.now();
  }

  public void markDelivered(Instant deliveredAt) {
    if (deliveredAt == null) {
      return;
    }
    this.deliveredAt = deliveredAt;
  }

  public boolean isExpired(Instant now) {
    return expiresAt.isBefore(now);
  }

  public boolean isUsed() {
    return usedAt != null;
  }
}
