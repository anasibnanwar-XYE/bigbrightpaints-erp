package com.bigbrightpaints.erp.core.validationharness;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("validation-harness")
public class ValidationFaultHealthIndicator implements HealthIndicator {

  private final ValidationFaultInjectionService faultInjectionService;

  public ValidationFaultHealthIndicator(ValidationFaultInjectionService faultInjectionService) {
    this.faultInjectionService = faultInjectionService;
  }

  @Override
  public Health health() {
    ValidationFaultInjectionService.FaultState state =
        faultInjectionService.state(ValidationFaultKind.HEALTH_DEGRADED);
    if (state.enabled()) {
      return Health.down()
          .withDetail("validationOnly", true)
          .withDetail("fault", state.code())
          .withDetail("runMarker", state.runMarker())
          .build();
    }
    return Health.up().withDetail("validationOnly", true).build();
  }
}
