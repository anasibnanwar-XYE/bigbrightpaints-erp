package com.bigbrightpaints.erp.core.validationharness;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Profile("validation-harness")
public class ValidationFaultInjectionService {

  private final Map<ValidationFaultKind, FaultState> states =
      new EnumMap<>(ValidationFaultKind.class);

  public ValidationFaultInjectionService() {
    Arrays.stream(ValidationFaultKind.values())
        .forEach(kind -> states.put(kind, FaultState.disabled(kind, null, null, null)));
  }

  public synchronized FaultState setFault(
      ValidationFaultKind kind, boolean enabled, String runMarker, String reason) {
    String safeMarker = ValidationRunNamespace.requireSafeRunMarker(runMarker);
    String safeReason = sanitizeReason(reason);
    FaultState state =
        new FaultState(kind.code(), enabled, safeMarker, safeReason, Instant.now(), true);
    states.put(kind, state);
    return state;
  }

  public synchronized FaultState state(ValidationFaultKind kind) {
    return states.getOrDefault(kind, FaultState.disabled(kind, null, null, null));
  }

  public synchronized List<FaultState> states() {
    return states.values().stream().sorted(Comparator.comparing(FaultState::code)).toList();
  }

  public boolean isEnabled(ValidationFaultKind kind) {
    return state(kind).enabled();
  }

  private String sanitizeReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      return null;
    }
    String normalized = reason.trim().replaceAll("[\\r\\n\\t]+", " ");
    return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
  }

  public record FaultState(
      String code,
      boolean enabled,
      String runMarker,
      String reason,
      Instant updatedAt,
      boolean validationOnly) {
    private static FaultState disabled(
        ValidationFaultKind kind, String runMarker, String reason, Instant updatedAt) {
      return new FaultState(kind.code(), false, runMarker, reason, updatedAt, true);
    }
  }
}
