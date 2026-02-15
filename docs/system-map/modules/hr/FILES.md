# HR Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/AGENTS.md | module governance notes and constraints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/controller/HrController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/controller/HrPayrollController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/Attendance.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/AttendanceRepository.java | JPA repository for Attendance persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/Employee.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/EmployeeRepository.java | JPA repository for Employee persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/LeaveRequest.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/LeaveRequestRepository.java | JPA repository for LeaveRequest persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRun.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRunLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRunLineRepository.java | JPA repository for PayrollRunLine persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRunRepository.java | JPA repository for PayrollRun persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/AttendanceDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/AttendanceSummaryDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/BulkMarkAttendanceRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/EmployeeDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/EmployeeRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/LeaveRequestDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/LeaveRequestRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/MarkAttendanceRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/PayrollRunDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/dto/PayrollRunRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/AttendanceService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/HrService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollCalculationService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V57__payroll_run_lines.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V7__hr_tables.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V5__purchasing_hr.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/HrControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/PayrollRunApiIdempotencyIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/PayrollRunIdempotencyIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/hr/domain/EmployeeTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/controller/HrController.java | HR service endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/controller/HrPayrollController.java | Payroll API endpoints

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java | Payroll lifecycle orchestration
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollCalculationService.java | Payroll calculation policy
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollCalculationService.java | Payroll rate and attendance calculation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/PayrollRun.java | Payroll run state transitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/domain/Attendance.java | Attendance source-of-truth
