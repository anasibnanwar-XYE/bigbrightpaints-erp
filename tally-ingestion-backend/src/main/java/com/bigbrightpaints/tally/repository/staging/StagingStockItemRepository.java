package com.bigbrightpaints.tally.repository.staging;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingStockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StagingStockItemRepository extends JpaRepository<StagingStockItem, Long> {

    List<StagingStockItem> findByRun(IngestionRun run);

    List<StagingStockItem> findByRunIdOrderByRowNumber(Long runId);

    List<StagingStockItem> findByRunAndProcessedFalse(IngestionRun run);

    @Query("SELECT s FROM StagingStockItem s WHERE s.run.id = :runId AND s.itemType = :itemType")
    List<StagingStockItem> findByRunIdAndItemType(@Param("runId") Long runId,
                                                    @Param("itemType") StagingStockItem.ItemType itemType);

    @Query("SELECT COUNT(s) FROM StagingStockItem s WHERE s.run.id = :runId AND s.itemType = :itemType")
    long countByRunIdAndItemType(@Param("runId") Long runId,
                                  @Param("itemType") StagingStockItem.ItemType itemType);

    @Query("SELECT COUNT(s) FROM StagingStockItem s WHERE s.run.id = :runId AND s.validationStatus = :status")
    long countByRunIdAndValidationStatus(@Param("runId") Long runId,
                                         @Param("status") StagingStockItem.ValidationStatus status);

    @Modifying
    @Query("DELETE FROM StagingStockItem s WHERE s.run = :run")
    void deleteByRun(@Param("run") IngestionRun run);

    @Query("""
        SELECT s FROM StagingStockItem s
        WHERE s.run.id = :runId
        AND s.processed = false
        AND s.validationStatus = 'VALID'
        ORDER BY s.rowNumber
        """)
    List<StagingStockItem> findReadyForImport(@Param("runId") Long runId);
}
