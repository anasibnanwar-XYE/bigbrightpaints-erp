package com.bigbrightpaints.erp.modules.hr.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    List<PayrollRun> findByCompanyOrderByRunDateDesc(Company company);
    Optional<PayrollRun> findByCompanyAndId(Company company, Long id);
    @EntityGraph(attributePaths = {"journalEntry", "journalEntry.lines", "journalEntry.lines.account"})
    Optional<PayrollRun> findById(Long id);
}
