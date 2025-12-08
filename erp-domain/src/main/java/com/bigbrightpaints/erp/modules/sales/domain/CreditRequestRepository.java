package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {
    List<CreditRequest> findByCompanyOrderByCreatedAtDesc(Company company);
    List<CreditRequest> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status);
    Optional<CreditRequest> findByCompanyAndId(Company company, Long id);
}
