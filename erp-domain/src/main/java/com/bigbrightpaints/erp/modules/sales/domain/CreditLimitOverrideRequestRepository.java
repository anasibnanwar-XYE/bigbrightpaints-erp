package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditLimitOverrideRequestRepository extends JpaRepository<CreditLimitOverrideRequest, Long> {
    List<CreditLimitOverrideRequest> findByCompanyOrderByCreatedAtDesc(Company company);
    List<CreditLimitOverrideRequest> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status);
    Optional<CreditLimitOverrideRequest> findByCompanyAndId(Company company, Long id);
    Optional<CreditLimitOverrideRequest> findByCompanyAndPackagingSlipAndStatus(Company company,
                                                                                PackagingSlip packagingSlip,
                                                                                String status);
}
