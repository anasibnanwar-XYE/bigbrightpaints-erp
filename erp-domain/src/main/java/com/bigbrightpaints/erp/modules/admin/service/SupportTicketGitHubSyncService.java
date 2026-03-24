package com.bigbrightpaints.erp.modules.admin.service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.Context;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;

@Service
public class SupportTicketGitHubSyncService {

  private static final Logger log = LoggerFactory.getLogger(SupportTicketGitHubSyncService.class);

  private final SupportTicketRepository supportTicketRepository;
  private final UserAccountRepository userAccountRepository;
  private final GitHubIssueClient gitHubIssueClient;
  private final EmailService emailService;

  public SupportTicketGitHubSyncService(
      SupportTicketRepository supportTicketRepository,
      UserAccountRepository userAccountRepository,
      GitHubIssueClient gitHubIssueClient,
      EmailService emailService) {
    this.supportTicketRepository = supportTicketRepository;
    this.userAccountRepository = userAccountRepository;
    this.gitHubIssueClient = gitHubIssueClient;
    this.emailService = emailService;
  }

  @Async("taskExecutor")
  @Transactional
  public void submitGitHubIssueAsync(Long ticketId) {
    if (ticketId == null) {
      return;
    }
    SupportTicket ticket = supportTicketRepository.findById(ticketId).orElse(null);
    if (ticket == null) {
      return;
    }
    if (!gitHubIssueClient.isEnabledAndConfigured()) {
      ticket.setGithubLastError("GitHub integration disabled or not configured");
      ticket.setGithubLastSyncAt(CompanyTime.now(ticket.getCompany()));
      supportTicketRepository.save(ticket);
      return;
    }

    try {
      GitHubIssueClient.GitHubIssueCreateResult result =
          gitHubIssueClient.createIssue(
              buildIssueTitle(ticket), buildIssueBody(ticket), labelsForCategory(ticket));
      ticket.setGithubIssueNumber(result.issueNumber());
      ticket.setGithubIssueUrl(result.issueUrl());
      ticket.setGithubIssueState(result.issueState());
      ticket.setGithubSyncedAt(result.syncedAt());
      ticket.setGithubLastSyncAt(result.syncedAt());
      ticket.setGithubLastError(null);
      supportTicketRepository.save(ticket);
    } catch (ApplicationException ex) {
      log.warn("Failed creating GitHub issue for support ticket {}", ticketId, ex);
      ticket.setGithubLastError(ex.getUserMessage());
      ticket.setGithubLastSyncAt(CompanyTime.now(ticket.getCompany()));
      supportTicketRepository.save(ticket);
    } catch (RuntimeException ex) {
      log.warn("Unexpected GitHub issue creation failure for support ticket {}", ticketId, ex);
      ticket.setGithubLastError("Unexpected GitHub sync error");
      ticket.setGithubLastSyncAt(CompanyTime.now(ticket.getCompany()));
      supportTicketRepository.save(ticket);
    }
  }

