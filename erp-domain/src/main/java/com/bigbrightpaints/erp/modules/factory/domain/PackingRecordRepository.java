package com.bigbrightpaints.erp.modules.factory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PackingRecordRepository extends JpaRepository<PackingRecord, Long> {
    boolean existsByCompanyAndPackagingMaterial(Company company, RawMaterial rawMaterial);

    List<PackingRecord> findByCompanyAndProductionLogOrderByPackedDateAscIdAsc(Company company, ProductionLog productionLog);
    List<PackingRecord> findByCompanyAndPackedDateBetween(Company company, LocalDate start, LocalDate end);
}
