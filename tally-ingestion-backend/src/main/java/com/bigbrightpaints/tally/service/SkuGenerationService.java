package com.bigbrightpaints.tally.service;

import com.bigbrightpaints.tally.config.TallyIngestionProperties;
import com.bigbrightpaints.tally.domain.SkuRegistry;
import com.bigbrightpaints.tally.repository.SkuRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for generating product SKUs with collision prevention
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkuGenerationService {

    private final SkuRegistryRepository skuRegistryRepository;
    private final TallyIngestionProperties properties;

    // Cache for SKU counters within a run
    private final ConcurrentHashMap<String, AtomicInteger> skuCounters = new ConcurrentHashMap<>();

    /**
     * Generate SKU for a product variant
     */
    @Transactional
    public GeneratedSku generateSku(
            Long companyId,
            String brand,
            String baseName,
            String color,
            String size,
            String pack) {

        // Get SKU pattern for brand
        String pattern = getSkuPattern(brand);

        // Build SKU components
        Map<String, String> components = new HashMap<>();
        components.put("BRAND", abbreviate(brand, properties.getSkuAbbreviations()));
        components.put("BASE", abbreviateProductName(baseName));
        components.put("COLOR", abbreviate(color, properties.getSkuAbbreviations()));
        components.put("SIZE", formatSize(size));
        components.put("PACK", formatPack(pack));

        // Generate base SKU
        String baseSku = buildSku(pattern, components);

        // Check for existing SKU
        Optional<SkuRegistry> existing = skuRegistryRepository
                .findByCompanyIdAndBaseSku(companyId, baseSku);

        if (existing.isPresent()) {
            SkuRegistry registry = existing.get();

            // Check if attributes match
            if (attributesMatch(registry, brand, baseName, color, size, pack)) {
                return new GeneratedSku(registry.getSku(), false, registry.getVariantId());
            }

            // Collision detected - generate with suffix
            return generateSkuWithSuffix(companyId, baseSku, brand, baseName, color, size, pack);
        }

        // Save new SKU
        SkuRegistry registry = SkuRegistry.builder()
                .companyId(companyId)
                .sku(baseSku)
                .brand(brand)
                .baseProduct(baseName)
                .color(color)
                .size(size)
                .pack(pack)
                .counterSuffix(0)
                .isActive(true)
                .build();

        skuRegistryRepository.save(registry);

        return new GeneratedSku(baseSku, true, null);
    }

    /**
     * Generate SKU with collision suffix
     */
    private GeneratedSku generateSkuWithSuffix(
            Long companyId,
            String baseSku,
            String brand,
            String baseName,
            String color,
            String size,
            String pack) {

        String counterKey = companyId + ":" + baseSku;
        AtomicInteger counter = skuCounters.computeIfAbsent(
                counterKey,
                k -> new AtomicInteger(0));

        for (int i = 1; i <= 99; i++) {
            String suffixedSku = baseSku + "-" + String.format("%02d", counter.incrementAndGet());

            Optional<SkuRegistry> existing = skuRegistryRepository
                    .findByCompanyIdAndSku(companyId, suffixedSku);

            if (existing.isEmpty()) {
                // Save new SKU with suffix
                SkuRegistry registry = SkuRegistry.builder()
                        .companyId(companyId)
                        .sku(suffixedSku)
                        .brand(brand)
                        .baseProduct(baseName)
                        .color(color)
                        .size(size)
                        .pack(pack)
                        .counterSuffix(i)
                        .isActive(true)
                        .build();

                skuRegistryRepository.save(registry);

                return new GeneratedSku(suffixedSku, true, null);
            }
        }

        // Fallback: append timestamp
        String fallbackSku = baseSku + "-" + System.currentTimeMillis();
        log.warn("SKU collision limit reached, using fallback: {}", fallbackSku);

        SkuRegistry registry = SkuRegistry.builder()
                .companyId(companyId)
                .sku(fallbackSku)
                .brand(brand)
                .baseProduct(baseName)
                .color(color)
                .size(size)
                .pack(pack)
                .counterSuffix(100)
                .isActive(true)
                .build();

        skuRegistryRepository.save(registry);

        return new GeneratedSku(fallbackSku, true, null);
    }

    /**
     * Get SKU pattern for brand
     */
    private String getSkuPattern(String brand) {
        if (brand == null) {
            return properties.getSkuPatterns().get("default");
        }

        String upperBrand = brand.toUpperCase();
        return properties.getSkuPatterns().getOrDefault(
                upperBrand,
                properties.getSkuPatterns().get("default"));
    }

    /**
     * Build SKU from pattern and components
     */
    private String buildSku(String pattern, Map<String, String> components) {
        String sku = pattern;

        for (Map.Entry<String, String> entry : components.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            sku = sku.replace(placeholder, value);
        }

        // Remove empty segments and clean up
        sku = sku.replaceAll("-{2,}", "-")  // Replace multiple dashes with single
                .replaceAll("^-|-$", "")     // Remove leading/trailing dashes
                .replaceAll("-+$", "");       // Remove trailing dashes

        // Remove {SEQ} placeholder if present (will be added as suffix if needed)
        sku = sku.replace("{SEQ}", "").replace("--", "-").replaceAll("-$", "");

        return sku.toUpperCase();
    }

    /**
     * Abbreviate based on mapping
     */
    private String abbreviate(String value, Map<String, String> abbreviations) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String upper = value.toUpperCase();
        return abbreviations.getOrDefault(upper, formatForSku(value));
    }

    /**
     * Abbreviate product name for SKU
     */
    private String abbreviateProductName(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        // Common paint product abbreviations
        Map<String, String> productAbbreviations = Map.of(
                "EMULSION", "EMUL",
                "ENAMEL", "ENAM",
                "PRIMER", "PRIM",
                "PUTTY", "PUTY",
                "THINNER", "THIN",
                "DISTEMPER", "DIST",
                "TEXTURE", "TEXT"
        );

        String upper = name.toUpperCase();
        for (Map.Entry<String, String> entry : productAbbreviations.entrySet()) {
            if (upper.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Take first 4 characters
        return formatForSku(name.substring(0, Math.min(name.length(), 4)));
    }

    /**
     * Format size for SKU
     */
    private String formatSize(String size) {
        if (size == null || size.isEmpty()) {
            return "";
        }

        // Remove spaces and standardize units
        return size.toUpperCase()
                .replace("LITRE", "L")
                .replace("LITER", "L")
                .replace("KILOGRAM", "KG")
                .replace("GRAM", "G")
                .replace("MILLILITER", "ML")
                .replaceAll("\\s+", "");
    }

    /**
     * Format pack for SKU
     */
    private String formatPack(String pack) {
        if (pack == null || pack.isEmpty()) {
            return "";
        }

        // Extract numeric part
        String numeric = pack.replaceAll("[^0-9]", "");
        if (!numeric.isEmpty()) {
            return numeric + "PK";
        }

        return formatForSku(pack);
    }

    /**
     * Format string for use in SKU
     */
    private String formatForSku(String value) {
        if (value == null) {
            return "";
        }

        return value.toUpperCase()
                .replaceAll("[^A-Z0-9]", "")
                .substring(0, Math.min(value.length(), 3));
    }

    /**
     * Check if SKU registry attributes match
     */
    private boolean attributesMatch(
            SkuRegistry registry,
            String brand,
            String baseName,
            String color,
            String size,
            String pack) {

        return equalsIgnoreCase(registry.getBrand(), brand) &&
                equalsIgnoreCase(registry.getBaseProduct(), baseName) &&
                equalsIgnoreCase(registry.getColor(), color) &&
                equalsIgnoreCase(registry.getSize(), size) &&
                equalsIgnoreCase(registry.getPack(), pack);
    }

    private boolean equalsIgnoreCase(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equalsIgnoreCase(s2);
    }

    /**
     * Validate SKU format
     */
    public boolean isValidSku(String sku) {
        if (sku == null || sku.isEmpty()) {
            return false;
        }

        // SKU should be alphanumeric with dashes, 5-50 characters
        return sku.matches("^[A-Z0-9-]{5,50}$");
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        skuCounters.clear();
    }

    /**
     * Result of SKU generation
     */
    public record GeneratedSku(
            String sku,
            boolean isNew,
            Long existingVariantId) {
    }
}