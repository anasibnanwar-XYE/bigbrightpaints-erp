package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.*;
import com.bigbrightpaints.erp.modules.accounting.dto.*;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRun;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class AccountingService {

    private final CompanyContextService companyContextService;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final DealerRepository dealerRepository;
    private final DealerLedgerService dealerLedgerService;
    private final PayrollRunRepository payrollRunRepository;

    public AccountingService(CompanyContextService companyContextService,
                             AccountRepository accountRepository,
                             JournalEntryRepository journalEntryRepository,
                             DealerRepository dealerRepository,
                             DealerLedgerService dealerLedgerService,
                             PayrollRunRepository payrollRunRepository) {
        this.companyContextService = companyContextService;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.dealerRepository = dealerRepository;
        this.dealerLedgerService = dealerLedgerService;
        this.payrollRunRepository = payrollRunRepository;
    }

    /* Accounts */
    public List<AccountDto> listAccounts() {
        Company company = companyContextService.requireCurrentCompany();
        return accountRepository.findByCompanyOrderByCodeAsc(company).stream().map(this::toDto).toList();
    }

    @Transactional
    public AccountDto createAccount(AccountRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Account account = new Account();
        account.setCompany(company);
        account.setCode(request.code());
        account.setName(request.name());
        account.setType(request.type());
        return toDto(accountRepository.save(account));
    }

    /* Journal Entries */
    public List<JournalEntryDto> listJournalEntries(Long dealerId) {
        Company company = companyContextService.requireCurrentCompany();
        List<JournalEntry> entries;
        if (dealerId != null) {
            Dealer dealer = requireDealer(company, dealerId);
            entries = journalEntryRepository.findByCompanyAndDealerOrderByEntryDateDesc(company, dealer);
        } else {
            entries = journalEntryRepository.findByCompanyOrderByEntryDateDesc(company);
        }
        return entries.stream().map(this::toDto).toList();
    }

    @Transactional
    public JournalEntryDto createJournalEntry(JournalEntryRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        BigDecimal totalDebit = request.lines().stream()
                .map(JournalEntryRequest.JournalLineRequest::debit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredit = request.lines().stream()
                .map(JournalEntryRequest.JournalLineRequest::credit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalDebit.subtract(totalCredit).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("Journal entry must balance");
        }
        JournalEntry entry = new JournalEntry();
        entry.setCompany(company);
        entry.setReferenceNumber(request.referenceNumber());
        entry.setEntryDate(request.entryDate());
        entry.setMemo(request.memo());
        entry.setStatus("POSTED");
        if (request.dealerId() != null) {
            entry.setDealer(requireDealer(company, request.dealerId()));
        }
        for (JournalEntryRequest.JournalLineRequest lineRequest : request.lines()) {
            Account account = accountRepository.findByCompanyAndId(company, lineRequest.accountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            JournalLine line = new JournalLine();
            line.setJournalEntry(entry);
            line.setAccount(account);
            line.setDescription(lineRequest.description());
            line.setDebit(lineRequest.debit());
            line.setCredit(lineRequest.credit());
            entry.getLines().add(line);
        }
        JournalEntry saved = journalEntryRepository.save(entry);
        if (saved.getDealer() != null) {
            BigDecimal delta = totalDebit.subtract(totalCredit);
            BigDecimal dealerDebit = delta.compareTo(BigDecimal.ZERO) > 0 ? delta : BigDecimal.ZERO;
            BigDecimal dealerCredit = delta.compareTo(BigDecimal.ZERO) < 0 ? delta.abs() : BigDecimal.ZERO;
            dealerLedgerService.recordLedgerEntry(
                    saved.getDealer(),
                    new DealerLedgerService.LocalLedgerContext(
                            saved.getEntryDate(),
                            saved.getReferenceNumber(),
                            saved.getMemo(),
                            dealerDebit,
                            dealerCredit,
                            saved));
        }
        return toDto(saved);
    }

    @Transactional
    public JournalEntryDto recordDealerReceipt(DealerReceiptRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Dealer dealer = requireDealer(company, request.dealerId());
        Account receivableAccount = dealer.getReceivableAccount();
        if (receivableAccount == null) {
            throw new IllegalStateException("Dealer " + dealer.getName() + " is missing a receivable account");
        }
        Account cashAccount = requireAccount(company, request.cashAccountId());
        BigDecimal amount = requirePositive(request.amount(), "amount");
        String memo = StringUtils.hasText(request.memo())
                ? request.memo().trim()
                : "Receipt for dealer " + dealer.getName();
        String reference = StringUtils.hasText(request.referenceNumber())
                ? request.referenceNumber().trim()
                : generateReference("RCPT-" + dealer.getCode());
        JournalEntryRequest payload = new JournalEntryRequest(
                reference,
                currentDate(company),
                memo,
                dealer.getId(),
                List.of(
                        new JournalEntryRequest.JournalLineRequest(cashAccount.getId(), memo, amount, BigDecimal.ZERO),
                        new JournalEntryRequest.JournalLineRequest(receivableAccount.getId(), memo, BigDecimal.ZERO, amount)
                )
        );
        return createJournalEntry(payload);
    }

    @Transactional
    public JournalEntryDto recordPayrollPayment(PayrollPaymentRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        PayrollRun run = payrollRunRepository.findByCompanyAndId(company, request.payrollRunId())
                .orElseThrow(() -> new IllegalArgumentException("Payroll run not found"));
        Account cashAccount = requireAccount(company, request.cashAccountId());
        Account expenseAccount = requireAccount(company, request.expenseAccountId());
        BigDecimal amount = requirePositive(request.amount(), "amount");
        String memo = StringUtils.hasText(request.memo())
                ? request.memo().trim()
                : "Payroll payment for " + run.getRunDate();
        String reference = StringUtils.hasText(request.referenceNumber())
                ? request.referenceNumber().trim()
                : "PAYROLL-" + run.getRunDate();
        JournalEntryRequest payload = new JournalEntryRequest(
                reference,
                currentDate(company),
                memo,
                null,
                List.of(
                        new JournalEntryRequest.JournalLineRequest(expenseAccount.getId(), memo, amount, BigDecimal.ZERO),
                        new JournalEntryRequest.JournalLineRequest(cashAccount.getId(), memo, BigDecimal.ZERO, amount)
                )
        );
        JournalEntryDto entry = createJournalEntry(payload);
        run.setStatus("PAID");
        payrollRunRepository.save(run);
        return entry;
    }

    private AccountDto toDto(Account account) {
        return new AccountDto(account.getId(), account.getPublicId(), account.getCode(), account.getName(), account.getType(), account.getBalance());
    }

    private JournalEntryDto toDto(JournalEntry entry) {
        List<JournalLineDto> lines = entry.getLines().stream()
                .map(line -> new JournalLineDto(
                        line.getAccount().getId(),
                        line.getAccount().getCode(),
                        line.getDescription(),
                        line.getDebit(),
                        line.getCredit()))
                .toList();
        Dealer dealer = entry.getDealer();
        String dealerName = dealer != null ? dealer.getName() : null;
        return new JournalEntryDto(entry.getId(), entry.getPublicId(), entry.getReferenceNumber(),
                entry.getEntryDate(), entry.getMemo(), entry.getStatus(),
                dealer != null ? dealer.getId() : null, dealerName, lines);
    }

    private Dealer requireDealer(Company company, Long dealerId) {
        return dealerRepository.findByCompanyAndId(company, dealerId)
                .orElseThrow(() -> new IllegalArgumentException("Dealer not found"));
    }

    private Account requireAccount(Company company, Long accountId) {
        return accountRepository.findByCompanyAndId(company, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Value for " + field + " must be greater than zero");
        }
        return value;
    }

    private LocalDate currentDate(Company company) {
        String timezone = company.getTimezone() == null ? "UTC" : company.getTimezone();
        return LocalDate.now(ZoneId.of(timezone));
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
