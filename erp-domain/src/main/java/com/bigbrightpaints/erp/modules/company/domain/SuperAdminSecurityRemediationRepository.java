package com.bigbrightpaints.erp.modules.company.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SuperAdminSecurityRemediationRepository
    extends JpaRepository<SuperAdminSecurityRemediation, Long> {

  Optional<SuperAdminSecurityRemediation> findByAuditEventId(Long auditEventId);

  List<SuperAdminSecurityRemediation> findByAuditEventIdIn(Collection<Long> auditEventIds);

  long countByStatusIn(Collection<String> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT remediation FROM SuperAdminSecurityRemediation remediation "
          + "WHERE remediation.auditEventId = :auditEventId")
  Optional<SuperAdminSecurityRemediation> lockByAuditEventId(
      @Param("auditEventId") Long auditEventId);

  @Query(
      "SELECT remediation.auditEventId FROM SuperAdminSecurityRemediation remediation "
          + "WHERE remediation.auditEventId IN :auditEventIds")
  Set<Long> findRemediatedAuditEventIds(@Param("auditEventIds") Collection<Long> auditEventIds);
}
