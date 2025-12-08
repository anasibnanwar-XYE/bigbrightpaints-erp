package com.bigbrightpaints.erp.modules.hr.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record MarkAttendanceRequest(
    LocalDate date,  // If null, uses today
    @NotNull String status,  // PRESENT, ABSENT, HALF_DAY, LEAVE
    LocalTime checkInTime,
    LocalTime checkOutTime,
    BigDecimal regularHours,
    BigDecimal overtimeHours,
    BigDecimal doubleOvertimeHours,
    boolean holiday,
    boolean weekend,
    String remarks
) {}
