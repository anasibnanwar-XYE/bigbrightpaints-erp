package com.bigbrightpaints.tally.controller;

import com.bigbrightpaints.tally.dto.StockImportSummaryDto;
import com.bigbrightpaints.tally.dto.StockItemDto;
import com.bigbrightpaints.tally.service.StockImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/stock-import")
@RequiredArgsConstructor
@Tag(name = "Stock Import", description = "Tally XML Stock Summary Import API")
@CrossOrigin(origins = "*")
public class StockImportController {

    private final StockImportService stockImportService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload Tally XML stock summary file")
    public ResponseEntity<StockImportSummaryDto> uploadXmlFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "companyId", required = false, defaultValue = "1") Long companyId) {
        try {
            log.info("Received file upload: name={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        StockImportSummaryDto.builder()
                                .status("ERROR")
                                .message("File is empty")
                                .build()
                );
            }

            StockImportSummaryDto summary = stockImportService.uploadAndParseXml(file, companyId);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.ok(
                    StockImportSummaryDto.builder()
                            .status("ERROR")
                            .message("Error: " + e.getClass().getSimpleName() + " - " + e.getMessage())
                            .build()
            );
        }
    }

    @GetMapping("/runs/{runId}/items")
    @Operation(summary = "Get all stock items for a run")
    public ResponseEntity<List<StockItemDto>> getStockItems(@PathVariable Long runId) {
        List<StockItemDto> items = stockImportService.getStockItems(runId);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/runs/{runId}/summary")
    @Operation(summary = "Get import summary for a run")
    public ResponseEntity<StockImportSummaryDto> getImportSummary(@PathVariable Long runId) {
        StockImportSummaryDto summary = stockImportService.getImportSummary(runId);
        return ResponseEntity.ok(summary);
    }

    @PutMapping("/items/{id}")
    @Operation(summary = "Update a single stock item")
    public ResponseEntity<StockItemDto> updateStockItem(
            @PathVariable Long id,
            @RequestBody StockItemDto dto) {
        StockItemDto updated = stockImportService.updateStockItem(id, dto);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/items/bulk")
    @Operation(summary = "Bulk update stock items")
    public ResponseEntity<List<StockItemDto>> bulkUpdateStockItems(
            @RequestBody List<StockItemDto> items) {
        List<StockItemDto> updated = stockImportService.bulkUpdateStockItems(items);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/item-types")
    @Operation(summary = "Get available item types for classification")
    public ResponseEntity<Map<String, String>> getItemTypes() {
        return ResponseEntity.ok(Map.of(
                "RAW_MATERIAL", "Raw Materials (pigments, chemicals, resins)",
                "FINISHED_PRODUCT", "Finished Products (paints)",
                "PACKAGING", "Packaging Materials (buckets, tins, boxes)",
                "ASSET", "Assets (machinery, equipment)",
                "EXPENSE", "Expense Items (t-shirts, pens, stationery)",
                "UNKNOWN", "Unknown / Not Classified"
        ));
    }
}
