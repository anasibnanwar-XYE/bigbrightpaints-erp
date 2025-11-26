package com.bigbrightpaints.erp.modules.inventory.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PackagingSlipDto(Long id,
                               UUID publicId,
                               Long salesOrderId,
                               String orderNumber,
                               String dealerName,
                               String slipNumber,
                               String status,
                               Instant createdAt,
                               Instant confirmedAt,
                               String confirmedBy,
                               Instant dispatchedAt,
                               String dispatchNotes,
                               Long journalEntryId,
                               Long cogsJournalEntryId,
                               List<PackagingSlipLineDto> lines) {}
