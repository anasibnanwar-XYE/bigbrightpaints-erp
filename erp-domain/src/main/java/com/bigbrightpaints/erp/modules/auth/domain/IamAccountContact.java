package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "iam_account_contacts")
public class IamAccountContact extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @Column(name = "primary_email", nullable = false)
  private String primaryEmail;

  @Column(name = "secondary_email")
  private String secondaryEmail;

  @Column(name = "phone_secondary", length = 64)
  private String phoneSecondary;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected IamAccountContact() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public String getPrimaryEmail() {
    return primaryEmail;
  }

  public String getSecondaryEmail() {
    return secondaryEmail;
  }

  public String getPhoneSecondary() {
    return phoneSecondary;
  }
}
