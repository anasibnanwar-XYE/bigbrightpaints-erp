package com.bigbrightpaints.tally.repository;

import com.bigbrightpaints.tally.domain.IngestionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing ingestion runs
 */
@Repository
public interface IngestionRunRepository extends JpaRepository<IngestionRun, Long> {

    Optional<IngestionRun> findByRunId(UUID runId);

    List<IngestionRun> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<IngestionRun> findByCompanyIdAndStatus(Long companyId, IngestionRun.RunStatus status);

    @Query("SELECT r FROM IngestionRun r WHERE r.companyId = :companyId " +
            "AND r.status = :status AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<IngestionRun> findRecentRunsByStatus(
            @Param("companyId") Long companyId,
            @Param("status") IngestionRun.RunStatus status,
            @Param("since") Instant since);

    @Query("SELECT r FROM IngestionRun r WHERE r.status = 'RUNNING' " +
            "AND r.startedAt < :staleTime")
    List<IngestionRun> findStaleRunningRuns(@Param("staleTime") Instant staleTime);

    @Query("SELECT COUNT(r) FROM IngestionRun r WHERE r.companyId = :companyId " +
            "AND r.status = 'RUNNING'")
    long countRunningRuns(@Param("companyId") Long companyId);
}