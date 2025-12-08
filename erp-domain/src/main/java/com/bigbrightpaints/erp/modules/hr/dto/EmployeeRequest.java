package com.bigbrightpaints.erp.modules.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email @NotBlank String email,
        String phone,
        String role,
        LocalDate hiredDate,
        // Payroll fields
        String employeeType,        // STAFF or LABOUR
        String paymentSchedule,     // MONTHLY or WEEKLY
        BigDecimal monthlySalary,   // For STAFF
        BigDecimal dailyWage,       // For LABOUR
        Integer workingDaysPerMonth,
        Integer weeklyOffDays,
        BigDecimal standardHoursPerDay,
        BigDecimal overtimeRateMultiplier,
        BigDecimal doubleOtRateMultiplier,
        // Bank details
        String bankAccountNumber,
        String bankName,
        String ifscCode
) {}
