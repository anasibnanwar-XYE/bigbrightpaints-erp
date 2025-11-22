package com.bigbrightpaints.tally.repository;

import com.bigbrightpaints.tally.domain.IdRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing deterministic ID registry
 */
@Repository
public interface IdRegistryRepository extends JpaRepository<IdRegistry, Long> {

    Optional<IdRegistry> findByCompanyIdAndEntityTypeAndSourceHash(
            Long companyId,
            IdRegistry.EntityType entityType,
            String sourceHash);

    List<IdRegistry> findByCompanyIdAndEntityType(
            Long companyId,
            IdRegistry.EntityType entityType);

    Optional<IdRegistry> findByCompanyIdAndGeneratedId(
            Long companyId,
            String generatedId);

    @Query("SELECT r FROM IdRegistry r WHERE r.companyId = :companyId " +
            "AND r.entityType = :entityType AND r.generatedCode = :code")
    Optional<IdRegistry> findByGeneratedCode(
            @Param("companyId") Long companyId,
            @Param("entityType") IdRegistry.EntityType entityType,
            @Param("code") String code);

    @Query("SELECT r FROM IdRegistry r WHERE r.companyId = :companyId " +
            "AND r.entityType = 'VARIANT' AND r.generatedSku = :sku")
    Optional<IdRegistry> findByGeneratedSku(
            @Param("companyId") Long companyId,
            @Param("sku") String sku);

    @Query("SELECT COUNT(r) FROM IdRegistry r WHERE r.companyId = :companyId " +
            "AND r.entityType = :entityType AND r.mappedEntityId IS NULL")
    long countUnmappedEntities(
            @Param("companyId") Long companyId,
            @Param("entityType") IdRegistry.EntityType entityType);
}