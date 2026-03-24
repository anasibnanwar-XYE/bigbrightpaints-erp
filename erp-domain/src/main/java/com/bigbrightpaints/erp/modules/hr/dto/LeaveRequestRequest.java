package com.bigbrightpaints.erp.modules.hr.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeaveRequestRequest(
    Long employeeId,
    @NotBlank String leaveType,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @Size(max = 2000) String reason,
    String status,
    @Size(max = 2000) String decisionReason) {}
