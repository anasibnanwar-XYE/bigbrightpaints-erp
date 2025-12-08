package com.bigbrightpaints.tally.service;

import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingStockItem;
import com.bigbrightpaints.tally.dto.StockImportSummaryDto;
import com.bigbrightpaints.tally.dto.StockItemDto;
import com.bigbrightpaints.tally.repository.IngestionRunRepository;
import com.bigbrightpaints.tally.repository.staging.StagingStockItemRepository;
import com.bigbrightpaints.tally.service.xml.TallyXmlStockParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockImportService {

    private final TallyXmlStockParser xmlParser;
    private final StagingStockItemRepository stockItemRepository;
    private final IngestionRunRepository ingestionRunRepository;

    /**
     * Upload and parse Tally XML stock summary file
     */
    @Transactional
    public StockImportSummaryDto uploadAndParseXml(MultipartFile file, Long companyId) throws Exception {
        log.info("Processing XML file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

        // Create ingestion run with required fields
        Map<String, Object> config = new HashMap<>();
        config.put("fileName", file.getOriginalFilename());
        config.put("fileSize", file.getSize());
        config.put("fileType", "XML_STOCK_SUMMARY");

        IngestionRun run = IngestionRun.builder()
                .companyId(companyId != null ? companyId : 1L)
                .runType(IngestionRun.RunType.FULL_IMPORT)
                .status(IngestionRun.RunStatus.RUNNING)
                .dryRun(false)
                .sourceSystem("TALLY_XML")
                .startedAt(Instant.now())
                .totalFiles(1)
                .filesProcessed(0)
                .totalRows(0)
                .rowsProcessed(0)
                .rowsSucceeded(0)
                .rowsFailed(0)
                .rowsSkipped(0)
                .configuration(config)
                .build();
        run = ingestionRunRepository.save(run);

        String fileName = file.getOriginalFilename();

        try (InputStream inputStream = file.getInputStream()) {
            // Parse XML
            List<StagingStockItem> items = xmlParser.parseStockSummary(inputStream, run);

            // Save to database
            stockItemRepository.saveAll(items);

            // Update run status
            run.setStatus(IngestionRun.RunStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            run.setTotalRows(items.size());
            run.setRowsProcessed(items.size());
            run.setRowsSucceeded(items.size());
            run.setFilesProcessed(1);
            ingestionRunRepository.save(run);

            // Build summary
            return buildSummary(run, items, fileName);

        } catch (Exception e) {
            log.error("Error processing XML file: {}", e.getMessage(), e);
            run.failRun(e.getMessage());
            ingestionRunRepository.save(run);
            throw e;
        }
    }

    /**
     * Get stock items for a run
     */
    @Transactional(readOnly = true)
    public List<StockItemDto> getStockItems(Long runId) {
        return stockItemRepository.findByRunIdOrderByRowNumber(runId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Update stock item (for user edits)
     */
    @Transactional
    public StockItemDto updateStockItem(Long id, StockItemDto dto) {
        StagingStockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Stock item not found: " + id));

        // Update user-editable fields
        item.setItemType(dto.getItemType());
        item.setCategory(dto.getCategory());
        item.setBrand(dto.getBrand());
        item.setSizeLabel(dto.getSizeLabel());
        item.setColor(dto.getColor());
        item.setMappedSku(dto.getMappedSku());
        item.setMappedProductCode(dto.getMappedProductCode());
        item.setBaseProductName(dto.getBaseProductName());
        item.setGstRate(dto.getGstRate());
        item.setHsnCode(dto.getHsnCode());
        item.setNotes(dto.getNotes());

        // Validate
        validateStockItem(item);

        item = stockItemRepository.save(item);
        return toDto(item);
    }

    /**
     * Bulk update stock items
     */
    @Transactional
    public List<StockItemDto> bulkUpdateStockItems(List<StockItemDto> dtos) {
        List<StockItemDto> updated = new ArrayList<>();
        for (StockItemDto dto : dtos) {
            if (dto.getId() != null) {
                updated.add(updateStockItem(dto.getId(), dto));
            }
        }
        return updated;
    }

    /**
     * Get import summary
     */
    @Transactional(readOnly = true)
    public StockImportSummaryDto getImportSummary(Long runId) {
        IngestionRun run = ingestionRunRepository.findById(runId)
                .orElseThrow(() -> new RuntimeException("Run not found: " + runId));
        List<StagingStockItem> items = stockItemRepository.findByRunIdOrderByRowNumber(runId);

        // Get fileName from configuration
        String fileName = "unknown";
        if (run.getConfiguration() != null && run.getConfiguration().get("fileName") != null) {
            fileName = run.getConfiguration().get("fileName").toString();
        }

        return buildSummary(run, items, fileName);
    }

    /**
     * Build summary from items
     */
    private StockImportSummaryDto buildSummary(IngestionRun run, List<StagingStockItem> items, String fileName) {
        Map<String, Integer> itemTypeCounts = new HashMap<>();
        Map<String, Integer> validationStatusCounts = new HashMap<>();

        for (StagingStockItem item : items) {
            String itemType = item.getItemType() != null ? item.getItemType().name() : "UNKNOWN";
            itemTypeCounts.put(itemType, itemTypeCounts.getOrDefault(itemType, 0) + 1);

            String validationStatus = item.getValidationStatus() != null ? item.getValidationStatus().name() : "PENDING";
            validationStatusCounts.put(validationStatus, validationStatusCounts.getOrDefault(validationStatus, 0) + 1);
        }

        return StockImportSummaryDto.builder()
                .runId(run.getId())
                .fileName(fileName)
                .totalItems(items.size())
                .itemTypeCounts(itemTypeCounts)
                .validationStatusCounts(validationStatusCounts)
                .status(run.getStatus() != null ? run.getStatus().name() : "UNKNOWN")
                .message("Successfully parsed " + items.size() + " items")
                .build();
    }

    /**
     * Validate stock item
     */
    private void validateStockItem(StagingStockItem item) {
        Map<String, String> errors = new HashMap<>();

        if (item.getItemType() == null || item.getItemType() == StagingStockItem.ItemType.UNKNOWN) {
            errors.put("itemType", "Item type is required");
        }

        if (item.getClosingQuantity() == null || item.getClosingQuantity().compareTo(BigDecimal.ZERO) < 0) {
            errors.put("quantity", "Quantity must be positive");
        }

        if (item.getUnitOfMeasure() == null || item.getUnitOfMeasure().trim().isEmpty()) {
            errors.put("unit", "Unit of measure is required");
        }

        if (errors.isEmpty()) {
            item.setValidationStatus(StagingStockItem.ValidationStatus.VALID);
            item.setValidationErrors(null);
        } else {
            item.setValidationStatus(StagingStockItem.ValidationStatus.INVALID);
            item.setValidationErrors(errors);
        }
    }

    /**
     * Convert entity to DTO
     */
    private StockItemDto toDto(StagingStockItem item) {
        return StockItemDto.builder()
                .id(item.getId())
                .rowNumber(item.getRowNumber())
                .itemName(item.getItemName())
                .closingQuantity(item.getClosingQuantity())
                .unitOfMeasure(item.getUnitOfMeasure())
                .closingRate(item.getClosingRate())
                .closingAmount(item.getClosingAmount())
                .itemType(item.getItemType())
                .category(item.getCategory())
                .brand(item.getBrand())
                .sizeLabel(item.getSizeLabel())
                .color(item.getColor())
                .mappedSku(item.getMappedSku())
                .mappedProductCode(item.getMappedProductCode())
                .baseProductName(item.getBaseProductName())
                .gstRate(item.getGstRate())
                .hsnCode(item.getHsnCode())
                .notes(item.getNotes())
                .validationStatus(item.getValidationStatus())
                .validationErrors(item.getValidationErrors())
                .processed(item.getProcessed())
                .rawData(item.getRawData())
                .build();
    }
}
