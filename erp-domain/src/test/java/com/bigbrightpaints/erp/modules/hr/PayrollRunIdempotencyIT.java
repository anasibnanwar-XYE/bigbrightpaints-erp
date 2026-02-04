package com.bigbrightpaints.erp.modules.hr;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.hr.service.PayrollService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("HR: Payroll run idempotency scope")
public class PayrollRunIdempotencyIT extends AbstractIntegrationTest {

    @Autowired
    private PayrollService payrollService;

    @Autowired
    private PayrollRunRepository payrollRunRepository;

    @AfterEach
    void clearContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void idempotency_key_is_scoped_by_company() {
        Company companyA = dataSeeder.ensureCompany("IDEMP-A", "Idempotency A Co");
        Company companyB = dataSeeder.ensureCompany("IDEMP-B", "Idempotency B Co");
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now().minusDays(1);

        CompanyContextHolder.setCompanyId(companyA.getCode());
        PayrollService.PayrollRunDto runA1 = payrollService.createPayrollRun(
                new PayrollService.CreatePayrollRunRequest(
                        PayrollRun.RunType.WEEKLY,
                        start,
                        end,
                        "Payroll run A"
                ));
        PayrollService.PayrollRunDto runA2 = payrollService.createPayrollRun(
                new PayrollService.CreatePayrollRunRequest(
                        PayrollRun.RunType.WEEKLY,
                        start,
                        end,
                        "Payroll run A"
                ));
        assertThat(runA2.id()).isEqualTo(runA1.id());

        assertThatThrownBy(() -> payrollService.createPayrollRun(
                new PayrollService.CreatePayrollRunRequest(
                        PayrollRun.RunType.WEEKLY,
                        start,
                        end,
                        "Payroll run A updated"
                )))
                .isInstanceOf(ApplicationException.class)
                .extracting(ex -> ((ApplicationException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONCURRENCY_CONFLICT);

        CompanyContextHolder.setCompanyId(companyB.getCode());
        PayrollService.PayrollRunDto runB1 = payrollService.createPayrollRun(
                new PayrollService.CreatePayrollRunRequest(
                        PayrollRun.RunType.WEEKLY,
                        start,
                        end,
                        "Payroll run B"
                ));
        assertThat(runB1.id()).isNotEqualTo(runA1.id());

        String idempotencyKey = "PAYROLL:%s:%s:%s".formatted(PayrollRun.RunType.WEEKLY.name(), start, end);
        assertThat(payrollRunRepository.findByCompanyAndIdempotencyKey(companyA, idempotencyKey)).isPresent();
        assertThat(payrollRunRepository.findByCompanyAndIdempotencyKey(companyB, idempotencyKey)).isPresent();
    }
}
