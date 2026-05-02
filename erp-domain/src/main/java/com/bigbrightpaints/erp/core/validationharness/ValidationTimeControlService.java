package com.bigbrightpaints.erp.core.validationharness;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("validation-harness")
public class ValidationTimeControlService {

  private final AtomicReference<TimeState> state =
      new AtomicReference<>(new TimeState(false, null, null, null, true));

  public TimeState freeze(String runMarker, Instant instant) {
    if (instant == null) {
      throw new IllegalArgumentException("instant is required");
    }
    String safeMarker = ValidationRunNamespace.requireSafeRunMarker(runMarker);
    TimeState next = new TimeState(true, instant, safeMarker, Instant.now(), true);
    state.set(next);
    return next;
  }

  public TimeState clear(String runMarker) {
    String safeMarker = ValidationRunNamespace.requireSafeRunMarker(runMarker);
    TimeState next = new TimeState(false, null, safeMarker, Instant.now(), true);
    state.set(next);
    return next;
  }

  public Optional<Instant> fixedInstant() {
    TimeState current = state.get();
    return current.enabled() ? Optional.of(current.instant()) : Optional.empty();
  }

  public TimeState state() {
    return state.get();
  }

  public record TimeState(
      boolean enabled,
      Instant instant,
      String runMarker,
      Instant updatedAt,
      boolean validationOnly) {}
}
