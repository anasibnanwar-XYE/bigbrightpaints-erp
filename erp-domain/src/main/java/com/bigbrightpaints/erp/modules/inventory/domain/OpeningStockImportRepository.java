package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpeningStockImportRepository extends JpaRepository<OpeningStockImport, Long> {
    Optional<OpeningStockImport> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);
}
