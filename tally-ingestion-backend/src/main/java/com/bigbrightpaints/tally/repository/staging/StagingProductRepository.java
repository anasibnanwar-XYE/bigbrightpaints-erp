package com.bigbrightpaints.tally.repository.staging;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for staging product data
 */
@Repository
public interface StagingProductRepository extends JpaRepository<StagingProduct, Long> {

    List<StagingProduct> findByRun(IngestionRun run);

    Page<StagingProduct> findByRunAndProcessed(
            IngestionRun run,
            Boolean processed,
            Pageable pageable);

    Page<StagingProduct> findByRunAndValidationStatus(
            IngestionRun run,
            StagingProduct.ValidationStatus status,
            Pageable pageable);

    Optional<StagingProduct> findByRunAndSourceHash(
            IngestionRun run,
            String sourceHash);

    @Query("SELECT DISTINCT s.brand FROM StagingProduct s WHERE s.run = :run " +
            "AND s.brand IS NOT NULL ORDER BY s.brand")
    List<String> findDistinctBrandsByRun(@Param("run") IngestionRun run);

    @Query("SELECT COUNT(s) FROM StagingProduct s WHERE s.run = :run " +
            "AND s.validationStatus = 'INVALID'")
    long countInvalidProducts(@Param("run") IngestionRun run);

    @Query("SELECT COUNT(s) FROM StagingProduct s WHERE s.run = :run " +
            "AND s.processed = false")
    long countUnprocessedProducts(@Param("run") IngestionRun run);

    @Modifying
    @Query("UPDATE StagingProduct s SET s.processed = true, s.processedAt = CURRENT_TIMESTAMP " +
            "WHERE s.run = :run AND s.id IN :ids")
    int markAsProcessed(@Param("run") IngestionRun run, @Param("ids") List<Long> ids);
}