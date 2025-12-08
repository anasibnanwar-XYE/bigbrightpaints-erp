package com.bigbrightpaints.erp.modules.sales.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    @EntityGraph(attributePaths = {"items", "dealer"})
    List<SalesOrder> findByCompanyOrderByCreatedAtDesc(Company company);

    @EntityGraph(attributePaths = {"items", "dealer"})
    List<SalesOrder> findByCompanyAndStatusOrderByCreatedAtDesc(Company company, String status);

    Optional<SalesOrder> findByCompanyAndId(Company company, Long id);

    @EntityGraph(attributePaths = {"items", "dealer"})
    Optional<SalesOrder> findWithItemsByCompanyAndId(Company company, Long id);

    @EntityGraph(attributePaths = {"company", "dealer"})
    List<SalesOrder> findAll();

    Optional<SalesOrder> findByCompanyAndIdempotencyKey(Company company, String idempotencyKey);

    @EntityGraph(attributePaths = {"items"})
    List<SalesOrder> findByCompanyAndDealerOrderByCreatedAtDesc(Company company, Dealer dealer);
}
