package com.bigbrightpaints.erp.modules.company.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SuperAdminBillingLedgerEntryRepository
    extends JpaRepository<SuperAdminBillingLedgerEntry, Long> {

  Optional<SuperAdminBillingLedgerEntry> findByCompanyIdAndIdempotencyKey(
      Long companyId, String idempotencyKey);

  List<SuperAdminBillingLedgerEntry> findByCompanyIdOrderByCreatedAtAscIdAsc(Long companyId);

  @Query(
      "select coalesce(sum(case when e.direction = 'DEBIT' then e.amountMinorUnits else"
          + " -e.amountMinorUnits end), 0) from SuperAdminBillingLedgerEntry e where e.company.id ="
          + " :companyId")
  Long balanceForCompany(@Param("companyId") Long companyId);

  @Query("select count(e) from SuperAdminBillingLedgerEntry e where e.company.id = :companyId")
  long countByCompanyId(@Param("companyId") Long companyId);
}
