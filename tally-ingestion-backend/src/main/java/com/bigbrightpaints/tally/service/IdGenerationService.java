package com.bigbrightpaints.tally.service;

import com.bigbrightpaints.tally.config.TallyIngestionProperties;
import com.bigbrightpaints.tally.domain.IdRegistry;
import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.repository.IdRegistryRepository;
import com.bigbrightpaints.tally.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for generating deterministic IDs for entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdGenerationService {

    private final IdRegistryRepository idRegistryRepository;
    private final TallyIngestionProperties properties;

    // Local cache for generated IDs within a run
    private final ConcurrentHashMap<String, String> idCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> codeCounters = new ConcurrentHashMap<>();

    /**
     * Generate or retrieve deterministic ID for a dealer
     */
    @Transactional
    public GeneratedId generateDealerId(
            Long companyId,
            IngestionRun run,
            String name,
            String gstin,
            String pan) {

        String sourceKey = buildDealerSourceKey(name, gstin, pan);
        return generateId(companyId, run, IdRegistry.EntityType.DEALER, sourceKey, "DLR");
    }

    /**
     * Generate or retrieve deterministic ID for a supplier
     */
    @Transactional
    public GeneratedId generateSupplierId(
            Long companyId,
            IngestionRun run,
            String name,
            String gstin,
            String pan) {

        String sourceKey = buildSupplierSourceKey(name, gstin, pan);
        return generateId(companyId, run, IdRegistry.EntityType.SUPPLIER, sourceKey, "SUP");
    }

    /**
     * Generate or retrieve deterministic ID for a product
     */
    @Transactional
    public GeneratedId generateProductId(
            Long companyId,
            IngestionRun run,
            String brand,
            String baseName) {

        String sourceKey = buildProductSourceKey(brand, baseName);
        return generateId(companyId, run, IdRegistry.EntityType.PRODUCT, sourceKey, "PRD");
    }

    /**
     * Generate or retrieve deterministic ID for a product variant
     */
    @Transactional
    public GeneratedId generateVariantId(
            Long companyId,
            IngestionRun run,
            String productId,
            String color,
            String size,
            String pack) {

        String sourceKey = buildVariantSourceKey(productId, color, size, pack);
        return generateId(companyId, run, IdRegistry.EntityType.VARIANT, sourceKey, "VAR");
    }

    /**
     * Generate or retrieve deterministic ID for an account
     */
    @Transactional
    public GeneratedId generateAccountId(
            Long companyId,
            IngestionRun run,
            String ledgerName,
            String ledgerGroup) {

        String sourceKey = buildAccountSourceKey(ledgerName, ledgerGroup);
        return generateId(companyId, run, IdRegistry.EntityType.ACCOUNT, sourceKey, "ACC");
    }

    /**
     * Generate human-readable dealer code
     */
    public String generateDealerCode(Long companyId, String name, String city) {
        String prefix = extractInitials(name, 3);
        String location = city != null && city.length() >= 2
                ? city.substring(0, 2).toUpperCase()
                : "XX";

        String baseCode = prefix + location;
        return generateUniqueCode(companyId, IdRegistry.EntityType.DEALER, baseCode);
    }

    /**
     * Generate human-readable supplier code
     */
    public String generateSupplierCode(Long companyId, String name) {
        String prefix = extractInitials(name, 4);
        return generateUniqueCode(companyId, IdRegistry.EntityType.SUPPLIER, prefix);
    }

    /**
     * Core ID generation logic
     */
    private GeneratedId generateId(
            Long companyId,
            IngestionRun run,
            IdRegistry.EntityType entityType,
            String sourceKey,
            String idPrefix) {

        // Generate deterministic hash
        String sourceHash = HashUtil.generateHash(sourceKey);

        // Check cache first
        String cacheKey = buildCacheKey(companyId, entityType, sourceHash);
        String cachedId = idCache.get(cacheKey);
        if (cachedId != null) {
            return new GeneratedId(cachedId, null, false);
        }

        // Check database
        Optional<IdRegistry> existing = idRegistryRepository
                .findByCompanyIdAndEntityTypeAndSourceHash(companyId, entityType, sourceHash);

        if (existing.isPresent()) {
            IdRegistry registry = existing.get();
            registry.incrementOccurrence(run);
            idRegistryRepository.save(registry);

            // Cache the ID
            idCache.put(cacheKey, registry.getGeneratedId());

            return new GeneratedId(
                    registry.getGeneratedId(),
                    registry.getGeneratedCode(),
                    false);
        }

        // Generate new ID
        String prefix = properties.getCompanyPrefix() != null
                ? properties.getCompanyPrefix()
                : "BBP";

        String generatedId = HashUtil.generateDeterministicId(
                prefix + "_" + idPrefix,
                sourceKey);

        // Save to registry
        IdRegistry registry = IdRegistry.builder()
                .companyId(companyId)
                .entityType(entityType)
                .sourceKey(sourceKey)
                .sourceHash(sourceHash)
                .generatedId(generatedId)
                .firstSeenRun(run)
                .lastSeenRun(run)
                .occurrenceCount(1)
                .build();

        idRegistryRepository.save(registry);

        // Cache the ID
        idCache.put(cacheKey, generatedId);

        return new GeneratedId(generatedId, null, true);
    }

    /**
     * Generate unique code with counter suffix if needed
     */
    private String generateUniqueCode(
            Long companyId,
            IdRegistry.EntityType entityType,
            String baseCode) {

        String counterKey = companyId + ":" + entityType + ":" + baseCode;
        AtomicInteger counter = codeCounters.computeIfAbsent(
                counterKey,
                k -> new AtomicInteger(0));

        String code = baseCode;
        int attempt = 0;

        while (attempt < 100) {
            if (attempt > 0) {
                code = baseCode + String.format("%02d", counter.incrementAndGet());
            }

            Optional<IdRegistry> existing = idRegistryRepository
                    .findByGeneratedCode(companyId, entityType, code);

            if (existing.isEmpty()) {
                return code;
            }

            attempt++;
        }

        // Fallback to hash-based code
        return baseCode + "_" + HashUtil.generateShortCode(baseCode + System.nanoTime(), 4);
    }

    // Source key builders
    private String buildDealerSourceKey(String name, String gstin, String pan) {
        StringBuilder key = new StringBuilder();
        key.append("DEALER:");

        if (gstin != null && !gstin.isEmpty()) {
            key.append("GSTIN:").append(gstin.toUpperCase());
        } else if (pan != null && !pan.isEmpty()) {
            key.append("PAN:").append(pan.toUpperCase());
        } else {
            key.append("NAME:").append(normalizeForKey(name));
        }

        return key.toString();
    }

    private String buildSupplierSourceKey(String name, String gstin, String pan) {
        StringBuilder key = new StringBuilder();
        key.append("SUPPLIER:");

        if (gstin != null && !gstin.isEmpty()) {
            key.append("GSTIN:").append(gstin.toUpperCase());
        } else if (pan != null && !pan.isEmpty()) {
            key.append("PAN:").append(pan.toUpperCase());
        } else {
            key.append("NAME:").append(normalizeForKey(name));
        }

        return key.toString();
    }

    private String buildProductSourceKey(String brand, String baseName) {
        return String.format("PRODUCT:BRAND:%s:NAME:%s",
                normalizeForKey(brand),
                normalizeForKey(baseName));
    }

    private String buildVariantSourceKey(String productId, String color, String size, String pack) {
        return String.format("VARIANT:PRODUCT:%s:COLOR:%s:SIZE:%s:PACK:%s",
                productId,
                normalizeForKey(color),
                normalizeForKey(size),
                normalizeForKey(pack));
    }

    private String buildAccountSourceKey(String ledgerName, String ledgerGroup) {
        return String.format("ACCOUNT:LEDGER:%s:GROUP:%s",
                normalizeForKey(ledgerName),
                normalizeForKey(ledgerGroup));
    }

    private String buildCacheKey(Long companyId, IdRegistry.EntityType entityType, String hash) {
        return companyId + ":" + entityType + ":" + hash;
    }

    /**
     * Normalize string for use in source key
     */
    private String normalizeForKey(String value) {
        if (value == null) {
            return "NULL";
        }
        return value.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /**
     * Extract initials from name
     */
    private String extractInitials(String name, int maxLength) {
        if (name == null || name.isEmpty()) {
            return "XXX";
        }

        String[] words = name.split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty() && Character.isLetter(word.charAt(0))) {
                initials.append(word.charAt(0));
                if (initials.length() >= maxLength) {
                    break;
                }
            }
        }

        String result = initials.toString().toUpperCase();
        if (result.isEmpty()) {
            result = name.substring(0, Math.min(name.length(), maxLength)).toUpperCase();
        }

        return result;
    }

    /**
     * Clear cache (useful after run completion)
     */
    public void clearCache() {
        idCache.clear();
        codeCounters.clear();
    }

    /**
     * Result of ID generation
     */
    public record GeneratedId(
            String id,
            String code,
            boolean isNew) {
    }
}