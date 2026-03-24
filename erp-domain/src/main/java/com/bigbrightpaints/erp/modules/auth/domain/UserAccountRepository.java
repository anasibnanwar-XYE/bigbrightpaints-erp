package com.bigbrightpaints.erp.modules.auth.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByEmailIgnoreCase(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u where u.id = :id")
  Optional<UserAccount> lockById(@Param("id") Long id);

  List<UserAccount> findDistinctByCompanies_Id(Long companyId);

  long countDistinctByCompanies_IdAndEnabledTrue(Long companyId);

  Optional<UserAccount> findByIdAndCompanies_Id(Long id, Long companyId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from UserAccount u join u.companies c where u.id = :id and c.id = :companyId")
  Optional<UserAccount> lockByIdAndCompanyId(
      @Param("id") Long id, @Param("companyId") Long companyId);
}
