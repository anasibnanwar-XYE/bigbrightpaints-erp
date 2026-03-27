# HR Entities

## Employee
**Location:** `domain/Employee.java`
**Table:** `employees`

### Purpose
Core employee entity supporting both salaried staff and daily-wage labourers.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `firstName`, `lastName` | String | Name |
| `email` | String | Email (unique per company) |
| `phone` | String | Phone |
| `role` | String | Job role |
| `status` | String | ACTIVE/INACTIVE |
| `dateOfBirth` | LocalDate | Birth date |
| `dateOfJoining` | LocalDate | Joining date |
| `hiredDate` | LocalDate | Hire date |
| `gender` | Gender | MALE/FEMALE/OTHER/UNDISCLOSED |
| `employmentType` | EmploymentType | FULL_TIME/PART_TIME/CONTRACT/INTERN |
| `employeeType` | EmployeeType | **STAFF/LABOUR** |
| `paymentSchedule` | PaymentSchedule | MONTHLY/WEEKLY |
| `monthlySalary` | BigDecimal | For STAFF |
| `dailyWage` | BigDecimal | For LABOUR |
| `workingDaysPerMonth` | Integer | Default: 26 |
| `weeklyOffDays` | Integer | Default: 1 |
| `salaryStructureTemplate` | SalaryStructureTemplate | Linked template |
| `pfNumber` | String | PF account number |
| `esiNumber` | String | ESI number |
| `panNumber` | String | PAN (validated) |
| `taxRegime` | TaxRegime | OLD/NEW |
| `bankAccountNumberEncrypted` | String | Encrypted bank account |
| `bankNameEncrypted` | String | Encrypted bank name |
| `ifscCodeEncrypted` | String | Encrypted IFSC |
| `advanceBalance` | BigDecimal | Advance payment balance |
| `overtimeRateMultiplier` | BigDecimal | Default: 1.5x |
| `doubleOtRateMultiplier` | BigDecimal | Default: 2.0x |
| `standardHoursPerDay` | BigDecimal | Default: 8 |

### Enums
```java
enum EmployeeType { STAFF, LABOUR }
enum PaymentSchedule { MONTHLY, WEEKLY }
enum Gender { MALE, FEMALE, OTHER, UNDISCLOSED }
enum EmploymentType { FULL_TIME, PART_TIME, CONTRACT, INTERN }
enum TaxRegime { OLD, NEW }
```

### Computed Properties
- `getDailyRate()` - Calculates daily rate from monthly salary (for STAFF) or returns daily wage (for LABOUR)
- `getFullName()` - Returns "firstName lastName"
- `calculatePay(presentDays, halfDays)` - Calculates pay for attendance

---

## Attendance
**Location:** `domain/Attendance.java`
**Table:** `attendance`
**Unique Constraint:** (company_id, employee_id, attendance_date)

### Purpose
Daily attendance record tracking presence, hours, and overtime.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `company` | Company | Tenant |
| `employee` | Employee | Linked employee |
| `attendanceDate` | LocalDate | Date of attendance |
| `status` | AttendanceStatus | PRESENT/HALF_DAY/ABSENT/LEAVE/HOLIDAY/WEEKEND |
| `checkInTime` | LocalTime | Check-in time |
| `checkOutTime` | LocalTime | Check-out time |
| `regularHours` | BigDecimal | Standard work hours |
| `overtimeHours` | BigDecimal | Regular OT hours (1.5x) |
| `doubleOvertimeHours` | BigDecimal | Double OT hours (2x) |
| `holiday` | boolean | Is holiday |
| `weekend` | boolean | Is weekend |
| `remarks` | String | Notes |
| `markedBy` | String | Username who marked |
| `markedAt` | Instant | Timestamp |
| `basePay` | BigDecimal | Calculated base pay |
| `overtimePay` | BigDecimal | Calculated OT pay |
| `totalPay` | BigDecimal | Total pay |
| `payrollRunId` | Long | Linked payroll run |

### AttendanceStatus Enum
```java
enum AttendanceStatus {
    PRESENT,    // Full day
    HALF_DAY,   // Half day (0.5 day pay)
    ABSENT,     // No pay
    LEAVE,      // Approved leave
    HOLIDAY,    // Paid holiday
    WEEKEND     // Weekend (no work)
}
```

---

## LeaveRequest
**Location:** `domain/LeaveRequest.java`
**Table:** `leave_requests`

### Purpose
Leave application with approval workflow.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `employee` | Employee | Applicant |
| `leaveType` | String | Leave type code |
| `startDate` | LocalDate | Start date |
| `endDate` | LocalDate | End date |
| `totalDays` | BigDecimal | Calculated days |
| `status` | String | PENDING/APPROVED/REJECTED/CANCELLED |
| `reason` | String | Leave reason |
| `decisionReason` | String | Approval/rejection reason |
| `approvedBy` | String | Approver username |
| `approvedAt` | Instant | Approval timestamp |
| `rejectedBy` | String | Rejector username |
| `rejectedAt` | Instant | Rejection timestamp |
| `createdAt` | Instant | Creation timestamp |

---

## LeaveBalance
**Location:** `domain/LeaveBalance.java`
**Table:** `leave_balances`

