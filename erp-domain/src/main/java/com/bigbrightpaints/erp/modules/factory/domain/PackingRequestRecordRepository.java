package com.bigbrightpaints.erp.modules.factory.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface PackingRequestRecordRepository extends JpaRepository<PackingRequestRecord, Long> {
  Optional<PackingRequestRecord> findByCompanyAndIdempotencyKey(
      Company company, String idempotencyKey);
}
