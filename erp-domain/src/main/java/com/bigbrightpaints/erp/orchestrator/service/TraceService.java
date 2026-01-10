package com.bigbrightpaints.erp.orchestrator.service;

import com.bigbrightpaints.erp.orchestrator.repository.AuditRecord;
import com.bigbrightpaints.erp.orchestrator.repository.AuditRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TraceService {

    private final AuditRepository auditRepository;

    public TraceService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void record(String traceId, String eventType, Map<String, Object> details) {
        AuditRecord record = new AuditRecord(traceId, eventType, Instant.now(), details.toString());
        auditRepository.save(record);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TraceEvent> getTrace(String traceId) {
        return auditRepository.findByTraceIdOrderByTimestampAsc(traceId).stream()
                .map(record -> new TraceEvent(
                        record.getId().toString(),
                        record.getEventType(),
                        record.getTimestamp(),
                        record.getDetails()))
                .toList();
    }

    public record TraceEvent(String id, String eventType, Instant timestamp, String details) {}
}
