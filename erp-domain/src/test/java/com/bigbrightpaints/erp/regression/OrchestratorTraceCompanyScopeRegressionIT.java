package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.orchestrator.service.TraceService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Regression: Orchestrator traces are company-scoped")
class OrchestratorTraceCompanyScopeRegressionIT extends AbstractIntegrationTest {

    private static final String COMPANY_A = "LF-008-A";
    private static final String COMPANY_B = "LF-008-B";

    @Autowired private TraceService traceService;

    @AfterEach
    void tearDown() {
        CompanyContextHolder.clear();
    }

    @Test
    void traceLookupIsIsolatedByCompany() {
        dataSeeder.ensureCompany(COMPANY_A, "LF-008 A Ltd");
        dataSeeder.ensureCompany(COMPANY_B, "LF-008 B Ltd");

        String traceId = UUID.randomUUID().toString();
        traceService.record(traceId, "ORDER_APPROVED", Map.of("orderId", "ORDER-008"), COMPANY_A);

        CompanyContextHolder.setCompanyId(COMPANY_B);
        assertThat(traceService.getTrace(traceId)).isEmpty();

        CompanyContextHolder.setCompanyId(COMPANY_A);
        assertThat(traceService.getTrace(traceId)).hasSize(1);
    }
}
