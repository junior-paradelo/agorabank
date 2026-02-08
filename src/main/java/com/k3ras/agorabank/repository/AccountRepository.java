package com.k3ras.agorabank.repository;

import com.k3ras.agorabank.model.Account;
import com.k3ras.agorabank.model.Customer;
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
    List<Account> findByCustomerIdAndStatus(Long customerId, String status);
    List<Account> findByCustomerIdAndType(Long customerId, String type);
    List<Account> findByCustomerIdAndCurrency(Long customerId, String currency);
}
