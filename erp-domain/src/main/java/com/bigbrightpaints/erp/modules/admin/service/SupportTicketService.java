package com.bigbrightpaints.erp.modules.admin.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicket;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketCategory;
import com.bigbrightpaints.erp.modules.admin.domain.SupportTicketRepository;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketCreateRequest;
import com.bigbrightpaints.erp.modules.admin.dto.SupportTicketResponse;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class SupportTicketService {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_ACCOUNTING = "ROLE_ACCOUNTING";

    private final SupportTicketRepository supportTicketRepository;
    private final CompanyContextService companyContextService;
    private final UserAccountRepository userAccountRepository;
    private final SupportTicketGitHubSyncService supportTicketGitHubSyncService;

    public SupportTicketService(SupportTicketRepository supportTicketRepository,
                                CompanyContextService companyContextService,
                                UserAccountRepository userAccountRepository,
                                SupportTicketGitHubSyncService supportTicketGitHubSyncService) {
        this.supportTicketRepository = supportTicketRepository;
        this.companyContextService = companyContextService;
        this.userAccountRepository = userAccountRepository;
        this.supportTicketGitHubSyncService = supportTicketGitHubSyncService;
    }

    @Transactional
    public SupportTicketResponse create(SupportTicketCreateRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        UserAccount actor = requireCurrentUser();

        SupportTicket ticket = new SupportTicket();
        ticket.setCompany(company);
        ticket.setUserId(actor.getId());
        ticket.setCategory(parseCategory(request.category()));
        ticket.setSubject(normalizeRequired(request.subject(), "subject", 255));
        ticket.setDescription(normalizeRequired(request.description(), "description", 4000));

        SupportTicket saved = supportTicketRepository.save(ticket);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    supportTicketGitHubSyncService.submitGitHubIssueAsync(saved.getId());
                }
            });
        } else {
            supportTicketGitHubSyncService.submitGitHubIssueAsync(saved.getId());
        }
        return toResponse(saved, actor);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> list() {
        Company company = companyContextService.requireCurrentCompany();
        UserAccount actor = requireCurrentUser();
        if (hasRole(actor, ROLE_SUPER_ADMIN)) {
            return supportTicketRepository.findAllByOrderByCreatedAtDesc().stream()
                    .map(ticket -> toResponse(ticket, null))
                    .toList();
        }
        if (hasAnyRole(actor, ROLE_ADMIN, ROLE_ACCOUNTING)) {
            return supportTicketRepository.findByCompanyOrderByCreatedAtDesc(company).stream()
                    .map(ticket -> toResponse(ticket, null))
                    .toList();
        }
        return supportTicketRepository.findByCompanyAndUserIdOrderByCreatedAtDesc(company, actor.getId()).stream()
                .map(ticket -> toResponse(ticket, actor))
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getById(Long ticketId) {
        if (ticketId == null) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "ticketId is required");
        }

        Company company = companyContextService.requireCurrentCompany();
        UserAccount actor = requireCurrentUser();

        SupportTicket ticket;
        if (hasRole(actor, ROLE_SUPER_ADMIN)) {
            ticket = supportTicketRepository.findById(ticketId)
                    .orElseThrow(() -> notFound(ticketId));
        } else {
            ticket = supportTicketRepository.findByCompanyAndId(company, ticketId)
                    .orElseThrow(() -> notFound(ticketId));
            if (!hasAnyRole(actor, ROLE_ADMIN, ROLE_ACCOUNTING)
                    && (ticket.getUserId() == null || !ticket.getUserId().equals(actor.getId()))) {
                throw notFound(ticketId);
            }
        }

        return toResponse(ticket, null);
    }

    private ApplicationException notFound(Long ticketId) {
        return new ApplicationException(ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
                "Support ticket not found: " + ticketId);
    }

    private UserAccount requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException(ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, "Authentication is required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal && userPrincipal.getUser() != null) {
            return userPrincipal.getUser();
        }
        String actor = SecurityActorResolver.resolveActorOrUnknown();
        if (!StringUtils.hasText(actor)
                || SecurityActorResolver.UNKNOWN_AUTH_ACTOR.equals(actor)
                || SecurityActorResolver.SYSTEM_PROCESS_ACTOR.equals(actor)) {
            throw new ApplicationException(ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
                    "Authenticated user account is required");
        }
        return userAccountRepository.findByEmailIgnoreCase(actor)
                .orElseThrow(() -> new ApplicationException(ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
                        "User not found for actor: " + actor));
    }

    private boolean hasRole(UserAccount user, String roleName) {
        if (user == null || user.getRoles() == null || !StringUtils.hasText(roleName)) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }

    private boolean hasAnyRole(UserAccount user, String... roleNames) {
        for (String roleName : roleNames) {
            if (hasRole(user, roleName)) {
                return true;
            }
        }
        return false;
    }

    private SupportTicketCategory parseCategory(String rawCategory) {
        String normalized = normalizeRequired(rawCategory, "category", 32).toUpperCase(Locale.ROOT);
        try {
            return SupportTicketCategory.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Invalid category: " + rawCategory);
        }
    }

    private String normalizeRequired(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    fieldName + " is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ApplicationException(ErrorCode.VALIDATION_OUT_OF_RANGE,
                    fieldName + " exceeds max length " + maxLength);
        }
        return trimmed;
    }

    private SupportTicketResponse toResponse(SupportTicket ticket, @Nullable UserAccount requesterHint) {
        UserAccount requester = requesterHint;
        if (requester == null && ticket.getUserId() != null) {
            requester = userAccountRepository.findById(ticket.getUserId()).orElse(null);
        }
        String requesterEmail = requester != null ? requester.getEmail() : null;
        String companyCode = ticket.getCompany() != null ? ticket.getCompany().getCode() : null;

        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getPublicId(),
                companyCode,
                ticket.getUserId(),
                requesterEmail,
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getGithubIssueNumber(),
                ticket.getGithubIssueUrl(),
                ticket.getGithubIssueState(),
                ticket.getGithubSyncedAt(),
                ticket.getGithubLastError(),
                ticket.getResolvedAt(),
                ticket.getResolvedNotificationSentAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
