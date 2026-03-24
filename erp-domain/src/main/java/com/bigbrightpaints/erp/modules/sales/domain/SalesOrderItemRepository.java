package com.bigbrightpaints.erp.modules.sales.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
  boolean existsBySalesOrderCompanyAndProductCodeIgnoreCase(Company company, String productCode);
}
