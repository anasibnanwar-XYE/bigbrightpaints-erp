package com.bigbrightpaints.erp.core.audittrail;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Tag("critical")
class AuditCorrelationIdResolverTest {

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void resolveCorrelationId_prefersExplicitCorrelationHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID expected = UUID.fromString("11111111-1111-1111-1111-111111111111");
    request.addHeader("X-Correlation-Id", expected.toString());
    request.addHeader("X-Trace-Id", UUID.randomUUID().toString());
    request.addHeader("X-Request-Id", "REQ-1");

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request, "fallback-key");

    assertThat(resolved).isEqualTo(expected);
  }

  @Test
  void resolveCorrelationId_usesTraceHeaderWhenCorrelationHeaderIsInvalid() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    UUID expected = UUID.fromString("22222222-2222-2222-2222-222222222222");
    request.addHeader("X-Correlation-Id", "not-a-uuid");
    request.addHeader("X-Trace-Id", expected.toString());

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request);

    assertThat(resolved).isEqualTo(expected);
  }

  @Test
  void resolveCorrelationId_generatesDeterministicUuidFromFallbackCandidate() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "REQ-123");
    UUID expected =
        UUID.nameUUIDFromBytes("AUDIT-CORRELATION|REQ-123".getBytes(StandardCharsets.UTF_8));

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request, "fallback-key");

    assertThat(resolved).isEqualTo(expected);
  }

  @Test
  void resolveCorrelationId_usesRequestAttributeAndExplicitFallbackKeys() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("traceId", "trace-attribute");
    UUID expected =
        UUID.nameUUIDFromBytes(
            "AUDIT-CORRELATION|trace-attribute".getBytes(StandardCharsets.UTF_8));

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request, "secondary-key");

    assertThat(resolved).isEqualTo(expected);
  }

  @Test
  void resolveCorrelationId_returnsNullWhenNoCandidatesArePresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request, "  ", null);

    assertThat(resolved).isNull();
  }

  @Test
  void resolveCorrelationId_handlesNullFallbackArray() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request, (String[]) null);

    assertThat(resolved).isNull();
  }

  @Test
  void resolveCorrelationId_ignoresBlankRequestAttributeValues() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("traceId", "   ");

    UUID resolved = AuditCorrelationIdResolver.resolveCorrelationId(request);

    assertThat(resolved).isNull();
  }

  @Test
  void currentRequest_returnsBoundServletRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    assertThat(AuditCorrelationIdResolver.currentRequest()).isSameAs(request);
  }

  @Test
  void parseUuidOrNull_returnsNullForBlankOrMalformedValues() {
    assertThat(AuditCorrelationIdResolver.parseUuidOrNull(" ")).isNull();
    assertThat(AuditCorrelationIdResolver.parseUuidOrNull("bad-value")).isNull();
    assertThat(AuditCorrelationIdResolver.parseUuidOrNull("33333333-3333-3333-3333-333333333333"))
        .isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
  }
}
