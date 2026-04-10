package com.bigbrightpaints.erp.core.audittrail;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditActionEventRepository
    extends JpaRepository<AuditActionEvent, Long>, JpaSpecificationExecutor<AuditActionEvent> {

  Optional<AuditActionEvent>
      findTopByCompanyIdAndModuleIgnoreCaseAndEntityTypeIgnoreCaseAndEntityIdOrderByOccurredAtDesc(
          Long companyId, String module, String entityType, String entityId);

  Optional<AuditActionEvent>
      findTopByCompanyIdAndModuleIgnoreCaseAndActionIgnoreCaseAndEntityTypeIgnoreCaseAndEntityIdOrderByOccurredAtDesc(
          Long companyId, String module, String action, String entityType, String entityId);
}
