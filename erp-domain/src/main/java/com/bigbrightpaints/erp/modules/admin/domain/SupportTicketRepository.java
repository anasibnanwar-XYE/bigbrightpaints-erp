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

  long countByCategoryAndStatusIn(
      SupportTicketCategory category, Collection<SupportTicketStatus> statuses);

  long countBySlaStatus(SupportTicketSlaStatus slaStatus);

  List<SupportTicket> findTop200ByStatusInAndSlaStatusNotOrderByResolutionDueAtAscIdAsc(
      Collection<SupportTicketStatus> statuses, SupportTicketSlaStatus slaStatus);

  @Query(
      value =
          """
          SELECT t FROM SupportTicket t
          JOIN t.company c
          WHERE (:status IS NULL OR t.status = :status)
            AND (:category IS NULL OR t.category = :category)
            AND (:slaStatus IS NULL OR t.slaStatus = :slaStatus)
            AND (
              :query IS NULL
              OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            )
          """)
  Page<SupportTicket> findSuperAdminQueue(
      @Param("status") SupportTicketStatus status,
      @Param("category") SupportTicketCategory category,
      @Param("slaStatus") SupportTicketSlaStatus slaStatus,
      @Param("query") String query,
      Pageable pageable);

  @Query(
      value =
          """
          SELECT t FROM SupportTicket t
          JOIN t.company c
          WHERE (:status IS NULL OR t.status = :status)
            AND (:category IS NULL OR t.category = :category)
            AND (:slaStatus IS NULL OR t.slaStatus = :slaStatus)
            AND (
              :query IS NULL
              OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            )
          ORDER BY
            CASE t.priority
              WHEN com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority.URGENT THEN 0
              WHEN com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority.HIGH THEN 1
              WHEN com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority.NORMAL THEN 2
              WHEN com.bigbrightpaints.erp.modules.admin.domain.SupportTicketPriority.LOW THEN 3
              ELSE 4
            END ASC,
            t.id ASC
          """,
      countQuery =
          """
          SELECT COUNT(t) FROM SupportTicket t
          JOIN t.company c
          WHERE (:status IS NULL OR t.status = :status)
            AND (:category IS NULL OR t.category = :category)
            AND (:slaStatus IS NULL OR t.slaStatus = :slaStatus)
            AND (
              :query IS NULL
              OR LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
            )
          """)
  Page<SupportTicket> findSuperAdminQueueOrderByPriorityRank(
      @Param("status") SupportTicketStatus status,
      @Param("category") SupportTicketCategory category,
      @Param("slaStatus") SupportTicketSlaStatus slaStatus,
      @Param("query") String query,
      Pageable pageable);

  List<SupportTicket> findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc(
      Collection<SupportTicketStatus> statuses);

  @Query("SELECT u FROM UserAccount u WHERE u.id IN :ids")
  List<UserAccount> findUsersByIdIn(@Param("ids") Set<Long> ids);
}
