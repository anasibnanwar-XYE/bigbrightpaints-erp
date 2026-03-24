package com.bigbrightpaints.erp.modules.company.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CompanyRepository extends JpaRepository<Company, Long> {
  Optional<Company> findByCodeIgnoreCase(String code);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Company c where c.id = :id")
  Optional<Company> lockById(@Param("id") Long id);
}
