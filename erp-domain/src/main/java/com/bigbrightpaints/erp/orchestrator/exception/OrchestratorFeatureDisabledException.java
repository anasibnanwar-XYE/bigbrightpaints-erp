package com.bigbrightpaints.erp.orchestrator.exception;

public class OrchestratorFeatureDisabledException extends RuntimeException {

  private final String canonicalPath;

  public OrchestratorFeatureDisabledException(String message, String canonicalPath) {
    super(message);
    this.canonicalPath = canonicalPath;
  }

  public String getCanonicalPath() {
    return canonicalPath;
  }
}
