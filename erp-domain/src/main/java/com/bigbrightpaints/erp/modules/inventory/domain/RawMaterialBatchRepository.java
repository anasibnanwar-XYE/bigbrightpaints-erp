package com.bigbrightpaints.erp.modules.inventory.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RawMaterialBatchRepository extends JpaRepository<RawMaterialBatch, Long> {
    List<RawMaterialBatch> findByRawMaterial(RawMaterial rawMaterial);
    List<RawMaterialBatch> findByRawMaterial_InventoryAccountId(Long inventoryAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from RawMaterialBatch b
            where b.rawMaterial = :rawMaterial
              and b.quantity > 0
            order by b.receivedAt asc, b.id asc
            """)
    List<RawMaterialBatch> findAvailableBatchesFIFO(@Param("rawMaterial") RawMaterial rawMaterial);
}
