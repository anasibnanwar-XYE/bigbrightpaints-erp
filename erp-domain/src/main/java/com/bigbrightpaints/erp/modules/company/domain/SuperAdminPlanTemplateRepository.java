package com.bigbrightpaints.erp.modules.company.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SuperAdminPlanTemplateRepository
    extends JpaRepository<SuperAdminPlanTemplate, Long> {

  boolean existsByStableIdIgnoreCase(String stableId);

  List<SuperAdminPlanTemplate> findByStatusNotOrderByStableIdAscTemplateVersionDesc(String status);

  List<SuperAdminPlanTemplate> findByStableIdIgnoreCaseOrderByTemplateVersionDesc(String stableId);

  Optional<SuperAdminPlanTemplate>
      findTopByStableIdIgnoreCaseAndStatusNotOrderByTemplateVersionDesc(
          String stableId, String status);

  Optional<SuperAdminPlanTemplate> findTopByStableIdIgnoreCaseOrderByTemplateVersionDesc(
      String stableId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select p from SuperAdminPlanTemplate p where lower(p.stableId) = lower(:stableId) order by"
          + " p.templateVersion desc")
  List<SuperAdminPlanTemplate> lockAllByStableId(@Param("stableId") String stableId);
}
