package com.bigbrightpaints.erp.modules.production.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductionBrandRepository extends JpaRepository<ProductionBrand, Long> {
    Optional<ProductionBrand> findByCompanyAndCodeIgnoreCase(Company company, String code);
    Optional<ProductionBrand> findByCompanyAndNameIgnoreCase(Company company, String name);
    Optional<ProductionBrand> findByCompanyAndId(Company company, Long id);
    List<ProductionBrand> findByCompanyOrderByNameAsc(Company company);
    List<ProductionBrand> findByCompanyAndActiveOrderByNameAsc(Company company, boolean active);

    @Query("select b from ProductionBrand b where b.company = :company and lower(b.name) in :names")
    List<ProductionBrand> findByCompanyAndNameInIgnoreCase(@Param("company") Company company,
                                                           @Param("names") Collection<String> names);
}
