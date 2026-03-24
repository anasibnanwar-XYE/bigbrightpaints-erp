package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRevocationRepository extends JpaRepository<UserTokenRevocation, Long> {

  Optional<UserTokenRevocation> findByUserId(String userId);

  @Modifying
  @Query("DELETE FROM UserTokenRevocation r WHERE r.revokedAt < :cutoff")
  int deleteOldRevocations(@Param("cutoff") Instant cutoff);
}
