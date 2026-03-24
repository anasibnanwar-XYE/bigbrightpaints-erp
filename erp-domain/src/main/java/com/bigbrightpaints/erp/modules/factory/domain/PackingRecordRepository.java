package com.bigbrightpaints.erp.modules.factory.domain;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;

public interface PackingRecordRepository extends JpaRepository<PackingRecord, Long> {
  boolean existsByCompanyAndPackagingMaterial(Company company, RawMaterial rawMaterial);

  List<PackingRecord> findByCompanyAndProductionLogOrderByPackedDateAscIdAsc(
      Company company, ProductionLog productionLog);

  List<PackingRecord> findByCompanyAndPackedDateBetween(
      Company company, LocalDate start, LocalDate end);
}
