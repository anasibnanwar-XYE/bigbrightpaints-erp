package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.MaterialType;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodBatchRequest;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodDto;
import com.bigbrightpaints.erp.modules.inventory.dto.FinishedGoodRequest;
import com.bigbrightpaints.erp.modules.inventory.dto.OpeningStockImportResponse;
import com.bigbrightpaints.erp.modules.inventory.dto.OpeningStockImportResponse.ImportError;
import com.bigbrightpaints.erp.modules.inventory.dto.RawMaterialRequest;
import com.bigbrightpaints.erp.modules.inventory.dto.RawMaterialDto;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OpeningStockImportService {

    private static final String DEFAULT_BATCH_REF = "OPENING";

    private final CompanyContextService companyContextService;
    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialBatchRepository rawMaterialBatchRepository;
    private final RawMaterialMovementRepository rawMaterialMovementRepository;
    private final FinishedGoodRepository finishedGoodRepository;
    private final FinishedGoodBatchRepository finishedGoodBatchRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final BatchNumberService batchNumberService;
    private final RawMaterialService rawMaterialService;
    private final FinishedGoodsService finishedGoodsService;

    public OpeningStockImportService(CompanyContextService companyContextService,
                                     RawMaterialRepository rawMaterialRepository,
                                     RawMaterialBatchRepository rawMaterialBatchRepository,
                                     RawMaterialMovementRepository rawMaterialMovementRepository,
                                     FinishedGoodRepository finishedGoodRepository,
                                     FinishedGoodBatchRepository finishedGoodBatchRepository,
                                     InventoryMovementRepository inventoryMovementRepository,
                                     BatchNumberService batchNumberService,
                                     RawMaterialService rawMaterialService,
                                     FinishedGoodsService finishedGoodsService) {
        this.companyContextService = companyContextService;
        this.rawMaterialRepository = rawMaterialRepository;
        this.rawMaterialBatchRepository = rawMaterialBatchRepository;
        this.rawMaterialMovementRepository = rawMaterialMovementRepository;
        this.finishedGoodRepository = finishedGoodRepository;
        this.finishedGoodBatchRepository = finishedGoodBatchRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.batchNumberService = batchNumberService;
        this.rawMaterialService = rawMaterialService;
        this.finishedGoodsService = finishedGoodsService;
    }

    @Transactional
    public OpeningStockImportResponse importOpeningStock(MultipartFile file) {
        Company company = companyContextService.requireCurrentCompany();
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        int rowsProcessed = 0;
        int rawMaterialsCreated = 0;
        int rawMaterialBatchesCreated = 0;
        int finishedGoodsCreated = 0;
        int finishedGoodBatchesCreated = 0;
        List<ImportError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                OpeningRow row;
                try {
                    row = OpeningRow.from(record);
                } catch (IllegalArgumentException ex) {
                    errors.add(new ImportError(record.getRecordNumber(), ex.getMessage()));
                    continue;
                }
                if (row == null) {
                    continue;
                }
                try {
                    if (row.type == StockType.RAW_MATERIAL) {
                        boolean created = handleRawMaterial(company, row);
                        if (created) {
                            rawMaterialsCreated++;
                        }
                        rawMaterialBatchesCreated++;
                    } else {
                        boolean created = handleFinishedGood(company, row);
                        if (created) {
                            finishedGoodsCreated++;
                        }
                        finishedGoodBatchesCreated++;
                    }
                    rowsProcessed++;
                } catch (IllegalArgumentException ex) {
                    errors.add(new ImportError(record.getRecordNumber(), ex.getMessage()));
                } catch (Exception ex) {
                    errors.add(new ImportError(record.getRecordNumber(), "Unexpected error: " + ex.getMessage()));
                }
            }

            return new OpeningStockImportResponse(
                    rowsProcessed,
                    rawMaterialsCreated,
                    rawMaterialBatchesCreated,
                    finishedGoodsCreated,
                    finishedGoodBatchesCreated,
                    errors
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read CSV file", ex);
        }
    }

    private boolean handleRawMaterial(Company company, OpeningRow row) {
        RawMaterial material = resolveRawMaterial(company, row);
        String unit = firstNonBlank(row.unitType, row.unit, material.getUnitType());
        if (!StringUtils.hasText(unit)) {
            throw new IllegalArgumentException("Unit is required for raw material " + row.displayKey());
        }
        BigDecimal quantity = requirePositive(row.quantity, "quantity");
        BigDecimal unitCost = requirePositive(row.unitCost, "unit_cost");
        String batchCode = StringUtils.hasText(row.batchCode)
                ? row.batchCode.trim()
                : batchNumberService.nextRawMaterialBatchCode(material);

        RawMaterialBatch batch = new RawMaterialBatch();
        batch.setRawMaterial(material);
        batch.setBatchCode(batchCode);
        batch.setQuantity(quantity);
        batch.setUnit(unit);
        batch.setCostPerUnit(unitCost);
        batch.setSupplierName(DEFAULT_BATCH_REF);
        RawMaterialBatch savedBatch = rawMaterialBatchRepository.save(batch);

        BigDecimal currentStock = Optional.ofNullable(material.getCurrentStock()).orElse(BigDecimal.ZERO);
        material.setCurrentStock(currentStock.add(quantity));
        rawMaterialRepository.save(material);

        RawMaterialMovement movement = new RawMaterialMovement();
        movement.setRawMaterial(material);
        movement.setRawMaterialBatch(savedBatch);
        movement.setReferenceType(InventoryReference.OPENING_STOCK);
        movement.setReferenceId(batchCode);
        movement.setMovementType("RECEIPT");
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        rawMaterialMovementRepository.save(movement);

        return row.createdNew;
    }

    private boolean handleFinishedGood(Company company, OpeningRow row) {
        FinishedGood finishedGood = resolveFinishedGood(company, row);
        BigDecimal quantity = requirePositive(row.quantity, "quantity");
        BigDecimal unitCost = requirePositive(row.unitCost, "unit_cost");
        Instant manufacturedAt = row.manufacturedDate != null
                ? row.manufacturedDate.atStartOfDay(resolveZone(company)).toInstant()
                : null;
        String batchCode = StringUtils.hasText(row.batchCode)
                ? row.batchCode.trim()
                : batchNumberService.nextFinishedGoodBatchCode(finishedGood, row.manufacturedDate);

        FinishedGoodBatch batch = new FinishedGoodBatch();
        batch.setFinishedGood(finishedGood);
        batch.setBatchCode(batchCode);
        batch.setQuantityTotal(quantity);
        batch.setQuantityAvailable(quantity);
        batch.setUnitCost(unitCost);
        batch.setManufacturedAt(manufacturedAt != null ? manufacturedAt : Instant.now());
        FinishedGoodBatch savedBatch = finishedGoodBatchRepository.save(batch);

        BigDecimal currentStock = Optional.ofNullable(finishedGood.getCurrentStock()).orElse(BigDecimal.ZERO);
        finishedGood.setCurrentStock(currentStock.add(quantity));
        finishedGoodRepository.save(finishedGood);

        InventoryMovement movement = new InventoryMovement();
        movement.setFinishedGood(finishedGood);
        movement.setFinishedGoodBatch(savedBatch);
        movement.setReferenceType(InventoryReference.OPENING_STOCK);
        movement.setReferenceId(batchCode);
        movement.setMovementType("RECEIPT");
        movement.setQuantity(quantity);
        movement.setUnitCost(unitCost);
        inventoryMovementRepository.save(movement);

        return row.createdNew;
    }

    private RawMaterial resolveRawMaterial(Company company, OpeningRow row) {
        if (StringUtils.hasText(row.sku)) {
            Optional<RawMaterial> existing = rawMaterialRepository.findByCompanyAndSku(company, row.sku.trim());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (!StringUtils.hasText(row.name)) {
            throw new IllegalArgumentException("Raw material name is required when SKU is missing: " + row.displayKey());
        }
        String unitType = firstNonBlank(row.unitType, row.unit);
        if (!StringUtils.hasText(unitType)) {
            throw new IllegalArgumentException("Unit type is required for raw material " + row.displayKey());
        }
        RawMaterialRequest request = new RawMaterialRequest(
                row.name.trim(),
                StringUtils.hasText(row.sku) ? row.sku.trim() : null,
                unitType.trim(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
        RawMaterialDto created = rawMaterialService.createRawMaterial(request);
        RawMaterial material = rawMaterialRepository.findByCompanyAndId(company, created.id())
                .orElseThrow(() -> new IllegalStateException("Raw material creation failed"));
        if (row.materialType != null) {
            material.setMaterialType(row.materialType);
            rawMaterialRepository.save(material);
        }
        row.markCreated();
        return material;
    }

    private FinishedGood resolveFinishedGood(Company company, OpeningRow row) {
        if (!StringUtils.hasText(row.sku)) {
            throw new IllegalArgumentException("Finished good SKU is required");
        }
        Optional<FinishedGood> existing = finishedGoodRepository.findByCompanyAndProductCode(company, row.sku.trim());
        if (existing.isPresent()) {
            return existing.get();
        }
        String name = StringUtils.hasText(row.name) ? row.name.trim() : row.sku.trim();
        String unit = StringUtils.hasText(row.unit) ? row.unit.trim() : "PCS";
        FinishedGoodRequest request = new FinishedGoodRequest(
                row.sku.trim(),
                name,
                unit,
                null,
                null,
                null,
                null,
                null,
                null
        );
        FinishedGoodDto created = finishedGoodsService.createFinishedGood(request);
        FinishedGood finishedGood = finishedGoodRepository.findByCompanyAndId(company, created.id())
                .orElseThrow(() -> new IllegalStateException("Finished good creation failed"));
        row.markCreated();
        return finishedGood;
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private static ZoneId resolveZone(Company company) {
        return ZoneId.of(company.getTimezone() == null ? "UTC" : company.getTimezone());
    }

    private enum StockType {
        RAW_MATERIAL,
        FINISHED_GOOD
    }

    private static final class OpeningRow {
        private final StockType type;
        private final String sku;
        private final String name;
        private final String unit;
        private final String unitType;
        private final String batchCode;
        private final BigDecimal quantity;
        private final BigDecimal unitCost;
        private final MaterialType materialType;
        private final LocalDate manufacturedDate;
        private boolean createdNew;

        private OpeningRow(StockType type,
                           String sku,
                           String name,
                           String unit,
                           String unitType,
                           String batchCode,
                           BigDecimal quantity,
                           BigDecimal unitCost,
                           MaterialType materialType,
                           LocalDate manufacturedDate) {
            this.type = type;
            this.sku = sku;
            this.name = name;
            this.unit = unit;
            this.unitType = unitType;
            this.batchCode = batchCode;
            this.quantity = quantity;
            this.unitCost = unitCost;
            this.materialType = materialType;
            this.manufacturedDate = manufacturedDate;
        }

        static OpeningRow from(CSVRecord record) {
            String typeValue = readValue(record, "type", "item_type");
            String sku = readValue(record, "sku", "product_code", "sku_code");
            String name = readValue(record, "name", "product_name");
            String unit = readValue(record, "unit", "unit_of_measure");
            String unitType = readValue(record, "unit_type");
            String batchCode = readValue(record, "batch_code", "batch");
            BigDecimal quantity = decimal(record, "quantity", "qty");
            BigDecimal unitCost = decimal(record, "unit_cost", "cost_per_unit", "cost");
            String materialTypeRaw = readValue(record, "material_type");
            MaterialType materialType = parseMaterialType(materialTypeRaw);
            LocalDate manufacturedDate = date(record, "manufactured_at", "batch_date");

            if (!StringUtils.hasText(typeValue) && !StringUtils.hasText(sku) && !StringUtils.hasText(name)) {
                return null;
            }
            StockType type = parseType(typeValue);
            return new OpeningRow(type, sku, name, unit, unitType, batchCode, quantity, unitCost, materialType, manufacturedDate);
        }

        String displayKey() {
            String id = StringUtils.hasText(sku) ? sku : name;
            return StringUtils.hasText(id) ? id : "row";
        }

        void markCreated() {
            this.createdNew = true;
        }

        private static StockType parseType(String value) {
            if (!StringUtils.hasText(value)) {
                throw new IllegalArgumentException("type is required (RAW_MATERIAL or FINISHED_GOOD)");
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("RAW") || normalized.equals("RM") || normalized.equals("RAW_MATERIAL")) {
                return StockType.RAW_MATERIAL;
            }
            if (normalized.startsWith("FINISH") || normalized.equals("FG") || normalized.equals("FINISHED_GOOD")) {
                return StockType.FINISHED_GOOD;
            }
            throw new IllegalArgumentException("Unknown type: " + value);
        }

        private static MaterialType parseMaterialType(String value) {
            if (!StringUtils.hasText(value)) {
                return null;
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "PACKAGING" -> MaterialType.PACKAGING;
                case "PRODUCTION" -> MaterialType.PRODUCTION;
                default -> throw new IllegalArgumentException("Unknown material_type: " + value);
            };
        }

        private static String readValue(CSVRecord record, String... keys) {
            Map<String, String> map = record.toMap();
            for (String key : keys) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                        String value = entry.getValue();
                        return StringUtils.hasText(value) ? value.trim() : null;
                    }
                }
            }
            return null;
        }

        private static BigDecimal decimal(CSVRecord record, String... keys) {
            String value = readValue(record, keys);
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid numeric value: " + value);
            }
        }

        private static LocalDate date(CSVRecord record, String... keys) {
            String value = readValue(record, keys);
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                return LocalDate.parse(value.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid date value: " + value + " (expected YYYY-MM-DD)");
            }
        }
    }
}
