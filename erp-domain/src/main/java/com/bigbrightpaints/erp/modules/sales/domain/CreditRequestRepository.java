package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditRequestRepository extends JpaRepository<CreditRequest, Long> {
    List<CreditRequest> findByCompanyOrderByCreatedAtDesc(Company company);
    List<CreditRequest> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status);

    @Query("""
            select request
            from CreditRequest request
            where request.company = :company
              and upper(trim(request.status)) = 'PENDING'
            order by request.createdAt desc
            """)
    List<CreditRequest> findPendingByCompanyOrderByCreatedAtDesc(@Param("company") Company company);

    Optional<CreditRequest> findByCompanyAndId(Company company, Long id);
}
