# HR Services

## HrService
**Location:** `service/HrService.java`

### Purpose
Facade service coordinating HR operations.

### Key Dependencies
- `EmployeeService` - Employee CRUD
- `LeaveService` - Leave management
- `AttendanceService` - Attendance tracking
- `SalaryStructureTemplateService` - Salary templates

### Key Methods

| Method | Description |
|--------|-------------|
| `listEmployees()` | List all employees |
| `createEmployee(EmployeeRequest)` | Create employee |
| `updateEmployee(Long, EmployeeRequest)` | Update employee |
| `deleteEmployee(Long)` | Delete employee |
| `listSalaryStructureTemplates()` | List templates |
| `createSalaryStructureTemplate(SalaryStructureTemplateRequest)` | Create template |
| `updateSalaryStructureTemplate(Long, SalaryStructureTemplateRequest)` | Update template |
| `listLeaveRequests()` | List leave requests |
| `listLeaveTypePolicies()` | List leave policies |
| `getLeaveBalances(Long, Integer)` | Get leave balances |
| `createLeaveRequest(LeaveRequestRequest)` | Create leave request |
| `updateLeaveStatus(Long, LeaveStatusUpdateRequest)` | Update leave status |
| `listAttendanceByDate(LocalDate)` | Get attendance by date |
| `markAttendance(Long, MarkAttendanceRequest)` | Mark attendance |
| `bulkMarkAttendance(BulkMarkAttendanceRequest)` | Bulk mark |
| `getTodayAttendanceSummary()` | Today's summary |
| `getMonthlyAttendanceSummary(int, int)` | Monthly summary |

---

## EmployeeService
**Location:** `service/EmployeeService.java`

### Purpose
Employee CRUD operations with bank detail encryption.

### Key Dependencies
- `CompanyContextService` - Company context
- `EmployeeRepository` - Data access
- `CompanyEntityLookup` - Entity lookup
- `SalaryStructureTemplateRepository` - Templates
- `CryptoService` - Bank encryption

### Key Methods

| Method | Description |
|--------|-------------|
| `listEmployees()` | List all employees for company |
| `createEmployee(EmployeeRequest)` | Create with validation |
| `updateEmployee(Long, EmployeeRequest)` | Update with validation |
| `deleteEmployee(Long)` | Delete employee |

### Validation
- PAN format validation
- Date chronology (DOB before joining date)
- Compensation: Staff needs monthly salary or template; Labour needs daily wage
- Bank details encrypted before storage

---

## AttendanceService
**Location:** `service/AttendanceService.java`

### Purpose
Track daily employee attendance with overtime.

### Key Dependencies
- `CompanyContextService` - Company context
- `AttendanceRepository` - Data access
- `EmployeeRepository` - Employee lookup
- `CompanyEntityLookup` - Entity resolution
- `CompanyClock` - Date/time

### Key Methods

| Method | Description |
|--------|-------------|
| `listAttendanceByDate(LocalDate)` | Get attendance for date |
| `listEmployeeAttendance(Long, LocalDate, LocalDate)` | Employee history |
| `markAttendance(Long, MarkAttendanceRequest)` | Single mark |
| `bulkMarkAttendance(BulkMarkAttendanceRequest)` | Multiple marks |
| `bulkImportAttendance(AttendanceBulkImportRequest)` | Bulk import |
| `getTodayAttendanceSummary()` | Today's count |
| `getMonthlyAttendanceSummary(int, int)` | Monthly totals |

### Attendance Statuses
- `PRESENT` - Full day
- `HALF_DAY` - Half day (0.5)
- `ABSENT` - No pay
- `LEAVE` - Approved leave
- `HOLIDAY` - Paid holiday
- `WEEKEND` - No work

---

## LeaveService
**Location:** `service/LeaveService.java`

### Purpose
Manage leave requests and balances.

### Key Dependencies
- `CompanyContextService` - Company context
- `EmployeeRepository` - Employee lookup
- `LeaveRequestRepository` - Leave data
- `LeaveTypePolicyRepository` - Policies
- `LeaveBalanceRepository` - Balances

### Key Methods

| Method | Description |
|--------|-------------|
| `listLeaveRequests()` | List all requests |
| `listLeaveTypePolicies()` | List policies |
| `getLeaveBalances(Long, Integer)` | Get balances |
| `createLeaveRequest(LeaveRequestRequest)` | Create with validation |
| `updateLeaveStatus(Long, LeaveStatusUpdateRequest)` | Update status |

### Business Rules
- No overlapping requests
- Balance check before approval
- Carry-forward on new year
- Can't revert to PENDING from APPROVED

- Balance adjustments on status change

---

## PayrollService
**Location:** `service/PayrollService.java`

### Purpose
Facade for payroll processing workflow.

### Key Dependencies
- `PayrollRunRepository` - Run data
- `PayrollRunLineRepository` - Line data
- `CompanyContextService` - Company context
- `PayrollRunService` - Run creation
- `PayrollCalculationService` - Calculations
- `PayrollPostingService` - Accounting integration

### Key Methods

