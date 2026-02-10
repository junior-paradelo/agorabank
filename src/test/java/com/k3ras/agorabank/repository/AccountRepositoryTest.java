package com.k3ras.agorabank.repository;


import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Customer persistCustomer(String email, String documentNumber) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setDocumentNumber(documentNumber);
        return entityManager.persistAndFlush(customer);
    }

    private void persistAccount(Customer customer,
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
        entityManager.persistAndFlush(account);
    }

    @Test
    void findByAccountNumber_returnsAccount_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        Optional<Account> found = accountRepository.findByAccountNumber("ES00-123");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getAccountNumber()).isEqualTo("ES00-123");
        assertThat(found.get().getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    void findByAccountNumber_returnsAccount_whenDoesNotExist() {
        // when
        Optional<Account> found = accountRepository.findByAccountNumber("NOPE");

        // then
        assertThat(found).isNotPresent();
    }

    @Test
    void existsByAccountNumber_returnsTrueFalse() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when-then
        assertThat(accountRepository.existsByAccountNumber("ES00-123")).isTrue();
        assertThat(accountRepository.existsByAccountNumber("ES00-000")).isFalse();
    }

    @Test
    void findByCustomer_returnsOnlyCustomerAccounts() {
        // given
        Customer c1 = persistCustomer("test@example.com", "12345678");
        Customer c2 = persistCustomer("test2@example.com", "87654321");

        persistAccount(c1, "ES00-123", AccountStatus.ACTIVE, AccountType.CHECKING, AccountCurrency.EUR);
        persistAccount(c1, "ES00-456", AccountStatus.BLOCKED, AccountType.SAVINGS, AccountCurrency.EUR);
        persistAccount(c2, "ES00-789", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        List<Account> accounts = accountRepository.findByCustomer(c1);

        // then
        assertThat(accounts).hasSize(2);
        assertThat(accounts).allMatch(a -> a.getCustomer().getId().equals(c1.getId()));
    }

    @Test
    void findByCustomerId_returnsAccounts_forThatId() {
        // given
        Customer c1 = persistCustomer("test@example.com", "12345678");
        Customer c2 = persistCustomer("test2@example.com", "87654321");

        persistAccount(c1, "ES00-123", AccountStatus.ACTIVE, AccountType.CHECKING, AccountCurrency.EUR);
        persistAccount(c2, "ES00-789", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        List<Account> accounts = accountRepository.findByCustomerId(c1.getId());

        // then
        assertThat(accounts).hasSize(1);
        assertThat(accounts.getFirst().getAccountNumber()).isEqualTo("ES00-123");
    }

    @Test
    void findByCustomerIdAndStatus_filtersByStatus() {
        // given
        Customer c1 = persistCustomer("test@example.com", "12345678");
        persistAccount(c1, "ES00-123", AccountStatus.ACTIVE, AccountType.CHECKING, AccountCurrency.EUR);
        persistAccount(c1, "ES00-456", AccountStatus.BLOCKED, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        List<Account> active = accountRepository.findByCustomerIdAndStatus(c1.getId(), AccountStatus.ACTIVE);

        // then
        assertThat(active).hasSize(1);
        assertThat(active.getFirst().getAccountNumber()).isEqualTo("ES00-123");
    }

    @Test
    void findByCustomerIdAndType_filtersByType() {
        // given
        Customer c1 = persistCustomer("test@example.com", "12345678");
        persistAccount(c1, "ES00-123", AccountStatus.ACTIVE, AccountType.CHECKING, AccountCurrency.EUR);
        persistAccount(c1, "ES00-456", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        List<Account> checking = accountRepository.findByCustomerIdAndType(c1.getId(), AccountType.CHECKING);

        // then
        assertThat(checking).hasSize(1);
        assertThat(checking.getFirst().getAccountNumber()).isEqualTo("ES00-123");
    }

    @Test
    void findByCustomerIdAndCurrency_filtersByCurrency() {
        // given
        Customer c1 = persistCustomer("test@example.com", "12345678");
        persistAccount(c1, "ES00-123", AccountStatus.ACTIVE, AccountType.CHECKING, AccountCurrency.USD);
        persistAccount(c1, "ES00-456", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        List<Account> euroAccounts = accountRepository.findByCustomerIdAndCurrency(c1.getId(), AccountCurrency.EUR);

        // then
        assertThat(euroAccounts).hasSize(1);
        assertThat(euroAccounts.getFirst().getAccountNumber()).isEqualTo("ES00-456");
    }

}
