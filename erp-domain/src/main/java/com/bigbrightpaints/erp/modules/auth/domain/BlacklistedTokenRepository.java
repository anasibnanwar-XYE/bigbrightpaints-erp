package com.bigbrightpaints.erp.modules.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    Optional<BlacklistedToken> findByTokenId(String tokenId);

    boolean existsByTokenId(String tokenId);

    @Modifying
    @Query("DELETE FROM BlacklistedToken t WHERE t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") Instant now);

    @Query("SELECT MAX(t.blacklistedAt) FROM BlacklistedToken t WHERE t.userId = :userId")
    Optional<Instant> findLatestRevocationTimeForUser(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM BlacklistedToken t WHERE t.userId = :userId AND t.expiresAt < :cutoff")
    int deleteExpiredTokensForUser(@Param("userId") String userId, @Param("cutoff") Instant cutoff);
}
