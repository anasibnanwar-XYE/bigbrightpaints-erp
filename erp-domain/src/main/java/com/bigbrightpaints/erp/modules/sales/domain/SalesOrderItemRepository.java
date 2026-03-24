package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {
    boolean existsBySalesOrderCompanyAndProductCodeIgnoreCase(Company company, String productCode);
}
