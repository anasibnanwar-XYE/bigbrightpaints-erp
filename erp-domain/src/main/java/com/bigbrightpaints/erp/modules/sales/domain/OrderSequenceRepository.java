package com.bigbrightpaints.erp.modules.sales.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.LockModeType;

public interface OrderSequenceRepository extends JpaRepository<OrderSequence, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<OrderSequence> findByCompanyAndFiscalYear(Company company, Integer fiscalYear);
}
