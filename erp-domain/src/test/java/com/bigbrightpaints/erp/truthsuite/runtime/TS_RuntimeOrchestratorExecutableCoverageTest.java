package com.bigbrightpaints.erp.truthsuite.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.orchestrator.config.OrchestratorFeatureFlags;
import com.bigbrightpaints.erp.orchestrator.dto.ApproveOrderRequest;
import com.bigbrightpaints.erp.orchestrator.dto.DispatchRequest;
import com.bigbrightpaints.erp.orchestrator.dto.PayrollRunRequest;
import com.bigbrightpaints.erp.orchestrator.exception.OrchestratorFeatureDisabledException;
import com.bigbrightpaints.erp.orchestrator.policy.PolicyEnforcer;
import com.bigbrightpaints.erp.orchestrator.repository.OrchestratorCommand;
import com.bigbrightpaints.erp.orchestrator.service.CommandDispatcher;
import com.bigbrightpaints.erp.orchestrator.service.EventPublisherService;
import com.bigbrightpaints.erp.orchestrator.service.IntegrationCoordinator;
import com.bigbrightpaints.erp.orchestrator.service.OrchestratorIdempotencyService;
import com.bigbrightpaints.erp.orchestrator.service.TraceService;
import com.bigbrightpaints.erp.orchestrator.workflow.WorkflowService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

@Tag("concurrency")
@Tag("reconciliation")
@Tag("critical")
class TS_RuntimeOrchestratorExecutableCoverageTest {

    @Test
    void policyEnforcer_rejects_missing_user_or_company_context() {
        PolicyEnforcer enforcer = new PolicyEnforcer();

        assertThatThrownBy(() -> enforcer.checkOrderApprovalPermissions(null, "C1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkOrderApprovalPermissions("u1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkDispatchPermissions("u1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkDispatchPermissions(null, "C1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkPayrollPermissions(null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkPayrollPermissions("u1", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");
        assertThatThrownBy(() -> enforcer.checkPayrollPermissions(null, "C1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Missing user or company context");

        enforcer.checkOrderApprovalPermissions("u1", "C1");
        enforcer.checkDispatchPermissions("u1", "C1");
        enforcer.checkPayrollPermissions("u1", "C1");
    }

    @Test
    void commandDispatcher_covers_success_replay_and_feature_gated_paths() {
        WorkflowService workflowService = mock(WorkflowService.class);
        IntegrationCoordinator integrationCoordinator = mock(IntegrationCoordinator.class);
        EventPublisherService eventPublisherService = mock(EventPublisherService.class);
        TraceService traceService = mock(TraceService.class);
        PolicyEnforcer policyEnforcer = new PolicyEnforcer();
        OrchestratorIdempotencyService idempotencyService = mock(OrchestratorIdempotencyService.class);
        OrchestratorFeatureFlags featureFlags = mock(OrchestratorFeatureFlags.class);

        CommandDispatcher dispatcher = new CommandDispatcher(
                workflowService,
                integrationCoordinator,
                eventPublisherService,
                traceService,
                policyEnforcer,
                idempotencyService,
                featureFlags
        );

        OrchestratorCommand orderCommand = new OrchestratorCommand(1L, "ORCH.ORDER.APPROVE", "idem-order", "hash", "trace-order");
        OrchestratorIdempotencyService.CommandLease orderLease =
                new OrchestratorIdempotencyService.CommandLease("trace-order", orderCommand, true);

        when(idempotencyService.start(eq("ORCH.ORDER.APPROVE"), eq("idem-order"), any(), any()))
                .thenReturn(orderLease);
        when(integrationCoordinator.reserveInventory("42", "C1"))
                .thenReturn(new InventoryReservationResult(null, List.of()));

        String trace = dispatcher.approveOrder(
                new ApproveOrderRequest("42", "ops@bbp.com", new BigDecimal("100.00")),
                "idem-order",
                "req-order",
                "C1",
                "ops@bbp.com"
        );

        assertThat(trace).isEqualTo("trace-order");
        verify(eventPublisherService, atLeastOnce()).enqueue(any());
        verify(traceService, atLeastOnce()).record(eq("trace-order"), any(), eq("C1"), any(), eq("req-order"), eq("idem-order"));
        verify(idempotencyService).markSuccess(orderCommand);

        OrchestratorCommand replayCommand = new OrchestratorCommand(1L, "ORCH.ORDER.APPROVE", "idem-order-replay", "hash", "trace-replay");
        OrchestratorIdempotencyService.CommandLease replayLease =
                new OrchestratorIdempotencyService.CommandLease("trace-replay", replayCommand, false);
        when(idempotencyService.start(eq("ORCH.ORDER.APPROVE"), eq("idem-order-replay"), any(), any()))
                .thenReturn(replayLease);

        String replayTrace = dispatcher.approveOrder(
                new ApproveOrderRequest("42", "ops@bbp.com", new BigDecimal("100.00")),
                "idem-order-replay",
                null,
                "C1",
                "ops@bbp.com"
        );
        assertThat(replayTrace).isEqualTo("trace-replay");

        OrchestratorCommand dispatchCommand = new OrchestratorCommand(1L, "ORCH.FACTORY.BATCH.DISPATCH", "idem-dispatch", "hash", "trace-dispatch");
        OrchestratorIdempotencyService.CommandLease dispatchLease =
                new OrchestratorIdempotencyService.CommandLease("trace-dispatch", dispatchCommand, true);
        when(idempotencyService.start(eq("ORCH.FACTORY.BATCH.DISPATCH"), eq("idem-dispatch"), any(), any()))
                .thenReturn(dispatchLease);
        when(featureFlags.isFactoryDispatchEnabled()).thenReturn(false);

        assertThatThrownBy(() -> dispatcher.dispatchBatch(
                new DispatchRequest("BATCH-1", "ops@bbp.com", new BigDecimal("50.00")),
                "idem-dispatch",
                "req-dispatch",
                "C1",
                "ops@bbp.com"
        )).isInstanceOf(OrchestratorFeatureDisabledException.class)
                .hasMessageContaining("disabled (CODE-RED)");

        verify(idempotencyService).markFailed(eq(dispatchCommand), any(RuntimeException.class));

        OrchestratorCommand payrollCommand = new OrchestratorCommand(1L, "ORCH.PAYROLL.RUN", "idem-payroll", "hash", "trace-payroll");
        OrchestratorIdempotencyService.CommandLease payrollLease =
                new OrchestratorIdempotencyService.CommandLease("trace-payroll", payrollCommand, true);
        when(idempotencyService.start(eq("ORCH.PAYROLL.RUN"), eq("idem-payroll"), any(), any()))
                .thenReturn(payrollLease);
        when(featureFlags.isPayrollEnabled()).thenReturn(false);

        assertThatThrownBy(() -> dispatcher.runPayroll(
                new PayrollRunRequest(LocalDate.of(2026, 2, 1), "ops@bbp.com", 11L, 12L, new BigDecimal("500.00")),
                "idem-payroll",
                "req-payroll",
                "C1",
                "ops@bbp.com"
        )).isInstanceOf(OrchestratorFeatureDisabledException.class)
                .hasMessageContaining("disabled (CODE-RED)");

        verify(idempotencyService).markFailed(eq(payrollCommand), any(RuntimeException.class));
        assertThat(dispatcher.generateTraceId()).isNotBlank();
    }
}
