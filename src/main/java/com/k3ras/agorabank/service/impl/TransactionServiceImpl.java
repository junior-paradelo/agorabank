package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.exception.AccountNotActiveException;
import com.k3ras.agorabank.exception.IllegalTransactionStateException;
import com.k3ras.agorabank.exception.InsufficientFundsException;
import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.Transaction;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.IdempotencyRecordScope;
import com.k3ras.agorabank.model.enums.TransactionCurrency;
import com.k3ras.agorabank.model.enums.TransactionStatus;
import com.k3ras.agorabank.model.enums.TransactionType;
import com.k3ras.agorabank.repository.AccountRepository;
import com.k3ras.agorabank.repository.TransactionRepository;
import com.k3ras.agorabank.service.IdempotencyService;
import com.k3ras.agorabank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;

    @Autowired
    public TransactionServiceImpl(TransactionRepository transactionRepository,
                                  AccountRepository accountRepository,
                                  IdempotencyService idempotencyService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;
    }

    @Override
    public Transaction deposit(UUID accountId, BigDecimal amount, TransactionCurrency currency,
                               String idempotencyKey) {
        return executeSingleAccount(accountId, amount, currency, idempotencyKey, TransactionType.DEPOSIT,
                IdempotencyRecordScope.DEPOSIT, BalanceDirection.INCREASE);
    }

    @Override
    public Transaction withdraw(UUID accountId, BigDecimal amount, TransactionCurrency currency,
                                String idempotencyKey) {
        return executeSingleAccount(accountId, amount, currency, idempotencyKey, TransactionType.WITHDRAW,
                IdempotencyRecordScope.WITHDRAW, BalanceDirection.DECREASE);
    }

    @Override
    public Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount, TransactionCurrency currency,
                                String idempotencyKey) {
        validateAmount(amount);
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalTransactionStateException("Cannot transfer to the same account");
        }

        Optional<Transaction> replay = findReplay(fromAccountId, IdempotencyRecordScope.TRANSFER, idempotencyKey,
                currency, amount);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account from = getActiveAccount(fromAccountId);
        Account to = getActiveAccount(toAccountId);
        validateCurrency(from, currency);
        validateCurrency(to, currency);
        ensureSufficientFunds(from, amount);

        BigDecimal fromNewBalance = from.getAvailableBalance().subtract(amount);
        BigDecimal toNewBalance = to.getAvailableBalance().add(amount);
        from.setAvailableBalance(fromNewBalance);
        to.setAvailableBalance(toNewBalance);

        String correlationId = UUID.randomUUID().toString();
        IdempotencyRecord record = startRecord(fromAccountId, from, IdempotencyRecordScope.TRANSFER, idempotencyKey,
                currency, amount);

        Transaction transferOut = new Transaction();
        transferOut.setAccount(from);
        transferOut.setCounterpartyAccount(to);
        transferOut.setType(TransactionType.TRANSFER_OUT);
        transferOut.setAmount(amount);
        transferOut.setCurrency(currency);
        transferOut.setStatus(TransactionStatus.POSTED);
        transferOut.setCorrelationId(correlationId);
        transferOut.setIdempotencyKey(idempotencyKey);
        transferOut.setBalanceAfter(fromNewBalance);
        transactionRepository.save(transferOut);

        Transaction transferIn = new Transaction();
        transferIn.setAccount(to);
        transferIn.setCounterpartyAccount(from);
        transferIn.setType(TransactionType.TRANSFER_IN);
        transferIn.setAmount(amount);
        transferIn.setCurrency(currency);
        transferIn.setStatus(TransactionStatus.POSTED);
        transferIn.setCorrelationId(correlationId);
        transferIn.setBalanceAfter(toNewBalance);
        transactionRepository.save(transferIn);

        completeRecord(record);
        return transferOut;
    }

    @Override
    @Transactional(readOnly = true)
    public Transaction getById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findByCorrelationId(String correlationId) {
        return transactionRepository.findByCorrelationId(correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByCorrelationId(String correlationId) {
        return transactionRepository.findAllByCorrelationId(correlationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByAccountId(UUID accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getByAccountId(UUID accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByAccountIdAndType(UUID accountId, TransactionType type) {
        return transactionRepository.findByAccountIdAndType(accountId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByAccountIdAndStatus(UUID accountId, TransactionStatus status) {
        return transactionRepository.findByAccountIdAndStatus(accountId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getByAccountIdAndStatus(UUID accountId, TransactionStatus status, Pageable pageable) {
        return transactionRepository.findByAccountIdAndStatus(accountId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByAccountIdBetweenDates(UUID accountId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByAccountIdAndCreatedAtBetween(accountId, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Transaction> getByAccountIdBetweenDates(UUID accountId, LocalDateTime from, LocalDateTime to,
                                                        Pageable pageable) {
        return transactionRepository.findByAccountIdAndCreatedAtBetween(accountId, from, to, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getByAccountIdAndCounterpartyAccountId(UUID accountId, UUID counterpartyAccountId) {
        return transactionRepository.findByAccountIdAndCounterpartyAccountId(accountId, counterpartyAccountId);
    }

    private Transaction executeSingleAccount(UUID accountId, BigDecimal amount, TransactionCurrency currency,
                                             String idempotencyKey, TransactionType type,
                                             IdempotencyRecordScope scope, BalanceDirection direction) {
        validateAmount(amount);

        Optional<Transaction> replay = findReplay(accountId, scope, idempotencyKey, currency, amount);
        if (replay.isPresent()) {
            return replay.get();
        }

        Account account = getActiveAccount(accountId);
        validateCurrency(account, currency);

        BigDecimal newBalance;
        if (direction == BalanceDirection.INCREASE) {
            newBalance = account.getAvailableBalance().add(amount);
        } else {
            ensureSufficientFunds(account, amount);
            newBalance = account.getAvailableBalance().subtract(amount);
        }
        account.setAvailableBalance(newBalance);

        IdempotencyRecord record = startRecord(accountId, account, scope, idempotencyKey, currency, amount);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setCorrelationId(UUID.randomUUID().toString());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setBalanceAfter(newBalance);
        transactionRepository.save(transaction);

        completeRecord(record);
        return transaction;
    }

    private Optional<Transaction> findReplay(UUID accountId, IdempotencyRecordScope scope, String idempotencyKey,
                                             TransactionCurrency currency, BigDecimal amount) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        String requestHash = buildRequestHash(accountId, scope, currency, amount);
        Optional<IdempotencyRecord> existing = idempotencyService.findExisting(accountId, scope, idempotencyKey);
        if (existing.isPresent()) {
            idempotencyService.verifyRequestHash(existing.get(), requestHash);
            return Optional.of(transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new IllegalTransactionStateException(
                            "Idempotency record exists without an associated transaction")));
        }
        return Optional.empty();
    }

    private IdempotencyRecord startRecord(UUID accountId, Account account, IdempotencyRecordScope scope,
                                          String idempotencyKey, TransactionCurrency currency, BigDecimal amount) {
        if (idempotencyKey == null) {
            return null;
        }
        return idempotencyService.startRecord(account, scope, idempotencyKey,
                buildRequestHash(accountId, scope, currency, amount));
    }

    private void completeRecord(IdempotencyRecord record) {
        if (record != null) {
            idempotencyService.completeRecord(record, 200, null);
        }
    }

    private String buildRequestHash(UUID accountId, IdempotencyRecordScope scope, TransactionCurrency currency,
                                    BigDecimal amount) {
        return idempotencyService.hashRequest(accountId + "|" + scope + "|" + amount.toPlainString() + "|" + currency);
    }

    private Account getActiveAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account is not active: " + accountId);
        }
        return account;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalTransactionStateException("Amount must be greater than zero");
        }
    }

    private void validateCurrency(Account account, TransactionCurrency currency) {
        if (!account.getCurrency().name().equals(currency.name())) {
            throw new IllegalTransactionStateException("Transaction currency does not match the account currency");
        }
    }

    private void ensureSufficientFunds(Account account, BigDecimal amount) {
        BigDecimal balance = account.getAvailableBalance() == null ? BigDecimal.ZERO : account.getAvailableBalance();
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + account.getId());
        }
    }

    private enum BalanceDirection {
        INCREASE,
        DECREASE
    }
}
