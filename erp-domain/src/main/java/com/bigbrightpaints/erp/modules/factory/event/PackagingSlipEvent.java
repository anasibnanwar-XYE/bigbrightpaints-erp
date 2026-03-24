package com.bigbrightpaints.erp.modules.factory.event;

/**
 * Lightweight domain event to signal factory-related changes on packaging slips.
 */
public record PackagingSlipEvent(
    Long companyId, Long salesOrderId, Long packagingSlipId, String status, String reason) {}
