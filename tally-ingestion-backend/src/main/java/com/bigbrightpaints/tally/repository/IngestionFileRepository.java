package com.bigbrightpaints.tally.repository;

import com.bigbrightpaints.tally.domain.IngestionFile;
import com.bigbrightpaints.tally.domain.IngestionRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing ingestion file records
 */
@Repository
public interface IngestionFileRepository extends JpaRepository<IngestionFile, Long> {

    List<IngestionFile> findByRun(IngestionRun run);

    List<IngestionFile> findByRunAndStatus(
            IngestionRun run,
            IngestionFile.FileStatus status);

    Optional<IngestionFile> findByRunAndFileHash(
            IngestionRun run,
            String fileHash);

    @Query("SELECT COUNT(f) FROM IngestionFile f WHERE f.run = :run " +
            "AND f.status = 'FAILED'")
    long countFailedFiles(@Param("run") IngestionRun run);

    @Query("SELECT COUNT(f) FROM IngestionFile f WHERE f.run = :run " +
            "AND f.status = 'COMPLETED'")
    long countCompletedFiles(@Param("run") IngestionRun run);
}