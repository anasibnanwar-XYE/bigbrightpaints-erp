package com.bigbrightpaints.tally.repository.staging;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingDealer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for staging dealer data
 */
@Repository
public interface StagingDealerRepository extends JpaRepository<StagingDealer, Long> {

    List<StagingDealer> findByRun(IngestionRun run);

    Page<StagingDealer> findByRunAndProcessed(
            IngestionRun run,
            Boolean processed,
            Pageable pageable);

    Optional<StagingDealer> findByRunAndGstin(
            IngestionRun run,
            String gstin);

    Optional<StagingDealer> findByRunAndPan(
            IngestionRun run,
            String pan);

    @Query("SELECT s FROM StagingDealer s WHERE s.run = :run " +
            "AND (s.gstin = :gstin OR s.pan = :pan OR LOWER(s.partyName) = LOWER(:name))")
    List<StagingDealer> findPotentialMatches(
            @Param("run") IngestionRun run,
            @Param("gstin") String gstin,
            @Param("pan") String pan,
            @Param("name") String name);

    @Query("SELECT COUNT(s) FROM StagingDealer s WHERE s.run = :run " +
            "AND s.validationStatus = 'INVALID'")
    long countInvalidDealers(@Param("run") IngestionRun run);
}