### Purpose
Track employee leave balances per year and type.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `company` | Company | Tenant |
| `employee` | Employee | Employee |
| `leaveType` | String | Leave type code |
| `balanceYear` | int | Calendar year |
| `openingBalance` | BigDecimal | Starting balance |
| `carryForwardApplied` | BigDecimal | From previous year |
| `accrued` | BigDecimal | This year's accrual |
| `used` | BigDecimal | Days used |
| `remaining` | BigDecimal | Available balance |
| `lastRecalculatedAt` | Instant | Last calculation |

---

## LeaveTypePolicy
**Location:** `domain/LeaveTypePolicy.java`
**Table:** `leave_type_policies`

### Purpose
Define leave types and entitlements.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `leaveType` | String | Type code |
| `displayName` | String | Display name |
| `annualEntitlement` | BigDecimal | Days per year |
| `carryForwardLimit` | BigDecimal | Max carry-forward |
| `active` | boolean | Is active |

---

## PayrollRun
**Location:** `domain/PayrollRun.java`
**Table:** `payroll_runs`

### Purpose
Represents a payroll batch for a period (weekly or monthly).

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `runNumber` | String | e.g., PR-2024-W52 |
| `runType` | RunType | WEEKLY/MONTHLY |
| `periodStart` | LocalDate | Period start |
| `periodEnd` | LocalDate | Period end |
| `status` | PayrollStatus | DRAFT/CALCULATED/APPROVED/POSTED/PAID/CANCELLED |
| `totalEmployees` | Integer | Employee count |
| `totalPresentDays` | BigDecimal | Total days |
| `totalOvertimeHours` | BigDecimal | Total OT |
| `totalBasePay` | BigDecimal | Base pay total |
| `totalOvertimePay` | BigDecimal | OT pay total |
| `totalDeductions` | BigDecimal | Deductions total |
| `totalNetPay` | BigDecimal | Net pay total |
| `journalEntryId` | Long | Posting journal |
| `paymentJournalEntryId` | Long | Payment journal |
| `paymentReference` | String | Payment reference |
| `paymentDate` | LocalDate | Payment date |
| `createdBy` | String | Creator |
| `createdAt` | Instant | Created |
| `approvedBy` | String | Approver |
| `approvedAt` | Instant | Approved |
| `postedBy` | String | Poster |
| `postedAt` | Instant | Posted |

### Enums
```java
enum RunType { WEEKLY, MONTHLY }
enum PayrollStatus { DRAFT, CALCULATED, APPROVED, POSTED, PAID, CANCELLED }
```

---

## PayrollRunLine
**Location:** `domain/PayrollRunLine.java`
**Table:** `payroll_run_lines`

### Purpose
Employee-level breakdown of payroll run.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `payrollRun` | PayrollRun | Parent run |
| `employee` | Employee | Employee |
| `presentDays` | BigDecimal | Days present |
| `halfDays` | BigDecimal | Half days |
| `absentDays` | BigDecimal | Days absent |
| `leaveDays` | BigDecimal | Leave days |
| `holidayDays` | BigDecimal | Holiday days |
| `regularHours` | BigDecimal | Regular hours |
| `overtimeHours` | BigDecimal | OT hours |
| `doubleOtHours` | BigDecimal | Double OT |
| `dailyRate` | BigDecimal | Daily rate |
| `hourlyRate` | BigDecimal | Hourly rate |
| `basePay` | BigDecimal | Base pay |
| `overtimePay` | BigDecimal | OT pay |
| `holidayPay` | BigDecimal | Holiday pay |
| `grossPay` | BigDecimal | Gross pay |
| `basicSalaryComponent` | BigDecimal | Basic |
| `hraComponent` | BigDecimal | HRA |
| `daComponent` | BigDecimal | DA |
| `specialAllowanceComponent` | BigDecimal | Special |
| `advanceDeduction` | BigDecimal | Advance |
| `loanDeduction` | BigDecimal | Loan |
| `pfDeduction` | BigDecimal | PF |
| `esiDeduction` | BigDecimal | ESI |
| `taxDeduction` | BigDecimal | TDS |
| `professionalTaxDeduction` | BigDecimal | PT |
| `leaveWithoutPayDeduction` | BigDecimal | LWP |
| `totalDeductions` | BigDecimal | Total deductions |
| `netPay` | BigDecimal | Net pay |
| `paymentStatus` | PaymentStatus | PENDING/PAID |
| `paymentReference` | String | Payment ref |

---

## SalaryStructureTemplate
**Location:** `domain/SalaryStructureTemplate.java`
**Table:** `salary_structure_templates`

### Purpose
Define salary structure with earnings components and statutory rates.

### Key Fields
| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Primary key |
| `publicId` | UUID | Public identifier |
| `company` | Company | Tenant |
| `code` | String | Unique code |
| `name` | String | Display name |
| `description` | String | Description |
| `basicPay` | BigDecimal | Basic pay |
| `hra` | BigDecimal | HRA |
| `da` | BigDecimal | Dearness allowance |
| `specialAllowance` | BigDecimal | Special allowance |
| `employeePfRate` | BigDecimal | PF rate (default 12%) |
| `employeeEsiRate` | BigDecimal | ESI rate (default 0.75%) |
| `esiEligibilityThreshold` | BigDecimal | ESI threshold (default 21000) |
| `professionalTax` | BigDecimal | PT amount (default 200) |
| `active` | boolean | Is active |

### Computed
- `totalEarnings()` - Sum of all earnings components
