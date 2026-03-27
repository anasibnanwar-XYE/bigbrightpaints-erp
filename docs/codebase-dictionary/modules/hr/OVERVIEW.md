# HR Module Overview

## Purpose
The HR (Human Resources) module manages employee lifecycle, attendance tracking, leave management, and payroll processing for BigBright ERP. It supports both salaried staff (monthly payroll) and labourers (weekly wage-based payroll).

## Module Location
`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/`

## File Count
- **Total Java files:** 47
- **Controllers:** 2
- **Services:** 10
- **Entities:** 14 (including repositories)
- **DTOs:** 16

## Key Capabilities

### Employee Management
- Employee CRUD operations
- Support for STAFF (monthly salary) and LABOUR (daily wage) employee types
- Salary structure templates with configurable components (Basic, HRA, DA, Special Allowance)
- Bank details with encrypted storage
- Tax regime configuration (Old/New)

### Attendance Tracking
- Daily attendance marking (PRESENT, HALF_DAY, ABSENT, LEAVE, HOLIDAY, WEEKEND)
- Bulk attendance operations
- Check-in/Check-out time tracking
- Regular hours, overtime hours, and double overtime hours tracking
- Monthly attendance summaries

### Leave Management
- Leave request workflow (PENDING → APPROVED/REJECTED/CANCELLED)
- Leave type policies with annual entitlement and carry-forward rules
- Leave balance tracking with opening balance, accrued, used, and remaining

### Payroll Processing
- **Weekly payroll** for LABOUR employees
- **Monthly payroll** for STAFF employees
- Statutory deductions (PF, ESI, TDS, Professional Tax)
- Salary structure templates for earnings breakdown
- Journal posting integration with accounting module
- Payment workflow with journal linkage

## Dependencies
- `modules/company` - Company context and multi-tenancy
- `modules/accounting` - Journal entries for payroll posting and payments
- `core/security` - CryptoService for bank detail encryption
- `core/util` - CompanyClock, CompanyEntityLookup

## API Endpoints

### HR Controller (`/api/v1/hr`)
- `GET /employees` - List all employees
- `POST /employees` - Create employee
- `PUT /employees/{id}` - Update employee
- `DELETE /employees/{id}` - Delete employee
- `GET /salary-structures` - List salary structure templates
- `POST /salary-structures` - Create salary template
- `PUT /salary-structures/{id}` - Update salary template
- `GET /leave-requests` - List leave requests
- `POST /leave-requests` - Create leave request
- `PATCH /leave-requests/{id}/status` - Update leave status
- `GET /leave-types` - List leave type policies
- `GET /employees/{employeeId}/leave-balances` - Get leave balances
- `GET /attendance/today` - Today's attendance
- `GET /attendance/date/{date}` - Attendance by date
- `GET /attendance/summary` - Today's attendance summary
- `POST /attendance/mark/{employeeId}` - Mark attendance for single employee
- `POST /attendance/bulk-mark` - Bulk mark attendance
- `POST /attendance/bulk-import` - Bulk import attendance records

### Payroll Controller (`/api/v1/payroll`)
- `GET /runs` - List all payroll runs
- `GET /runs/weekly` - List weekly payroll runs
- `GET /runs/monthly` - List monthly payroll runs
- `GET /runs/{id}` - Get payroll run details
- `GET /runs/{id}/lines` - Get payroll run lines (employee breakdown)
- `POST /runs` - Create payroll run
- `POST /runs/weekly` - Create weekly payroll run
- `POST /runs/monthly` - Create monthly payroll run
- `POST /runs/{id}/calculate` - Calculate payroll
- `POST /runs/{id}/approve` - Approve payroll
- `POST /runs/{id}/post` - Post to accounting
- `POST /runs/{id}/mark-paid` - Mark as paid
- `GET /summary/weekly` - Weekly pay summary
- `GET /summary/monthly` - Monthly pay summary
- `GET /summary/current-week` - Current week pay summary
- `GET /summary/current-month` - Current month pay summary

## Security
- All endpoints require `ROLE_ADMIN` or `ROLE_ACCOUNTING` authority
- Tenant isolation enforced via Company context
