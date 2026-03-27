# HR Services

## HrService
**Location:** `service/HrService.java`

Facade service that delegates to specialized services for each HR domain.

### Delegated Services
- `EmployeeService` - Employee CRUD
- `LeaveService` - Leave requests and balances
- `AttendanceService` - Attendance tracking
- `SalaryStructureTemplateService` - Salary structure management

### Key Methods
| Method | Description |
|--------|-------------|
| `listEmployees()` | List all employees |
| `createEmployee(EmployeeRequest)` | Create employee |
| `updateEmployee(Long, EmployeeRequest)` | Update employee |
| `deleteEmployee(Long)` | Delete employee |
| `listSalaryStructureTemplates()` | List salary templates |
| `createSalaryStructureTemplate(...)` | Create salary template |
| `listLeaveRequests()` | List leave requests |
| `createLeaveRequest(LeaveRequestRequest)` | Create leave request |
| `updateLeaveStatus(Long, LeaveStatusUpdateRequest)` | Update leave status |
| `listAttendanceByDate(LocalDate)` | Attendance by date |
| `markAttendance(Long, MarkAttendanceRequest)` | Mark attendance |
| `bulkMarkAttendance(BulkMarkAttendanceRequest)` | Bulk mark attendance |
| `getTodayAttendanceSummary()` | Today's summary |

---

## EmployeeService
**Location:** `service/EmployeeService.java`

### Responsibilities
- Employee CRUD operations
- Field validation and normalization
- Bank details encryption
- Salary structure template linking
- PAN validation

### Key Validations
- Staff employees require `monthlySalary` or `salaryStructureTemplateId`
- Labour employees require positive `dailyWage`
- PAN format validation (AAAAA9999A)
- Date chronology validation (dateOfBirth must precede dateOfJoining)

### Dependencies
- `CompanyContextService`
- `EmployeeRepository`
- `CompanyEntityLookup`
- `SalaryStructureTemplateRepository`
- `CryptoService`

---

## AttendanceService
**Location:** `service/AttendanceService.java`

### Responsibilities
- Daily attendance tracking
- Bulk attendance operations
- Overtime hours tracking
- Attendance summaries

### Key Methods
| Method | Description |
|--------|-------------|
| `listAttendanceByDate(LocalDate)` | Get attendance for date |
| `listEmployeeAttendance(...)` | Employee attendance history |
| `markAttendance(...)` | Mark single attendance |
| `bulkMarkAttendance(...)` | Bulk mark for multiple employees |
| `bulkImportAttendance(...)` | Import attendance records |
| `getTodayAttendanceSummary()` | Today's summary |
| `getMonthlyAttendanceSummary(...)` | Monthly summary by employee |

### Attendance Status Values
- `PRESENT` - Full day
- `HALF_DAY` - Half day (0.5 day pay)
- `ABSENT` - No pay
- `LEAVE` - Approved leave
- `HOLIDAY` - Paid holiday
- `WEEKEND` - Weekend

---

## LeaveService
**Location:** `service/LeaveService.java`

### Responsibilities
- Leave request workflow management
- Leave balance tracking
- Leave type policy management

### Leave Status Workflow
```
PENDING → APPROVED | REJECTED | CANCELLED
```

### Key Methods
| Method | Description |
|--------|-------------|
| `listLeaveRequests()` | List all leave requests |
| `listLeaveTypePolicies()` | List active leave policies |
| `getLeaveBalances(Long, Integer)` | Get employee leave balances |
| `createLeaveRequest(...)` | Create leave request |
| `updateLeaveStatus(...)` | Approve/reject/cancel |

### Balance Management
- Tracks opening balance, accrued, used, and remaining
- Supports carry-forward from previous year
- Validates balance before approval

---

## SalaryStructureTemplateService
**Location:** `service/SalaryStructureTemplateService.java`

### Responsibilities
- Salary structure CRUD
- Earnings component configuration (Basic, HRA, DA, Special Allowance)
- Statutory deduction rates (PF, ESI)
- Professional tax configuration

