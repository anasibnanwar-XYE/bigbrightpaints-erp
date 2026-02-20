package com.bigbrightpaints.erp.truthsuite.o2c;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.modules.accounting.service.DealerLedgerService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.sales.domain.CreditLimitOverrideRequest;
import com.bigbrightpaints.erp.modules.sales.domain.CreditLimitOverrideRequestRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.dto.CreditLimitOverrideDecisionRequest;
import com.bigbrightpaints.erp.modules.sales.dto.CreditLimitOverrideRequestDto;
import com.bigbrightpaints.erp.modules.sales.service.CreditLimitOverrideService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@Tag("critical")
@ExtendWith(MockitoExtension.class)
class TS_truthsuite_o2c_Override_RuntimeTest {

    @Mock
    private CompanyContextService companyContextService;
    @Mock
    private CreditLimitOverrideRequestRepository creditLimitOverrideRequestRepository;
    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private PackagingSlipRepository packagingSlipRepository;
    @Mock
    private DealerLedgerService dealerLedgerService;
    @Mock
    private AuditService auditService;

    private CreditLimitOverrideService service;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new CreditLimitOverrideService(
                companyContextService,
                creditLimitOverrideRequestRepository,
                dealerRepository,
                salesOrderRepository,
                packagingSlipRepository,
                dealerLedgerService,
                auditService);
        company = new Company();
        company.setTimezone("UTC");
        lenient().when(companyContextService.requireCurrentCompany()).thenReturn(company);
    }

    @Test
    void legacyApprovedRecordsRemainValidWhenMakerCheckerMetadataIsComplete() {
        CreditLimitOverrideRequest request = new CreditLimitOverrideRequest();
        request.setCompany(company);
        request.setStatus("APPROVED");
        request.setDispatchAmount(new BigDecimal("120.00"));
        request.setRequiredHeadroom(new BigDecimal("80.00"));
        request.setRequestedBy("maker@bbp.com");
        request.setReviewedBy("checker@bbp.com");
        request.setReviewedAt(Instant.parse("2026-02-20T00:00:00Z"));
        request.setReason("Approved by finance lead");

        Dealer dealer = new Dealer();
        dealer.setCreditLimit(new BigDecimal("200.00"));
        ReflectionTestUtils.setField(dealer, "id", 42L);

        when(creditLimitOverrideRequestRepository.findByCompanyAndId(company, 1001L))
                .thenReturn(Optional.of(request));
        when(dealerLedgerService.currentBalance(42L)).thenReturn(new BigDecimal("100.00"));

        boolean approved = service.isOverrideApproved(
                1001L,
                company,
                dealer,
                null,
                null,
                new BigDecimal("120.00"));

        assertThat(approved).isTrue();
    }

    @Test
    void approvedRecordsFailClosedWhenMakerCheckerMetadataIsInvalid() {
        CreditLimitOverrideRequest request = new CreditLimitOverrideRequest();
        request.setCompany(company);
        request.setStatus("APPROVED");
        request.setDispatchAmount(new BigDecimal("100.00"));
        request.setRequiredHeadroom(new BigDecimal("40.00"));
        request.setRequestedBy("maker@bbp.com");
        request.setReviewedBy("maker@bbp.com");
        request.setReviewedAt(Instant.parse("2026-02-20T00:00:00Z"));
        request.setReason("[CREDIT_LIMIT_EXCEPTION_APPROVED] reviewed");

        Dealer dealer = new Dealer();
        dealer.setCreditLimit(new BigDecimal("200.00"));
        ReflectionTestUtils.setField(dealer, "id", 42L);

        when(creditLimitOverrideRequestRepository.findByCompanyAndId(company, 1002L))
                .thenReturn(Optional.of(request));

        boolean approved = service.isOverrideApproved(
                1002L,
                company,
                dealer,
                null,
                null,
                new BigDecimal("100.00"));

        assertThat(approved).isFalse();
    }

    @Test
    void approveUsesLegacyDecisionFallbackWhenReasonMissing() {
        CreditLimitOverrideRequest request = new CreditLimitOverrideRequest();
        request.setCompany(company);
        request.setStatus("PENDING");
        request.setRequestedBy("maker@bbp.com");
        request.setReason("[CREDIT_LIMIT_EXCEPTION_REQUESTED] Need urgent dispatch headroom");

        when(creditLimitOverrideRequestRepository.findByCompanyAndId(company, 1003L))
                .thenReturn(Optional.of(request));

        CreditLimitOverrideRequestDto response = service.approveRequest(
                1003L,
                new CreditLimitOverrideDecisionRequest(null, null),
                "checker@bbp.com");

        assertThat(response.status()).isEqualTo("APPROVED");
        assertThat(request.getReason()).contains("[CREDIT_LIMIT_EXCEPTION_APPROVED]");
        assertThat(request.getReason()).contains("Need urgent dispatch headroom");
    }

    @Test
    void rejectUsesDefaultDecisionFallbackWhenLegacyPayloadOmitsReason() {
        CreditLimitOverrideRequest request = new CreditLimitOverrideRequest();
        request.setCompany(company);
        request.setStatus("PENDING");
        request.setRequestedBy("maker@bbp.com");

        when(creditLimitOverrideRequestRepository.findByCompanyAndId(company, 1004L))
                .thenReturn(Optional.of(request));

        CreditLimitOverrideRequestDto response = service.rejectRequest(
                1004L,
                new CreditLimitOverrideDecisionRequest(null, null),
                "checker@bbp.com");

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(request.getReason()).contains("[CREDIT_LIMIT_EXCEPTION_REJECTED]");
        assertThat(request.getReason()).contains("Rejected via legacy decision payload");
    }
}
