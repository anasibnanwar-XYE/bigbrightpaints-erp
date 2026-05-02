package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenDigest(String tokenDigest);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RefreshToken t where t.tokenDigest = :tokenDigest")
  Optional<RefreshToken> findForUpdateByTokenDigest(@Param("tokenDigest") String tokenDigest);

  List<RefreshToken> findByUserPublicIdOrderByIssuedAtDesc(UUID userPublicId);

  @Modifying
  @Query("delete from RefreshToken t where t.userPublicId = :userPublicId")
  int deleteByUserPublicId(@Param("userPublicId") UUID userPublicId);

  @Modifying
  @Query("delete from RefreshToken t where t.id = :id and t.userPublicId = :userPublicId")
  int deleteByIdAndUserPublicId(@Param("id") Long id, @Param("userPublicId") UUID userPublicId);

  @Modifying
  @Query("delete from RefreshToken t where t.tokenDigest = :tokenDigest")
  int deleteByTokenDigest(@Param("tokenDigest") String tokenDigest);

  @Modifying
  @Query("delete from RefreshToken t where t.expiresAt < :cutoff")
  int deleteExpiredTokens(@Param("cutoff") Instant cutoff);
}
