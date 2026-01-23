package com.bigbrightpaints.erp.modules.purchasing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(Long id,
                                    UUID publicId,
                                    String orderNumber,
                                    LocalDate orderDate,
                                    BigDecimal totalAmount,
                                    String status,
                                    String memo,
                                    Long supplierId,
                                    String supplierCode,
                                    String supplierName,
                                    Instant createdAt,
                                    List<PurchaseOrderLineResponse> lines) {}
