package com.bigbrightpaints.erp.orchestrator.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.orchestrator.dto.ApproveOrderRequest;
import com.bigbrightpaints.erp.orchestrator.dto.OrderFulfillmentRequest;
import com.bigbrightpaints.erp.orchestrator.service.CommandDispatcher;
import com.bigbrightpaints.erp.orchestrator.service.CorrelationIdentifierSanitizer;
import com.bigbrightpaints.erp.orchestrator.service.TraceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orchestrator")
public class OrchestratorController {
  private final CommandDispatcher commandDispatcher;
  private final TraceService traceService;

  public OrchestratorController(CommandDispatcher commandDispatcher, TraceService traceService) {
    this.commandDispatcher = commandDispatcher;
    this.traceService = traceService;
  }

  @PostMapping("/orders/{orderId}/approve")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')")
  public ResponseEntity<Map<String, Object>> approveOrder(
      @PathVariable String orderId,
      @Valid @RequestBody ApproveOrderRequest request,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "Idempotency-Key",
              required = false)
          String idempotencyKey,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Request-Id",
              required = false)
          String requestId,
      Principal principal) {
    String companyCode = requireCompanyCode();
    String sanitizedRequestId = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(requestId);
    String sanitizedHeaderIdempotencyKey =
        CorrelationIdentifierSanitizer.sanitizeOptionalIdempotencyKey(idempotencyKey);
    ApproveOrderRequest raw =
        new ApproveOrderRequest(orderId, request.approvedBy(), request.totalAmount());
    ApproveOrderRequest normalized =
        new ApproveOrderRequest(
            orderId,
            canonicalText(request.approvedBy()),
            stripTrailingZeros(request.totalAmount()));
    String traceId =
        commandDispatcher.approveOrder(
            selectPayloadForIdempotency(sanitizedHeaderIdempotencyKey, raw, normalized),
            resolveIdempotencyKey(
                sanitizedHeaderIdempotencyKey,
                sanitizedRequestId,
                "ORCH.ORDER.APPROVE",
                companyCode,
                normalized.orderId()
                    + "|"
                    + canonicalText(normalized.approvedBy())
                    + "|"
                    + canonicalAmount(normalized.totalAmount())),
            sanitizedRequestId,
            companyCode,
            principal.getName());
    return ResponseEntity.accepted().body(Map.of("traceId", traceId));
  }

  @PostMapping("/orders/{orderId}/fulfillment")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')")
  public ResponseEntity<Map<String, Object>> fulfillOrder(
      @PathVariable String orderId,
      @Valid @RequestBody OrderFulfillmentRequest request,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "Idempotency-Key",
              required = false)
          String idempotencyKey,
      @org.springframework.web.bind.annotation.RequestHeader(
              value = "X-Request-Id",
              required = false)
          String requestId,
      Principal principal) {
    String companyCode = requireCompanyCode();
    String sanitizedRequestId = CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(requestId);
    String sanitizedHeaderIdempotencyKey =
        CorrelationIdentifierSanitizer.sanitizeOptionalIdempotencyKey(idempotencyKey);
    OrderFulfillmentRequest normalized =
        new OrderFulfillmentRequest(
            normalizeFulfillmentStatus(request.status()), canonicalText(request.notes()));
    String traceId =
        commandDispatcher.updateOrderFulfillment(
            orderId,
            selectPayloadForIdempotency(sanitizedHeaderIdempotencyKey, request, normalized),
            resolveIdempotencyKey(
                sanitizedHeaderIdempotencyKey,
                sanitizedRequestId,
                "ORCH.ORDER.FULFILLMENT.UPDATE",
                companyCode,
                orderId
                    + "|"
                    + canonicalText(normalized.status())
                    + "|"
                    + canonicalText(normalized.notes())),
            sanitizedRequestId,
            companyCode,
            principal.getName());
    return ResponseEntity.accepted().body(Map.of("traceId", traceId));
  }

  @GetMapping("/traces/{traceId}")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_SALES','ROLE_FACTORY')")
  public ResponseEntity<Map<String, Object>> trace(@PathVariable String traceId) {
    String sanitizedTraceId = CorrelationIdentifierSanitizer.sanitizeRequiredTraceId(traceId);
    return ResponseEntity.ok(
        Map.of("traceId", sanitizedTraceId, "events", traceService.getTrace(sanitizedTraceId)));
  }

  @GetMapping("/health/integrations")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> integrationsHealth() {
    return ResponseEntity.ok(commandDispatcher.integrationHealth());
  }

  @GetMapping("/health/events")
  @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
  public ResponseEntity<Map<String, Object>> eventHealth() {
    return ResponseEntity.ok(commandDispatcher.eventHealth());
  }

  private String requireCompanyCode() {
    String companyCode = CompanyContextHolder.getCompanyCode();
    if (!StringUtils.hasText(companyCode)) {
      throw new IllegalStateException("Company context is required");
    }
    return companyCode.trim();
  }

  private String resolveIdempotencyKey(
      String idempotencyKey,
      String requestId,
      String commandName,
      String companyCode,
      String payloadSignature) {
    if (StringUtils.hasText(idempotencyKey)) {
      return CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey(idempotencyKey);
    }
    if (StringUtils.hasText(requestId)) {
      String requestScoped = "REQ|" + commandName + "|" + requestId;
      if (requestScoped.length() <= CorrelationIdentifierSanitizer.MAX_IDEMPOTENCY_KEY_LENGTH) {
        return CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey(requestScoped);
      }
      return CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey(
          "REQH|" + commandName + "|" + IdempotencyUtils.sha256Hex(requestScoped));
    }
    String source =
        commandName + "|" + canonicalText(companyCode) + "|" + canonicalText(payloadSignature);
    return CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey(
        "AUTO|" + commandName + "|" + IdempotencyUtils.sha256Hex(source));
  }

  private static String normalizeFulfillmentStatus(String status) {
    return canonicalText(status).toUpperCase(Locale.ROOT);
  }

  private static String canonicalText(String value) {
    return value == null ? "" : value.trim();
  }

  private static <T> T selectPayloadForIdempotency(
      String idempotencyKey, T rawPayload, T normalizedPayload) {
    return StringUtils.hasText(idempotencyKey) ? rawPayload : normalizedPayload;
  }

  private static BigDecimal stripTrailingZeros(BigDecimal amount) {
    return amount == null ? null : amount.stripTrailingZeros();
  }

  private static String canonicalAmount(BigDecimal amount) {
    return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
  }
}
