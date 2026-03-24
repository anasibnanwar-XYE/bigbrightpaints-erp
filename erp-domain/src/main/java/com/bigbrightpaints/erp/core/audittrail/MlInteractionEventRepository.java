package com.bigbrightpaints.erp.core.audittrail;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MlInteractionEventRepository
    extends JpaRepository<MlInteractionEvent, Long>, JpaSpecificationExecutor<MlInteractionEvent> {}
