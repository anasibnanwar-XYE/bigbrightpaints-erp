package com.bigbrightpaints.erp.orchestrator.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.modules.company.service.TenantRealActionUsageService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.orchestrator.config.OrchestratorFeatureFlags;
import com.bigbrightpaints.erp.orchestrator.dto.ApproveOrderRequest;
import com.bigbrightpaints.erp.orchestrator.dto.OrderFulfillmentRequest;
import com.bigbrightpaints.erp.orchestrator.event.DomainEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OrchestratorCommand;

@Service
public class CommandDispatcher {

  private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);
  private final IntegrationCoordinator integrationCoordinator;
  private final EventPublisherService eventPublisherService;
  private final TraceService traceService;
  private final OrchestratorIdempotencyService idempotencyService;
  private final OrchestratorFeatureFlags featureFlags;
  private final TenantRealActionUsageService realActionUsageService;

  public CommandDispatcher(
      IntegrationCoordinator integrationCoordinator,
      EventPublisherService eventPublisherService,
      TraceService traceService,
      OrchestratorIdempotencyService idempotencyService,
      OrchestratorFeatureFlags featureFlags,
      TenantRealActionUsageService realActionUsageService) {
    this.integrationCoordinator = integrationCoordinator;
    this.eventPublisherService = eventPublisherService;
    this.traceService = traceService;
    this.idempotencyService = idempotencyService;
    this.featureFlags = featureFlags;
    this.realActionUsageService = realActionUsageService;
  }

  @Transactional
  public String approveOrder(
      ApproveOrderRequest request,
      String idempotencyKey,
      String requestId,
      String companyId,
      String userId) {
    requireUserAndCompanyContext(userId, companyId);
    LeaseEnvelope leaseEnvelope =
        startLease("ORCH.ORDER.APPROVE", idempotencyKey, request, requestId);
    OrchestratorIdempotencyService.CommandLease lease = leaseEnvelope.lease();
    String normalizedRequestId = leaseEnvelope.normalizedRequestId();
    String canonicalIdempotencyKey = leaseEnvelope.canonicalIdempotencyKey();
    return executeWithLease(
        lease,
        companyId,
        () -> {
          String traceId = lease.traceId();
          InventoryReservationResult reservation =
              integrationCoordinator.reserveInventory(
                  request.orderId(), companyId, traceId, canonicalIdempotencyKey);
          boolean awaitingProduction = reservation != null && !reservation.shortages().isEmpty();
          String orderStatus = awaitingProduction ? "PENDING_PRODUCTION" : "READY_TO_SHIP";
          DomainEvent event =
              DomainEvent.of(
                  "OrderApprovedEvent",
                  companyId,
                  userId,
                  "Order",
                  request.orderId(),
                  Map.of(
                      "orderStatus",
                      orderStatus,
                      "awaitingProduction",
                      awaitingProduction,
                      "approvedBy",
                      request.approvedBy(),
                      "totalAmount",
                      request.totalAmount(),
                      "traceId",
                      traceId,
                      "idempotencyKey",
                      canonicalIdempotencyKey),
                  traceId,
                  normalizedRequestId,
                  canonicalIdempotencyKey);
          eventPublisherService.enqueue(event);
          traceService.record(
              traceId,
              "ORDER_APPROVED",
              companyId,
              Map.of("orderId", request.orderId(), "idempotencyKey", canonicalIdempotencyKey),
              normalizedRequestId,
              canonicalIdempotencyKey);
          return traceId;
        });
  }

  @Transactional
  public String autoApproveOrder(
      String orderId,
      BigDecimal totalAmount,
      String companyId,
      String idempotencyKey,
      String requestId) {
    LeaseEnvelope leaseEnvelope =
        startLease(
            "ORCH.ORDER.AUTO_APPROVE",
            idempotencyKey,
            Map.of("orderId", orderId, "totalAmount", totalAmount),
            requestId);
    OrchestratorIdempotencyService.CommandLease lease = leaseEnvelope.lease();
    String normalizedRequestId = leaseEnvelope.normalizedRequestId();
    String canonicalIdempotencyKey = leaseEnvelope.canonicalIdempotencyKey();
    return executeWithLease(
        lease,
        companyId,
        () -> {
          String traceId = lease.traceId();
          IntegrationCoordinator.AutoApprovalResult result =
              integrationCoordinator.autoApproveOrder(
                  orderId, companyId, traceId, canonicalIdempotencyKey);
          DomainEvent event =
              DomainEvent.of(
                  "OrderAutoApprovedEvent",
                  companyId,
                  "system",
                  "Order",
                  orderId,
                  Map.of(
                      "orderStatus",
                      result.orderStatus(),
                      "awaitingProduction",
                      result.awaitingProduction(),
                      "totalAmount",
                      totalAmount,
                      "traceId",
                      traceId,
                      "idempotencyKey",
                      canonicalIdempotencyKey),
                  traceId,
                  normalizedRequestId,
                  canonicalIdempotencyKey);
          eventPublisherService.enqueue(event);
          traceService.record(
              traceId,
              "ORDER_AUTO_APPROVED",
              companyId,
              Map.of("orderId", orderId, "idempotencyKey", canonicalIdempotencyKey),
              normalizedRequestId,
              canonicalIdempotencyKey);
          return traceId;
        });
  }

  @Transactional
  public String updateOrderFulfillment(
      String orderId,
      OrderFulfillmentRequest request,
      String idempotencyKey,
      String requestId,
      String companyId,
      String userId) {
    requireUserAndCompanyContext(userId, companyId);
    LeaseEnvelope leaseEnvelope =
        startLease(
            "ORCH.ORDER.FULFILLMENT.UPDATE",
            idempotencyKey,
            Map.of("orderId", orderId, "request", request),
            requestId);
    OrchestratorIdempotencyService.CommandLease lease = leaseEnvelope.lease();
    String normalizedRequestId = leaseEnvelope.normalizedRequestId();
    String canonicalIdempotencyKey = leaseEnvelope.canonicalIdempotencyKey();
    return executeWithLease(
        lease,
        companyId,
        () -> {
          String traceId = lease.traceId();
          IntegrationCoordinator.AutoApprovalResult result =
              integrationCoordinator.updateFulfillment(
                  orderId, request.status(), companyId, traceId, canonicalIdempotencyKey);
          Map<String, Object> payload = new HashMap<>();
          payload.put("status", request.status());
          payload.put("awaitingProduction", result.awaitingProduction());
          payload.put("notes", request.notes());
          payload.put("traceId", traceId);
          payload.put("idempotencyKey", canonicalIdempotencyKey);
          DomainEvent event =
              DomainEvent.of(
                  "OrderFulfillmentUpdated",
                  companyId,
                  userId,
                  "Order",
                  orderId,
                  payload,
                  traceId,
                  normalizedRequestId,
                  canonicalIdempotencyKey);
          eventPublisherService.enqueue(event);
          traceService.record(
              traceId,
              "ORDER_FULFILLMENT_UPDATED",
              companyId,
              Map.of(
                  "orderId", orderId,
                  "status", request.status(),
                  "idempotencyKey", canonicalIdempotencyKey),
              normalizedRequestId,
              canonicalIdempotencyKey);
          return traceId;
        });
  }

  public Map<String, Object> integrationHealth() {
    return integrationCoordinator.health();
  }

  public Map<String, Object> eventHealth() {
    return eventPublisherService.healthSnapshot();
  }

  public Map<String, Object> traceSummary(String traceId) {
    String sanitizedTraceId = CorrelationIdentifierSanitizer.sanitizeRequiredTraceId(traceId);
    return Map.of("traceId", sanitizedTraceId, "events", traceService.getTrace(sanitizedTraceId));
  }

  public String generateTraceId() {
    return UUID.randomUUID().toString();
  }

  private LeaseEnvelope startLease(
      String commandName, String idempotencyKey, Object payload, String requestId) {
    String normalizedRequestId = normalizeRequestId(requestId);
    OrchestratorIdempotencyService.CommandLease lease =
        idempotencyService.start(commandName, idempotencyKey, payload, this::generateTraceId);
    String canonicalIdempotencyKey = canonicalIdempotencyKey(lease);
    return new LeaseEnvelope(lease, normalizedRequestId, canonicalIdempotencyKey);
  }

  private void requireUserAndCompanyContext(String userId, String companyId) {
    if (!StringUtils.hasText(userId) || !StringUtils.hasText(companyId)) {
      throw new AccessDeniedException("Missing user or company context");
    }
  }

  private String normalizeRequestId(String requestId) {
    return CorrelationIdentifierSanitizer.sanitizeOptionalRequestId(requestId);
  }

  private String canonicalIdempotencyKey(OrchestratorIdempotencyService.CommandLease lease) {
    if (lease == null || lease.command() == null) {
      throw new IllegalStateException("Orchestrator idempotency lease is missing command");
    }
    return CorrelationIdentifierSanitizer.sanitizeRequiredIdempotencyKey(
        lease.command().getIdempotencyKey());
  }

  private String executeWithLease(
      OrchestratorIdempotencyService.CommandLease lease,
      String companyId,
      CommandExecution execution) {
    if (!lease.shouldExecute()) {
      return lease.traceId();
    }
    realActionUsageService.enforceJobSubmissionAllowed(companyId);
    try {
      String traceId = execution.execute();
      realActionUsageService.recordJobSubmission(companyId);
      idempotencyService.markSuccess(lease.command());
      return traceId;
    } catch (RuntimeException ex) {
      idempotencyService.markFailed(lease.command(), ex);
      throw ex;
    }
  }

  private void ensurePositivePostingAmount(
      OrchestratorCommand command, BigDecimal postingAmount, String operation) {
    if (postingAmount != null && postingAmount.compareTo(BigDecimal.ZERO) > 0) {
      return;
    }
    com.bigbrightpaints.erp.core.exception.ApplicationException ex =
        com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
            "Posting amount must be greater than zero for " + operation);
    idempotencyService.markFailed(command, ex);
    throw ex;
  }

  @FunctionalInterface
  private interface CommandExecution {
    String execute();
  }

  private record LeaseEnvelope(
      OrchestratorIdempotencyService.CommandLease lease,
      String normalizedRequestId,
      String canonicalIdempotencyKey) {}
}