### Key Methods
| Method | Description |
|--------|-------------|
| `listTemplates()` | List all templates |
| `createTemplate(...)` | Create salary template |
| `updateTemplate(...)` | Update salary template |

---

## PayrollService
**Location:** `service/PayrollService.java`

Facade service for payroll operations.

### Delegated Services
- `PayrollRunService` - Payroll run lifecycle
- `PayrollCalculationService` - Pay calculations
- `PayrollPostingService` - Journal posting

### Key Methods
| Method | Description |
|--------|-------------|
| `createPayrollRun(CreatePayrollRunRequest)` | Create payroll run |
| `createWeeklyPayrollRun(LocalDate)` | Create weekly run |
| `createMonthlyPayrollRun(int, int)` | Create monthly run |
| `calculatePayroll(Long)` | Calculate amounts |
| `approvePayroll(Long)` | Approve for posting |
| `postPayrollToAccounting(Long)` | Create journal entries |
| `markAsPaid(Long, String)` | Mark as paid |
| `listPayrollRuns()` | List all runs |
| `getPayrollRun(Long)` | Get run details |
| `getWeeklyPaySummary(LocalDate)` | Weekly summary |
| `getMonthlyPaySummary(int, int)` | Monthly summary |

---

## PayrollCalculationService
**Location:** `service/PayrollCalculationService.java`

### Responsibilities
- Calculate payroll for all eligible employees
- Apply attendance-based deductions
- Calculate statutory deductions
- Generate pay summaries

### Calculation Logic
1. Fetch attendance records for period
2. Calculate present days, half days, absent days, holidays
3. Calculate base pay based on employee type:
   - Staff: Apply pro-rated salary structure or monthly salary
   - Labour: Daily wage × days worked
4. Calculate overtime pay (regular and double)
5. Apply statutory deductions (PF, ESI, TDS, PT)
6. Apply loan/advance deductions

---

## PayrollPostingService
**Location:** `service/PayrollPostingService.java`

### Responsibilities
- Approve payroll runs
- Post to accounting (create journal entries)
- Mark as paid after payment

### Required Accounts
- `SALARY-EXP` - Salary expense
- `WAGE-EXP` - Wage expense
- `SALARY-PAYABLE` - Salary payable liability
- `EMP-ADV` - Employee advance asset
- `PF-PAYABLE` - PF payable liability
- `ESI-PAYABLE` - ESI payable liability
- `TDS-PAYABLE` - TDS payable liability
- `PROFESSIONAL-TAX-PAYABLE` - PT payable liability

### Journal Entry Structure
```
Dr: SALARY-EXP/WAGE-EXP    XXX
    Cr: SALARY-PAYABLE              (Net pay)
    Cr: PF-PAYABLE                  (PF deduction)
    Cr: ESI-PAYABLE                 (ESI deduction)
    Cr: TDS-PAYABLE                 (TDS deduction)
    Cr: PROFESSIONAL-TAX-PAYABLE    (PT deduction)
    Cr: EMP-ADV                     (Loan/advance recovery)
```

---

## StatutoryDeductionEngine
**Location:** `service/StatutoryDeductionEngine.java`

### Responsibilities
- PF (Provident Fund) deduction calculation
- ESI (Employee State Insurance) deduction calculation
- TDS (Tax Deducted at Source) calculation
- Professional Tax calculation

### Default Rates
- PF: 12% of basic
- ESI: 0.75% of gross (threshold: ₹21,000)
- TDS: 10% above exemption limit
- PT: From salary structure template

---

## PayrollCalculationSupport
**Location:** `service/PayrollCalculationSupport.java`

Support utilities for payroll calculations including standard hours validation and loan deduction calculation.

---

## PayrollRunService
**Location:** `service/PayrollRunService.java` (internal to PayrollService)

Handles payroll run lifecycle:
- Create runs with idempotency
- Generate run numbers
- Manage run status transitions
