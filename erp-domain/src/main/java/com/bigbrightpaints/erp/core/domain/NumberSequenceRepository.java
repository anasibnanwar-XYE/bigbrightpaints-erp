package com.bigbrightpaints.erp.core.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.LockModeType;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {
  Optional<NumberSequence> findByCompanyAndSequenceKey(Company company, String sequenceKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<NumberSequence> findWithLockByCompanyAndSequenceKey(Company company, String sequenceKey);
}
