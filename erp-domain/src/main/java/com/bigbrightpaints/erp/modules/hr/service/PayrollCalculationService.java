package com.bigbrightpaints.erp.modules.hr.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Auto-calculates payroll based on attendance.
 * 
 * Business Rules:
 * - Staff (MONTHLY): Salary / working days * present days
 * - Labour (WEEKLY): Daily wage * present days
 * - Half day = 0.5 day pay
 * - Weekends excluded from calculations
 * - Advance payments deducted from final amount
 * 
 * Scheduled: Every Saturday at 3:00 PM IST
 */
@Service
public class PayrollCalculationService {

    private static final Logger log = LoggerFactory.getLogger(PayrollCalculationService.class);

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollRunRepository payrollRunRepository;
    private final PayrollRunLineRepository payrollRunLineRepository;
    private final CompanyContextService companyContextService;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${erp.payroll.notification-email:mdanas7869292@gmail.com}")
    private String payrollNotificationEmail;

    @Value("${erp.mail.from-address:bigbrightpaints@gmail.com}")
    private String fromAddress;

    public PayrollCalculationService(EmployeeRepository employeeRepository,
                                     AttendanceRepository attendanceRepository,
                                     PayrollRunRepository payrollRunRepository,
                                     PayrollRunLineRepository payrollRunLineRepository,
                                     CompanyContextService companyContextService,
                                     JavaMailSender mailSender,
                                     TemplateEngine templateEngine) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.payrollRunRepository = payrollRunRepository;
        this.payrollRunLineRepository = payrollRunLineRepository;
        this.companyContextService = companyContextService;
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Calculate weekly payroll for labourers (runs every Saturday)
     */
    @Transactional
    public PayrollSummary calculateWeeklyPayroll() {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate today = LocalDate.now();
        
        // Get the week range (Monday to Sunday of current week, or previous week if today is Saturday)
        LocalDate weekEnd = today.getDayOfWeek() == DayOfWeek.SATURDAY 
                ? today.minusDays(1) // Friday
                : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
        LocalDate weekStart = weekEnd.minusDays(5); // Monday
        
        log.info("Calculating weekly payroll for {} to {}", weekStart, weekEnd);
        
        // Get all active labourers
        List<Employee> labourers = employeeRepository.findByCompanyAndPaymentScheduleAndStatus(
                company, Employee.PaymentSchedule.WEEKLY, "ACTIVE");
        
        List<PayrollLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Employee labourer : labourers) {
            PayrollLineItem item = calculateEmployeePay(company, labourer, weekStart, weekEnd);
            if (item.netPay().compareTo(BigDecimal.ZERO) > 0) {
                lineItems.add(item);
                totalAmount = totalAmount.add(item.netPay());
            }
        }
        
        // Create payroll run
        String reference = "WEEKLY-" + weekEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        PayrollRun run = createPayrollRun(company, reference, today, totalAmount, lineItems);
        
        PayrollSummary summary = new PayrollSummary(
                run.getId(),
                reference,
                "WEEKLY",
                weekStart,
                weekEnd,
                lineItems,
                totalAmount,
                LocalDateTime.now()
        );
        
        // Send notification email
        sendPayrollNotification(summary);
        
