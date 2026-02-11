package com.bigbrightpaints.erp.core.config;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyContextTaskDecoratorTest {

    @AfterEach
    void cleanup() {
        CompanyContextHolder.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void decorate_propagatesCompanyAndSecurityWithoutRequestAttributes() {
        CompanyContextHolder.setCompanyCode("BBP");
        SecurityContext callerContext = SecurityContextHolder.createEmptyContext();
        callerContext.setAuthentication(new UsernamePasswordAuthenticationToken("qa-user", "n/a"));
        SecurityContextHolder.setContext(callerContext);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        CompanyContextTaskDecorator decorator = new CompanyContextTaskDecorator();
        AtomicReference<String> companyInside = new AtomicReference<>();
        AtomicReference<Authentication> authInside = new AtomicReference<>();
        AtomicReference<Object> requestInside = new AtomicReference<>();

        Runnable wrapped = decorator.decorate(() -> {
            companyInside.set(CompanyContextHolder.getCompanyCode());
            authInside.set(SecurityContextHolder.getContext().getAuthentication());
            requestInside.set(RequestContextHolder.getRequestAttributes());
        });

        CompanyContextHolder.clear();
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();

        wrapped.run();

        assertThat(companyInside.get()).isEqualTo("BBP");
        assertThat(authInside.get()).isNotNull();
        assertThat(authInside.get().getName()).isEqualTo("qa-user");
        assertThat(requestInside.get()).isNull();
        assertThat(CompanyContextHolder.getCompanyCode()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void decorate_snapshotsAuthenticationAtDecorationTime() {
        CompanyContextHolder.setCompanyCode("BBP");
        SecurityContext callerContext = SecurityContextHolder.createEmptyContext();
        callerContext.setAuthentication(new UsernamePasswordAuthenticationToken("decorated-user", "n/a"));
        SecurityContextHolder.setContext(callerContext);

        CompanyContextTaskDecorator decorator = new CompanyContextTaskDecorator();
        AtomicReference<String> authNameInside = new AtomicReference<>();

        Runnable wrapped = decorator.decorate(() -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            authNameInside.set(authentication != null ? authentication.getName() : null);
        });

        callerContext.setAuthentication(new UsernamePasswordAuthenticationToken("mutated-user", "n/a"));

        CompanyContextHolder.clear();
        SecurityContextHolder.clearContext();

        wrapped.run();

        assertThat(authNameInside.get()).isEqualTo("decorated-user");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(CompanyContextHolder.getCompanyCode()).isNull();
    }
}
