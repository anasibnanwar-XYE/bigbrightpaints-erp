package com.bigbrightpaints.erp.modules.production.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionProductRepository extends JpaRepository<ProductionProduct, Long> {
    Optional<ProductionProduct> findByCompanyAndSkuCode(Company company, String skuCode);
    Optional<ProductionProduct> findByCompanyAndId(Company company, Long id);
    Optional<ProductionProduct> findByBrandAndProductNameIgnoreCase(ProductionBrand brand, String productName);
    Optional<ProductionProduct> findTopByCompanyAndSkuCodeStartingWithOrderBySkuCodeDesc(Company company, String prefix);
    List<ProductionProduct> findByCompanyOrderByProductNameAsc(Company company);
    List<ProductionProduct> findByBrandOrderByProductNameAsc(ProductionBrand brand);
}
