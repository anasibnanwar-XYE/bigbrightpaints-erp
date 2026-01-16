package com.bigbrightpaints.erp.orchestrator.service;

import com.bigbrightpaints.erp.orchestrator.repository.AuditRecord;
import com.bigbrightpaints.erp.orchestrator.repository.AuditRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TraceService {

    private final AuditRepository auditRepository;
    private final CompanyContextService companyContextService;
    private final CompanyRepository companyRepository;

    public TraceService(AuditRepository auditRepository,
                        CompanyContextService companyContextService,
                        CompanyRepository companyRepository) {
        this.auditRepository = auditRepository;
        this.companyContextService = companyContextService;
        this.companyRepository = companyRepository;
    }

    public void record(String traceId, String eventType, Map<String, Object> details, String companyCode) {
        Company company = companyRepository.findByCodeIgnoreCase(companyCode)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyCode));
        AuditRecord record = new AuditRecord(company, traceId, eventType, Instant.now(), details.toString());
        auditRepository.save(record);
    }

    public List<AuditRecord> getTrace(String traceId) {
        Company company = companyContextService.requireCurrentCompany();
        return auditRepository.findByCompanyAndTraceIdOrderByTimestampAsc(company, traceId);
    }
}
