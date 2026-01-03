package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawMaterialMovementRepository extends JpaRepository<RawMaterialMovement, Long> {
    List<RawMaterialMovement> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
    List<RawMaterialMovement> findByReferenceTypeAndReferenceIdAndRawMaterialCompany(String referenceType,
                                                                                     String referenceId,
                                                                                     Company company);
    List<RawMaterialMovement> findByRawMaterialBatch(RawMaterialBatch batch);
}
