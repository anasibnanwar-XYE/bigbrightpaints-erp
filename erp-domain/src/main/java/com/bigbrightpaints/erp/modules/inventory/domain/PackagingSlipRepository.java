package com.bigbrightpaints.erp.modules.inventory.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PackagingSlipRepository extends JpaRepository<PackagingSlip, Long> {
    List<PackagingSlip> findByCompanyOrderByCreatedAtDesc(Company company);
    Optional<PackagingSlip> findByIdAndCompany(Long id, Company company);
    Optional<PackagingSlip> findBySalesOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PackagingSlip p where p.salesOrder.id = :orderId")
    Optional<PackagingSlip> findAndLockBySalesOrderId(@Param("orderId") Long orderId);
}
