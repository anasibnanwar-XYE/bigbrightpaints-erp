package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "iam_account_profiles")
public class IamAccountProfile extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Column(name = "preferred_name")
  private String preferredName;

  @Column(name = "profile_picture_url", length = 2048)
  private String profilePictureUrl;

  @Column(name = "job_title")
  private String jobTitle;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected IamAccountProfile() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPreferredName() {
    return preferredName;
  }

  public String getProfilePictureUrl() {
    return profilePictureUrl;
  }

  public String getJobTitle() {
    return jobTitle;
  }
}
