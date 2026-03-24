package com.bigbrightpaints.erp.modules.sales.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
  List<Promotion> findByCompanyOrderByStartDateDesc(Company company);

  Optional<Promotion> findByCompanyAndId(Company company, Long id);
}
