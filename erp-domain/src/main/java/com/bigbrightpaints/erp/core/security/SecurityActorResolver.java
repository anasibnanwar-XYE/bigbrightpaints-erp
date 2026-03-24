package com.bigbrightpaints.erp.core.security;

import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

/**
 * Centralized actor resolution for security-sensitive audit attribution.
 */
public final class SecurityActorResolver {

  public static final String UNKNOWN_AUTH_ACTOR = "UNKNOWN_AUTH_ACTOR";
  public static final String SYSTEM_PROCESS_ACTOR = "SYSTEM_PROCESS";

  private static final List<String> BACKGROUND_THREAD_PREFIXES =
      List.of("erp-async-", "orchestrator-scheduler-");

  private SecurityActorResolver() {}

  public static String resolveActorOrUnknown() {
    return currentAuthenticationName().orElse(UNKNOWN_AUTH_ACTOR);
  }

  public static String resolveActorWithSystemProcessFallback() {
    return currentAuthenticationName()
        .orElseGet(() -> isBackgroundExecutionThread() ? SYSTEM_PROCESS_ACTOR : UNKNOWN_AUTH_ACTOR);
  }

  public static String resolvePrincipalOrUnknown(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      return UNKNOWN_AUTH_ACTOR;
    }
    return principal.getName().trim();
  }

  public static Authentication systemProcessAuthentication() {
    return new UsernamePasswordAuthenticationToken(
        SYSTEM_PROCESS_ACTOR, "N/A", List.of(new SimpleGrantedAuthority("ROLE_SYSTEM")));
  }

  private static Optional<String> currentAuthenticationName() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return Optional.empty();
    }
    return Optional.of(authentication.getName().trim());
  }

  private static boolean isBackgroundExecutionThread() {
    String threadName = Thread.currentThread().getName();
    if (!StringUtils.hasText(threadName)) {
      return false;
    }
    String normalized = threadName.toLowerCase(Locale.ROOT);
    return BACKGROUND_THREAD_PREFIXES.stream().anyMatch(normalized::startsWith);
  }
}
