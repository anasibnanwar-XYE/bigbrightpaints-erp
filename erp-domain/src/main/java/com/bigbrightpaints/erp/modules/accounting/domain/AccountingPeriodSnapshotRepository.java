package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface AccountingPeriodSnapshotRepository
    extends JpaRepository<AccountingPeriodSnapshot, Long> {
  Optional<AccountingPeriodSnapshot> findByCompanyAndPeriod(
      Company company, AccountingPeriod period);
}
