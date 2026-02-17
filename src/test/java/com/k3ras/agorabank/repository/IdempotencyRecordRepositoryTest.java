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

    @Test
    void findByIdempotencyKey_returnsRecord_whenExists() {
        // given
        Customer customer = persistCustomer("test2@example.com", "87654321");
        Account account = persistAccount(customer, "ES00-456", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        IdempotencyRecord record = persistTransaction(account);

        // when
        Optional<IdempotencyRecord> found =
                idempotencyRecordRepository.findByIdempotencyKey(record.getIdempotencyKey());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(record.getId());
        assertThat(found.get().getIdempotencyKey()).isEqualTo(record.getIdempotencyKey());
    }

    @Test
    void findByIdempotencyKey_returnsEmpty_whenNotExists() {
        // when
        Optional<IdempotencyRecord> found = idempotencyRecordRepository.findByIdempotencyKey("NON_EXISTENT_KEY");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    void findByAccountId_returnsRecords_whenExists() {
        // given
        Customer customer1 = persistCustomer("acc1@example.com", "11111111");
        Customer customer2 = persistCustomer("acc2@example.com", "22222222");

        Account account1 = persistAccount(customer1, "ES00-AAA", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        Account account2 = persistAccount(customer2, "ES00-BBB", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        IdempotencyRecord record1 = persistTransaction(account1);
        IdempotencyRecord record2 = persistTransaction(account1);
        persistTransaction(account2);

        // when
        var results = idempotencyRecordRepository.findByAccountId(account1.getId());

        // then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(IdempotencyRecord::getAccount)
                .allMatch(account -> account.getId().equals(account1.getId()));
        assertThat(results)
                .extracting(IdempotencyRecord::getId)
                .containsExactlyInAnyOrder(record1.getId(), record2.getId());
    }

    @Test
    void findByAccountId_returnsEmpty_whenNoRecords() {
        // given
        Customer customer = persistCustomer("acc3@example.com", "33333333");
        Account account = persistAccount(customer, "ES00-CCC", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        // when
        var results = idempotencyRecordRepository.findByAccountId(account.getId());

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void findByAccountIdAndScope_returnsRecords_whenExists() {
        // given
        Customer customer = persistCustomer("scope@example.com", "44444444");
        Account account = persistAccount(customer, "ES00-SCP", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        IdempotencyRecord transferRecord = persistTransaction(account); // scope = TRANSFER

        IdempotencyRecord paymentRecord = new IdempotencyRecord();
        paymentRecord.setAccount(account);
        paymentRecord.setScope(IdempotencyRecordScope.DEPOSIT);
        paymentRecord.setStatus(IdempotencyRecordStatus.IN_PROGRESS);
        paymentRecord.setIdempotencyKey(UUID.randomUUID().toString());
        paymentRecord.setRequestHash(UUID.randomUUID().toString());
        paymentRecord.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        entityManager.persistAndFlush(paymentRecord);

        // when
        var results = idempotencyRecordRepository.findByAccountIdAndScope(account.getId(), IdempotencyRecordScope.TRANSFER);

        // then
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getId()).isEqualTo(transferRecord.getId());
    }

    @Test
    void findByAccountIdAndScope_returnsEmpty_whenNoMatchingScope() {
        // given
        Customer customer = persistCustomer("scope2@example.com", "55555555");
        Account account = persistAccount(customer, "ES00-SCP2", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        persistTransaction(account); // scope = TRANSFER

        // when
        var results = idempotencyRecordRepository.findByAccountIdAndScope(account.getId(), IdempotencyRecordScope.DEPOSIT);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void findByStatus_returnsRecords_whenExists() {
        // given
        Customer customer1 = persistCustomer("status1@example.com", "66666666");
        Customer customer2 = persistCustomer("status2@example.com", "77777777");

        Account account1 = persistAccount(customer1, "ES00-STS1", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        Account account2 = persistAccount(customer2, "ES00-STS2", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        IdempotencyRecord inProgress1 = persistTransaction(account1); // status = IN_PROGRESS (helper)
        IdempotencyRecord inProgress2 = persistTransaction(account2); // status = IN_PROGRESS (helper)

        IdempotencyRecord completed = new IdempotencyRecord();
        completed.setAccount(account1);
        completed.setScope(IdempotencyRecordScope.TRANSFER);
        completed.setStatus(IdempotencyRecordStatus.COMPLETED);
        completed.setIdempotencyKey(UUID.randomUUID().toString());
        completed.setRequestHash(UUID.randomUUID().toString());
        completed.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        entityManager.persistAndFlush(completed);

        // when
        var results = idempotencyRecordRepository.findByStatus(IdempotencyRecordStatus.IN_PROGRESS);

        // then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(IdempotencyRecord::getId)
                .containsExactlyInAnyOrder(inProgress1.getId(), inProgress2.getId());
    }

    @Test
    void findByStatus_returnsEmpty_whenNoRecordsWithStatus() {
        // given
        Customer customer = persistCustomer("status3@example.com", "88888888");
        Account account = persistAccount(customer, "ES00-STS3", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        persistTransaction(account); // status = IN_PROGRESS

        // when
        var results = idempotencyRecordRepository.findByStatus(IdempotencyRecordStatus.COMPLETED);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    void findByAccountIdAndStatus_returnsRecords_whenExists() {
        // given
        Customer customer1 = persistCustomer("accstatus1@example.com", "99990001");
        Customer customer2 = persistCustomer("accstatus2@example.com", "99990002");

        Account account1 = persistAccount(customer1, "ES00-AS1", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);
        Account account2 = persistAccount(customer2, "ES00-AS2", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        IdempotencyRecord inProgress1 = persistTransaction(account1); // status = IN_PROGRESS (helper)
        IdempotencyRecord inProgress2 = persistTransaction(account1); // status = IN_PROGRESS (helper)

        IdempotencyRecord completed = new IdempotencyRecord();
        completed.setAccount(account1);
        completed.setScope(IdempotencyRecordScope.TRANSFER);
        completed.setStatus(IdempotencyRecordStatus.COMPLETED);
        completed.setIdempotencyKey(UUID.randomUUID().toString());
        completed.setRequestHash(UUID.randomUUID().toString());
        completed.setExpiresAt(LocalDateTime.now().plusMinutes(1));
        entityManager.persistAndFlush(completed);

        persistTransaction(account2); // IN_PROGRESS another account

        // when
        var results = idempotencyRecordRepository.findByAccountIdAndStatus(account1.getId(), IdempotencyRecordStatus.IN_PROGRESS);

        // then
        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(IdempotencyRecord::getId)
                .containsExactlyInAnyOrder(inProgress1.getId(), inProgress2.getId());
    }

    @Test
    void findByAccountIdAndStatus_returnsEmpty_whenNoMatchingStatus() {
        // given
        Customer customer = persistCustomer("accstatus3@example.com", "99990003");
        Account account = persistAccount(customer, "ES00-AS3", AccountStatus.ACTIVE, AccountType.SAVINGS, AccountCurrency.EUR);

        persistTransaction(account); // status = IN_PROGRESS

        // when
        var results = idempotencyRecordRepository.findByAccountIdAndStatus(account.getId(), IdempotencyRecordStatus.COMPLETED);

        // then
        assertThat(results).isEmpty();
    }
}
