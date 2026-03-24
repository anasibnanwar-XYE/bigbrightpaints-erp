package com.bigbrightpaints.erp.modules.hr.dto;

import java.time.LocalDate;

public record AttendanceSummaryDto(
    LocalDate date,
    long totalEmployees,
    long present,
    long absent,
    long halfDay,
    long onLeave,
    long notMarked) {}
