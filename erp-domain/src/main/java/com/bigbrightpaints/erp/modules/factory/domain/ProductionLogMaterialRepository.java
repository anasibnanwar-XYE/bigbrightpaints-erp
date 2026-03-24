package com.bigbrightpaints.erp.modules.factory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;

public interface ProductionLogMaterialRepository
    extends JpaRepository<ProductionLogMaterial, Long> {
  boolean existsByLogCompanyAndRawMaterial(Company company, RawMaterial rawMaterial);
}
