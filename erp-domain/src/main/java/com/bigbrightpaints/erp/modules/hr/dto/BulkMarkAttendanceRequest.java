package com.bigbrightpaints.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BulkMarkAttendanceRequest(
    @NotEmpty List<Long> employeeIds,
    @NotNull LocalDate date,
    @NotNull String status, // PRESENT, ABSENT, HALF_DAY, LEAVE
    LocalTime checkInTime,
    LocalTime checkOutTime,
    BigDecimal regularHours,
    BigDecimal overtimeHours,
    String remarks) {}
