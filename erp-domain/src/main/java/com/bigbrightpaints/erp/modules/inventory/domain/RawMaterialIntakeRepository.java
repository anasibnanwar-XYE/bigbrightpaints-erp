package com.bigbrightpaints.erp.modules.inventory.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface RawMaterialIntakeRepository extends JpaRepository<RawMaterialIntakeRecord, Long> {
  Optional<RawMaterialIntakeRecord> findByCompanyAndIdempotencyKey(
      Company company, String idempotencyKey);
}