| Method | Description |
|--------|-------------|
| `createPayrollRun(CreatePayrollRunRequest)` | Create run |
| `createWeeklyPayrollRun(LocalDate)` | Weekly run |
| `createMonthlyPayrollRun(int, int)` | Monthly run |
| `calculatePayroll(Long)` | Calculate pay |
| `approvePayroll(Long)` | Approve |
| `postPayrollToAccounting(Long)` | Post to GL |
| `markAsPaid(Long, String)` | Mark paid |
| `listPayrollRuns()` | List runs |
| `listPayrollRunsByType(RunType)` | Filtered list |
| `getPayrollRun(Long)` | Get details |
| `getPayrollRunLines(Long)` | Get lines |
| `getWeeklyPaySummary(LocalDate)` | Weekly preview |
| `getMonthlyPaySummary(int, int)` | Monthly preview |

---

## PayrollCalculationService
**Location:** `service/PayrollCalculationService.java`

### Purpose
Calculate payroll amounts from attendance.

### Key Dependencies
- `PayrollRunRepository` - Run data
- `PayrollRunLineRepository` - Line data
- `EmployeeRepository` - Employees
- `AttendanceRepository` - Attendance
- `CompanyContextService` - Company context
- `CompanyClock` - Date/time
- `StatutoryDeductionEngine` - Deductions
- `PayrollCalculationSupport` - Helper

### Key Methods

| Method | Description |
|--------|-------------|
| `calculatePayroll(Long)` | Calculate for run |
| `getWeeklyPaySummary(LocalDate)` | Preview weekly |
| `getMonthlyPaySummary(int, int)` | Preview monthly |

### Calculation Logic
1. Load employees by type (LABOUR for weekly, STAFF for monthly)
2. Clear existing lines
3. For each employee:
   - Get attendance for period
   - Count present, half, absent, leave, holiday days
   - Calculate base pay from salary/wage
   - Apply salary structure or daily rate
   - Calculate overtime pay
   - Apply statutory deductions (PF, ESI, PT, TDS)
   - Calculate net pay
4. Update run totals
5. Set status to CALCULATED

---

## PayrollPostingService
**Location:** `service/PayrollPostingService.java`

### Purpose
Handle payroll approval, posting, and payment.

### Key Dependencies
- `PayrollRunRepository` - Run data
- `PayrollRunLineRepository` - Line data
- `EmployeeRepository` - Employees
- `AttendanceRepository` - Attendance
- `AccountingFacade` - Accounting integration
- `AccountRepository` - Chart of accounts
- `CompanyContextService` - Company context
- `CompanyEntityLookup` - Entity lookup
- `CompanyClock` - Date/time
- `AuditService` - Audit logging

### Key Methods

| Method | Description |
|--------|-------------|
| `approvePayroll(Long)` | Mark as approved |
| `postPayrollToAccounting(Long)` | Create journal |
| `markAsPaid(Long, String)` | Record payment |

### Required Accounts
- SALARY-EXP, WAGE-EXP (Expense)
- SALARY-PAYABLE (Liability)
- PF-PAYABLE, ESI-PAYABLE, TDS-PAYABLE (Liabilities)
- EMP-ADV (Asset for advances)

### Posting Flow
1. Validate status (APPROVED)
2. Load lines and validate amounts
3. Build journal lines:
   - Debit: Salary/Wage Expense
   - Credit: Salary Payable
   - Credit: PF Payable
   - Credit: ESI Payable
   - Credit: TDS Payable
   - Credit: Professional Tax Payable
   - Credit: Employee Advances (if applicable)
4. Post via AccountingFacade
5. Link journal to run
6. Link attendance to run
7. Set status to POSTED

---

## StatutoryDeductionEngine
**Location:** `service/StatutoryDeductionEngine.java`

### Purpose
Calculate Indian statutory deductions.

### Default Rates
- **PF (Provident Fund):** 12% of basic salary
- **ESI (Employee State Insurance):** 0.5% of gross (if ≤ 21,000)
- **TDS (Tax Deducted at Source):** 10% above exemption
- **Professional Tax:** Fixed amount (default ₹200)

### Key Methods

| Method | Description |
|--------|-------------|
| `calculatePfDeduction(BigDecimal, Employee)` | PF on basic |
| `calculateEsiDeduction(BigDecimal, Employee)` | ESI on gross |
| `calculateTdsDeduction(BigDecimal, PayrollRun, Employee)` | TDS on projected annual |
| `calculateProfessionalTaxDeduction(Employee, PayrollRun)` | Fixed PT for monthly only |

### ESI Eligibility
- Only if gross pay ≤ threshold (default ₹21,000)
- Rate from template or default 0.5%

### Tax Regime
- OLD: Exemption ₹2,50,000
- NEW: Exemption ₹3,00,000

---

## SalaryStructureTemplateService
**Location:** `service/SalaryStructureTemplateService.java`

### Purpose
Manage reusable salary structures.

### Key Dependencies
- `CompanyContextService` - Company context
- `SalaryStructureTemplateRepository` - Data access

### Key Methods

| Method | Description |
|--------|-------------|
| `listTemplates()` | List all templates |
| `createTemplate(SalaryStructureTemplateRequest)` | Create with validation |
| `updateTemplate(Long, SalaryStructureTemplateRequest)` | Update |

### Validation
- Duplicate code check
- Positive total earnings required
- Non-negative rates
