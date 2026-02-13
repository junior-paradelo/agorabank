package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.Transaction;
import com.k3ras.agorabank.model.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private Customer persistCustomer(String email, String documentNumber) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setDocumentType(CustomerDocumentType.DNI);
        customer.setDocumentNumber(documentNumber);
        customer.setStatus(CustomerStatus.ENABLED);
        return entityManager.persistAndFlush(customer);
    }

    private Account persistAccount(Customer customer,
                                   String accountNumber,
                                   AccountStatus status,
                                   AccountType type,
                                   AccountCurrency currency) {
        Account account = new Account();
        account.setCustomer(customer);
        account.setAccountNumber(accountNumber);
        account.setStatus(status);
        account.setType(type);
        account.setCurrency(currency);
        return entityManager.persistAndFlush(account);
    }

    private void persistTransactionWithoutReturn(Account account, String correlationId) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency(TransactionCurrency.EUR);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setCorrelationId(correlationId);
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setReference("ADD MONEY");
        transaction.setBalanceAfter(new BigDecimal("1500.00"));
        entityManager.persistAndFlush(transaction);
    }

    private Transaction persistTransactionWithReturn(Account account, String correlationId) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency(TransactionCurrency.EUR);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setCorrelationId(correlationId);
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setReference("ADD MONEY");
        transaction.setBalanceAfter(new BigDecimal("1500.00"));
        return entityManager.persistAndFlush(transaction);
    }

    private void persistTransactionWithoutReturn(Account account, Account counterPartyAccount, String correlationId) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency(TransactionCurrency.EUR);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setCorrelationId(correlationId);
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        transaction.setReference("ADD MONEY");
        transaction.setBalanceAfter(new BigDecimal("1500.00"));
        transaction.setCounterpartyAccount(counterPartyAccount);
        entityManager.persistAndFlush(transaction);
    }

    @Test
    void findByIdempotencyKey_returnsTransaction_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        Transaction transaction = persistTransactionWithReturn(account, "CORR-123");

        // when
        Optional<Transaction> found = transactionRepository.findByIdempotencyKey(transaction.getIdempotencyKey());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo(transaction.getIdempotencyKey());
    }

    @Test
    void findByCorrelationId_returnsTransaction_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-123");

        // when
        Optional<Transaction> found = transactionRepository.findByCorrelationId("CORR-123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getCorrelationId()).isEqualTo("CORR-123");
    }

    @Test
    void findAllByCorrelationId_returnsTransaction_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");

        // when
        List<Transaction> found = transactionRepository.findAllByCorrelationId("CORR-X");

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(2);
    }

    @Test
    void findByAccount_returnsTransactions() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");

        // when
        List<Transaction> found = transactionRepository.findByAccount(account);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(3);
    }

    @Test
    void findByAccountId_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");

        // when
        List<Transaction> found = transactionRepository.findByAccountId(account.getId());
        List<Transaction> notFound = transactionRepository.findByAccountId(UUID.randomUUID());

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(3);
        assertThat(notFound.size()).isEqualTo(0);
    }

    @Test
    void findByAccountId_returnsPageableTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");

        // when
        Pageable pageable = PageRequest.of(1, 2);
        Page<Transaction> found = transactionRepository.findByAccountId(account.getId(), pageable);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.getContent().size()).isEqualTo(1);
    }

    @Test
    void findByAccountIdAndType_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Z");

        // when
        List<Transaction> found = transactionRepository.findByAccountIdAndType(account.getId(), TransactionType.DEPOSIT);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(4);
    }

    @Test
    void findByAccountIdAndStatus_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Z");

        // when
        List<Transaction> found = transactionRepository.findByAccountIdAndStatus(account.getId(), TransactionStatus.POSTED);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(4);
    }

    @Test
    void findByAccountIdAndStatusPageable_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Z");

        // when
        Pageable pageable = PageRequest.of(0, 3);
        Page<Transaction> found = transactionRepository.findByAccountIdAndStatus(account.getId(), TransactionStatus.POSTED, pageable);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.getContent().size()).isEqualTo(3);
    }

    @Test
    void findByAccountIdAndCreatedAtBetween_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        LocalDateTime startedTime = LocalDateTime.now();
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Z");

        // when
        List<Transaction> found = transactionRepository.findByAccountIdAndCreatedAtBetween(account.getId(), startedTime, LocalDateTime.now().plusDays(1));

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(4);
    }

    @Test
    void findByAccountIdAndCreatedAtBetweenPageable_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        LocalDateTime startedTime = LocalDateTime.now();
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Y");
        persistTransactionWithoutReturn(account, "CORR-X");
        persistTransactionWithoutReturn(account, "CORR-Z");

        // when
        Pageable pageable = PageRequest.of(0, 3);
        Page<Transaction> found = transactionRepository.findByAccountIdAndCreatedAtBetween(account.getId(), startedTime, LocalDateTime.now().plusDays(1), pageable);

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.getContent().size()).isEqualTo(3);
    }

    @Test
    void findByAccountIdAndCounterpartyAccountId_returnsTransactions_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Customer customer2 = persistCustomer("test2@example.com", "87654321");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        Account counterPartyAccount = persistAccount(customer2, "ES00-456", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransactionWithoutReturn(account, counterPartyAccount, "CORR-X");
        persistTransactionWithoutReturn(account, counterPartyAccount, "CORR-Y");
        persistTransactionWithoutReturn(account, counterPartyAccount, "CORR-X");
        persistTransactionWithoutReturn(account, counterPartyAccount, "CORR-Z");

        // when
        List<Transaction> found = transactionRepository.findByAccountIdAndCounterpartyAccountId(account.getId(), counterPartyAccount.getId());

        // then
        assertThat(found).isNotEmpty();
        assertThat(found.size()).isEqualTo(4);
    }
}
