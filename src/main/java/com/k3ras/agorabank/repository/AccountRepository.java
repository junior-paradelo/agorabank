package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
import com.k3ras.agorabank.model.enums.AccountCurrency;
import com.k3ras.agorabank.model.enums.AccountStatus;
import com.k3ras.agorabank.model.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Basic searches
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    // Operations by customer
    List<Account> findByCustomer(Customer customer);

    List<Account> findByCustomerId(Long customerId);

    List<Account> findByCustomerIdAndStatus(Long customerId, AccountStatus status);

    List<Account> findByCustomerIdAndType(Long customerId, AccountType type);

    List<Account> findByCustomerIdAndCurrency(Long customerId, AccountCurrency currency);
}
