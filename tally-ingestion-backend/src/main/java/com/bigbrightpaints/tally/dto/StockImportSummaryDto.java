package com.bigbrightpaints.tally.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImportSummaryDto {
    private Long runId;
    private String fileName;
    private Integer totalItems;
    private Map<String, Integer> itemTypeCounts;
    private Map<String, Integer> validationStatusCounts;
    private String status;
    private String message;
}
