package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface TallyImportRepository extends JpaRepository<TallyImport, Long> {
  Optional<TallyImport> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);
}
