package com.k3ras.agorabank.model;

import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_account_customer"))
    private Customer customer;

    @Column(name = "account_number", unique = true, nullable = false, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 3, nullable = false)
    private AccountCurrency currency;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AccountStatus status;

    @Column(name = "available_balance", precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Account() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public AccountCurrency getCurrency() {
        return currency;
    }

    public void setCurrency(AccountCurrency currency) {
        this.currency = currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
