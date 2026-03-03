package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.notification.EmailService;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketStatus;
import com.bigbrightpaints.erp.modules.admin.service.GitHubIssueClient;
import com.bigbrightpaints.erp.modules.admin.service.SupportTicketGitHubSyncService;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;

@Tag("critical")
class TS_RuntimeSupportTicketSyncExecutableCoverageTest {

    @Test
    void submitGitHubIssueAsync_handlesDisabledIntegrationGracefully() {
        SupportTicketRepository supportTicketRepository = org.mockito.Mockito.mock(SupportTicketRepository.class);
        UserAccountRepository userAccountRepository = org.mockito.Mockito.mock(UserAccountRepository.class);
        GitHubIssueClient gitHubIssueClient = org.mockito.Mockito.mock(GitHubIssueClient.class);
        EmailService emailService = org.mockito.Mockito.mock(EmailService.class);

        SupportTicketGitHubSyncService service = new SupportTicketGitHubSyncService(
                supportTicketRepository,
                userAccountRepository,
                gitHubIssueClient,
                emailService);

        SupportTicket ticket = ticket(1101L, 71L, "Disabled sync");
        when(supportTicketRepository.findById(1101L)).thenReturn(Optional.of(ticket));
        when(gitHubIssueClient.isEnabledAndConfigured()).thenReturn(false);

        service.submitGitHubIssueAsync(1101L);

        assertThat(ticket.getGithubLastError()).contains("disabled");
        verify(supportTicketRepository).save(ticket);
    }

    @Test
    void syncGithubStatus_closedIssue_marksResolvedAndSendsNotification() {
        SupportTicketRepository supportTicketRepository = org.mockito.Mockito.mock(SupportTicketRepository.class);
        UserAccountRepository userAccountRepository = org.mockito.Mockito.mock(UserAccountRepository.class);
        GitHubIssueClient gitHubIssueClient = org.mockito.Mockito.mock(GitHubIssueClient.class);
        EmailService emailService = org.mockito.Mockito.mock(EmailService.class);

        SupportTicketGitHubSyncService service = new SupportTicketGitHubSyncService(
                supportTicketRepository,
                userAccountRepository,
                gitHubIssueClient,
                emailService);

        SupportTicket ticket = ticket(1201L, 72L, "Resolution expected");
        ticket.setGithubIssueNumber(4567L);
        ticket.setStatus(SupportTicketStatus.OPEN);

        UserAccount requester = new UserAccount("requester@acme.com", "hash", "Requester");
        ReflectionTestUtils.setField(requester, "id", 72L);

        when(gitHubIssueClient.isEnabledAndConfigured()).thenReturn(true);
        when(supportTicketRepository.findTop200ByGithubIssueNumberIsNotNullAndStatusInOrderByCreatedAtAsc(
                List.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS))).thenReturn(List.of(ticket));
        when(gitHubIssueClient.fetchIssueState(4567L)).thenReturn(
                new GitHubIssueClient.GitHubIssueStateResult(
                        4567L,
                        "https://github.com/acme/repo/issues/4567",
                        "closed",
                        Instant.parse("2026-03-04T05:00:00Z")));
        when(userAccountRepository.findById(72L)).thenReturn(Optional.of(requester));

        service.syncGitHubIssueStatuses();

        assertThat(ticket.getStatus()).isEqualTo(SupportTicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();
        assertThat(ticket.getResolvedNotificationSentAt()).isNotNull();
        assertThat(ticket.getGithubIssueState()).isEqualTo("CLOSED");
        verify(emailService).sendTemplatedEmailRequired(
                eq("requester@acme.com"),
                eq("Support ticket resolved - #1201"),
                eq("mail/ticket-resolved"),
                any(Context.class));
        verify(supportTicketRepository).save(ticket);
    }

    private SupportTicket ticket(Long id, Long userId, String subject) {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "id", 901L);
        company.setCode("ACME");

        SupportTicket ticket = new SupportTicket();
        ReflectionTestUtils.setField(ticket, "id", id);
        ticket.setCompany(company);
        ticket.setUserId(userId);
        ticket.setCategory(SupportTicketCategory.SUPPORT);
        ticket.setSubject(subject);
        ticket.setDescription("Issue description");
        ticket.setStatus(SupportTicketStatus.OPEN);
        return ticket;
    }
}
