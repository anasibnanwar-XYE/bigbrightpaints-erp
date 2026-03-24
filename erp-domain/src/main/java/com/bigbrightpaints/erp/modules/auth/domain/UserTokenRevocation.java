package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(
    name = "user_token_revocations",
    indexes = {@Index(name = "idx_user_token_revocations_user_id", columnList = "user_id")})
public class UserTokenRevocation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private String userId;

  @Column(name = "revoked_at", nullable = false)
  private Instant revokedAt;

  @Column(name = "reason")
  private String reason;

  public UserTokenRevocation() {}

  public UserTokenRevocation(String userId, String reason) {
    this.userId = userId;
    this.revokedAt = Instant.now();
    this.reason = reason;
  }

  public Long getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
