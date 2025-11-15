package com.bigbrightpaints.erp.modules.factory.service;

import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLog;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogMaterial;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogRepository;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogDetailDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogMaterialDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionLogRequest;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductionLogService {

    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REFERENCE_TYPE_PRODUCTION_LOG = "PRODUCTION_LOG";
    private static final String MOVEMENT_TYPE_ISSUE = "ISSUE";
    private static final String MOVEMENT_TYPE_RECEIPT = "RECEIPT";
    private static final RoundingMode COST_ROUNDING = RoundingMode.HALF_UP;

    private final CompanyContextService companyContextService;
    private final ProductionLogRepository logRepository;
    private final ProductionBrandRepository brandRepository;
    private final ProductionProductRepository productRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final FinishedGoodRepository finishedGoodRepository;
    private final RawMaterialBatchRepository rawMaterialBatchRepository;
    private final RawMaterialMovementRepository rawMaterialMovementRepository;
    private final FinishedGoodBatchRepository finishedGoodBatchRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final AccountingService accountingService;

    public ProductionLogService(CompanyContextService companyContextService,
                                ProductionLogRepository logRepository,
                                ProductionBrandRepository brandRepository,
                                ProductionProductRepository productRepository,
                                RawMaterialRepository rawMaterialRepository,
                                FinishedGoodRepository finishedGoodRepository,
                                RawMaterialBatchRepository rawMaterialBatchRepository,
                                RawMaterialMovementRepository rawMaterialMovementRepository,
                                FinishedGoodBatchRepository finishedGoodBatchRepository,
                                InventoryMovementRepository inventoryMovementRepository,
                                AccountingService accountingService) {
        this.companyContextService = companyContextService;
        this.logRepository = logRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.finishedGoodRepository = finishedGoodRepository;
        this.rawMaterialBatchRepository = rawMaterialBatchRepository;
        this.rawMaterialMovementRepository = rawMaterialMovementRepository;
        this.finishedGoodBatchRepository = finishedGoodBatchRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.accountingService = accountingService;
    }

    @Transactional
    public ProductionLogDetailDto createLog(ProductionLogRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        ProductionBrand brand = brandRepository.findByCompanyAndId(company, request.brandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        ProductionProduct product = productRepository.findByCompanyAndId(company, request.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (!product.getBrand().getId().equals(brand.getId())) {
            throw new IllegalArgumentException("Product does not belong to brand");
        }
        BigDecimal batchSize = positive(request.batchSize(), "batchSize");
        BigDecimal producedQty = positive(request.producedQuantity(), "producedQuantity");
        String unitOfMeasure = StringUtils.hasText(request.unitOfMeasure())
                ? request.unitOfMeasure().trim()
                : Optional.ofNullable(product.getUnitOfMeasure()).filter(StringUtils::hasText).orElse("UNIT");
        String productionCode = nextProductionCode(company);

        ProductionLog log = new ProductionLog();
        log.setCompany(company);
        log.setBrand(brand);
        log.setProduct(product);
        log.setProductionCode(productionCode);
        log.setBatchColour(clean(request.batchColour()));
        log.setBatchSize(batchSize);
        log.setUnitOfMeasure(unitOfMeasure);
        log.setProducedQuantity(producedQty);
        log.setProducedAt(resolveProducedAt(request.producedAt()));
        log.setNotes(clean(request.notes()));
        log.setCreatedBy(clean(request.createdBy()));

        if (request.materials() == null || request.materials().isEmpty()) {
            throw new IllegalArgumentException("Materials are required");
        }

        MaterialIssueSummary issueSummary = issueMaterials(company, log, request.materials());
        log.setMaterialCostTotal(issueSummary.totalCost());
        log.setUnitCost(calculateUnitCost(issueSummary.totalCost(), producedQty));

        ProductionLog saved = logRepository.save(log);

        boolean restock = request.addToFinishedGoods() == null || Boolean.TRUE.equals(request.addToFinishedGoods());
        FinishedGoodReceipt receipt = null;
        if (restock) {
            receipt = restockFinishedGood(company, product, saved, producedQty, saved.getUnitCost());
            if (receipt != null) {
                saved.setFinishedGoodBatch(receipt.batch());
            }
        }

        postMaterialJournal(company, saved, product, issueSummary);
        if (restock && receipt != null) {
            postCompletionJournal(company, saved, product, receipt.finishedGood());
        }

        return toDetailDto(saved);
    }

    public List<ProductionLogDto> recentLogs() {
        Company company = companyContextService.requireCurrentCompany();
        return logRepository.findTop25ByCompanyOrderByProducedAtDesc(company).stream()
                .map(this::toDto)
                .toList();
    }

    public ProductionLogDetailDto getLog(Long id) {
        Company company = companyContextService.requireCurrentCompany();
        ProductionLog log = logRepository.findByCompanyAndId(company, id)
                .orElseThrow(() -> new IllegalArgumentException("Production log not found"));
        return toDetailDto(log);
    }

    private MaterialIssueSummary issueMaterials(Company company,
                                                ProductionLog log,
                                                List<ProductionLogRequest.MaterialUsageRequest> usages) {
        BigDecimal totalCost = BigDecimal.ZERO;
        Map<Long, BigDecimal> accountTotals = new HashMap<>();
        for (ProductionLogRequest.MaterialUsageRequest usage : usages) {
            MaterialConsumption consumption = consumeMaterial(company, log, usage);
            log.getMaterials().add(consumption.material());
            totalCost = totalCost.add(consumption.totalCost());
            accountTotals.merge(consumption.inventoryAccountId(), consumption.totalCost(), BigDecimal::add);
        }
        return new MaterialIssueSummary(totalCost, Map.copyOf(accountTotals));
    }

    private MaterialConsumption consumeMaterial(Company company,
                                                ProductionLog log,
                                                ProductionLogRequest.MaterialUsageRequest usage) {
        BigDecimal qty = positive(usage.quantity(), "materials.quantity");
        RawMaterial rawMaterial = rawMaterialRepository.findByCompanyAndId(company, usage.rawMaterialId())
                .orElseThrow(() -> new IllegalArgumentException("Raw material not found"));
        if (rawMaterial.getCurrentStock().compareTo(qty) < 0) {
            throw new IllegalArgumentException("Insufficient stock for " + rawMaterial.getName());
        }
        if (rawMaterial.getInventoryAccountId() == null) {
            throw new IllegalStateException("Raw material " + rawMaterial.getName() + " missing inventory account");
        }

        BigDecimal totalCost = issueFromBatches(rawMaterial, qty, log.getProductionCode());
        rawMaterial.setCurrentStock(rawMaterial.getCurrentStock().subtract(qty));
        rawMaterialRepository.save(rawMaterial);

        ProductionLogMaterial material = new ProductionLogMaterial();
        material.setLog(log);
        material.setRawMaterial(rawMaterial);
        material.setMaterialName(rawMaterial.getName());
        material.setQuantity(qty);
        material.setUnitOfMeasure(StringUtils.hasText(usage.unitOfMeasure())
                ? usage.unitOfMeasure().trim()
                : rawMaterial.getUnitType());
        material.setCostPerUnit(calculateUnitCost(totalCost, qty));
        material.setTotalCost(totalCost);
        return new MaterialConsumption(material, totalCost, rawMaterial.getInventoryAccountId());
    }

    private BigDecimal issueFromBatches(RawMaterial rawMaterial, BigDecimal requiredQty, String referenceId) {
        List<RawMaterialBatch> batches = rawMaterialBatchRepository.findByRawMaterial(rawMaterial).stream()
                .sorted(Comparator.comparing(RawMaterialBatch::getReceivedAt))
                .toList();
        BigDecimal remaining = requiredQty;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (RawMaterialBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = Optional.ofNullable(batch.getQuantity()).orElse(BigDecimal.ZERO);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal take = available.min(remaining);
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            batch.setQuantity(available.subtract(take));
            rawMaterialBatchRepository.save(batch);

            RawMaterialMovement movement = new RawMaterialMovement();
            movement.setRawMaterial(rawMaterial);
            movement.setRawMaterialBatch(batch);
            movement.setReferenceType(REFERENCE_TYPE_PRODUCTION_LOG);
            movement.setReferenceId(referenceId);
            movement.setMovementType(MOVEMENT_TYPE_ISSUE);
            movement.setQuantity(take);
            BigDecimal unitCost = Optional.ofNullable(batch.getCostPerUnit()).orElse(BigDecimal.ZERO);
            movement.setUnitCost(unitCost);
            rawMaterialMovementRepository.save(movement);

            totalCost = totalCost.add(unitCost.multiply(take));
            remaining = remaining.subtract(take);
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Insufficient batch availability for " + rawMaterial.getName());
        }
        return totalCost;
    }

    private FinishedGoodReceipt restockFinishedGood(Company company,
                                                    ProductionProduct product,
                                                    ProductionLog log,
                                                    BigDecimal producedQty,
                                                    BigDecimal unitCost) {
        FinishedGood finishedGood = finishedGoodRepository.findByCompanyAndProductCode(company, product.getSkuCode())
                .orElseGet(() -> initializeFinishedGood(company, product));
        if (finishedGood.getValuationAccountId() == null) {
            throw new IllegalStateException("Finished good " + finishedGood.getProductCode() + " missing valuation account");
        }
        FinishedGoodBatch batch = new FinishedGoodBatch();
        batch.setFinishedGood(finishedGood);
        batch.setBatchCode(log.getProductionCode());
        batch.setQuantityTotal(producedQty);
        batch.setQuantityAvailable(producedQty);
        batch.setUnitCost(Optional.ofNullable(unitCost).orElse(BigDecimal.ZERO));
        batch.setManufacturedAt(log.getProducedAt());
        FinishedGoodBatch savedBatch = finishedGoodBatchRepository.save(batch);

        BigDecimal current = Optional.ofNullable(finishedGood.getCurrentStock()).orElse(BigDecimal.ZERO);
        finishedGood.setCurrentStock(current.add(producedQty));
        finishedGoodRepository.save(finishedGood);

        InventoryMovement movement = new InventoryMovement();
        movement.setFinishedGood(finishedGood);
        movement.setFinishedGoodBatch(savedBatch);
        movement.setReferenceType(REFERENCE_TYPE_PRODUCTION_LOG);
        movement.setReferenceId(log.getProductionCode());
        movement.setMovementType(MOVEMENT_TYPE_RECEIPT);
        movement.setQuantity(producedQty);
        movement.setUnitCost(Optional.ofNullable(unitCost).orElse(BigDecimal.ZERO));
        inventoryMovementRepository.save(movement);

        return new FinishedGoodReceipt(finishedGood, savedBatch);
    }

    private FinishedGood initializeFinishedGood(Company company, ProductionProduct product) {
        Long valuationAccountId = metadataLong(product, "fgValuationAccountId");
        Long cogsAccountId = metadataLong(product, "fgCogsAccountId");
        Long revenueAccountId = metadataLong(product, "fgRevenueAccountId");
        Long discountAccountId = metadataLong(product, "fgDiscountAccountId");
        Long taxAccountId = metadataLong(product, "fgTaxAccountId");
        if (valuationAccountId == null || cogsAccountId == null || revenueAccountId == null
                || discountAccountId == null || taxAccountId == null) {
            throw new IllegalStateException("Product " + product.getProductName()
                    + " missing fgValuationAccountId, fgCogsAccountId, fgRevenueAccountId, "
                    + "fgDiscountAccountId or fgTaxAccountId in metadata");
        }
        FinishedGood created = new FinishedGood();
        created.setCompany(company);
        created.setProductCode(product.getSkuCode());
        created.setName(product.getProductName());
        created.setUnit(Optional.ofNullable(product.getUnitOfMeasure()).orElse("UNIT"));
        created.setCostingMethod("FIFO");
        created.setValuationAccountId(valuationAccountId);
        created.setCogsAccountId(cogsAccountId);
        created.setRevenueAccountId(revenueAccountId);
        created.setDiscountAccountId(discountAccountId);
        created.setTaxAccountId(taxAccountId);
        created.setCurrentStock(BigDecimal.ZERO);
        created.setReservedStock(BigDecimal.ZERO);
        return finishedGoodRepository.save(created);
    }

    private void postMaterialJournal(Company company,
                                     ProductionLog log,
                                     ProductionProduct product,
                                     MaterialIssueSummary summary) {
        if (summary.totalCost().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long wipAccountId = requireWipAccountId(product);
        List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>();
        lines.add(new JournalEntryRequest.JournalLineRequest(
                wipAccountId,
                "WIP charge " + log.getProductionCode(),
                summary.totalCost(),
                BigDecimal.ZERO
        ));
        for (Map.Entry<Long, BigDecimal> entry : summary.accountTotals().entrySet()) {
            lines.add(new JournalEntryRequest.JournalLineRequest(
                    entry.getKey(),
                    "Raw material issue " + log.getProductionCode(),
                    BigDecimal.ZERO,
                    entry.getValue()
            ));
        }
        accountingService.createJournalEntry(new JournalEntryRequest(
                log.getProductionCode() + "-RM",
                resolveJournalDate(company, log),
                "Raw material consumption for " + log.getProductionCode(),
                null,
                lines
        ));
    }

    private void postCompletionJournal(Company company,
                                       ProductionLog log,
                                       ProductionProduct product,
                                       FinishedGood finishedGood) {
        BigDecimal totalCost = log.getMaterialCostTotal();
        if (totalCost.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        Long wipAccountId = requireWipAccountId(product);
        Long fgAccountId = finishedGood.getValuationAccountId();
        if (fgAccountId == null) {
            throw new IllegalStateException("Finished good " + finishedGood.getProductCode() + " missing valuation account");
        }
        List<JournalEntryRequest.JournalLineRequest> lines = List.of(
                new JournalEntryRequest.JournalLineRequest(
                        fgAccountId,
                        "Finished goods receipt " + log.getProductionCode(),
                        totalCost,
                        BigDecimal.ZERO
                ),
                new JournalEntryRequest.JournalLineRequest(
                        wipAccountId,
                        "Close WIP " + log.getProductionCode(),
                        BigDecimal.ZERO,
                        totalCost
                )
        );
        accountingService.createJournalEntry(new JournalEntryRequest(
                log.getProductionCode() + "-FG",
                resolveJournalDate(company, log),
                "Production completion for " + log.getProductionCode(),
                null,
                lines
        ));
    }

    private LocalDate resolveJournalDate(Company company, ProductionLog log) {
        ZoneId zoneId = Optional.ofNullable(company.getTimezone())
                .filter(StringUtils::hasText)
                .map(ZoneId::of)
                .orElse(ZoneOffset.UTC);
        return log.getProducedAt().atZone(zoneId).toLocalDate();
    }

    private Long requireWipAccountId(ProductionProduct product) {
        Long accountId = metadataLong(product, "wipAccountId");
        if (accountId == null) {
            throw new IllegalStateException("Product " + product.getProductName() + " missing wipAccountId metadata");
        }
        return accountId;
    }

    private Long metadataLong(ProductionProduct product, String key) {
        if (product.getMetadata() == null) {
            return null;
        }
        Object candidate = product.getMetadata().get(key);
        if (candidate instanceof Number number) {
            return number.longValue();
        }
        if (candidate instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal calculateUnitCost(BigDecimal total, BigDecimal quantity) {
        if (total == null || quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(quantity, 6, COST_ROUNDING);
    }

    private record MaterialIssueSummary(BigDecimal totalCost, Map<Long, BigDecimal> accountTotals) {}

    private record MaterialConsumption(ProductionLogMaterial material, BigDecimal totalCost, Long inventoryAccountId) {}

    private record FinishedGoodReceipt(FinishedGood finishedGood, FinishedGoodBatch batch) {}

    private String nextProductionCode(Company company) {
        String prefix = "PROD-" + CODE_DATE.format(LocalDate.now(ZoneOffset.UTC));
        return logRepository.findTopByCompanyAndProductionCodeStartingWithOrderByProductionCodeDesc(company, prefix)
                .map(ProductionLog::getProductionCode)
                .map(existing -> incrementCode(existing, prefix))
                .orElse(prefix + "-001");
    }

    private String incrementCode(String existing, String prefix) {
        try {
            String[] parts = existing.split("-");
            int seq = Integer.parseInt(parts[parts.length - 1]);
            return prefix + "-" + String.format("%03d", seq + 1);
        } catch (Exception ignored) {
            return prefix + "-001";
        }
    }

    private Instant resolveProducedAt(String producedAt) {
        if (!StringUtils.hasText(producedAt)) {
            return Instant.now();
        }
        try {
            return OffsetDateTime.parse(producedAt).toInstant();
        } catch (Exception ex) {
            try {
                return Instant.parse(producedAt);
            } catch (Exception inner) {
                throw new IllegalArgumentException("Invalid producedAt format");
            }
        }
    }

    private ProductionLogDto toDto(ProductionLog log) {
        return new ProductionLogDto(
                log.getId(),
                log.getPublicId(),
                log.getProductionCode(),
                log.getProducedAt(),
                log.getBrand().getName(),
                log.getProduct().getProductName(),
                log.getProduct().getSkuCode(),
                log.getBatchColour(),
                log.getBatchSize(),
                log.getUnitOfMeasure(),
                log.getProducedQuantity(),
                log.getCreatedBy(),
                log.getUnitCost()
        );
    }

    private ProductionLogDetailDto toDetailDto(ProductionLog log) {
        List<ProductionLogMaterialDto> materials = log.getMaterials().stream()
                .map(material -> new ProductionLogMaterialDto(
                        material.getRawMaterial() != null ? material.getRawMaterial().getId() : null,
                        material.getMaterialName(),
                        material.getQuantity(),
                        material.getUnitOfMeasure(),
                        material.getCostPerUnit(),
                        material.getTotalCost()
                ))
                .toList();
        FinishedGoodBatch batch = log.getFinishedGoodBatch();
        return new ProductionLogDetailDto(
                log.getId(),
                log.getPublicId(),
                log.getProductionCode(),
                log.getProducedAt(),
                log.getBrand().getName(),
                log.getProduct().getProductName(),
                log.getProduct().getSkuCode(),
                log.getBatchColour(),
                log.getBatchSize(),
                log.getUnitOfMeasure(),
                log.getProducedQuantity(),
                log.getMaterialCostTotal(),
                log.getUnitCost(),
                batch != null ? batch.getBatchCode() : null,
                batch != null ? batch.getPublicId() : null,
                log.getNotes(),
                log.getCreatedBy(),
                materials
        );
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
