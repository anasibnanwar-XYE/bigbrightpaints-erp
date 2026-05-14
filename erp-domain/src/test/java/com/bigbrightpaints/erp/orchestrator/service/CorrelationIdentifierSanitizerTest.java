package com.bigbrightpaints.erp.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

class CorrelationIdentifierSanitizerTest {

  @Test
  void sanitizeOptionalRequestIdHashesOversizedValuesDeterministically() {
    String oversized = "req-" + "a".repeat(220);

    String first = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(oversized);
    String second = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(oversized);

    assertThat(first).startsWith("RIDH|");
    assertThat(first.length())
        .isLessThanOrEqualTo(CorrelationIdentifierSanitizer.MAX_REQUEST_ID_LENGTH);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void sanitizeRequiredIdempotencyKeyRejectsDisallowedCharacterClass() {
    assertThatThrownBy(
            () -> CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey("idem malformed"))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((ApplicationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT));
  }

  @Test
  void sanitizeOptionalRequestIdKeepsMissingRequestIdMissing() {
    String normalized = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(null);

    assertThat(normalized).isNull();
  }

  @Test
  void sanitizeTraceIdOrGenerateKeepsValidCandidate() {
    String normalized =
        CorrelationIdentifierSanitizer.sanitizeTraceIdOrGenerate("trace-900", () -> "generated");

    assertThat(normalized).isEqualTo("trace-900");
  }

  @Test
  void sanitizeTraceIdOrGenerateUsesGeneratedValueWhenCandidateInvalid() {
    String normalized =
        CorrelationIdentifierSanitizer.sanitizeTraceIdOrGenerate("bad trace", () -> "trace-901");

    assertThat(normalized).isEqualTo("trace-901");
  }

  @Test
  void sanitizeTraceIdOrGenerateRethrowsOriginalFailureWhenGeneratorMissing() {
    assertThatThrownBy(
            () -> CorrelationIdentifierSanitizer.sanitizeTraceIdOrGenerate("bad trace", null))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((ApplicationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT));
  }

  @Test
  void sanitizeTraceIdOrGeneratePropagatesGeneratedValueFailure() {
    assertThatThrownBy(
            () ->
                CorrelationIdentifierSanitizer.sanitizeTraceIdOrGenerate(
                    "bad trace", () -> "also bad"))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((ApplicationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT));
  }

  @Test
  void safeIdempotencyForLogRedactsInvalidRawValue() {
    String raw = "idem-malformed\nlog-injection";

    String safe = CorrelationIdentifierSanitizer.safeIdempotencyForLog(raw);

    assertThat(safe).startsWith("invalid#");
    assertThat(safe).doesNotContain("\n");
    assertThat(safe).doesNotContain("log-injection");
  }

  @Test
  void sanitizeOptionalTraceIdTrimsWhitespace() {
    String traceId = CorrelationIdentifierSanitizer.sanitizeOptionalTraceId(" trace-900 ");

    assertThat(traceId).isEqualTo("trace-900");
  }
}
