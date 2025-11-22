package com.bigbrightpaints.tally.repository;

import com.bigbrightpaints.tally.domain.SkuRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing SKU registry
 */
@Repository
public interface SkuRegistryRepository extends JpaRepository<SkuRegistry, Long> {

    Optional<SkuRegistry> findByCompanyIdAndSku(Long companyId, String sku);

    @Query("SELECT s FROM SkuRegistry s WHERE s.companyId = :companyId " +
            "AND s.sku LIKE :baseSku% ORDER BY s.counterSuffix DESC")
    List<SkuRegistry> findByCompanyIdAndBaseSku(
            @Param("companyId") Long companyId,
            @Param("baseSku") String baseSku);

    List<SkuRegistry> findByCompanyIdAndBrand(Long companyId, String brand);

    Optional<SkuRegistry> findByVariantId(Long variantId);

    @Query("SELECT COUNT(s) FROM SkuRegistry s WHERE s.companyId = :companyId " +
            "AND s.brand = :brand")
    long countByBrand(@Param("companyId") Long companyId, @Param("brand") String brand);

    @Query("SELECT s FROM SkuRegistry s WHERE s.companyId = :companyId " +
            "AND s.brand = :brand AND s.baseProduct = :baseProduct " +
            "AND s.color = :color AND s.size = :size")
    Optional<SkuRegistry> findByAttributes(
            @Param("companyId") Long companyId,
            @Param("brand") String brand,
            @Param("baseProduct") String baseProduct,
            @Param("color") String color,
            @Param("size") String size);
}