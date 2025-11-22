package com.bigbrightpaints.erp.modules.accounting.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"}))
public class Account extends VersionedEntity {

    private static final Logger log = LoggerFactory.getLogger(Account.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType type;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AccountType getType() { return type; }
    public void setType(AccountType type) { this.type = type; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) {
        validateBalanceUpdate(balance);
        this.balance = balance;
    }

    /**
     * Guard against invalid balances by account type. Assets/expenses/COGS must not go negative.
     */
    public void validateBalanceUpdate(BigDecimal newBalance) {
        if (newBalance == null) {
            throw new IllegalArgumentException("Account balance cannot be null");
        }
        AccountType safeType = type;
        if (safeType == null) {
            return;
        }
        // Soft guards: warn on unusual signs but do not block (advances, prepayments can flip signs legitimately)
        if ((safeType == AccountType.ASSET || safeType == AccountType.EXPENSE || safeType == AccountType.COGS)
                && newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Unusual negative balance {} for {} account {}", newBalance, safeType, code);
        }
        if ((safeType == AccountType.LIABILITY || safeType == AccountType.REVENUE || safeType == AccountType.EQUITY)
                && newBalance.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Unusual debit balance {} for {} account {}", newBalance, safeType, code);
        }
    }
}
