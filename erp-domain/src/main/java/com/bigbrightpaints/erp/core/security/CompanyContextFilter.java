package com.bigbrightpaints.erp.core.security;

import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserPrincipal;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CompanyContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CompanyContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String companyId = request.getHeader("X-Company-Id");
            Object claimsAttr = request.getAttribute("jwtClaims");
            if (claimsAttr instanceof Claims claims) {
                companyId = claims.get("cid", String.class);
            }
            if (companyId != null) {
                // Only set company context for authenticated users with access
                if (validateCompanyAccess(companyId)) {
                    CompanyContextHolder.setCompanyId(companyId);
                } else if (isAuthenticated()) {
                    // Authenticated but unauthorized
                    log.warn("User attempted to access unauthorized company: {}", companyId);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to company: " + companyId);
                    return;
                }
                // If unauthenticated, we do not set context, effectively ignoring the header.
            }
            filterChain.doFilter(request, response);
        } finally {
            CompanyContextHolder.clear();
        }
    }

    private boolean validateCompanyAccess(String companyCode) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            UserAccount user = userPrincipal.getUser();
            if (user == null || user.getCompanies() == null) {
                return false;
            }
            // Check if user has access to the requested company
            return user.getCompanies().stream()
                    .anyMatch(c -> c.getCode().equalsIgnoreCase(companyCode));
        }
        // For non-UserPrincipal (e.g., service accounts), allow access
        return true;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3");
    }
}