        return summary;
    }

    /**
     * Calculate monthly payroll for staff (runs end of month)
     */
    @Transactional
    public PayrollSummary calculateMonthlyPayroll() {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate today = LocalDate.now();
        
        // Get the month range
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth());
        
        log.info("Calculating monthly payroll for {} to {}", monthStart, monthEnd);
        
        // Get all active staff
        List<Employee> staff = employeeRepository.findByCompanyAndPaymentScheduleAndStatus(
                company, Employee.PaymentSchedule.MONTHLY, "ACTIVE");
        
        List<PayrollLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Employee employee : staff) {
            PayrollLineItem item = calculateEmployeePay(company, employee, monthStart, monthEnd);
            if (item.netPay().compareTo(BigDecimal.ZERO) > 0) {
                lineItems.add(item);
                totalAmount = totalAmount.add(item.netPay());
            }
        }
        
        // Create payroll run
        String reference = "MONTHLY-" + monthEnd.format(DateTimeFormatter.ofPattern("yyyyMM"));
        PayrollRun run = createPayrollRun(company, reference, today, totalAmount, lineItems);
        
        PayrollSummary summary = new PayrollSummary(
                run.getId(),
                reference,
                "MONTHLY",
                monthStart,
                monthEnd,
                lineItems,
                totalAmount,
                LocalDateTime.now()
        );
        
        // Send notification email
        sendPayrollNotification(summary);
        
        return summary;
    }

    /**
     * Calculate pay for a single employee based on attendance
     */
    private PayrollLineItem calculateEmployeePay(Company company, Employee employee, 
                                                  LocalDate startDate, LocalDate endDate) {
        // Count attendance
        long presentDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.PRESENT);
        long halfDays = attendanceRepository.countByEmployeeAndDateRangeAndStatus(
                company, employee, startDate, endDate, Attendance.AttendanceStatus.HALF_DAY);
        
        // Calculate gross pay
        BigDecimal grossPay = employee.calculatePay((int) presentDays, (int) halfDays);
        
        // Deduct advance balance if any
        BigDecimal advanceDeduction = BigDecimal.ZERO;
        if (employee.getAdvanceBalance().compareTo(BigDecimal.ZERO) > 0) {
            // Deduct up to 50% of gross pay or remaining advance, whichever is less
            BigDecimal maxDeduction = grossPay.multiply(new BigDecimal("0.5")).setScale(2, RoundingMode.HALF_UP);
            advanceDeduction = employee.getAdvanceBalance().min(maxDeduction);
        }
        
        BigDecimal netPay = grossPay.subtract(advanceDeduction);
        
        return new PayrollLineItem(
                employee.getId(),
                employee.getFullName(),
                employee.getEmployeeType().name(),
                employee.getDailyRate(),
                (int) presentDays,
                (int) halfDays,
                grossPay,
                advanceDeduction,
                netPay
        );
    }

    /**
     * Preview payroll without creating a run
     */
    @Transactional(readOnly = true)
    public PayrollSummary previewPayroll(Employee.PaymentSchedule schedule) {
        Company company = companyContextService.requireCurrentCompany();
        LocalDate today = LocalDate.now();
        
        LocalDate startDate, endDate;
        String type;
        
        if (schedule == Employee.PaymentSchedule.WEEKLY) {
            endDate = today.getDayOfWeek() == DayOfWeek.SATURDAY 
                    ? today.minusDays(1) 
                    : today.with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));
            startDate = endDate.minusDays(5);
            type = "WEEKLY";
        } else {
            startDate = today.withDayOfMonth(1);
            endDate = today.with(TemporalAdjusters.lastDayOfMonth());
            type = "MONTHLY";
        }
        
        List<Employee> employees = employeeRepository.findByCompanyAndPaymentScheduleAndStatus(
                company, schedule, "ACTIVE");
        
        List<PayrollLineItem> lineItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Employee employee : employees) {
            PayrollLineItem item = calculateEmployeePay(company, employee, startDate, endDate);
            lineItems.add(item);
            totalAmount = totalAmount.add(item.netPay());
        }
        
        return new PayrollSummary(
                null, // Not saved yet
                "PREVIEW-" + type,
                type,
                startDate,
                endDate,
                lineItems,
                totalAmount,
                LocalDateTime.now()
        );
    }

    /**
     * Record an advance payment to an employee
     */
    @Transactional
    public void recordAdvancePayment(Long employeeId, BigDecimal amount) {
        Company company = companyContextService.requireCurrentCompany();
        Employee employee = employeeRepository.findByCompanyAndId(company, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        
        BigDecimal newBalance = employee.getAdvanceBalance().add(amount);
        employee.setAdvanceBalance(newBalance);
        employeeRepository.save(employee);
        
        log.info("Recorded advance payment of {} for {}. New balance: {}", 
                amount, employee.getFullName(), newBalance);
    }

    private PayrollRun createPayrollRun(Company company, String reference, LocalDate runDate,
                                         BigDecimal totalAmount, List<PayrollLineItem> lineItems) {
        PayrollRun run = new PayrollRun();
        run.setCompany(company);
        run.setRunDate(runDate);
        run.setTotalAmount(totalAmount);
        run.setStatus("PENDING");
        run.setIdempotencyKey(reference);
        run.setNotes("Auto-calculated from attendance");
        payrollRunRepository.save(run);
        
        // Create line items
        for (PayrollLineItem item : lineItems) {
            PayrollRunLine line = new PayrollRunLine();
            line.setPayrollRun(run);
            line.setName(item.employeeName());
            line.setDaysWorked(item.presentDays() + item.halfDays()); // Total days (half days count as 1 for display)
            line.setDailyWage(item.dailyRate());
            line.setAdvances(item.advanceDeduction());
            line.setLineTotal(item.netPay());
            line.setNotes(item.employeeType() + " - " + item.presentDays() + " full + " + item.halfDays() + " half days");
            payrollRunLineRepository.save(line);
        }
        
        return run;
    }

    private void sendPayrollNotification(PayrollSummary summary) {
        Company company = companyContextService.requireCurrentCompany();
        try {
            // Generate PDF
            byte[] pdfContent = generatePayrollPdf(company, summary);
            
            String subject = String.format("Payroll Payment Sheet: %s - Total ₹%,.2f", 
                    summary.reference(), summary.totalAmount());
            
            String emailBody = String.format("""
                Dear Admin,
                
                Please find attached the %s Payroll Payment Sheet for the period %s to %s.
                
                SUMMARY:
                - Total Employees: %d
                - Total Gross: ₹%,.2f
                - Total Advance Deductions: ₹%,.2f
                - CASH TO WITHDRAW: ₹%,.2f
                
                Please print the attached PDF, withdraw the cash, and distribute payments.
                Each employee should sign against their name upon receiving payment.
                
                Generated: %s
                
                Regards,
                Big Bright Paints ERP System
                """,
                    summary.type(),
                    summary.startDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                    summary.endDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")),
                    summary.lineItems().size(),
                    summary.lineItems().stream().map(PayrollLineItem::grossPay).reduce(BigDecimal.ZERO, BigDecimal::add),
                    summary.lineItems().stream().map(PayrollLineItem::advanceDeduction).reduce(BigDecimal.ZERO, BigDecimal::add),
                    summary.totalAmount(),
                    summary.calculatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
            );
            
            String fileName = String.format("Payroll-%s.pdf", summary.reference());
            
            MimeMessagePreparator preparator = mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(payrollNotificationEmail);
                helper.setFrom(fromAddress);
                helper.setSubject(subject);
                helper.setText(emailBody);
                helper.addAttachment(fileName, () -> new java.io.ByteArrayInputStream(pdfContent), "application/pdf");
            };
            
            mailSender.send(preparator);
            log.info("Sent payroll PDF notification to {}", payrollNotificationEmail);
        } catch (Exception e) {
            log.error("Failed to send payroll notification: {}", e.getMessage(), e);
        }
    }

    private byte[] generatePayrollPdf(Company company, PayrollSummary summary) {
        Context context = new Context();
        context.setVariable("companyName", company.getName());
        context.setVariable("payrollType", summary.type());
        context.setVariable("reference", summary.reference());
        context.setVariable("startDate", summary.startDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        context.setVariable("endDate", summary.endDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        context.setVariable("generatedAt", summary.calculatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")));
        context.setVariable("lineItems", summary.lineItems());
        context.setVariable("totalGross", summary.lineItems().stream()
                .map(PayrollLineItem::grossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        context.setVariable("totalAdvance", summary.lineItems().stream()
                .map(PayrollLineItem::advanceDeduction)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        context.setVariable("totalAmount", summary.totalAmount());
        
        String html = templateEngine.process("payroll-sheet", context);
        
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate payroll PDF", e);
        }
    }

    // DTOs
    public record PayrollLineItem(
            Long employeeId,
            String employeeName,
            String employeeType,
            BigDecimal dailyRate,
            int presentDays,
            int halfDays,
            BigDecimal grossPay,
            BigDecimal advanceDeduction,
            BigDecimal netPay
    ) {}

    public record PayrollSummary(
            Long payrollRunId,
            String reference,
            String type,
            LocalDate startDate,
            LocalDate endDate,
            List<PayrollLineItem> lineItems,
            BigDecimal totalAmount,
            LocalDateTime calculatedAt
    ) {}
}
