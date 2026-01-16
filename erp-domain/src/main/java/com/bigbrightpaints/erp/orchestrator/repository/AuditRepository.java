package com.bigbrightpaints.erp.orchestrator.repository;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditRecord, UUID> {
    List<AuditRecord> findByCompanyAndTraceIdOrderByTimestampAsc(Company company, String traceId);
}
