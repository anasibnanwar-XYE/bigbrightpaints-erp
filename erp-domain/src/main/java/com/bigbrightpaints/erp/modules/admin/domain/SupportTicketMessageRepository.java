package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;

public interface SupportTicketMessageRepository extends JpaRepository<SupportTicketMessage, Long> {

  Page<SupportTicketMessage> findByTicketAndVisibilityOrderByCreatedAtAscIdAsc(
      SupportTicket ticket, SupportTicketMessageVisibility visibility, Pageable pageable);

  List<SupportTicketMessage> findByTicketAndVisibilityInOrderByCreatedAtAscIdAsc(
      SupportTicket ticket, Collection<SupportTicketMessageVisibility> visibilities);

  @Query("SELECT u FROM UserAccount u WHERE u.id IN :ids")
  List<UserAccount> findUsersByIdIn(@Param("ids") Set<Long> ids);
}
