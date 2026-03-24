package com.bigbrightpaints.erp.modules.production.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface CatalogImportRepository extends JpaRepository<CatalogImport, Long> {
  Optional<CatalogImport> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);
}
