package com.bigbrightpaints.erp.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.company.service.TenantRealActionUsageService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryShortage;
import com.bigbrightpaints.erp.orchestrator.config.OrchestratorFeatureFlags;
import com.bigbrightpaints.erp.orchestrator.dto.ApproveOrderRequest;
import com.bigbrightpaints.erp.orchestrator.dto.OrderFulfillmentRequest;
import com.bigbrightpaints.erp.orchestrator.event.DomainEvent;
import com.bigbrightpaints.erp.orchestrator.repository.OrchestratorCommand;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

  @Mock private IntegrationCoordinator integrationCoordinator;
  @Mock private EventPublisherService eventPublisherService;
  @Mock private TraceService traceService;
  @Mock private OrchestratorIdempotencyService idempotencyService;
  @Mock private TenantRealActionUsageService realActionUsageService;

  private CommandDispatcher commandDispatcher;
  private OrchestratorFeatureFlags featureFlags;

  @BeforeEach
  void setUp() {
    featureFlags = new OrchestratorFeatureFlags(true, true);
    commandDispatcher =
        new CommandDispatcher(
            integrationCoordinator,
            eventPublisherService,
            traceService,
            idempotencyService,
            featureFlags,
            realActionUsageService);
  }

  @Test
  void approveOrderQueuesProductionAndPublishesAwaitingProductionEvent() {
    OrchestratorCommand command =
        new OrchestratorCommand(1L, "ORCH.ORDER.APPROVE", "idem-1", "hash", "trace-123");
    ApproveOrderRequest request =
        new ApproveOrderRequest("101", "approver@bbp.com", new BigDecimal("5000"));
    InventoryShortage shortage = new InventoryShortage("SKU-1", BigDecimal.ONE, "Red Paint");
    InventoryReservationResult reservation =
        new InventoryReservationResult(null, List.of(shortage));
    when(integrationCoordinator.reserveInventory("101", "COMP", "trace-123", "idem-1"))
        .thenReturn(reservation);
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.APPROVE"),
            ArgumentMatchers.eq("idem-1"),
            ArgumentMatchers.eq(request),
            ArgumentMatchers.any()))
        .thenReturn(new OrchestratorIdempotencyService.CommandLease("trace-123", command, true));

    String traceId = commandDispatcher.approveOrder(request, "idem-1", "req-1", "COMP", "user-1");

    assertThat(traceId).isEqualTo("trace-123");
    verify(integrationCoordinator).reserveInventory("101", "COMP", "trace-123", "idem-1");

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(eventPublisherService).enqueue(eventCaptor.capture());
    DomainEvent published = eventCaptor.getValue();
    assertThat(published.eventType()).isEqualTo("OrderApprovedEvent");
    assertThat(published.companyId()).isEqualTo("COMP");
    assertThat(published.userId()).isEqualTo("user-1");
    assertThat(published.traceId()).isEqualTo("trace-123");
    assertThat(published.requestId()).isEqualTo("req-1");
    assertThat(published.idempotencyKey()).isEqualTo("idem-1");
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) published.payload();
    assertThat(payload)
        .containsEntry("awaitingProduction", true)
        .containsEntry("orderStatus", "PENDING_PRODUCTION")
        .containsEntry("approvedBy", "approver@bbp.com")
        .containsEntry("totalAmount", new BigDecimal("5000"))
        .containsEntry("traceId", "trace-123");

    verify(traceService)
        .record(
            ArgumentMatchers.eq("trace-123"),
            ArgumentMatchers.eq("ORDER_APPROVED"),
            ArgumentMatchers.eq("COMP"),
            ArgumentMatchers.<Map<String, Object>>argThat(
                map ->
                    "101".equals(map.get("orderId")) && "idem-1".equals(map.get("idempotencyKey"))),
            ArgumentMatchers.eq("req-1"),
            ArgumentMatchers.eq("idem-1"));

    verify(idempotencyService).markSuccess(command);
  }

  @Test
  void approveOrderRequiresUserAndCompanyContextBeforeStartingLease() {
    ApproveOrderRequest request =
        new ApproveOrderRequest("101", "approver@bbp.com", new BigDecimal("5000"));

    assertThatThrownBy(
            () -> commandDispatcher.approveOrder(request, "idem-ctx", "req-ctx", "COMP", " "))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("Missing user or company context");

    verifyNoInteractions(
        idempotencyService, integrationCoordinator, eventPublisherService, traceService);
  }

  @Test
  void approveOrderMalformedOrderIdFailsBeforeOutboxAndTrace() {
    OrchestratorCommand command =
        new OrchestratorCommand(
            1L, "ORCH.ORDER.APPROVE", "idem-invalid-order", "hash", "trace-invalid-order");
    ApproveOrderRequest request =
        new ApproveOrderRequest("abc", "approver@bbp.com", new BigDecimal("5000"));
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.APPROVE"),
            ArgumentMatchers.eq("idem-invalid-order"),
            ArgumentMatchers.eq(request),
            ArgumentMatchers.any()))
        .thenReturn(
            new OrchestratorIdempotencyService.CommandLease("trace-invalid-order", command, true));
    when(integrationCoordinator.reserveInventory(
            "abc", "COMP", "trace-invalid-order", "idem-invalid-order"))
        .thenThrow(
            new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Invalid orderId format"));

    assertThatThrownBy(
            () ->
                commandDispatcher.approveOrder(
                    request, "idem-invalid-order", "req-invalid-order", "COMP", "user-1"))
        .isInstanceOf(ApplicationException.class);

    verify(eventPublisherService, never()).enqueue(ArgumentMatchers.any());
    verify(traceService, never())
        .record(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any());
    verify(idempotencyService)
        .markFailed(
            ArgumentMatchers.eq(command),
            ArgumentMatchers.argThat(
                ex ->
                    ex instanceof ApplicationException
                        && ((ApplicationException) ex).getErrorCode()
                            == ErrorCode.VALIDATION_INVALID_INPUT));
    verify(idempotencyService, never()).markSuccess(ArgumentMatchers.any());
  }

  @Test
  void updateOrderFulfillmentPropagatesTraceAndIdempotencyToCoordinator() {
    OrchestratorCommand command =
        new OrchestratorCommand(
            1L, "ORCH.ORDER.FULFILLMENT.UPDATE", "idem-fulfillment", "hash", "trace-fulfillment");
    OrderFulfillmentRequest request = new OrderFulfillmentRequest("PROCESSING", "start");
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.FULFILLMENT.UPDATE"),
            ArgumentMatchers.eq("idem-fulfillment"),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenReturn(
            new OrchestratorIdempotencyService.CommandLease("trace-fulfillment", command, true));
    when(integrationCoordinator.updateFulfillment(
            "101", "PROCESSING", "COMP", "trace-fulfillment", "idem-fulfillment"))
        .thenReturn(new IntegrationCoordinator.AutoApprovalResult("PROCESSING", false));

    String traceId =
        commandDispatcher.updateOrderFulfillment(
            "101", request, "idem-fulfillment", "req-fulfillment", "COMP", "user-1");

    assertThat(traceId).isEqualTo("trace-fulfillment");
    verify(integrationCoordinator)
        .updateFulfillment("101", "PROCESSING", "COMP", "trace-fulfillment", "idem-fulfillment");
    verify(idempotencyService).markSuccess(command);
  }

  @Test
  void jobQuotaCountsOnlyExecutedCommandAndNotIdempotentReplay() {
    CommandDispatcher quotaAwareDispatcher =
        new CommandDispatcher(
            integrationCoordinator,
            eventPublisherService,
            traceService,
            idempotencyService,
            featureFlags,
            realActionUsageService);
    OrchestratorCommand command =
        new OrchestratorCommand(
            1L, "ORCH.ORDER.FULFILLMENT.UPDATE", "idem-job", "hash", "trace-job");
    OrderFulfillmentRequest request = new OrderFulfillmentRequest("PROCESSING", "start");
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.FULFILLMENT.UPDATE"),
            ArgumentMatchers.eq("idem-job"),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenReturn(
            new OrchestratorIdempotencyService.CommandLease("trace-job", command, true),
            new OrchestratorIdempotencyService.CommandLease("trace-job", command, false));
    when(integrationCoordinator.updateFulfillment(
            "101", "PROCESSING", "COMP", "trace-job", "idem-job"))
        .thenReturn(new IntegrationCoordinator.AutoApprovalResult("PROCESSING", false));

    assertThat(
            quotaAwareDispatcher.updateOrderFulfillment(
                "101", request, "idem-job", "req-job", "COMP", "user-1"))
        .isEqualTo("trace-job");
    assertThat(
            quotaAwareDispatcher.updateOrderFulfillment(
                "101", request, "idem-job", "req-job", "COMP", "user-1"))
        .isEqualTo("trace-job");

    verify(realActionUsageService).enforceJobSubmissionAllowed("COMP");
    verify(realActionUsageService).recordJobSubmission("COMP");
  }

  @Test
  void updateOrderFulfillmentMalformedOrderIdFailsBeforeOutboxAndTrace() {
    OrchestratorCommand command =
        new OrchestratorCommand(
            1L,
            "ORCH.ORDER.FULFILLMENT.UPDATE",
            "idem-invalid-fulfillment",
            "hash",
            "trace-invalid-fulfillment");
    OrderFulfillmentRequest request = new OrderFulfillmentRequest("PROCESSING", "start");
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.FULFILLMENT.UPDATE"),
            ArgumentMatchers.eq("idem-invalid-fulfillment"),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenReturn(
            new OrchestratorIdempotencyService.CommandLease(
                "trace-invalid-fulfillment", command, true));
    when(integrationCoordinator.updateFulfillment(
            "abc", "PROCESSING", "COMP", "trace-invalid-fulfillment", "idem-invalid-fulfillment"))
        .thenThrow(
            new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Invalid orderId format"));

    assertThatThrownBy(
            () ->
                commandDispatcher.updateOrderFulfillment(
                    "abc",
                    request,
                    "idem-invalid-fulfillment",
                    "req-invalid-fulfillment",
                    "COMP",
                    "user-1"))
        .isInstanceOf(ApplicationException.class);

    verify(eventPublisherService, never()).enqueue(ArgumentMatchers.any());
    verify(traceService, never())
        .record(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyMap(),
            ArgumentMatchers.any(),
            ArgumentMatchers.any());
    verify(idempotencyService)
        .markFailed(
            ArgumentMatchers.eq(command),
            ArgumentMatchers.argThat(
                ex ->
                    ex instanceof ApplicationException
                        && ((ApplicationException) ex).getErrorCode()
                            == ErrorCode.VALIDATION_INVALID_INPUT));
    verify(idempotencyService, never()).markSuccess(ArgumentMatchers.any());
  }

  @Test
  void retiredDispatchShortcutIsRemovedFromCommandDispatcher() {
    assertThat(java.util.Arrays.stream(CommandDispatcher.class.getDeclaredMethods()))
        .extracting(java.lang.reflect.Method::getName)
        .doesNotContain("dispatchBatch");

    verifyNoInteractions(
        integrationCoordinator, eventPublisherService, traceService, idempotencyService);
  }

  @Test
  void autoApproveOrderUsesIdempotencyAndRecordsIdentifiers() {
    OrchestratorCommand command =
        new OrchestratorCommand(1L, "ORCH.ORDER.AUTO_APPROVE", "auto-1", "hash", "trace-999");
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.AUTO_APPROVE"),
            ArgumentMatchers.eq("auto-1"),
            ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenReturn(new OrchestratorIdempotencyService.CommandLease("trace-999", command, true));
    IntegrationCoordinator.AutoApprovalResult result =
        new IntegrationCoordinator.AutoApprovalResult("READY_TO_SHIP", false);
    when(integrationCoordinator.autoApproveOrder("101", "COMP", "trace-999", "auto-1"))
        .thenReturn(result);

    String traceId =
        commandDispatcher.autoApproveOrder(
            "101", new BigDecimal("5000"), "COMP", "auto-1", "req-9");

    assertThat(traceId).isEqualTo("trace-999");
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(eventPublisherService).enqueue(eventCaptor.capture());
    DomainEvent published = eventCaptor.getValue();
    assertThat(published.eventType()).isEqualTo("OrderAutoApprovedEvent");
    assertThat(published.traceId()).isEqualTo("trace-999");
    assertThat(published.requestId()).isEqualTo("req-9");
    assertThat(published.idempotencyKey()).isEqualTo("auto-1");
    verify(traceService)
        .record(
            ArgumentMatchers.eq("trace-999"),
            ArgumentMatchers.eq("ORDER_AUTO_APPROVED"),
            ArgumentMatchers.eq("COMP"),
            ArgumentMatchers.<Map<String, Object>>argThat(
                map ->
                    "101".equals(map.get("orderId")) && "auto-1".equals(map.get("idempotencyKey"))),
            ArgumentMatchers.eq("req-9"),
            ArgumentMatchers.eq("auto-1"));
    verify(idempotencyService).markSuccess(command);
  }

  @Test
  void approveOrderUsesCanonicalLeaseIdempotencyKeyForEventAndTrace() {
    OrchestratorCommand command =
        new OrchestratorCommand(
            1L, "ORCH.ORDER.APPROVE", "idem-canonical", "hash", "trace-canonical");
    ApproveOrderRequest request =
        new ApproveOrderRequest("201", "approver@bbp.com", new BigDecimal("1200"));
    when(integrationCoordinator.reserveInventory(
            "201", "COMP", "trace-canonical", "idem-canonical"))
        .thenReturn(new InventoryReservationResult(null, List.of()));
    when(idempotencyService.start(
            ArgumentMatchers.eq("ORCH.ORDER.APPROVE"),
            ArgumentMatchers.eq("  idem-canonical  "),
            ArgumentMatchers.eq(request),
            ArgumentMatchers.any()))
        .thenReturn(
            new OrchestratorIdempotencyService.CommandLease("trace-canonical", command, true));

    String traceId =
        commandDispatcher.approveOrder(
            request, "  idem-canonical  ", "req-canonical", "COMP", "user-1");

    assertThat(traceId).isEqualTo("trace-canonical");

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(eventPublisherService).enqueue(eventCaptor.capture());
    DomainEvent published = eventCaptor.getValue();
    assertThat(published.idempotencyKey()).isEqualTo("idem-canonical");
    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) published.payload();
    assertThat(payload).containsEntry("idempotencyKey", "idem-canonical");

    verify(traceService)
        .record(
            ArgumentMatchers.eq("trace-canonical"),
            ArgumentMatchers.eq("ORDER_APPROVED"),
            ArgumentMatchers.eq("COMP"),
            ArgumentMatchers.<Map<String, Object>>argThat(
                map ->
                    "201".equals(map.get("orderId"))
                        && "idem-canonical".equals(map.get("idempotencyKey"))),
            ArgumentMatchers.eq("req-canonical"),
            ArgumentMatchers.eq("idem-canonical"));
    verify(idempotencyService).markSuccess(command);
  }
}
