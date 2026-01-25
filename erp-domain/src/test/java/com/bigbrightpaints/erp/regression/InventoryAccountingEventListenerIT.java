package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.event.InventoryAccountingEventListener;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryMovementEvent;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryValuationChangedEvent;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "erp.inventory.accounting.events.enabled=true")
class InventoryAccountingEventListenerIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "INV-EVT-01";
    private static final String REFERENCE = "INV-MOVE-TEST-1";

    @Autowired
    private InventoryAccountingEventListener listener;

    @Autowired
    private AccountingService accountingService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    private Company company;
    private Account inventoryAccount;
    private Account cogsAccount;

    @BeforeEach
    void setUp() {
        company = dataSeeder.ensureCompany(COMPANY_CODE, "Inventory Events Co");
        CompanyContextHolder.setCompanyId(company.getCode());
        inventoryAccount = ensureAccount("INV-TEST", "Inventory", AccountType.ASSET);
        cogsAccount = ensureAccount("COGS-TEST", "COGS", AccountType.EXPENSE);
    }

    @AfterEach
    void tearDown() {
        CompanyContextHolder.clear();
    }

    @Test
    void movementEventSkipsWhenJournalAlreadyExists() {
        long before = journalEntryRepository.count();
        accountingService.createJournalEntry(new JournalEntryRequest(
                REFERENCE,
                LocalDate.of(2026, 1, 10),
                "Manual movement journal",
                null,
                null,
                Boolean.FALSE,
                List.of(
                        new JournalEntryRequest.JournalLineRequest(
                                inventoryAccount.getId(),
                                "Inventory",
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO
                        ),
                        new JournalEntryRequest.JournalLineRequest(
                                cogsAccount.getId(),
                                "COGS",
                                BigDecimal.ZERO,
                                new BigDecimal("10.00")
                        )
                )
        ));
        long afterCreate = journalEntryRepository.count();
        assertThat(afterCreate).isEqualTo(before + 1);

        InventoryMovementEvent event = InventoryMovementEvent.builder()
                .companyId(company.getId())
                .movementType(InventoryMovementEvent.MovementType.ISSUE)
                .inventoryType(InventoryValuationChangedEvent.InventoryType.FINISHED_GOOD)
                .itemId(1L)
                .itemCode("FG-TEST")
                .itemName("Finished Good Test")
                .quantity(new BigDecimal("2"))
                .unitCost(new BigDecimal("5.00"))
                .totalCost(new BigDecimal("10.00"))
                .sourceAccountId(inventoryAccount.getId())
                .destinationAccountId(cogsAccount.getId())
                .referenceNumber(REFERENCE)
                .movementDate(LocalDate.of(2026, 1, 10))
                .memo("Test movement event")
                .relatedEntityId(99L)
                .relatedEntityType("TEST")
                .build();

        listener.onInventoryMovement(event);

        long afterEvent = journalEntryRepository.count();
        assertThat(afterEvent).isEqualTo(afterCreate);
    }

    private Account ensureAccount(String code, String name, AccountType type) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setCompany(company);
                    account.setCode(code);
                    account.setName(name);
                    account.setType(type);
                    return accountRepository.save(account);
                });
    }
}
