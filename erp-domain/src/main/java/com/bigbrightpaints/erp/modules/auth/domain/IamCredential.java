package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "iam_credentials")
public class IamCredential extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "password_changed_at")
  private Instant passwordChangedAt;

  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected IamCredential() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Instant getPasswordChangedAt() {
    return passwordChangedAt;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }
}
