package com.bigbrightpaints.erp.modules.auth.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "blacklisted_tokens", indexes = {
    @Index(name = "idx_blacklisted_tokens_token_id", columnList = "token_id"),
    @Index(name = "idx_blacklisted_tokens_user_id", columnList = "user_id"),
    @Index(name = "idx_blacklisted_tokens_expires_at", columnList = "expires_at")
})
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "blacklisted_at", nullable = false)
    private Instant blacklistedAt;

    @Column(name = "reason")
    private String reason;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String tokenId, Instant expiresAt, String userId, String reason) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.reason = reason;
        this.blacklistedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getBlacklistedAt() {
        return blacklistedAt;
    }

    public void setBlacklistedAt(Instant blacklistedAt) {
        this.blacklistedAt = blacklistedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }
}
