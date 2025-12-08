package com.bigbrightpaints.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceDto(
    Long id,
    Long employeeId,
    String employeeName,
    String employeeType,
    LocalDate date,
    String status,
    LocalTime checkInTime,
    LocalTime checkOutTime,
    BigDecimal regularHours,
    BigDecimal overtimeHours,
    BigDecimal doubleOvertimeHours,
    boolean holiday,
    boolean weekend,
    String remarks,
    String markedBy,
    Instant markedAt
) {}
