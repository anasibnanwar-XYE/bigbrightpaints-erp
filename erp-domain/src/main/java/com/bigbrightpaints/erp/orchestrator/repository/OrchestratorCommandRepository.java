package com.bigbrightpaints.erp.orchestrator.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface OrchestratorCommandRepository extends JpaRepository<OrchestratorCommand, UUID> {

    Optional<OrchestratorCommand> findByCompanyIdAndCommandNameAndIdempotencyKey(
            Long companyId, String commandName, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from OrchestratorCommand c
            where c.companyId = :companyId
              and c.commandName = :commandName
              and c.idempotencyKey = :idempotencyKey
            """)
    Optional<OrchestratorCommand> lockByScope(@Param("companyId") Long companyId,
                                              @Param("commandName") String commandName,
                                              @Param("idempotencyKey") String idempotencyKey);
}

