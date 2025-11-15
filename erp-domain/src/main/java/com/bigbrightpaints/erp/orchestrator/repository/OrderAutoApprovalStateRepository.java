package com.bigbrightpaints.erp.orchestrator.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface OrderAutoApprovalStateRepository extends JpaRepository<OrderAutoApprovalState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    Optional<OrderAutoApprovalState> findByCompanyCodeAndOrderId(String companyCode, Long orderId);
}
