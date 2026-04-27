package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "iam_mfa_factors",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_iam_mfa_factors_account_type",
          columnNames = {"account_id", "factor_type"})
    })
public class IamMfaFactor extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @Column(name = "factor_type", nullable = false, length = 32)
  private String factorType;

  @Column(name = "encrypted_secret", nullable = false, columnDefinition = "text")
  private String encryptedSecret;

  @Column(nullable = false, length = 32)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "activated_at")
  private Instant activatedAt;

  @Column(name = "disabled_at")
  private Instant disabledAt;

  protected IamMfaFactor() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public String getFactorType() {
    return factorType;
  }

  public String getEncryptedSecret() {
    return encryptedSecret;
  }

  public String getStatus() {
    return status;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public Instant getDisabledAt() {
    return disabledAt;
  }
}
