# HR Canonical Flows

## Employee Onboarding Flow

```
EmployeeRequest → HrService.createEmployee()
                ↓
          EmployeeService.createEmployee()
                ↓
          1. Validate required fields (firstName, lastName, email)
          2. Resolve company from context
          3. Apply mutable fields (personal info, employment details)
          4. Validate PAN format (if provided)
          5. Validate date chronology (DOB < joining date)
          6. Validate compensation (salary for staff, wage for labour)
          7. Encrypt bank details
          8. Save Employee entity
                ↓
          Return EmployeeDto
```

## Attendance Marking Flow

```
MarkAttendanceRequest → HrService.markAttendance()
                        ↓
                  AttendanceService.markAttendance()
                        ↓
                  1. Resolve company from context
                  2. Resolve employee
                  3. Determine date (from request or today)
                  4. Find or create Attendance record
                  5. Apply attendance fields (status, hours, OT)
                  6. Set marked by/at metadata
                  7. Save Attendance entity
                        ↓
                  Return AttendanceDto
```

## Leave Request Flow

```
LeaveRequestRequest → HrService.createLeaveRequest()
                        ↓
                  LeaveService.createLeaveRequest()
                        ↓
                  1. Resolve company and employee
                  2. Validate date range
                  3. Check for overlapping requests
                  4. Create LeaveRequest entity
                  5. If APPROVED:
                     - Apply balance delta
                     - Create/update LeaveBalance
                  6. Save LeaveRequest
                        ↓
                  Return LeaveRequestDto
```

## Payroll Processing Flow

```
1. Create Run
   POST /api/v1/payroll/runs
        ↓
   PayrollService.createPayrollRun()
        ↓
   PayrollRunService.createPayrollRun()
        ↓
   1. Validate period dates
   2. Generate run number
   3. Create PayrollRun (status: DRAFT)
        ↓
   Return PayrollRunDto

2. Calculate
   POST /api/v1/payroll/runs/{id}/calculate
        ↓
   PayrollService.calculatePayroll()
        ↓
   PayrollCalculationService.calculatePayroll()
        ↓
   1. Validate run exists and is DRAFT
   2. Clear existing lines
   3. Load employees by type
   4. For each employee:
      a. Get attendance for period
      b. Count present/half/absent/leave/holiday days
      c. Calculate base pay
      d. Apply salary structure (for staff) or daily rate (for labour)
      e. Calculate OT pay
      f. Apply statutory deductions (PF, ESI, PT, TDS)
      g. Calculate net pay
      h. Create PayrollRunLine
   5. Update run totals
   6. Set status to CALCULATED
        ↓
   Return PayrollRunDto

3. Approve
   POST /api/v1/payroll/runs/{id}/approve
        ↓
   PayrollService.approvePayroll()
        ↓
   PayrollPostingService.approvePayroll()
        ↓
   1. Validate status is CALCULATED
   2. Validate lines exist
   3. Set status to APPROVED
   4. Set approved by/at
        ↓
   Return PayrollRunDto

4. Post to Accounting
   POST /api/v1/payroll/runs/{id}/post
        ↓
   PayrollService.postPayrollToAccounting()
        ↓
   PayrollPostingService.postPayrollToAccounting()
        ↓
   1. Lock payroll run
   2. Validate status is APPROVED
   3. Load all lines
   4. Validate total gross > 0
   5. Sum deductions by type
   6. Build journal lines:
      - Debit: Salary/Wage Expense
      - Credit: Salary Payable
      - Credit: PF Payable
      - Credit: ESI Payable
      - Credit: TDS Payable
      - Credit: PT Payable
      - Credit: Advances (if any)
   7. Post via AccountingFacade
   8. Link journal to run
   9. Link attendance records to run
   10. Set status to POSTED
   11. Log audit event
        ↓
   Return PayrollRunDto

5. Mark Paid
   POST /api/v1/payroll/runs/{id}/mark-paid
        ↓
   PayrollService.markAsPaid()
        ↓
   PayrollPostingService.markAsPaid()
        ↓
   1. Validate payment journal exists
   2. Validate status is POSTED
   3. Update all line payment statuses
   4. Deduct advances from employee balances
   5. Set status to PAID
        ↓
   Return PayrollRunDto
```

## Statutory Deduction Flow

```
PayrollCalculationService → StatutoryDeductionEngine
                            ↓
                  For each employee:
                  
                  1. PF Deduction:
                     - Based on basic component
                     - Rate from template or default 12%
                     - PF = basic × rate / 100
                  
                  2. ESI Deduction:
                     - Only if gross ≤ threshold (default ₹21,000)
                     - Rate from template or default 0.5%
                     - ESI = gross × rate / 100
                  
                  3. TDS Deduction:
                     - Project to annual (×52 for weekly, ×12 for monthly)
                     - Subtract exemption (OLD: ₹2.5L, NEW: ₹3L)
                     - If taxable > 0: TDS = (taxable × 10%) / periods
                  
                  4. Professional Tax:
                     - Only for monthly runs
                     - Fixed amount from template (default ₹200)
```
