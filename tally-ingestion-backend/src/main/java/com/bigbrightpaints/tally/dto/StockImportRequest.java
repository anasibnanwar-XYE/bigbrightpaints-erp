package com.bigbrightpaints.tally.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockImportRequest {
    private Long runId;
    private List<StockItemDto> items;
    private Long companyId;
    private String importMode; // "OPENING_STOCK" or "UPDATE_STOCK"
}
