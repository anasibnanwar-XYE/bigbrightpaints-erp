package com.bigbrightpaints.erp.modules.hr.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record AttendanceBulkImportRequest(
    @NotEmpty @Valid List<BulkMarkAttendanceRequest> records) {}
