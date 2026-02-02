package com.bigbrightpaints.erp.core.security;

import com.bigbrightpaints.erp.modules.auth.domain.BlacklistedTokenRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserTokenRevocation;
import com.bigbrightpaints.erp.modules.auth.domain.UserTokenRevocationRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityVerificationTest {

    private BlacklistedTokenRepository blacklistedTokenRepository;
    private UserTokenRevocationRepository userTokenRevocationRepository;
    private JwtProperties jwtProperties;
    private TokenBlacklistService tokenBlacklistService;
    private CompanyContextFilter filter;

    @BeforeEach
    void setUp() {
        blacklistedTokenRepository = mock(BlacklistedTokenRepository.class);
        userTokenRevocationRepository = mock(UserTokenRevocationRepository.class);
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("verylongsecretkeythatisatleast32byteslong"); // Must be valid

        tokenBlacklistService = new TokenBlacklistService(
                blacklistedTokenRepository,
                userTokenRevocationRepository,
                jwtProperties
        );

        filter = new CompanyContextFilter();

        SecurityContextHolder.clearContext();
        CompanyContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        CompanyContextHolder.clear();
    }

    @Test
    void unauthenticated_request_should_not_set_company_context() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Company-Id", "COMP1");
        request.setAttribute("jwtClaims", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Ensure no auth
        SecurityContextHolder.clearContext();

        doAnswer(invocation -> {
            request.setAttribute("capturedContext", CompanyContextHolder.getCompanyId());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        // With current bug, this will be "COMP1" (captured inside chain)
        // We want it to be null
        assertThat(request.getAttribute("capturedContext"))
                .as("CompanyContext should be null for unauthenticated request")
                .isNull();

        verify(chain).doFilter(request, response);
    }

    @Test
    void authenticated_request_should_set_company_context() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Company-Id", "COMP1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // Mock authenticated user
        com.bigbrightpaints.erp.modules.auth.domain.UserAccount user = new com.bigbrightpaints.erp.modules.auth.domain.UserAccount();
        com.bigbrightpaints.erp.modules.company.domain.Company company = new com.bigbrightpaints.erp.modules.company.domain.Company();
        company.setCode("COMP1");
        company.setName("Test Company");
        user.addCompany(company);

        com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal principal = new com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, "creds", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        doAnswer(invocation -> {
            request.setAttribute("capturedContext", CompanyContextHolder.getCompanyId());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("capturedContext"))
                .as("CompanyContext should be set for authorized user")
                .isEqualTo("COMP1");

        verify(chain).doFilter(request, response);
    }

    @Test
    void cleanup_should_retain_revocations_safely() {
        // Mock Short TTL config
        jwtProperties.setRefreshTokenTtlSeconds(3600); // 1 hour

        // We want to verify that deleteOldRevocations is called with a cutoff that is SAFE.
        // Current logic uses now - TTL.
        // If we change logic to now - max(TTL, 30 days).

        // Mock Instant.now() is tricky without a clock in service.
        // But we can check the argument passed to deleteOldRevocations.

        tokenBlacklistService.cleanupExpiredTokens();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(userTokenRevocationRepository).deleteOldRevocations(captor.capture());

        Instant cutoff = captor.getValue();
        Instant now = Instant.now();

        // Expected behavior:
        // Current code: cutoff ~= now - 1 hour.
        // New code: cutoff ~= now - 30 days.

        // Assert that cutoff is older than 29 days (proving the fix)
        // For reproduction (current code), it will be recent.

        long diffSeconds = now.getEpochSecond() - cutoff.getEpochSecond();

        // We expect failure here because current code uses 1 hour (3600s)
        // We assert what we WANT (>= 30 days)
        assertThat(diffSeconds)
                .as("Revocation retention should be at least 30 days")
                .isGreaterThanOrEqualTo(2592000L - 100); // 30 days +/- buffer
    }
}
