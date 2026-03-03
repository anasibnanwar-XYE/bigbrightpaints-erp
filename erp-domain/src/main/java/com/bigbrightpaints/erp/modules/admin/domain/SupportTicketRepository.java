package com.bigbrightpaints.erp.modules.admin.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    List<SupportTicket> findByCompanyOrderByCreatedAtDesc(Company company);

    List<SupportTicket> findByCompanyAndUserIdOrderByCreatedAtDesc(Company company, Long userId);

    Optional<SupportTicket> findByCompanyAndId(Company company, Long id);

    List<SupportTicket> findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc(
            Collection<SupportTicketStatus> statuses);
}
