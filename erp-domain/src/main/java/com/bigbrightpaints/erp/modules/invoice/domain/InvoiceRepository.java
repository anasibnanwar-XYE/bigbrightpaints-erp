package com.bigbrightpaints.erp.modules.invoice.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = "lines")
    List<Invoice> findByCompanyOrderByIssueDateDesc(Company company);

    @EntityGraph(attributePaths = "lines")
    List<Invoice> findByCompanyAndDealerOrderByIssueDateDesc(Company company, Dealer dealer);

    Optional<Invoice> findBySalesOrderId(Long salesOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.salesOrder.id = :salesOrderId")
    Optional<Invoice> lockBySalesOrderId(@Param("salesOrderId") Long salesOrderId);

    @EntityGraph(attributePaths = "lines")
    Optional<Invoice> findByCompanyAndId(Company company, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.company = :company and i.id = :id")
    Optional<Invoice> lockByCompanyAndId(@Param("company") Company company, @Param("id") Long id);
}
