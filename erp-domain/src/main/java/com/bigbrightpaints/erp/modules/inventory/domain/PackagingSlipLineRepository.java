package com.bigbrightpaints.erp.modules.inventory.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PackagingSlipLineRepository extends JpaRepository<PackagingSlipLine, Long> {
  List<PackagingSlipLine> findByPackagingSlipId(Long packagingSlipId);
}
