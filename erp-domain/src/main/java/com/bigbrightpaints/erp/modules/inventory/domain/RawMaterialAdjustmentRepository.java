package com.bigbrightpaints.erp.modules.inventory.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface RawMaterialAdjustmentRepository
    extends JpaRepository<RawMaterialAdjustment, Long> {

  List<RawMaterialAdjustment> findByCompanyOrderByAdjustmentDateDesc(Company company);

  Optional<RawMaterialAdjustment> findByCompanyAndIdempotencyKey(
      Company company, String idempotencyKey);

  @EntityGraph(attributePaths = {"lines", "lines.rawMaterial"})
  Optional<RawMaterialAdjustment> findWithLinesByCompanyAndIdempotencyKey(
      Company company, String idempotencyKey);
}
