package com.bigbrightpaints.erp.modules.company.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantDefaultSeedRunRepository extends JpaRepository<TenantDefaultSeedRun, Long> {

  List<TenantDefaultSeedRun> findByCompany_IdOrderByCategoryAsc(Long companyId);

  boolean existsByCompany_Id(Long companyId);
}
