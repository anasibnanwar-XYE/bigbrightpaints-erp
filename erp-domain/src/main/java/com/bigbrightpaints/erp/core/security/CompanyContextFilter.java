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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CompanyContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CompanyContextFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String headerCompanyId = request.getHeader("X-Company-Id");
            Object claimsAttr = request.getAttribute("jwtClaims");
            if (claimsAttr instanceof Claims claims) {
                String tokenCompanyId = claims.get("cid", String.class);
                if (StringUtils.hasText(tokenCompanyId) && StringUtils.hasText(headerCompanyId)
                        && !tokenCompanyId.trim().equalsIgnoreCase(headerCompanyId.trim())) {
                    log.warn("Rejecting X-Company-Id mismatch. tokenCid={}, headerCid={}, path={}",
                            tokenCompanyId, headerCompanyId, request.getRequestURI());
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "X-Company-Id does not match authenticated company context");
                    return;
                }
                if (StringUtils.hasText(tokenCompanyId)) {
                    headerCompanyId = tokenCompanyId;
                }
            } else {
                // Do not allow unauthenticated requests to set tenant context via header.
                headerCompanyId = null;
            }
            String companyId = StringUtils.hasText(headerCompanyId) ? headerCompanyId.trim() : null;
            if (companyId != null) {
                // Validate user has access to this company
                if (!validateCompanyAccess(companyId)) {
                    log.warn("User attempted to access unauthorized company: {}", companyId);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to company: " + companyId);
                    return;
                }
                CompanyContextHolder.setCompanyId(companyId);
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
        // Fail closed for unknown principal types.
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/v3");
    }
}
