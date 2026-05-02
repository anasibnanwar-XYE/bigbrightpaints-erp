package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SuperAdminInfraCostSnapshotCorrectionRepository
    extends JpaRepository<SuperAdminInfraCostSnapshotCorrection, Long> {

  List<SuperAdminInfraCostSnapshotCorrection> findBySnapshotIdOrderByCorrectedAtDescIdDesc(
      Long snapshotId);
}
