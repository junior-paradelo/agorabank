package com.k3ras.agorabank.service.impl;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;
import com.k3ras.agorabank.repository.AccountRepository;
import com.k3ras.agorabank.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account create(Account account) {
        if (accountRepository.existsByAccountNumber(account.getAccountNumber())) {
            throw new IllegalArgumentException("Account with this number already exists");
        }

        if (account.getStatus() == null) {
            account.setStatus(AccountStatus.ACTIVE);
        }

        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getByCustomer(Customer customer) {
        return accountRepository.findByCustomer(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getByCustomerIdAndStatus(Long customerId, AccountStatus status) {
        return accountRepository.findByCustomerIdAndStatus(customerId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getByCustomerIdAndType(Long customerId, AccountType type) {
        return accountRepository.findByCustomerIdAndType(customerId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getByCustomerIdAndCurrency(Long customerId, AccountCurrency currency) {
        return accountRepository.findByCustomerIdAndCurrency(customerId, currency);
    }

    @Override
    public Account changeStatus(Long accountId, AccountStatus newStatus) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + accountId));
        account.setStatus(newStatus);
        return account;
    }

    @Override
    public void closeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found with id: " + accountId));
        account.setStatus(AccountStatus.CLOSED);
    }
}
