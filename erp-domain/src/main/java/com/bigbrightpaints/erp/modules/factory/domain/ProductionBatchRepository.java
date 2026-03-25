package com.bigbrightpaints.erp.modules.factory.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
  long countByCompany(Company company);

  List<ProductionBatch> findByCompanyOrderByProducedAtDesc(Company company);

  Optional<ProductionBatch> findByCompanyAndId(Company company, Long id);
}
