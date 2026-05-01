package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.*;

@Entity
@Table(
    name = "iam_accounts",
    uniqueConstraints = {
      @UniqueConstraint(name = "uq_iam_accounts_user_id", columnNames = "user_id"),
      @UniqueConstraint(name = "uq_iam_accounts_public_id", columnNames = "public_id"),
      @UniqueConstraint(
          name = "uq_iam_accounts_email_scope",
          columnNames = {"email", "auth_scope_code"})
    })
public class IamAccount extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @Column(name = "account_type", nullable = false, length = 24)
  private String accountType;

  @Column(name = "auth_scope_code", nullable = false, length = 64)
  private String authScopeCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false, length = 32)
  private String status = "ACTIVE";

  @Column(name = "locked_until")
  private Instant lockedUntil;

  @Column(name = "failed_login_attempts", nullable = false)
  private int failedLoginAttempts;

  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected IamAccount() {}

  public IamAccount(UserAccount user, String accountType, String status) {
    this.user = user;
    this.publicId = user.getPublicId();
    this.accountType = accountType;
    this.authScopeCode = user.getAuthScopeCode();
    this.company = user.getCompany();
    this.email = user.getEmail();
    this.status = status;
    this.lockedUntil = user.getLockedUntil();
    this.failedLoginAttempts = user.getFailedLoginAttempts();
    this.mustChangePassword = user.isMustChangePassword();
    this.createdAt = user.getCreatedAt();
    this.updatedAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public String getAccountType() {
    return accountType;
  }

  public String getAuthScopeCode() {
    return authScopeCode;
  }

  public Company getCompany() {
    return company;
  }

  public String getEmail() {
    return email;
  }

  public String getStatus() {
    return status;
  }

  public Instant getLockedUntil() {
    return lockedUntil;
  }

  public int getFailedLoginAttempts() {
    return failedLoginAttempts;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
