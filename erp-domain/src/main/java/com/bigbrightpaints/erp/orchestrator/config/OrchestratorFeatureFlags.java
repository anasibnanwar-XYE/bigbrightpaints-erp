package com.bigbrightpaints.erp.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrchestratorFeatureFlags {

  private final boolean payrollEnabled;
  private final boolean factoryDispatchEnabled;

  public OrchestratorFeatureFlags(
      @Value("${orchestrator.payroll.enabled:false}") boolean payrollEnabled,
      @Value("${orchestrator.factory-dispatch.enabled:false}") boolean factoryDispatchEnabled) {
    this.payrollEnabled = payrollEnabled;
    this.factoryDispatchEnabled = factoryDispatchEnabled;
  }

  public boolean isPayrollEnabled() {
    return payrollEnabled;
  }

  public boolean isFactoryDispatchEnabled() {
    return factoryDispatchEnabled;
  }
}
