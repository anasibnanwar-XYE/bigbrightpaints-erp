package com.bigbrightpaints.erp.modules.factory.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;

public interface SizeVariantRepository extends JpaRepository<SizeVariant, Long> {

  List<SizeVariant> findByCompanyAndProductOrderBySizeLabelAsc(
      Company company, ProductionProduct product);

  Optional<SizeVariant> findByCompanyAndProductAndSizeLabelIgnoreCase(
      Company company, ProductionProduct product, String sizeLabel);
}
