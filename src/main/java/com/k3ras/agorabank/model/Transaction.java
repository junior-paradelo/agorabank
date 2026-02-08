package com.k3ras.agorabank.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_account", columnList = "account_id"),
    @Index(name = "idx_transaction_counterparty", columnList = "counterparty_account_id"),
    @Index(name = "idx_transaction_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_transaction_idempotency_key", columnList = "idempotency_key")
})
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_account"))
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_account_id", foreignKey = @ForeignKey(name = "fk_transaction_counterparty"))
    private Account counterpartyAccount;
    
    @Column(length = 20, nullable = false)
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(length = 3, nullable = false)
    private String currency;
    
    @Column(length = 20, nullable = false)
    private String status; // POSTED, FAILED
    
    @Column(length = 100)
    private String reference;
    
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    
    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;
    
    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    
    public Transaction() {
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    public Account getCounterpartyAccount() {
        return counterpartyAccount;
    }
    
    public void setCounterpartyAccount(Account counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
    
    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
