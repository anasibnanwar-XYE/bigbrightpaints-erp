package com.bigbrightpaints.erp.modules.auth.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;

@Repository
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {

  @Query("SELECT rc FROM MfaRecoveryCode rc WHERE rc.user = :user AND rc.usedAt IS NULL")
  List<MfaRecoveryCode> findUnusedByUser(@Param("user") UserAccount user);

  /**
   * Find all unused recovery codes for a user while holding row locks for verifier consumption.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT rc FROM MfaRecoveryCode rc WHERE rc.user = :user AND rc.usedAt IS NULL")
  List<MfaRecoveryCode> findUnusedByUserForUpdate(@Param("user") UserAccount user);

  @Query(
      "SELECT rc FROM MfaRecoveryCode rc WHERE rc.user = :user AND rc.codeHash = :codeHash AND"
          + " rc.usedAt IS NULL")
  Optional<MfaRecoveryCode> findUnusedByUserAndCodeHash(
      @Param("user") UserAccount user, @Param("codeHash") String codeHash);

  @Modifying
  @Transactional
  @Query("DELETE FROM MfaRecoveryCode rc WHERE rc.user = :user")
  void deleteAllByUser(@Param("user") UserAccount user);

  @Query("SELECT COUNT(rc) FROM MfaRecoveryCode rc WHERE rc.user = :user AND rc.usedAt IS NULL")
  long countUnusedByUser(@Param("user") UserAccount user);

  @Modifying
  @Transactional
  @Query("DELETE FROM MfaRecoveryCode rc WHERE rc.usedAt IS NOT NULL AND rc.usedAt < :cutoffDate")
  int deleteUsedCodesBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
