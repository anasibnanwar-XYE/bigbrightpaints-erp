package com.bigbrightpaints.erp.modules.accounting.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReconciliationDiscrepancyRepository extends JpaRepository<ReconciliationDiscrepancy, Long> {

    Optional<ReconciliationDiscrepancy> findByCompanyAndId(Company company, Long id);

    List<ReconciliationDiscrepancy> findByCompanyAndStatusInAndPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(
            Company company,
            Collection<ReconciliationDiscrepancyStatus> statuses,
            LocalDate periodStart,
            LocalDate periodEnd);

    @Query("""
            select d
            from ReconciliationDiscrepancy d
            where d.company = :company
              and (:status is null or d.status = :status)
              and (:type is null or d.type = :type)
            order by d.createdAt desc, d.id desc
            """)
    List<ReconciliationDiscrepancy> findFiltered(@Param("company") Company company,
                                                 @Param("status") ReconciliationDiscrepancyStatus status,
                                                 @Param("type") ReconciliationDiscrepancyType type);

    List<ReconciliationDiscrepancy> findByCompanyAndTypeAndPeriodStartAndPeriodEndOrderByCreatedAtDesc(
            Company company,
            ReconciliationDiscrepancyType type,
            LocalDate periodStart,
            LocalDate periodEnd);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    int deleteByCompanyAndAccountingPeriodAndTypeAndStatus(Company company,
                                                            AccountingPeriod period,
                                                            ReconciliationDiscrepancyType type,
                                                            ReconciliationDiscrepancyStatus status);

    List<ReconciliationDiscrepancy> findByCompanyAndAccountingPeriodAndTypeAndStatusOrderByCreatedAtDesc(
            Company company,
            AccountingPeriod accountingPeriod,
            ReconciliationDiscrepancyType type,
            ReconciliationDiscrepancyStatus status);

    long countByCompanyAndAccountingPeriodAndStatus(Company company,
                                                    AccountingPeriod accountingPeriod,
                                                    ReconciliationDiscrepancyStatus status);
}