  @Scheduled(cron = "0 */5 * * * *")
  @Transactional
  public void syncGitHubIssueStatuses() {
    if (!gitHubIssueClient.isEnabledAndConfigured()) {
      return;
    }
    List<SupportTicket> tickets =
        supportTicketRepository
            .findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS));

    for (SupportTicket ticket : tickets) {
      Long issueNumber = ticket.getGithubIssueNumber();
      if (issueNumber == null || issueNumber <= 0L) {
        continue;
      }
      try {
        GitHubIssueClient.GitHubIssueStateResult stateResult =
            gitHubIssueClient.fetchIssueState(issueNumber);
        updateTicketFromGitHubState(ticket, stateResult);
        supportTicketRepository.save(ticket);
      } catch (ApplicationException ex) {
        log.warn("Failed syncing GitHub state for support ticket {}", ticket.getId(), ex);
        ticket.setGithubLastError(ex.getUserMessage());
        ticket.setGithubLastSyncAt(CompanyTime.now(ticket.getCompany()));
        supportTicketRepository.save(ticket);
      } catch (RuntimeException ex) {
        log.warn("Unexpected GitHub sync failure for support ticket {}", ticket.getId(), ex);
        ticket.setGithubLastError("Unexpected GitHub sync error");
        ticket.setGithubLastSyncAt(CompanyTime.now(ticket.getCompany()));
        supportTicketRepository.save(ticket);
      }
    }
  }

  private void updateTicketFromGitHubState(
      SupportTicket ticket, GitHubIssueClient.GitHubIssueStateResult stateResult) {
    String issueState =
        StringUtils.hasText(stateResult.issueState())
            ? stateResult.issueState().trim().toUpperCase(Locale.ROOT)
            : "UNKNOWN";

    ticket.setGithubIssueState(issueState);
    ticket.setGithubIssueUrl(stateResult.issueUrl());
    ticket.setGithubSyncedAt(stateResult.syncedAt());
    ticket.setGithubLastSyncAt(stateResult.syncedAt());
    ticket.setGithubLastError(null);

    SupportTicketStatus previousStatus = ticket.getStatus();
    if ("CLOSED".equals(issueState)) {
      ticket.setStatus(SupportTicketStatus.RESOLVED);
      boolean newlyResolved = ticket.getResolvedAt() == null;
      if (newlyResolved) {
        ticket.setResolvedAt(CompanyTime.now(ticket.getCompany()));
      }
      if (newlyResolved || ticket.getResolvedNotificationSentAt() == null) {
        notifyResolved(ticket);
      }
    } else if ((previousStatus == SupportTicketStatus.RESOLVED
            || previousStatus == SupportTicketStatus.CLOSED)
        && "OPEN".equals(issueState)) {
      ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
      ticket.setResolvedAt(null);
      ticket.setResolvedNotificationSentAt(null);
    } else if (previousStatus == SupportTicketStatus.OPEN && "OPEN".equals(issueState)) {
      ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
    }
  }

  private void notifyResolved(SupportTicket ticket) {
    UserAccount requester = userAccountRepository.findById(ticket.getUserId()).orElse(null);
    if (requester == null || !StringUtils.hasText(requester.getEmail())) {
      return;
    }

    try {
      Context context = new Context();
      context.setVariable("displayName", requester.getDisplayName());
      context.setVariable("ticketId", ticket.getId());
      context.setVariable("ticketSubject", ticket.getSubject());
      context.setVariable("ticketStatus", ticket.getStatus().name());
      context.setVariable("ticketCategory", ticket.getCategory().name());
      context.setVariable("githubIssueNumber", ticket.getGithubIssueNumber());
      context.setVariable("githubIssueUrl", ticket.getGithubIssueUrl());
      context.setVariable("resolvedAt", ticket.getResolvedAt());
      context.setVariable("preheader", "Your support ticket has been resolved.");
      context.setVariable("subject", "Support ticket resolved - #" + ticket.getId());

      emailService.sendTemplatedEmailRequired(
          requester.getEmail(),
          "Support ticket resolved - #" + ticket.getId(),
          "mail/ticket-resolved",
          context);
      ticket.setResolvedNotificationSentAt(CompanyTime.now(ticket.getCompany()));
    } catch (ApplicationException ex) {
      ticket.setGithubLastError("Resolved notification failed: " + ex.getUserMessage());
      log.warn("Failed to send support ticket resolved email for ticket {}", ticket.getId(), ex);
    } catch (RuntimeException ex) {
      ticket.setGithubLastError("Resolved notification failed: runtime error");
      log.warn("Failed to send support ticket resolved email for ticket {}", ticket.getId(), ex);
    }
  }

  private List<String> labelsForCategory(SupportTicket ticket) {
    if (ticket == null || ticket.getCategory() == null) {
      return Collections.emptyList();
    }
    return switch (ticket.getCategory()) {
      case BUG -> List.of("bug");
      case FEATURE_REQUEST -> List.of("enhancement");
      case SUPPORT -> List.of("support");
    };
  }

  private String buildIssueTitle(SupportTicket ticket) {
    return "[" + ticket.getCategory().name() + "] " + ticket.getSubject();
  }

  private String buildIssueBody(SupportTicket ticket) {
    StringBuilder builder = new StringBuilder();
    builder.append("Support ticket created from ERP\n\n");
    builder.append("- Ticket ID: ").append(ticket.getId()).append('\n');
    builder
        .append("- Company: ")
        .append(ticket.getCompany() != null ? ticket.getCompany().getCode() : "UNKNOWN")
        .append('\n');
    builder.append("- Requester User ID: ").append(ticket.getUserId()).append('\n');
    builder.append("- Category: ").append(ticket.getCategory().name()).append("\n\n");
    builder.append("Description:\n");
    builder.append(ticket.getDescription());
    return builder.toString();
  }
}
