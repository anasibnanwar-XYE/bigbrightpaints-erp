package com.bigbrightpaints.erp.core.config;

import org.springframework.core.task.TaskDecorator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;

/**
 * TaskDecorator that propagates CompanyContext and SecurityContext to async threads.
 * This ensures @Async methods have access to the company context and security principal.
 */
public class CompanyContextTaskDecorator implements TaskDecorator {

  @Override
  public Runnable decorate(Runnable runnable) {
    // Capture context from the calling thread
    String companyCode = CompanyContextHolder.getCompanyCode();
    SecurityContext callerContext = SecurityContextHolder.getContext();
    Authentication authenticationSnapshot =
        callerContext != null ? callerContext.getAuthentication() : null;

    return () -> {
      try {
        // Restore context in the async thread
        if (companyCode != null) {
          CompanyContextHolder.setCompanyCode(companyCode);
        }
        if (authenticationSnapshot != null) {
          SecurityContext workerContext = SecurityContextHolder.createEmptyContext();
          workerContext.setAuthentication(authenticationSnapshot);
          SecurityContextHolder.setContext(workerContext);
        } else {
          SecurityContextHolder.clearContext();
        }
        runnable.run();
      } finally {
        // Clean up to prevent context leaks
        CompanyContextHolder.clear();
        SecurityContextHolder.clearContext();
      }
    };
  }
}
