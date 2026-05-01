package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;

import jakarta.persistence.*;

@Entity
@Table(name = "iam_devices")
public class IamDevice extends VersionedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private IamAccount account;

  @Column(name = "public_id", nullable = false)
  private UUID publicId;

  @Column(name = "device_label")
  private String deviceLabel;

  @Column(name = "user_agent_hash", length = 128)
  private String userAgentHash;

  @Column(name = "ip_address_hash", length = 128)
  private String ipAddressHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  protected IamDevice() {}

  public Long getId() {
    return id;
  }

  public IamAccount getAccount() {
    return account;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public String getDeviceLabel() {
    return deviceLabel;
  }

  public String getUserAgentHash() {
    return userAgentHash;
  }

  public String getIpAddressHash() {
    return ipAddressHash;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }
}
