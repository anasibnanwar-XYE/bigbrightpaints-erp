package com.bigbrightpaints.erp.core.audittrail.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AuditEventIngestRequest(
        @Valid @NotEmpty List<AuditEventIngestItemRequest> events
) {
}
