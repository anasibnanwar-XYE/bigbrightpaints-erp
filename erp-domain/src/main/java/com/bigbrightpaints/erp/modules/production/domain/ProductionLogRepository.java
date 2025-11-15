package com.bigbrightpaints.erp.modules.production.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {
    List<ProductionLog> findByCompanyOrderByProducedAtDesc(Company company, Pageable pageable);
}
