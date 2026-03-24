package com.bigbrightpaints.erp.modules.factory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductionLogMaterialRepository extends JpaRepository<ProductionLogMaterial, Long> {
    boolean existsByLogCompanyAndRawMaterial(Company company, RawMaterial rawMaterial);
}
