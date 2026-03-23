package com.bigbrightpaints.erp.modules.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreditLimitRequestDto(Long id,
                                    UUID publicId,
                                    String dealerName,
                                    BigDecimal amountRequested,
                                    String status,
                                    String reason,
                                    Instant createdAt) {}
