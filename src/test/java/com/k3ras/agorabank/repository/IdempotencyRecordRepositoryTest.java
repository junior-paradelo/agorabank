package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.IdempotencyRecord;
import com.k3ras.agorabank.model.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class IdempotencyRecordRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IdempotencyRecordRepository repository;
    @Autowired
    private IdempotencyRecordRepository idempotencyRecordRepository;

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

    private IdempotencyRecord persistTransaction(Account account) {
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord();
        idempotencyRecord.setAccount(account);
        idempotencyRecord.setScope(IdempotencyRecordScope.TRANSFER);
        idempotencyRecord.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        idempotencyRecord.setIdempotencyKey(UUID.randomUUID().toString());
        idempotencyRecord.setRequestHash(UUID.randomUUID().toString());
        idempotencyRecord.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        return entityManager.persistAndFlush(idempotencyRecord);
    }

    @Test
    void findByAccountIdAndScopeAndKey_returnsIdempotencyRecord_whenExists() {
        // given
        Customer customer = persistCustomer("test@example.com", "12345678");
        Account account = persistAccount(customer, "ES00-123", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        IdempotencyRecord idempotencyRecord = persistTransaction(account);

        // when
        Optional<IdempotencyRecord> found = idempotencyRecordRepository.findByAccountIdAndScopeAndIdempotencyKey(account.getId(), IdempotencyRecordScope.TRANSFER, idempotencyRecord.getIdempotencyKey());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(idempotencyRecord.getId());

    }
}
