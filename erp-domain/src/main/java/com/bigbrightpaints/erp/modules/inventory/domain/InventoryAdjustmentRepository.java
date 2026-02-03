package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {
    List<InventoryAdjustment> findByCompanyOrderByAdjustmentDateDesc(Company company);
    Optional<InventoryAdjustment> findByCompanyAndId(Company company, Long id);
    Optional<InventoryAdjustment> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);

    @EntityGraph(attributePaths = {"lines", "lines.finishedGood"})
    Optional<InventoryAdjustment> findWithLinesByCompanyAndIdempotencyKey(Company company, String idempotencyKey);
}
