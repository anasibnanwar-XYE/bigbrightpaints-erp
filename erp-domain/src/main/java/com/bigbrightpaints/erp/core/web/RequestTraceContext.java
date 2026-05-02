package com.bigbrightpaints.erp.core.web;

import java.util.UUID;

import org.springframework.util.StringUtils;

public final class RequestTraceContext {

  private static final ThreadLocal<TraceMetadata> CURRENT = new ThreadLocal<>();

  private RequestTraceContext() {}

  public static TraceMetadata start(String traceId, String correlationId) {
    TraceMetadata metadata = new TraceMetadata(normalize(traceId), normalize(correlationId));
    CURRENT.set(metadata);
    return metadata;
  }

  public static TraceMetadata currentOrCreate() {
    TraceMetadata current = CURRENT.get();
    if (current != null) {
      return current;
    }
    String generated = UUID.randomUUID().toString();
    return start(generated, generated);
  }

  public static String traceId() {
    return currentOrCreate().traceId();
  }

  public static String correlationId() {
    return currentOrCreate().correlationId();
  }

  public static void clear() {
    CURRENT.remove();
  }

  private static String normalize(String value) {
    if (StringUtils.hasText(value)) {
      return value.trim();
    }
    return UUID.randomUUID().toString();
  }

  public record TraceMetadata(String traceId, String correlationId) {}
}
