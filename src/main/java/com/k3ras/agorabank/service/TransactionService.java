package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.Transaction;
import com.k3ras.agorabank.model.enums.TransactionCurrency;
import com.k3ras.agorabank.model.enums.TransactionStatus;
import com.k3ras.agorabank.model.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionService {

    Transaction deposit(UUID accountId, BigDecimal amount, TransactionCurrency currency, String idempotencyKey);

    Transaction withdraw(UUID accountId, BigDecimal amount, TransactionCurrency currency, String idempotencyKey);

    Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, TransactionCurrency currency,
                         String idempotencyKey);

    Transaction getById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByCorrelationId(String correlationId);

    List<Transaction> getByCorrelationId(String correlationId);

    List<Transaction> getByAccountId(UUID accountId);

    Page<Transaction> getByAccountId(UUID accountId, Pageable pageable);

    List<Transaction> getByAccountIdAndType(UUID accountId, TransactionType type);

    List<Transaction> getByAccountIdAndStatus(UUID accountId, TransactionStatus status);

    Page<Transaction> getByAccountIdAndStatus(UUID accountId, TransactionStatus status, Pageable pageable);

    List<Transaction> getByAccountIdBetweenDates(UUID accountId, LocalDateTime from, LocalDateTime to);

    Page<Transaction> getByAccountIdBetweenDates(UUID accountId, LocalDateTime from, LocalDateTime to,
                                                 Pageable pageable);

    List<Transaction> getByAccountIdAndCounterpartyAccountId(UUID accountId, UUID counterpartyAccountId);
}
