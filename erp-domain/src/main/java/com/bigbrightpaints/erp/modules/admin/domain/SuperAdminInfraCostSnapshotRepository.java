package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SuperAdminInfraCostSnapshotRepository
    extends JpaRepository<SuperAdminInfraCostSnapshot, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SuperAdminInfraCostSnapshot s where s.id = :id")
  java.util.Optional<SuperAdminInfraCostSnapshot> lockById(@Param("id") Long id);

  @Query(
      "select s from SuperAdminInfraCostSnapshot s where (:includeArchived = true or s.status ="
          + " 'ACTIVE') and (:currency is null or s.currency = :currency) order by s.periodEndAt"
          + " desc, s.createdAt desc, s.id desc")
  List<SuperAdminInfraCostSnapshot> findSnapshots(
      @Param("includeArchived") boolean includeArchived, @Param("currency") String currency);
}
