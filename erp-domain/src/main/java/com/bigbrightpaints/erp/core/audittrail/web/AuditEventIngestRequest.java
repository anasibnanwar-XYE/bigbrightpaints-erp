package com.bigbrightpaints.erp.core.audittrail.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AuditEventIngestRequest(@Valid @NotEmpty List<AuditEventIngestItemRequest> events) {}
