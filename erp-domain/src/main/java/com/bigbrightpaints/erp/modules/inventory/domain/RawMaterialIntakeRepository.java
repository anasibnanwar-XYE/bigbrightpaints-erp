package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RawMaterialIntakeRepository extends JpaRepository<RawMaterialIntakeRecord, Long> {
    Optional<RawMaterialIntakeRecord> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);
}
