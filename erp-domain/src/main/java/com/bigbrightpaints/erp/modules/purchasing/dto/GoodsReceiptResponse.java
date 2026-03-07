package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.bigbrightpaints.erp.shared.dto.DocumentLifecycleDto;
import com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record GoodsReceiptResponse(Long id,
                                   UUID publicId,
                                   String receiptNumber,
                                   LocalDate receiptDate,
                                   BigDecimal totalAmount,
                                   String status,
                                   String memo,
                                   Long supplierId,
                                   String supplierCode,
                                   String supplierName,
                                   Long purchaseOrderId,
                                   String purchaseOrderNumber,
                                   Instant createdAt,
                                   List<GoodsReceiptLineResponse> lines,
                                   DocumentLifecycleDto lifecycle,
                                   List<LinkedBusinessReferenceDto> linkedReferences) {

    public GoodsReceiptResponse(Long id,
                                UUID publicId,
                                String receiptNumber,
                                LocalDate receiptDate,
                                BigDecimal totalAmount,
                                String status,
                                String memo,
                                Long supplierId,
                                String supplierCode,
                                String supplierName,
                                Long purchaseOrderId,
                                String purchaseOrderNumber,
                                Instant createdAt,
                                List<GoodsReceiptLineResponse> lines) {
        this(
                id,
                publicId,
                receiptNumber,
                receiptDate,
                totalAmount,
                status,
                memo,
                supplierId,
                supplierCode,
                supplierName,
                purchaseOrderId,
                purchaseOrderNumber,
                createdAt,
                lines,
                null,
                List.of()
        );
    }
}
