package com.bigbrightpaints.erp.modules.factory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionLogRepository extends JpaRepository<ProductionLog, Long> {
    List<ProductionLog> findTop25ByCompanyOrderByProducedAtDesc(Company company);
    Optional<ProductionLog> findByCompanyAndId(Company company, Long id);
    Optional<ProductionLog> findTopByCompanyAndProductionCodeStartingWithOrderByProductionCodeDesc(Company company, String prefix);
}
