package com.bigbrightpaints.erp.modules.sales.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface SalesTargetRepository extends JpaRepository<SalesTarget, Long> {
  List<SalesTarget> findByCompanyOrderByPeriodStartDesc(Company company);

  Optional<SalesTarget> findByCompanyAndId(Company company, Long id);
}
