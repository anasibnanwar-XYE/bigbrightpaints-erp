package com.bigbrightpaints.erp.modules.company.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TenantActivationTokenRepository
    extends JpaRepository<TenantActivationToken, Long> {

  Optional<TenantActivationToken> findByTokenDigest(String tokenDigest);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TenantActivationToken t where t.company.id = :companyId")
  List<TenantActivationToken> lockByCompanyId(@Param("companyId") Long companyId);

  Optional<TenantActivationToken> findTopByCompany_IdAndStatusInOrderByCreatedAtDesc(
      Long companyId, Collection<String> statuses);
}
