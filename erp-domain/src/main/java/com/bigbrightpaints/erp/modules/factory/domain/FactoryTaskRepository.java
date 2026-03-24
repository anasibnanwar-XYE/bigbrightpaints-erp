package com.bigbrightpaints.erp.modules.factory.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface FactoryTaskRepository extends JpaRepository<FactoryTask, Long> {
  List<FactoryTask> findByCompanyOrderByCreatedAtDesc(Company company);

  Optional<FactoryTask> findByCompanyAndId(Company company, Long id);

  List<FactoryTask> findByCompanyAndSalesOrderId(Company company, Long salesOrderId);

  Optional<FactoryTask> findByCompanyAndSalesOrderIdAndTitleIgnoreCase(
      Company company, Long salesOrderId, String title);
}
