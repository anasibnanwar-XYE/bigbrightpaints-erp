package com.bigbrightpaints.erp.modules.auth.service;

import com.bigbrightpaints.erp.modules.auth.domain.RefreshToken;
import com.bigbrightpaints.erp.modules.auth.domain.RefreshTokenRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenServiceIT extends AbstractIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void clearTokens() {
        refreshTokenRepository.deleteAll();
    }

    @Test
    void issue_persists_and_consume_removes() {
        Instant expiresAt = Instant.now().plusSeconds(300);
        String token = refreshTokenService.issue("user@example.com", expiresAt);

        assertThat(refreshTokenRepository.findByToken(token)).isPresent();

        RefreshTokenService.TokenRecord record = refreshTokenService.consume(token).orElseThrow();
        assertThat(record.userEmail()).isEqualTo("user@example.com");
        assertThat(record.expiresAt()).isAfterOrEqualTo(expiresAt.minusSeconds(1));
        assertThat(record.expiresAt()).isBeforeOrEqualTo(expiresAt.plusSeconds(1));
        assertThat(refreshTokenRepository.findByToken(token)).isEmpty();
    }

    @Test
    void consume_expired_token_returns_empty_and_deletes() {
        Instant issuedAt = Instant.now().minusSeconds(120);
        Instant expiredAt = Instant.now().minusSeconds(60);
        RefreshToken token = new RefreshToken("expired-token", "user@example.com", issuedAt, expiredAt);
        refreshTokenRepository.save(token);

        assertThat(refreshTokenService.consume("expired-token")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("expired-token")).isEmpty();
    }

    @Test
    void revokeAllForUser_removes_user_tokens_only() {
        refreshTokenService.issue("user@example.com", Instant.now().plusSeconds(300));
        refreshTokenService.issue("User@Example.com", Instant.now().plusSeconds(300));
        String otherToken = refreshTokenService.issue("other@example.com", Instant.now().plusSeconds(300));

        refreshTokenService.revokeAllForUser("USER@example.com");

        assertThat(refreshTokenRepository.findByToken(otherToken)).isPresent();
        assertThat(refreshTokenRepository.findAll())
                .allMatch(record -> record.getUserEmail().equalsIgnoreCase("other@example.com"));
    }
}
