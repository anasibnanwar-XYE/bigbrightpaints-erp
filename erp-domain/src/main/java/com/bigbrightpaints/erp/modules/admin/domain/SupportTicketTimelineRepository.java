package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketTimelineRepository
    extends JpaRepository<SupportTicketTimelineEntry, Long> {

  List<SupportTicketTimelineEntry> findByTicketOrderByCreatedAtAscIdAsc(SupportTicket ticket);

  boolean existsByTicketAndEventType(SupportTicket ticket, String eventType);

  long countByTicketAndEventTypeIn(SupportTicket ticket, Collection<String> eventTypes);
}
