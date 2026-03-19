package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminApprovalItemDto(
        OriginType originType,
        OwnerType ownerType,
        Long id,
        UUID publicId,
        String reference,
        String status,
        String summary,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String reportType,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String parameters,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        Long requesterUserId,
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        String requesterEmail,
        String actionType,
        String actionLabel,
        String approveEndpoint,
        String rejectEndpoint,
        Instant createdAt
) {
    public enum OriginType {
        CREDIT_REQUEST,
        CREDIT_LIMIT_OVERRIDE_REQUEST,
        PAYROLL_RUN,
        PERIOD_CLOSE_REQUEST,
        EXPORT_REQUEST
    }

    public enum OwnerType {
        SALES,
        FACTORY,
        HR,
        ACCOUNTING,
        REPORTS
    }
}
