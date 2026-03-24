package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface OpeningBalanceImportRepository extends JpaRepository<OpeningBalanceImport, Long> {
  Optional<OpeningBalanceImport> findByCompanyAndIdempotencyKey(
      Company company, String idempotencyKey);
}
