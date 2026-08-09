package com.k3ras.agorabank.service;

import com.k3ras.agorabank.exception.DuplicatedResourceException;
import com.k3ras.agorabank.exception.ResourceNotFoundException;
import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;
import com.k3ras.agorabank.repository.AccountRepository;
import com.k3ras.agorabank.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository);
    }

    private Account account(String accountNumber) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setType(AccountType.CHECKING);
        account.setCurrency(AccountCurrency.EUR);
        return account;
    }

    @Test
    void create_savesAccount_whenNumberUnique() {
        // given
        Account account = account("ES001");
        when(accountRepository.existsByAccountNumber("ES001")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Account saved = accountService.create(account);

        // then
        assertThat(saved).isEqualTo(account);
        verify(accountRepository).save(account);
    }

    @Test
    void create_setsStatusActive_whenNull() {
        // given
        Account account = account("ES001");
        when(accountRepository.existsByAccountNumber("ES001")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        accountService.create(account);

        // then
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void create_keepsProvidedStatus() {
        // given
        Account account = account("ES001");
        account.setStatus(AccountStatus.BLOCKED);
        when(accountRepository.existsByAccountNumber("ES001")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        accountService.create(account);

        // then
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void create_throwsDuplicatedResource_whenNumberExists() {
        // given
        Account account = account("ES001");
        when(accountRepository.existsByAccountNumber("ES001")).thenReturn(true);

        // when-then
        assertThatThrownBy(() -> accountService.create(account))
                .isInstanceOf(DuplicatedResourceException.class);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void getById_returnsAccount_whenExists() {
        // given
        UUID id = UUID.randomUUID();
        Account account = account("ES001");
        account.setId(id);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        // when
        Account found = accountService.getById(id);

        // then
        assertThat(found).isEqualTo(account);
    }

    @Test
    void getById_throwsResourceNotFound_whenMissing() {
        // given
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> accountService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void findByAccountNumber_returnsAccount_whenExists() {
        // given
        Account account = account("ES001");
        when(accountRepository.findByAccountNumber("ES001")).thenReturn(Optional.of(account));

        // when
        Optional<Account> found = accountService.findByAccountNumber("ES001");

        // then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(account);
    }

    @Test
    void existsByAccountNumber_returnsTrue() {
        // given
        when(accountRepository.existsByAccountNumber("ES001")).thenReturn(true);

        // when-then
        assertThat(accountService.existsByAccountNumber("ES001")).isTrue();
    }

    @Test
    void getByCustomerId_delegates() {
        // given
        Customer customer = new Customer();
        Account account1 = account("ES001");
        Account account2 = account("ES002");
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(account1, account2));

        // when
        List<Account> accounts = accountService.getByCustomerId(1L);

        // then
        assertThat(accounts).containsExactlyInAnyOrder(account1, account2);
    }

    @Test
    void getByCustomerIdAndStatus_delegates() {
        // given
        Account account = account("ES001");
        when(accountRepository.findByCustomerIdAndStatus(1L, AccountStatus.ACTIVE))
                .thenReturn(List.of(account));

        // when
        List<Account> accounts = accountService.getByCustomerIdAndStatus(1L, AccountStatus.ACTIVE);

        // then
        assertThat(accounts).containsExactly(account);
    }

    @Test
    void getByCustomerIdAndType_delegates() {
        // given
        Account account = account("ES001");
        when(accountRepository.findByCustomerIdAndType(1L, AccountType.SAVINGS))
                .thenReturn(List.of(account));

        // when
        List<Account> accounts = accountService.getByCustomerIdAndType(1L, AccountType.SAVINGS);

        // then
        assertThat(accounts).containsExactly(account);
    }

    @Test
    void getByCustomerIdAndCurrency_delegates() {
        // given
        Account account = account("ES001");
        when(accountRepository.findByCustomerIdAndCurrency(1L, AccountCurrency.EUR))
                .thenReturn(List.of(account));

        // when
        List<Account> accounts = accountService.getByCustomerIdAndCurrency(1L, AccountCurrency.EUR);

        // then
        assertThat(accounts).containsExactly(account);
    }

    @Test
    void changeStatus_setsNewStatus() {
        // given
        UUID id = UUID.randomUUID();
        Account account = account("ES001");
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        // when
        Account result = accountService.changeStatus(id, AccountStatus.BLOCKED);

        // then
        assertThat(result.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    @Test
    void changeStatus_throwsResourceNotFound_whenMissing() {
        // given
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> accountService.changeStatus(id, AccountStatus.BLOCKED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closeAccount_setsClosed() {
        // given
        UUID id = UUID.randomUUID();
        Account account = account("ES001");
        account.setStatus(AccountStatus.ACTIVE);
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        // when
        accountService.closeAccount(id);

        // then
        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeAccount_throwsResourceNotFound_whenMissing() {
        // given
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        // when-then
        assertThatThrownBy(() -> accountService.closeAccount(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
