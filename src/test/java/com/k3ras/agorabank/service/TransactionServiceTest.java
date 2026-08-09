package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.AccountNotActiveException;
import com.k3ras.agorabank.exception.DuplicateIdempotentRequestException;
import com.k3ras.agorabank.exception.IllegalTransactionStateException;
import com.k3ras.agorabank.exception.InsufficientFundsException;
import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.Transaction;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.IdempotencyRecordScope;
import com.k3ras.agorabank.model.enums.TransactionCurrency;
import com.k3ras.agorabank.model.enums.TransactionStatus;
import com.k3ras.agorabank.model.enums.TransactionType;
import com.k3ras.agorabank.repository.AccountRepository;
import com.k3ras.agorabank.repository.TransactionRepository;
import com.k3ras.agorabank.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IdempotencyService idempotencyService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(transactionRepository, accountRepository, idempotencyService);
    }

    private Account activeAccount(UUID id, AccountCurrency currency, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency(currency);
        account.setAvailableBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void deposit_increasesBalance_andSetsBalanceAfterAndPosted() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "500.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when
        Transaction transaction = transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, null);

        // then
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("600.00");
        assertThat(transaction.getBalanceAfter()).isEqualByComparingTo("600.00");
        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.POSTED);
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.getAccount()).isEqualTo(account);
        assertThat(transaction.getCorrelationId()).isNotBlank();
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void deposit_throwsResourceNotFound_whenAccountMissing() {
        // given
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deposit_throwsAccountNotActive_whenAccountBlocked() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "500.00");
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when-then
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void deposit_throwsIllegalTransactionState_whenCurrencyMismatch() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "500.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when-then
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.USD, null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void deposit_throwsIllegalTransactionState_whenAmountNotPositive() {
        // given
        UUID accountId = UUID.randomUUID();

        // when-then
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, BigDecimal.ZERO, TransactionCurrency.EUR, null))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, new BigDecimal("-5.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(IllegalTransactionStateException.class);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void deposit_withIdempotencyKey_startsAndCompletesRecord() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "500.00");
        String key = "key-1";
        IdempotencyRecord record = new IdempotencyRecord();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(idempotencyService.hashRequest(anyString())).thenReturn("request-hash");
        when(idempotencyService.findExisting(
                eq(accountId), eq(IdempotencyRecordScope.DEPOSIT), eq(key))).thenReturn(Optional.empty());
        when(idempotencyService.startRecord(
                eq(account), eq(IdempotencyRecordScope.DEPOSIT), eq(key), eq("request-hash")))
                .thenReturn(record);

        // when
        transactionService.deposit(accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, key);

        // then
        verify(idempotencyService).startRecord(account, IdempotencyRecordScope.DEPOSIT, key, "request-hash");
        verify(idempotencyService).completeRecord(record, 200, null);
    }

    @Test
    void deposit_replay_returnsOriginalTransaction_withoutTouchingBalance() {
        // given
        UUID accountId = UUID.randomUUID();
        String key = "key-1";
        Transaction original = new Transaction();
        original.setIdempotencyKey(key);
        original.setBalanceAfter(new BigDecimal("600.00"));
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRequestHash("request-hash");
        when(idempotencyService.hashRequest(anyString())).thenReturn("request-hash");
        when(idempotencyService.findExisting(
                eq(accountId), eq(IdempotencyRecordScope.DEPOSIT), eq(key))).thenReturn(Optional.of(existing));
        when(transactionRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(original));

        // when
        Transaction replayed = transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, key);

        // then
        assertThat(replayed).isEqualTo(original);
        verify(accountRepository, never()).findById(any());
        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(idempotencyService).verifyRequestHash(existing, "request-hash");
    }

    @Test
    void deposit_replay_throwsDuplicateIdempotentRequest_whenHashDiffers() {
        // given
        UUID accountId = UUID.randomUUID();
        String key = "key-1";
        IdempotencyRecord existing = new IdempotencyRecord();
        existing.setRequestHash("other-hash");
        when(idempotencyService.hashRequest(anyString())).thenReturn("request-hash");
        when(idempotencyService.findExisting(
                eq(accountId), eq(IdempotencyRecordScope.DEPOSIT), eq(key))).thenReturn(Optional.of(existing));
        doThrow(new DuplicateIdempotentRequestException("Idempotency key reused with a different request payload"))
                .when(idempotencyService).verifyRequestHash(any(IdempotencyRecord.class), anyString());

        // when-then
        assertThatThrownBy(() -> transactionService.deposit(
                accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, key))
                .isInstanceOf(DuplicateIdempotentRequestException.class);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void withdraw_decreasesBalance_andSetsBalanceAfter() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "500.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when
        Transaction transaction = transactionService.withdraw(
                accountId, new BigDecimal("200.00"), TransactionCurrency.EUR, null);

        // then
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("300.00");
        assertThat(transaction.getBalanceAfter()).isEqualByComparingTo("300.00");
        assertThat(transaction.getType()).isEqualTo(TransactionType.WITHDRAW);
    }

    @Test
    void withdraw_throwsInsufficientFunds_whenBalanceTooLow() {
        // given
        UUID accountId = UUID.randomUUID();
        Account account = activeAccount(accountId, AccountCurrency.EUR, "100.00");
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // when-then
        assertThatThrownBy(() -> transactionService.withdraw(
                accountId, new BigDecimal("200.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_createsOutAndIn_withSharedCorrelationId() {
        // given
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = activeAccount(fromId, AccountCurrency.EUR, "1000.00");
        Account to = activeAccount(toId, AccountCurrency.EUR, "500.00");
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        // when
        transactionService.transfer(fromId, toId, new BigDecimal("200.00"), TransactionCurrency.EUR, null);

        // then
        assertThat(from.getAvailableBalance()).isEqualByComparingTo("800.00");
        assertThat(to.getAvailableBalance()).isEqualByComparingTo("700.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        Transaction transferOut = saved.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT).findFirst().orElseThrow();
        Transaction transferIn = saved.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_IN).findFirst().orElseThrow();

        assertThat(transferOut.getCorrelationId()).isEqualTo(transferIn.getCorrelationId());
        assertThat(transferOut.getBalanceAfter()).isEqualByComparingTo("800.00");
        assertThat(transferIn.getBalanceAfter()).isEqualByComparingTo("700.00");
        assertThat(transferOut.getAccount()).isEqualTo(from);
        assertThat(transferOut.getCounterpartyAccount()).isEqualTo(to);
        assertThat(transferIn.getAccount()).isEqualTo(to);
        assertThat(transferIn.getCounterpartyAccount()).isEqualTo(from);
    }

    @Test
    void transfer_throwsIllegalTransactionState_whenSameAccount() {
        // given
        UUID accountId = UUID.randomUUID();

        // when-then
        assertThatThrownBy(() -> transactionService.transfer(
                accountId, accountId, new BigDecimal("100.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(IllegalTransactionStateException.class);
        verify(accountRepository, never()).findById(any());
    }

    @Test
    void transfer_throwsInsufficientFunds_whenFromBalanceTooLow() {
        // given
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        Account from = activeAccount(fromId, AccountCurrency.EUR, "100.00");
        Account to = activeAccount(toId, AccountCurrency.EUR, "500.00");
        when(accountRepository.findById(fromId)).thenReturn(Optional.of(from));
        when(accountRepository.findById(toId)).thenReturn(Optional.of(to));

        // when-then
        assertThatThrownBy(() -> transactionService.transfer(
                fromId, toId, new BigDecimal("200.00"), TransactionCurrency.EUR, null))
                .isInstanceOf(InsufficientFundsException.class);
        assertThat(from.getAvailableBalance()).isEqualByComparingTo("100.00");
        assertThat(to.getAvailableBalance()).isEqualByComparingTo("500.00");
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
