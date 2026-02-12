package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CreditLimitOverrideRequestRepository extends JpaRepository<CreditLimitOverrideRequest, Long> {
    List<CreditLimitOverrideRequest> findByCompanyOrderByCreatedAtDesc(Company company);
    List<CreditLimitOverrideRequest> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status);

    @Query("""
            select request
            from CreditLimitOverrideRequest request
            where request.company = :company
              and upper(trim(request.status)) = 'PENDING'
            order by request.createdAt desc
            """)
    List<CreditLimitOverrideRequest> findPendingByCompanyOrderByCreatedAtDesc(@Param("company") Company company);

    Optional<CreditLimitOverrideRequest> findByCompanyAndId(Company company, Long id);
    Optional<CreditLimitOverrideRequest> findByCompanyAndPackagingSlipAndStatus(Company company,
                                                                                PackagingSlip packagingSlip,
                                                                                String status);
}
