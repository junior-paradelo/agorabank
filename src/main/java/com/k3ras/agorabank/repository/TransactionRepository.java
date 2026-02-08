package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    // Basic searches
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    Optional<Transaction> findByCorrelationId(String correlationId);
    List<Transaction> findAllByCorrelationId(String correlationId);
    
    // Operations by account
    List<Transaction> findByAccount(Account account);
    List<Transaction> findByAccountId(UUID accountId);
    Page<Transaction> findByAccountId(UUID accountId, Pageable pageable);
    
    // Operations by account with filters
    List<Transaction> findByAccountIdAndType(UUID accountId, String type);
    List<Transaction> findByAccountIdAndStatus(UUID accountId, String status);
    Page<Transaction> findByAccountIdAndStatus(UUID accountId, String status, Pageable pageable);
    
    // Operations by date range
    List<Transaction> findByAccountIdAndCreatedAtBetween(UUID accountId, LocalDateTime from, LocalDateTime to);
    Page<Transaction> findByAccountIdAndCreatedAtBetween(UUID accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    // Operations by counterparty
    List<Transaction> findByAccountIdAndCounterpartyAccountId(UUID accountId, UUID counterpartyAccountId);
}
