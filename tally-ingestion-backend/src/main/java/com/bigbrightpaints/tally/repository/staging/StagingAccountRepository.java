package com.bigbrightpaints.tally.repository.staging;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for staging account data
 */
@Repository
public interface StagingAccountRepository extends JpaRepository<StagingAccount, Long> {

    List<StagingAccount> findByRun(IngestionRun run);

    Page<StagingAccount> findByRunAndProcessed(
            IngestionRun run,
            Boolean processed,
            Pageable pageable);

    Page<StagingAccount> findByRunAndValidationStatus(
            IngestionRun run,
            StagingAccount.ValidationStatus status,
            Pageable pageable);

    Optional<StagingAccount> findByRunAndSourceHash(
            IngestionRun run,
            String sourceHash);

    @Query("SELECT DISTINCT s.ledgerGroup FROM StagingAccount s WHERE s.run = :run " +
            "AND s.ledgerGroup IS NOT NULL ORDER BY s.ledgerGroup")
    List<String> findDistinctLedgerGroupsByRun(@Param("run") IngestionRun run);

    @Query("SELECT COUNT(s) FROM StagingAccount s WHERE s.run = :run " +
            "AND s.validationStatus = 'INVALID'")
    long countInvalidAccounts(@Param("run") IngestionRun run);

    @Query("SELECT s FROM StagingAccount s WHERE s.run = :run " +
            "AND s.ledgerGroup = 'Sundry Debtors' AND s.processed = false")
    List<StagingAccount> findUnprocessedDealerAccounts(@Param("run") IngestionRun run);

    @Query("SELECT s FROM StagingAccount s WHERE s.run = :run " +
            "AND s.ledgerGroup = 'Sundry Creditors' AND s.processed = false")
    List<StagingAccount> findUnprocessedSupplierAccounts(@Param("run") IngestionRun run);
}