package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.Transaction;
import com.k3ras.agorabank.model.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

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

    private void persistTransaction(Account account) {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCurrency(TransactionCurrency.EUR);
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setCorrelationId("CORR-123");
        transaction.setIdempotencyKey("IDEMP-123");
        transaction.setReference("ADD MONEY");
        transaction.setBalanceAfter(new BigDecimal("1500.00"));
        entityManager.persistAndFlush(transaction);
    }

    @Test
    void findByIdempotencyKey_returnsTransaction_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        persistTransaction(account);

        // when
        Optional<Transaction> found = transactionRepository.findByIdempotencyKey("IDEMP-123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo("IDEMP-123");
    }
}
