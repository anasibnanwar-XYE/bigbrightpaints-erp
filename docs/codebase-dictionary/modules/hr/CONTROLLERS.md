# HR Controllers

## HrController
**Location:** `controller/HrController.java`
**Base Path:** `/api/v1/hr`
**Required Roles:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`

### Employee Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/employees` | List all employees for current company |
| POST | `/employees` | Create new employee |
| PUT | `/employees/{id}` | Update employee |
| DELETE | `/employees/{id}` | Delete employee |

### Salary Structure Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/salary-structures` | List salary structure templates |
| POST | `/salary-structures` | Create salary structure template |
| PUT | `/salary-structures/{id}` | Update salary structure template |

### Leave Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/leave-requests` | List all leave requests |
| POST | `/leave-requests` | Create leave request |
| PATCH | `/leave-requests/{id}/status` | Update leave request status |
| GET | `/leave-types` | List leave type policies |
| GET | `/employees/{employeeId}/leave-balances` | Get leave balances for employee |

### Attendance Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/attendance/date/{date}` | Get attendance for specific date |
| GET | `/attendance/today` | Get today's attendance |
| GET | `/attendance/summary` | Get today's attendance summary |
| GET | `/attendance/summary/monthly` | Get monthly attendance summary |
| GET | `/attendance/employee/{employeeId}` | Get employee attendance history |
| POST | `/attendance/mark/{employeeId}` | Mark attendance for single employee |
| POST | `/attendance/bulk-mark` | Bulk mark attendance for multiple employees |
| POST | `/attendance/bulk-import` | Bulk import attendance records |

### Dependencies
- `HrService` - Main facade service
- `CompanyContextService` - Current company resolution
- `CompanyClock` - Date/time resolution

---

## HrPayrollController
**Location:** `controller/HrPayrollController.java`
**Base Path:** `/api/v1/payroll`
**Required Roles:** `ROLE_ADMIN`, `ROLE_ACCOUNTING`

### Payroll Run Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/runs` | List all payroll runs |
| GET | `/runs/weekly` | List weekly payroll runs (labourers) |
| GET | `/runs/monthly` | List monthly payroll runs (staff) |
| GET | `/runs/{id}` | Get payroll run details |
| GET | `/runs/{id}/lines` | Get payroll run lines (employee breakdown) |

### Payroll Creation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/runs` | Create payroll run (generic) |
| POST | `/runs/weekly` | Create weekly payroll run |
| POST | `/runs/monthly` | Create monthly payroll run |

### Payroll Workflow Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/runs/{id}/calculate` | Calculate payroll amounts |
| POST | `/runs/{id}/approve` | Approve payroll for posting |
| POST | `/runs/{id}/post` | Post to accounting (creates journal entries) |
| POST | `/runs/{id}/mark-paid` | Mark as paid after payment |

### Pay Summary Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/summary/weekly` | Weekly pay summary for labourers |
| GET | `/summary/monthly` | Monthly pay summary for staff |
| GET | `/summary/current-week` | Current week pay summary |
| GET | `/summary/current-month` | Current month pay summary |

### Dependencies
- `PayrollService` - Main payroll facade
- `CompanyContextService` - Current company resolution
- `CompanyClock` - Date/time resolution
