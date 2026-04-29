package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

  List<SupportTicket> findAllByOrderByCreatedAtDesc();

  List<SupportTicket> findByCompanyOrderByCreatedAtDesc(Company company);

  List<SupportTicket> findByCompanyAndUserIdOrderByCreatedAtDesc(Company company, Long userId);

  Optional<SupportTicket> findByCompanyAndUserIdAndId(Company company, Long userId, Long id);

  Optional<SupportTicket> findByCompanyAndId(Company company, Long id);

  long countByCompanyAndStatus(Company company, SupportTicketStatus status);

  @Query(
      """
      SELECT t FROM SupportTicket t
      JOIN t.company c
      WHERE (:status IS NULL OR t.status = :status)
        AND (
          :query IS NULL
          OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
        )
      """)
  Page<SupportTicket> findSuperAdminQueue(
      @Param("status") SupportTicketStatus status, @Param("query") String query, Pageable pageable);

  List<SupportTicket> findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc(
      Collection<SupportTicketStatus> statuses);

  @Query("SELECT u FROM UserAccount u WHERE u.id IN :ids")
  List<UserAccount> findUsersByIdIn(@Param("ids") Set<Long> ids);
}
