package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.orchestrator.repository.AuditRecord;
import com.bigbrightpaints.erp.orchestrator.repository.AuditRepository;
import com.bigbrightpaints.erp.orchestrator.service.TraceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("concurrency")
@Tag("reconciliation")
class TS_RuntimeTraceServiceExecutableCoverageTest {

    @Test
    void record_persists_company_scoped_event_with_json_payload() {
        AuditRepository auditRepository = mock(AuditRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        CompanyContextService companyContextService = mock(CompanyContextService.class);
        TraceService service = new TraceService(
                auditRepository,
                companyRepository,
                companyContextService,
                new ObjectMapper()
        );

        Company company = company(11L, "C1");
        when(companyRepository.findByCodeIgnoreCase("C1")).thenReturn(Optional.of(company));

        service.record(
                "trace-1",
                "ORDER_APPROVED",
                " C1 ",
                Map.of("orderId", "SO-1001", "amount", "100.00"),
                "req-1",
                "idem-1"
        );

        ArgumentCaptor<AuditRecord> recordCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRepository).save(recordCaptor.capture());
        AuditRecord saved = recordCaptor.getValue();

        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getEventType()).isEqualTo("ORDER_APPROVED");
        assertThat(saved.getCompanyId()).isEqualTo(11L);
        assertThat(saved.getRequestId()).isEqualTo("req-1");
        assertThat(saved.getIdempotencyKey()).isEqualTo("idem-1");
        assertThat(saved.getDetails()).contains("\"orderId\":\"SO-1001\"");
    }

    @Test
    void record_handles_null_details_and_json_fallback_to_string() throws Exception {
        AuditRepository auditRepository = mock(AuditRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        CompanyContextService companyContextService = mock(CompanyContextService.class);
        Company company = company(21L, "C2");
        when(companyRepository.findByCodeIgnoreCase("C2")).thenReturn(Optional.of(company));

        TraceService serviceWithDefaultMapper = new TraceService(
                auditRepository,
                companyRepository,
                companyContextService,
                new ObjectMapper()
        );
        serviceWithDefaultMapper.record("trace-null", "NO_DETAILS", "C2", null, null, null);

        ArgumentCaptor<AuditRecord> nullDetailsCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRepository).save(nullDetailsCaptor.capture());
        assertThat(nullDetailsCaptor.getValue().getDetails()).isEqualTo("{}");

        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("broken payload") {
        });
        TraceService serviceWithBrokenMapper = new TraceService(
                auditRepository,
                companyRepository,
                companyContextService,
                brokenMapper
        );
        Map<String, Object> fallbackDetails = new LinkedHashMap<>();
        fallbackDetails.put("alpha", 1);
        fallbackDetails.put("beta", "two");

        serviceWithBrokenMapper.record("trace-fallback", "BROKEN_JSON", "C2", fallbackDetails);

        ArgumentCaptor<AuditRecord> fallbackCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRepository, org.mockito.Mockito.times(2)).save(fallbackCaptor.capture());
        List<AuditRecord> savedRecords = fallbackCaptor.getAllValues();
        AuditRecord fallbackRecord = savedRecords.get(1);
        assertThat(fallbackRecord.getDetails()).contains("alpha=1").contains("beta=two");
    }

    @Test
    void record_fails_closed_when_company_code_missing_or_unknown() {
        AuditRepository auditRepository = mock(AuditRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        CompanyContextService companyContextService = mock(CompanyContextService.class);
        TraceService service = new TraceService(
                auditRepository,
                companyRepository,
                companyContextService,
                new ObjectMapper()
        );

        assertThatThrownBy(() -> service.record("trace-1", "EVENT", " ", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Company context is required");

        when(companyRepository.findByCodeIgnoreCase(eq("UNKNOWN"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.record("trace-2", "EVENT", "UNKNOWN", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Company not found");
    }

    @Test
    void get_trace_reads_by_current_company_scope() {
        AuditRepository auditRepository = mock(AuditRepository.class);
        CompanyRepository companyRepository = mock(CompanyRepository.class);
        CompanyContextService companyContextService = mock(CompanyContextService.class);
        TraceService service = new TraceService(
                auditRepository,
                companyRepository,
                companyContextService,
                new ObjectMapper()
        );

        Company company = company(31L, "C3");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);

        AuditRecord first = new AuditRecord("trace-3", "STEP_1", null, "{}", 31L, "req-1", "idem-1");
        AuditRecord second = new AuditRecord("trace-3", "STEP_2", null, "{}", 31L, "req-1", "idem-1");
        when(auditRepository.findByTraceIdAndCompanyIdOrderByTimestampAsc("trace-3", 31L))
                .thenReturn(List.of(first, second));

        List<AuditRecord> records = service.getTrace("trace-3");

        assertThat(records).hasSize(2);
        verify(auditRepository).findByTraceIdAndCompanyIdOrderByTimestampAsc("trace-3", 31L);
    }

    private Company company(Long id, String code) {
        Company company = new Company();
        company.setCode(code);
        company.setName(code + " Pvt");
        company.setTimezone("Asia/Kolkata");
        ReflectionTestUtils.setField(company, "id", id);
        return company;
    }
}
