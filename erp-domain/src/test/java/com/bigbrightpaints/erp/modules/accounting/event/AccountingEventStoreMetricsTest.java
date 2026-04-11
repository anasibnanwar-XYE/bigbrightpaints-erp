package com.bigbrightpaints.erp.modules.accounting.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bigbrightpaints.erp.core.audittrail.AuditCorrelationIdResolver;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.test.support.ReflectionFieldAccess;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class AccountingEventStoreMetricsTest {

  @Mock private AccountingEventRepository eventRepository;

  @Mock private ApplicationEventPublisher eventPublisher;

  @Mock private CompanyClock companyClock;

  @Test
  void recordJournalEntryPosted_incrementsBusinessJournalMetrics() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, meterRegistry);

    JournalEntry entry = buildJournalEntry();

    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(1L, 2L, 1L, 1L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordJournalEntryPosted(
        entry, Map.of(101L, new BigDecimal("1000.00"), 202L, new BigDecimal("250.00")));

    assertThat(meterRegistry.get("erp.business.journals.created").counter().count())
        .isEqualTo(1.0d);
    assertThat(
            meterRegistry
                .get("erp.business.journals.created.by_company")
                .tag("company", "ACME")
                .counter()
                .count())
        .isEqualTo(1.0d);

    ArgumentCaptor<AccountingEventStore.JournalEntryPostedEvent> eventCaptor =
        ArgumentCaptor.forClass(AccountingEventStore.JournalEntryPostedEvent.class);
    verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().entryId()).isEqualTo(77L);

    ArgumentCaptor<AccountingEvent> accountingEventCaptor =
        ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository, times(4)).save(accountingEventCaptor.capture());
    assertThat(
            accountingEventCaptor.getAllValues().stream()
                .map(AccountingEvent::getEventType)
                .collect(Collectors.toSet()))
        .contains(
            AccountingEventType.JOURNAL_ENTRY_CREATED,
            AccountingEventType.JOURNAL_ENTRY_POSTED,
            AccountingEventType.ACCOUNT_DEBIT_POSTED,
            AccountingEventType.ACCOUNT_CREDIT_POSTED);
  }

  @Test
  void recordDealerReceiptPosted_persistsDealerReceiptEventType() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry entry = buildJournalEntry();
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(3L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordDealerReceiptPosted(
        entry, 501L, new BigDecimal("150.00"), "dealer-idempotency-key");

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    AccountingEvent persisted = eventCaptor.getValue();
    assertThat(persisted.getEventType()).isEqualTo(AccountingEventType.DEALER_RECEIPT_POSTED);
    assertThat(persisted.getJournalEntryId()).isEqualTo(entry.getId());
    assertThat(persisted.getPayload())
        .contains("\"partnerType\":\"DEALER\"")
        .contains("\"partnerId\":501")
        .contains("\"idempotencyKey\":\"dealer-idempotency-key\"");
  }

  @Test
  void recordSupplierPaymentPosted_persistsSupplierPaymentEventType() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry entry = buildJournalEntry();
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(4L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordSupplierPaymentPosted(
        entry, 902L, new BigDecimal("80.00"), "supplier-idempotency-key");

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    AccountingEvent persisted = eventCaptor.getValue();
    assertThat(persisted.getEventType()).isEqualTo(AccountingEventType.SUPPLIER_PAYMENT_POSTED);
    assertThat(persisted.getJournalEntryId()).isEqualTo(entry.getId());
    assertThat(persisted.getPayload())
        .contains("\"partnerType\":\"SUPPLIER\"")
        .contains("\"partnerId\":902")
        .contains("\"idempotencyKey\":\"supplier-idempotency-key\"");
  }

  @Test
  void recordSettlementAllocated_persistsSettlementAllocatedEventType() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry entry = buildJournalEntry();
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(5L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordSettlementAllocated(
        entry, "DEALER", 711L, new BigDecimal("245.00"), 3, "dealer-settlement-idempotency-key");

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    AccountingEvent persisted = eventCaptor.getValue();
    assertThat(persisted.getEventType()).isEqualTo(AccountingEventType.SETTLEMENT_ALLOCATED);
    assertThat(persisted.getJournalEntryId()).isEqualTo(entry.getId());
    assertThat(persisted.getPayload())
        .contains("\"partnerType\":\"DEALER\"")
        .contains("\"partnerId\":711")
        .contains("\"allocationCount\":3")
        .contains("\"idempotencyKey\":\"dealer-settlement-idempotency-key\"");
  }

  @Test
  void recordJournalEntryPosted_usesFlowCorrelationFallbackFromSourceFields() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry entry = buildJournalEntry();
    entry.setSourceReference("SRC-77");
    entry.setSourceModule("SALES");
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(1L, 2L, 1L, 1L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordJournalEntryPosted(entry, Map.of());

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository, times(4)).save(eventCaptor.capture());
    UUID expectedCorrelation =
        AuditCorrelationIdResolver.resolveCorrelationId(null, "SRC-77", "SALES");
    assertThat(eventCaptor.getAllValues())
        .extracting(AccountingEvent::getCorrelationId)
        .allMatch(expectedCorrelation::equals);
  }

  @Test
  void recordJournalCorrectionApplied_usesOriginalSourceReferenceWhenCorrectionHasNoSourceFields() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry original = buildJournalEntry();
    original.setSourceReference("ORIGINAL-SRC");
    JournalEntry reversal = buildJournalEntry();
    ReflectionFieldAccess.setField(reversal, "id", 88L);
    reversal.setSourceReference(null);
    reversal.setSourceModule(null);
    reversal.setReferenceNumber("REV-88");
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(5L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordJournalEntryReversed(original, reversal, "reversal reason");

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    UUID expectedCorrelation =
        AuditCorrelationIdResolver.resolveCorrelationId(null, "ORIGINAL-SRC");
    assertThat(eventCaptor.getValue().getCorrelationId()).isEqualTo(expectedCorrelation);
  }

  @Test
  void recordSettlementAllocated_keepsCorrelationNullWhenAllFallbackCandidatesAreBlank() {
    AccountingEventStore store =
        new AccountingEventStore(
            eventRepository, eventPublisher, new ObjectMapper(), companyClock, null);
    JournalEntry entry = buildJournalEntry();
    entry.setSourceReference(" ");
    entry.setSourceModule(null);
    when(eventRepository.getNextSequenceNumber(any(UUID.class))).thenReturn(6L);
    when(eventRepository.save(any(AccountingEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    store.recordSettlementAllocated(entry, " ", 701L, new BigDecimal("10.00"), 1, " ");

    ArgumentCaptor<AccountingEvent> eventCaptor = ArgumentCaptor.forClass(AccountingEvent.class);
    verify(eventRepository).save(eventCaptor.capture());
    assertThat(eventCaptor.getValue().getCorrelationId()).isNull();
  }

  private JournalEntry buildJournalEntry() {
    Company company = new Company();
    ReflectionFieldAccess.setField(company, "id", 10L);
    company.setCode("ACME");

    Account debitAccount = new Account();
    ReflectionFieldAccess.setField(debitAccount, "id", 101L);
    debitAccount.setCompany(company);
    debitAccount.setCode("1000");
    debitAccount.setName("Cash");
    debitAccount.setType(AccountType.ASSET);
    debitAccount.setBalance(new BigDecimal("1000.00"));

    Account creditAccount = new Account();
    ReflectionFieldAccess.setField(creditAccount, "id", 202L);
    creditAccount.setCompany(company);
    creditAccount.setCode("2000");
    creditAccount.setName("Revenue");
    creditAccount.setType(AccountType.REVENUE);
    creditAccount.setBalance(new BigDecimal("-250.00"));

    JournalLine debitLine = new JournalLine();
    debitLine.setAccount(debitAccount);
    debitLine.setDebit(new BigDecimal("150.00"));
    debitLine.setCredit(BigDecimal.ZERO);
    debitLine.setDescription("Debit line");

    JournalLine creditLine = new JournalLine();
    creditLine.setAccount(creditAccount);
    creditLine.setDebit(BigDecimal.ZERO);
    creditLine.setCredit(new BigDecimal("150.00"));
    creditLine.setDescription("Credit line");

    JournalEntry entry = new JournalEntry();
    ReflectionFieldAccess.setField(entry, "id", 77L);
    ReflectionFieldAccess.setField(
        entry, "publicId", UUID.fromString("11111111-1111-1111-1111-111111111111"));
    ReflectionFieldAccess.setField(entry, "lines", List.of(debitLine, creditLine));
    entry.setCompany(company);
    entry.setReferenceNumber("JRN-77");
    entry.setEntryDate(LocalDate.of(2026, 3, 3));
    entry.setMemo("Metrics test entry");
    entry.setStatus("POSTED");
    debitLine.setJournalEntry(entry);
    creditLine.setJournalEntry(entry);
    return entry;
  }
}
