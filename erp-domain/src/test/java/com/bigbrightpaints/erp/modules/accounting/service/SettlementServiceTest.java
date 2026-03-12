package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.dto.AutoSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerSettlementRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationApplication;
import com.bigbrightpaints.erp.modules.accounting.dto.SettlementAllocationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierPaymentRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierSettlementRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private AccountingIdempotencyService accountingIdempotencyService;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(accountingIdempotencyService);
    }

    @Test
    void recordSupplierPayment_normalizesTrimmedFieldsBeforeDelegating() {
        SettlementAllocationRequest allocation = new SettlementAllocationRequest(
                null,
                77L,
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                SettlementAllocationApplication.DOCUMENT,
                " memo "
        );
        when(accountingIdempotencyService.recordSupplierPayment(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        settlementService.recordSupplierPayment(new SupplierPaymentRequest(
                88L,
                99L,
                new BigDecimal("10.00"),
                "  SUP-REF-1  ",
                "  supplier memo  ",
                "  supplier-key  ",
                List.of(allocation)
        ));

        ArgumentCaptor<SupplierPaymentRequest> captor = ArgumentCaptor.forClass(SupplierPaymentRequest.class);
        verify(accountingIdempotencyService).recordSupplierPayment(captor.capture());
        assertThat(captor.getValue().referenceNumber()).isEqualTo("SUP-REF-1");
        assertThat(captor.getValue().memo()).isEqualTo("supplier memo");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("supplier-key");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void settlementRequests_normalizeAmountFlagsAndUnappliedApplication() {
        when(accountingIdempotencyService.settleDealerInvoices(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        when(accountingIdempotencyService.settleSupplierInvoices(org.mockito.ArgumentMatchers.any())).thenReturn(null);

        settlementService.settleDealerInvoices(new DealerSettlementRequest(
                71L,
                200L,
                null,
                null,
                null,
                null,
                new BigDecimal("25.00"),
                SettlementAllocationApplication.DOCUMENT,
                LocalDate.of(2026, 3, 1),
                "  DEALER-SET-1  ",
                "  dealer memo  ",
                "  dealer-key  ",
                null,
                List.of(),
                List.of()
        ));
        settlementService.settleSupplierInvoices(new SupplierSettlementRequest(
                72L,
                300L,
                null,
                null,
                null,
                null,
                new BigDecimal("15.00"),
                SettlementAllocationApplication.FUTURE_APPLICATION,
                LocalDate.of(2026, 3, 2),
                "  SUP-SET-1  ",
                "  supplier memo  ",
                "  supplier-key  ",
                Boolean.TRUE,
                List.of()
        ));

        ArgumentCaptor<DealerSettlementRequest> dealerCaptor = ArgumentCaptor.forClass(DealerSettlementRequest.class);
        ArgumentCaptor<SupplierSettlementRequest> supplierCaptor = ArgumentCaptor.forClass(SupplierSettlementRequest.class);
        verify(accountingIdempotencyService).settleDealerInvoices(dealerCaptor.capture());
        verify(accountingIdempotencyService).settleSupplierInvoices(supplierCaptor.capture());

        assertThat(dealerCaptor.getValue().amount()).isEqualByComparingTo("25.00");
        assertThat(dealerCaptor.getValue().unappliedAmountApplication()).isNull();
        assertThat(dealerCaptor.getValue().adminOverride()).isFalse();
        assertThat(dealerCaptor.getValue().referenceNumber()).isEqualTo("DEALER-SET-1");
        assertThat(supplierCaptor.getValue().unappliedAmountApplication()).isEqualTo(SettlementAllocationApplication.FUTURE_APPLICATION);
        assertThat(supplierCaptor.getValue().adminOverride()).isTrue();
        assertThat(supplierCaptor.getValue().referenceNumber()).isEqualTo("SUP-SET-1");
    }

    @Test
    void autoSettlement_generatesDeterministicReferencesAndNormalizesExplicitValues() {
        when(accountingIdempotencyService.autoSettleDealer(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);
        when(accountingIdempotencyService.autoSettleSupplier(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(null);

        settlementService.autoSettleDealer(91L, new AutoSettlementRequest(
                200L,
                new BigDecimal("50.00"),
                "   ",
                "  dealer auto  ",
                null
        ));
        settlementService.autoSettleSupplier(92L, new AutoSettlementRequest(
                300L,
                new BigDecimal("60.00"),
                "  SUP-CUSTOM-1  ",
                "  supplier auto  ",
                "  supplier-auto-key  "
        ));

        ArgumentCaptor<AutoSettlementRequest> dealerCaptor = ArgumentCaptor.forClass(AutoSettlementRequest.class);
        ArgumentCaptor<AutoSettlementRequest> supplierCaptor = ArgumentCaptor.forClass(AutoSettlementRequest.class);
        verify(accountingIdempotencyService).autoSettleDealer(org.mockito.ArgumentMatchers.eq(91L), dealerCaptor.capture());
        verify(accountingIdempotencyService).autoSettleSupplier(org.mockito.ArgumentMatchers.eq(92L), supplierCaptor.capture());

        assertThat(dealerCaptor.getValue().referenceNumber()).startsWith("SET-");
        assertThat(dealerCaptor.getValue().idempotencyKey()).isEqualTo(dealerCaptor.getValue().referenceNumber());
        assertThat(dealerCaptor.getValue().memo()).isEqualTo("dealer auto");

        assertThat(supplierCaptor.getValue().referenceNumber()).isEqualTo("SUP-CUSTOM-1");
        assertThat(supplierCaptor.getValue().idempotencyKey()).isEqualTo("supplier-auto-key");
        assertThat(supplierCaptor.getValue().memo()).isEqualTo("supplier auto");
    }
}
