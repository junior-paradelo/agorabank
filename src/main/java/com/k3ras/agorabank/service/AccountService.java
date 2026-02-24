package com.k3ras.agorabank.service;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;

import java.util.List;
import java.util.Optional;

public interface AccountService {

    // Create
    Account create(Account account);

    // Read
    Account getById(Long id);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> getByCustomer(Customer customer);

    List<Account> getByCustomerId(Long customerId);

    List<Account> getByCustomerIdAndStatus(Long customerId, AccountStatus status);

    List<Account> getByCustomerIdAndType(Long customerId, AccountType type);

    List<Account> getByCustomerIdAndCurrency(Long customerId, AccountCurrency currency);

    // State management
    Account changeStatus(Long accountId, AccountStatus newStatus);

    // Logical close
    void closeAccount(Long accountId);
}