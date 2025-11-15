package com.bigbrightpaints.erp.modules.production.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionBrandRepository extends JpaRepository<ProductionBrand, Long> {
    Optional<ProductionBrand> findByCompanyAndCodeIgnoreCase(Company company, String code);
    Optional<ProductionBrand> findByCompanyAndNameIgnoreCase(Company company, String name);
    Optional<ProductionBrand> findByCompanyAndId(Company company, Long id);
    List<ProductionBrand> findByCompanyOrderByNameAsc(Company company);
}
